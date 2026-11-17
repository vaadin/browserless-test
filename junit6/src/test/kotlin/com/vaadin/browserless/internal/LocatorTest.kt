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
        Locator._expectNoDialogs() // should succeed on no dialogs
        val dlg = Dialog()
        dlg.open()
        val exception = expectThrows(AssertionError::class,
            "Too many visible Dialogs (1) in MockedUI[] matching Dialog and count=0..0:") {
            Locator._expectNoDialogs()
        }
        expect(true) {
            val msg = exception.message!!
            println(msg)
            msg.matches(""".*Too many visible Dialogs \(1\) in MockedUI\[] matching Dialog and count=0\.\.0: \[Dialog\[.*]]\. Component tree:
└── MockedUI\[]
\s+└── Dialog\[.*].*
""".toRegex(RegexOption.MULTILINE)
            )
        }
        dlg.close()
        Locator._expectNoDialogs()
    }

    group("InternalServerError handling") {
        test("component lookup fails on navigation error") {
            // Vaadin shows InternalServerError when an exception occurs during the navigation phase.
            // the _expect*() functions should detect this and fail fast.
            Utils.getCurrentUI().addBeforeEnterListener { event -> event.rerouteToError(RuntimeException("Simulated"), "Simulated") }
            UI.getCurrent().navigate("")
            expectThrows<AssertionError>("An internal server error occurred; please check log for the actual stack-trace. Error text: There was an exception while trying to navigate to") {
                Locator._expectOne(UI::class.java) {}
            }
        }
        group("_expectInternalServerError") {
            test("fails on no error") {
                expectThrows<java.lang.AssertionError>(
                    """Expected an internal server error but none happened. Component tree:
└── MockedUI[]"""
                ) { Locator._expectInternalServerError("") }
            }
            test("succeeds on error") {
                Utils.getCurrentUI().addBeforeEnterListener { event -> event.rerouteToError(RuntimeException("Simulated"), "Simulated") }
                UI.getCurrent().navigate("")
                Locator._expectInternalServerError("")
            }
            test("matches error message correctly") {
                Utils.getCurrentUI().addBeforeEnterListener { event -> event.rerouteToError(RuntimeException("Simulated"), "Simulated") }
                UI.getCurrent().navigate("")
                Locator._expectInternalServerError("Simulated")
            }
        }
    }
}

