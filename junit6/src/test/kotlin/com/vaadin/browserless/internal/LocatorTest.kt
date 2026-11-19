/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.browserless.internal

import java.util.function.Predicate
import kotlin.streams.asSequence
import kotlin.test.expect
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.browserless.expectList
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.dynatest.expectThrows
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.text
import com.github.mvysny.karibudsl.v10.textField
import com.github.mvysny.karibudsl.v10.verticalLayout

@DynaTestDsl
internal fun DynaNodeGroup.locatorTest2() {
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("_expectNoDialogs()") {
        _expectNoDialogs() // should succeed on no dialogs
        val dlg = Dialog()
        dlg.open()
        val exception = expectThrows(AssertionError::class,
            "Too many visible Dialogs (1) in MockedUI[] matching Dialog and count=0..0:") {
            _expectNoDialogs()
        }
        expect(true) {
            val msg = exception.message!!
            println(msg)
            msg.matches(""".*Too many visible Dialogs \(1\) in MockedUI\[] matching Dialog and count=0\.\.0: \[Dialog\[.*]]\. Component tree:
└── MockedUI\[]
\s+└── Dialog\[.*].*
""".toRegex(RegexOption.MULTILINE))
        }
        dlg.close()
        _expectNoDialogs()
    }

    group("InternalServerError handling") {
        test("component lookup fails on navigation error") {
            // Vaadin shows InternalServerError when an exception occurs during the navigation phase.
            // the _expect*() functions should detect this and fail fast.
            currentUI.addBeforeEnterListener { event -> event.rerouteToError(RuntimeException("Simulated"), "Simulated") }
            UI.getCurrent().navigate("")
            expectThrows<AssertionError>("An internal server error occurred; please check log for the actual stack-trace. Error text: There was an exception while trying to navigate to") {
                _expectOne<UI>()
            }
        }
        group("_expectInternalServerError") {
            test("fails on no error") {
                expectThrows<java.lang.AssertionError>(
                    """Expected an internal server error but none happened. Component tree:
└── MockedUI[]"""
                ) { _expectInternalServerError() }
            }
            test("succeeds on error") {
                currentUI.addBeforeEnterListener { event -> event.rerouteToError(RuntimeException("Simulated"), "Simulated") }
                UI.getCurrent().navigate("")
                _expectInternalServerError()
            }
            test("matches error message correctly") {
                currentUI.addBeforeEnterListener { event -> event.rerouteToError(RuntimeException("Simulated"), "Simulated") }
                UI.getCurrent().navigate("")
                _expectInternalServerError("Simulated")
            }
        }
    }
}

@DynaTestDsl
internal fun DynaNodeGroup.locatorTest() {

    beforeEach { MockVaadin.setup() }
    beforeEach { testingLifecycleHook = MyLifecycleHook() }
    afterEach { testingLifecycleHook = TestingLifecycleHook.default }
    afterEach { MockVaadin.tearDown() }

    group("_get") {
        test("fails when no component match") {
            expectThrows(AssertionError::class) {
                _get(Button(), TextField::class.java)
            }
            expectAfterLookupCalled()
        }

        test("fail when multiple component match") {
            expectThrows(AssertionError::class) {
                _get(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java)
            }
            expectAfterLookupCalled()
        }

        test("ReturnsSelf") {
            val button = Button()
            expect(button) { _get(button, Button::class.java) }
            expectAfterLookupCalled()
        }

        test("ReturnsNested") {
            val button = Button()
            expect(button) { _get(VerticalLayout(button), Button::class.java) }
            expectAfterLookupCalled()
        }

        test("fails on invisible") {
            val button = Button().apply { isVisible = false }
            expectThrows(java.lang.AssertionError::class) { _get(button, Button::class.java) }
            expectAfterLookupCalled()
        }
    }

    group("_find") {
        test("findMatchingId") {
            val button = Button().apply { setId("foo") }
            expectList(button) { _find(VerticalLayout(button, Button()), Button::class.java) { id = "foo" } }
            expectAfterLookupCalled()
        }

        test("doesn't return invisible") {
            val button = Button().apply { isVisible = false }
            expectList() { _find(button, Button::class.java) }
            expectAfterLookupCalled()
        }

        test("returns nested") {
            val vl = VerticalLayout().apply { verticalLayout {} }
            expect(2) { _find(vl, VerticalLayout::class.java).size }
            expectAfterLookupCalled()
        }
    }

    group("_expectNone") {
        test("succeeds when no component match") {
            _expectNone(Button(), TextField::class.java)
            expectAfterLookupCalled()
        }

        test("fail when multiple component match") {
            expectThrows(AssertionError::class) {
                _expectNone(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java)
                expectAfterLookupCalled()
            }
        }

        test("fails if self matches") {
            val button = Button()
            expectThrows(AssertionError::class) { _expectNone(button, Button::class.java) }
            expectAfterLookupCalled()
        }

        test("fails if nested matches") {
            val button = Button()
            expectThrows(AssertionError::class) { _expectNone(VerticalLayout(button), Button::class.java) }
            expectAfterLookupCalled()
        }
    }

    test("simpleUITest") {
        lateinit var layout: VerticalLayout
        layout = UI.getCurrent().verticalLayout {
            val name = textField("Type your name here:")
            button("Click Me") {
                addClickListener {
                    println("Thanks ${name.value}, it works!")
                    layout.text("Thanks ${name.value}, it works!")
                }
            }
        }

        _get<TextField> { caption = "Type your name here:" }.value = "Baron Vladimir Harkonnen"
        expectAfterLookupCalled()

        // TODO: uncomment after importing _click() method
        // _get<Button> { caption = "Click Me" }._click()
        _get<Button> { caption = "Click Me" }.click()
        expectAfterLookupCalled()

        expect("Thanks Baron Vladimir Harkonnen, it works!") { _get<Text>{
            // Since Vaadin 25.1, Button contains a Text child
            this.predicates.add { textComponent -> textComponent.parent.filter { (it is Button) }.isEmpty }
        }.text }
        expectAfterLookupCalled()
        _get<TextField> { value = "Baron Vladimir Harkonnen" }  // test lookup by value
        expectAfterLookupCalled()
        expect("Thanks Baron Vladimir Harkonnen, it works!") { (layout.children.asSequence().last() as Text).text }
        expect(3) { layout.componentCount }
    }

    group("matcher") {
        test("id") {
            expect(true) { matches(Button()) { } }
            expect(false) { matches(Button()) { id = "a" } }
            expect(true) { matches(Button().apply { setId("a") }) { } }
            expect(true) { matches(Button().apply { setId("a") }) { id = "a" } }
            expect(false) { matches(Button().apply { setId("a b") }) { id = "a" } }
            expect(false) { matches(Button().apply { setId("a") }) { id = "a b" } }
        }
        test("caption") {
            expect(true) { matches(Button("click me")) { caption = "click me" } }
            expect(true) { matches(TextField("name:")) { caption = "name:" } }
            expect(true) { matches(Button("click me")) { } }
            expect(true) { matches(TextField("name:")) { } }
            expect(false) { matches(Button("click me")) { caption = "Click Me" } }
            expect(false) { matches(TextField("name:")) { caption = "Name"} }
        }
        test("placeholder") {
            expect(true) { matches(TextField("name", "the name")) { placeholder = "the name" } }
            expect(true) { matches(PasswordField("password", "at least 6 characters")) { placeholder = "at least 6 characters" } }
            expect(true) { matches(ComboBox<String>().apply { placeholder = "foo" }) { placeholder = "foo" } }
            expect(false) { matches(TextField("name", "the name")) { placeholder = "name" } }
            expect(false) { matches(PasswordField("password", "at least 6 characters")) { placeholder = "password" } }
        }
        test("value") {
            expect(true) { matches(TextField(null, "Mannerheim", "placeholder")) { value = "Mannerheim" } }
            expect(false) { matches(TextField("Mannerheim")) { value = "Mannerheim" } }
        }
        test("styles") {
            expect(true) { matches(Button().apply { addClassNames("a", "b") }) { } }
            expect(true) { matches(Button().apply { addClassNames("a", "b") }) { classes = "a" } }
            expect(true) { matches(Button().apply { addClassNames("a", "b") }) { classes = "b" } }
            expect(true) { matches(Button().apply { addClassNames("a", "b") }) { classes = "a b" } }
            expect(false) { matches(Button().apply { addClassNames("a", "b") }) { classes = "a c" } }
            expect(false) { matches(Button().apply { addClassNames("a", "b") }) { classes = "c" } }
        }
        test("withoutStyles") {
            expect(true) { matches(Button().apply { addClassNames("a", "b") }) { } }
            expect(false) { matches(Button().apply { addClassNames("a", "b") }) { withoutClasses = "a" } }
            expect(false) { matches(Button().apply { addClassNames("a", "b") }) { withoutClasses = "b" } }
            expect(false) { matches(Button().apply { addClassNames("a", "b") }) { withoutClasses = "a b" } }
            expect(false) { matches(Button().apply { addClassNames("a", "b") }) { withoutClasses = "a c" } }
            expect(true) { matches(Button().apply { addClassNames("a", "b") }) { withoutClasses = "c" } }
        }
        test("predicates") {
            expect(true) { matches(Button()) {}}
            expect(false) { matches(Button()) { predicates.add(Predicate { false }) }}
        }
        test("themes") {
            expect(true) { matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) {} }
            expect(true) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { themes = "custom-theme" }
            }
            expect(true) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { themes = "my-theme" }
            }
            expect(true) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { themes = "custom-theme my-theme" }
            }
            expect(false) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { themes = "no-theme" }
            }
        }
        test("withoutThemes") {
            expect(true) { matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) {} }
            expect(false) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { withoutThemes = "custom-theme" }
            }
            expect(false) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { withoutThemes = "my-theme" }
            }
            expect(false) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { withoutThemes = "custom-theme my-theme" }
            }
            expect(true) {
                matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { withoutThemes = "no-theme" }
            }
        }
    }

    group("unmocked env") {
        beforeEach { MockVaadin.tearDown(); testingLifecycleHook = TestingLifecycleHook.default }
        test("lookup functions should work in unmocked environment") {
            _get(Button(), Button::class.java)
            expectThrows(AssertionError::class) {
                _get(Button(), TextField::class.java)
            }
        }
    }

    group("_expectOne") {
        test("FailsOnNoComponents UI") {
            expectThrows(AssertionError::class) {
                _expectOne(Button::class.java)
            }
            expectAfterLookupCalled()
        }

        test("FailsOnNoComponents") {
            expectThrows(AssertionError::class) {
                _expectOne(Button(), Span::class.java)
            }
            expectAfterLookupCalled()
        }

        test("fails when multiple components match") {
            expectThrows(AssertionError::class) {
                _expectOne(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java)
            }
            expectAfterLookupCalled()
        }

        test("selects self") {
            _expectOne(Button(), Button::class.java)
            expectAfterLookupCalled()
        }

        test("returns nested") {
            _expectOne(VerticalLayout(Button()), Button::class.java)
            expectAfterLookupCalled()
        }

        test("spec") {
            expectThrows(AssertionError::class) {
                _expectOne(Button("foo"), Button::class.java) { caption = "bar" }
            }
            expectAfterLookupCalled()
        }
    }

    group("_expect") {
        test("FailsOnNoComponents UI") {
            expectThrows(AssertionError::class) { _expect<Span>() }
            expectAfterLookupCalled()
        }

        test("FailsOnNoComponents") {
            expectThrows(AssertionError::class) { _expect(Button(), Span::class.java) }
            expectAfterLookupCalled()
        }

        test("matching 0 components works") {
            _expect<Span>(0)
            expectAfterLookupCalled()
            _expect(Button(), Span::class.java, 0)
        }

        test("fails when the count is wrong") {
            expectThrows(AssertionError::class) {
                _expect(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java)
            }
            expectAfterLookupCalled()
        }

        test("succeeds when the count is right") {
            _expect(UI.getCurrent().verticalLayout {
                verticalLayout { }
            }, VerticalLayout::class.java, 2)
            expectAfterLookupCalled()
        }

        test("selects self") {
            _expect(Button(), Button::class.java, 1)
            expectAfterLookupCalled()
        }

        test("returns nested") {
            _expect(VerticalLayout(Button()), Button::class.java, 1)
            expectAfterLookupCalled()
        }

        test("spec") {
            expectThrows(AssertionError::class) {
                _expect(Button("foo"), Button::class.java) { caption = "bar" }
            }
            expectAfterLookupCalled()
        }
    }
}

data class MyLifecycleHook(var isBeforeLookupCalled: Boolean = false, var isAfterLookupCalled: Boolean = false) : TestingLifecycleHook {
    override fun awaitBeforeLookup() {
        check(!isBeforeLookupCalled) { "awaitBeforeLookup() has been already called" }
        check(!isAfterLookupCalled) { "awaitAfterLookup() has been already called" }
        isBeforeLookupCalled = true
    }

    override fun awaitAfterLookup() {
        check(isBeforeLookupCalled) { "awaitBeforeLookup() has not yet been called" }
        check(!isAfterLookupCalled) { "awaitAfterLookup() has been already called" }
        isAfterLookupCalled = true
    }
}

fun expectAfterLookupCalled() {
    expect(MyLifecycleHook(true, true)) { testingLifecycleHook }
    testingLifecycleHook = MyLifecycleHook()
}