@DynaTestDsl
internal fun DynaNodeGroup.locatorTest() {

    beforeEach { MockVaadin.setup() }
    beforeEach { TestingLifecycleHooks.setTestingLifecycleHook(MyLifecycleHook()) }
    afterEach { TestingLifecycleHooks.setTestingLifecycleHook(object : TestingLifecycleHook {}) }
    afterEach { MockVaadin.tearDown() }

    group("_get") {
        test("fails when no component match") {
            expectThrows(AssertionError::class) {
                Locator._get(Button(), TextField::class.java) {}
            }
            expectAfterLookupCalled()
        }

        test("fail when multiple component match") {
            expectThrows(AssertionError::class) {
                Locator._get(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java) {}
            }
            expectAfterLookupCalled()
        }

        test("ReturnsSelf") {
            val button = Button()
            expect(button) { Locator._get(button, Button::class.java) {} }
            expectAfterLookupCalled()
        }

        test("ReturnsNested") {
            val button = Button()
            expect(button) { Locator._get(VerticalLayout(button), Button::class.java) {} }
            expectAfterLookupCalled()
        }

        test("fails on invisible") {
            val button = Button().apply { isVisible = false }
            expectThrows(java.lang.AssertionError::class) { Locator._get(button, Button::class.java) {} }
            expectAfterLookupCalled()
        }
    }

    group("_find") {
        test("findMatchingId") {
            val button = Button().apply { BasicUtils.setId_(this, "foo") }
            expectList(button) { Locator._find(VerticalLayout(button, Button()), Button::class.java) { it.id = "foo" } }
            expectAfterLookupCalled()
        }

        test("doesn't return invisible") {
            val button = Button().apply { isVisible = false }
            expectList() { Locator._find(button, Button::class.java) {} }
            expectAfterLookupCalled()
        }

        test("returns nested") {
            val vl = VerticalLayout().apply { verticalLayout {} }
            expect(2) { Locator._find(vl, VerticalLayout::class.java) {}.size }
            expectAfterLookupCalled()
        }
    }

    group("_expectNone") {
        test("succeeds when no component match") {
            Locator._expectNone(Button(), TextField::class.java) {}
            expectAfterLookupCalled()
        }

        test("fail when multiple component match") {
            expectThrows(AssertionError::class) {
                Locator._expectNone(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java) {}
                expectAfterLookupCalled()
            }
        }

        test("fails if self matches") {
            val button = Button()
            expectThrows(AssertionError::class) { Locator._expectNone(button, Button::class.java) {} }
            expectAfterLookupCalled()
        }

        test("fails if nested matches") {
            val button = Button()
            expectThrows(AssertionError::class) { Locator._expectNone(VerticalLayout(button), Button::class.java) {} }
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

        Locator._get(TextField::class.java) { it.caption = "Type your name here:" }.value = "Baron Vladimir Harkonnen"
        expectAfterLookupCalled()

        // TODO: uncomment after importing _click() method
        // _get<Button> { caption = "Click Me" }._click()
        Locator._get(Button::class.java) { it.caption = "Click Me" }.click()
        expectAfterLookupCalled()

        expect("Thanks Baron Vladimir Harkonnen, it works!") { Locator._get(Text::class.java) { spec ->
            // Since Vaadin 25.1, Button contains a Text child
            spec.predicates.add { textComponent -> textComponent.parent.filter { (it is Button) }.isEmpty }
        }.text }
        expectAfterLookupCalled()
        Locator._get(TextField::class.java) { it.value = "Baron Vladimir Harkonnen" }  // test lookup by value
        expectAfterLookupCalled()
        expect("Thanks Baron Vladimir Harkonnen, it works!") { (layout.children.asSequence().last() as Text).text }
        expect(3) { layout.componentCount }
    }

    group("matcher") {
        test("id") {
            expect(true) { BasicUtils.matches(Button()) { } }
            expect(false) { BasicUtils.matches(Button()) { it.id = "a" } }
            expect(true) { BasicUtils.matches(Button().apply { BasicUtils.setId_(this, "a") }) { } }
            expect(true) { BasicUtils.matches(Button().apply { BasicUtils.setId_(this, "a") }) { it.id = "a" } }
            expect(false) { BasicUtils.matches(Button().apply { BasicUtils.setId_(this, "a b") }) { it.id = "a" } }
            expect(false) { BasicUtils.matches(Button().apply { BasicUtils.setId_(this, "a") }) { it.id = "a b" } }
        }
        test("caption") {
            expect(true) { BasicUtils.matches(Button("click me")) { it.caption = "click me" } }
            expect(true) { BasicUtils.matches(TextField("name:")) { it.caption = "name:" } }
            expect(true) { BasicUtils.matches(Button("click me")) { } }
            expect(true) { BasicUtils.matches(TextField("name:")) { } }
            expect(false) { BasicUtils.matches(Button("click me")) { it.caption = "Click Me" } }
            expect(false) { BasicUtils.matches(TextField("name:")) { it.caption = "Name" } }
        }
        test("placeholder") {
            expect(true) { BasicUtils.matches(TextField("name", "the name")) { it.placeholder = "the name" } }
            expect(true) { BasicUtils.matches(PasswordField("password", "at least 6 characters")) { it.placeholder = "at least 6 characters" } }
            expect(true) { BasicUtils.matches(ComboBox<String>().apply { placeholder = "foo" }) { it.placeholder = "foo" } }
            expect(false) { BasicUtils.matches(TextField("name", "the name")) { it.placeholder = "name" } }
            expect(false) { BasicUtils.matches(PasswordField("password", "at least 6 characters")) { it.placeholder = "password" } }
        }
        test("value") {
            expect(true) { BasicUtils.matches(TextField(null, "Mannerheim", "placeholder")) { it.value = "Mannerheim" } }
            expect(false) { BasicUtils.matches(TextField("Mannerheim")) { it.value = "Mannerheim" } }
        }
        test("styles") {
            expect(true) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { } }
            expect(true) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.classes = "a" } }
            expect(true) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.classes = "b" } }
            expect(true) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.classes = "a b" } }
            expect(false) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.classes = "a c" } }
            expect(false) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.classes = "c" } }
        }
        test("withoutStyles") {
            expect(true) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { } }
            expect(false) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.withoutClasses = "a" } }
            expect(false) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.withoutClasses = "b" } }
            expect(false) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.withoutClasses = "a b" } }
            expect(false) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.withoutClasses = "a c" } }
            expect(true) { BasicUtils.matches(Button().apply { addClassNames("a", "b") }) { it.withoutClasses = "c" } }
        }
        test("predicates") {
            expect(true) { BasicUtils.matches(Button()) {}}
            expect(false) { BasicUtils.matches(Button()) { it.predicates.add(Predicate { false }) }}
        }
        test("themes") {
            expect(true) { BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) {} }
            expect(true) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.themes = "custom-theme" }
            }
            expect(true) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.themes = "my-theme" }
            }
            expect(true) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.themes = "custom-theme my-theme" }
            }
            expect(false) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.themes = "no-theme" }
            }
        }
        test("withoutThemes") {
            expect(true) { BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) {} }
            expect(false) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.withoutThemes = "custom-theme" }
            }
            expect(false) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.withoutThemes = "my-theme" }
            }
            expect(false) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.withoutThemes = "custom-theme my-theme" }
            }
            expect(true) {
                BasicUtils.matches(Button().apply { addThemeNames("custom-theme", "my-theme") }) { it.withoutThemes = "no-theme" }
            }
        }
    }

    group("unmocked env") {
        beforeEach { MockVaadin.tearDown(); TestingLifecycleHooks.setTestingLifecycleHook(object : TestingLifecycleHook {}) }
        test("lookup functions should work in unmocked environment") {
            Locator._get(Button(), Button::class.java) {}
            expectThrows(AssertionError::class) {
                Locator._get(Button(), TextField::class.java) {}
            }
        }
    }

    group("_expectOne") {
        test("FailsOnNoComponents UI") {
            expectThrows(AssertionError::class) {
                Locator._expectOne(Button::class.java) {}
            }
            expectAfterLookupCalled()
        }

        test("FailsOnNoComponents") {
            expectThrows(AssertionError::class) {
                Locator._expectOne(Button(), Span::class.java) {}
            }
            expectAfterLookupCalled()
        }

        test("fails when multiple components match") {
            expectThrows(AssertionError::class) {
                Locator._expectOne(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java) {}
            }
            expectAfterLookupCalled()
        }

        test("selects self") {
            Locator._expectOne(Button(), Button::class.java) {}
            expectAfterLookupCalled()
        }

        test("returns nested") {
            Locator._expectOne(VerticalLayout(Button()), Button::class.java) {}
            expectAfterLookupCalled()
        }

        test("spec") {
            expectThrows(AssertionError::class) {
                Locator._expectOne(Button("foo"), Button::class.java) { it.caption = "bar" }
            }
            expectAfterLookupCalled()
            expectThrows(AssertionError::class) {
                Locator._expectOne(Button("foo"), Button::class.java) { it.caption = "bar" }
            }
        }
    }

    group("_expect") {
        test("FailsOnNoComponents UI") {
            expectThrows(AssertionError::class) { Locator._expect(Span::class.java, 1) {} }
            expectAfterLookupCalled()
        }

        test("FailsOnNoComponents") {
            expectThrows(AssertionError::class) { Locator._expect(Button(), Span::class.java, 1) {} }
            expectAfterLookupCalled()
        }

        test("matching 0 components works") {
            Locator._expect(Span::class.java, 0) {}
            expectAfterLookupCalled()
            Locator._expect(Button(), Span::class.java, 0) {}
        }

        test("fails when the count is wrong") {
            expectThrows(AssertionError::class) {
                Locator._expect(UI.getCurrent().verticalLayout {
                    verticalLayout { }
                }, VerticalLayout::class.java, 1) {}
            }
            expectAfterLookupCalled()
        }

        test("succeeds when the count is right") {
            Locator._expect(UI.getCurrent().verticalLayout {
                verticalLayout { }
            }, VerticalLayout::class.java, 2) {}
            expectAfterLookupCalled()
        }

        test("selects self") {
            Locator._expect(Button(), Button::class.java, 1) {}
            expectAfterLookupCalled()
        }

        test("returns nested") {
            Locator._expect(VerticalLayout(Button()), Button::class.java, 1) {}
            expectAfterLookupCalled()
        }

        test("spec") {
            expectThrows(AssertionError::class) {
                Locator._expect(Button("foo"), Button::class.java, 1) { it.caption = "bar" }
            }
            expectAfterLookupCalled()
            expectThrows(AssertionError::class) {
                Locator._expect(Button("foo"), Button::class.java, 1) { it.caption = "bar" }
            }
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
    expect(MyLifecycleHook(true, true)) { TestingLifecycleHooks.getTestingLifecycleHook() }
    TestingLifecycleHooks.setTestingLifecycleHook(MyLifecycleHook())
}
