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

import com.example.base.HelloWorldView
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.karibudsl.v10.contextMenu
import com.github.mvysny.karibudsl.v10.div
import com.github.mvysny.karibudsl.v10.item
import com.github.mvysny.karibudsl.v10.text
import com.github.mvysny.karibudsl.v10.tooltip
import com.vaadin.flow.component.Html
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.*
import com.vaadin.flow.server.VaadinService
import org.intellij.lang.annotations.Language
import kotlin.test.assertContains
import kotlin.test.expect
import kotlin.test.fail

@DynaTestDsl
internal fun DynaNodeGroup.prettyPrintTreeTest() {
    lateinit var routes: Routes
    beforeGroup { routes = Routes().autoDiscoverViews("com.example.base") }
    beforeEach { MockVaadin.setup(routes) }
    afterEach { MockVaadin.tearDown() }

    test("Simple dump") {
        val div = Div().apply {
            text("Foo")
        }
        expect("""
└── Div[text='Foo']
    └── Text[text='Foo']
""".trim()) { toPrettyTree(div).trim() }
    }

    test("toPrettyStringHtmlComponent()") {
        expect("Text[text='foo']") { toPrettyString(Text("foo")) }
        expect("Div[INVIS]") { toPrettyString(Div().apply { isVisible = false }) }
        expect("Html[<b>foo bar baz <i>foobar</i></b>]") {
            toPrettyString(Html("\n    <b>foo\nbar\n    baz\n<i>foobar</i></b>"))
        }
        expect("HtmlSpan[text='aaaccc', innerHTML='aaa<b>bbbb</b>ccc']") {
            toPrettyString(HtmlSpan("aaa<b>bbbb</b>ccc"))
        }
        expect("Div[@title='foobar']") {
            toPrettyString(Div().apply { tooltip = "foobar" })
        }
        expect("Span[text='hi', @slot='prefix']") {
            val testSpan = Span("hi")
            TextField().prefixComponent = testSpan
            toPrettyString(testSpan)
        }
    }
    test("toPrettyStringTextField()") {
        assertContains(toPrettyString(TextField().apply { setId("25") }),"TextField\\[#25, value=''.*]".toRegex())
        assertContains(toPrettyString(TextArea("label").apply { value = "some text" }),"TextArea\\[label='label', value='some text'.*]".toRegex())
        assertContains(toPrettyString(TextField().apply { setId("25"); errorMessage = "failed validation" }),"TextField\\[#25, value='', errorMessage='failed validation'.*]".toRegex())
        assertContains(toPrettyString(TextField("foobar")), "TextField\\[label='foobar', value=''.*]".toRegex() )
    }
    test("toPrettyStringButton()") {
        expect("Button[caption='click me']") { toPrettyString(Button("click me")) }
        expect("Button[icon='vaadin:abacus', @theme='icon']") { toPrettyString(Button(VaadinIcon.ABACUS.create())) }

    }
//    test("toPrettyStringGrid()") {
        /* TODO: uncomment when importing Grid stuff
        expect("Grid[<String>, dataprovider='ListDataProvider2{0 items}']") { toPrettyString(Grid<String>(String::class.java).apply { setItems2(listOf()) }) }
        expect("Column[header='My Header']") {
            toPrettyString(Grid<Any>().run { addColumn { it }.apply { header2 = "My Header" } })
        }
        expect("Column[header='My Header', key='foo']") {
            toPrettyString(Grid<Any>().run { addColumn { it }.apply { header2 = "My Header"; key = "foo" } })
        }
         */
//    }
    test("toPrettyStringAnchor()") {
        expect("Anchor[]") { toPrettyString(Anchor()) }
        expect("Anchor[href='vaadin.com']") { toPrettyString(Anchor("vaadin.com")) }
        // the href is dumped for any component declaring it, also when it is
        // inherited or declared as a plain href() method or a Kotlin property
        expect("MyAnchor[href='vaadin.com']") { toPrettyString(MyAnchor("vaadin.com")) }
        expect("ComponentWithHrefFunction[href='vaadin.com']") {
            toPrettyString(ComponentWithHrefFunction())
        }
        expect("ComponentWithHrefProperty[href='vaadin.com']") {
            toPrettyString(ComponentWithHrefProperty())
        }
        expect("ComponentWithHrefInterface[href='vaadin.com']") {
            toPrettyString(ComponentWithHrefInterface())
        }
        // a bean getter is consulted as well, which is all a Kotlin href property
        // without a backing field compiles to
        expect("ComponentWithComputedHref[href='vaadin.com']") {
            toPrettyString(ComponentWithComputedHref())
        }
        // a blank member doesn't end the lookup, the getter still gets its turn
        expect("ComponentWithBlankHrefField[href='vaadin.com']") {
            toPrettyString(ComponentWithBlankHrefField())
        }
    }
    test("toPrettyStringRouterLink()") {
        // getHref() reports a missing href as an empty string, which is not dumped
        expect("RouterLink[]") { toPrettyString(RouterLink()) }
        expect("RouterLink[text='Hello', href='helloworld']") {
            toPrettyString(RouterLink("Hello", HelloWorldView::class.java))
        }
    }
    test("toPrettyStringImage()") {
        expect("Image[]") { toPrettyString(Image()) }
        expect("Image[@src='vaadin.com']") { toPrettyString(Image("vaadin.com", "")) }
    }
    test("toPrettyStringIcon()") {
        expect("Icon[@icon='vaadin:abacus']") { toPrettyString(VaadinIcon.ABACUS.create()) }
    }
    test("toPrettyStringForm()") {
        expect("FormItem[label='foo']") { toPrettyString(FormLayout().addFormItem(TextField(), "foo")) }
    }
    test("toPrettyStringCustomComponent()") {
        expect("MyComponentWithToString[my-div(25)]") {
            toPrettyString(MyComponentWithToString())
        }
    }

    test("styles not duplicated") {
        val div = Div()
        expect("Div[]") { toPrettyString(div) }
        div.setWidthFull()
        expect("Div[@style='width:100%']") { toPrettyString(div) }
        div.style.set("flex-shrink", "1")
        expect("Div[@style='width:100%;flex-shrink:1']") { toPrettyString(div) }
    }

    test("menu dump") {
        lateinit var cm: ContextMenu
        UI.getCurrent().div {
            cm = contextMenu {
                item("menu") {
                    isEnabled = false
                    item("click me", { fail("shouldn't be called") })
                }
                item("save as")
            }
        }
        expect("""
└── ContextMenu[opened='false']
    ├── MenuItem[DISABLED, text='menu']
    │   └── MenuItem[text='click me']
    └── MenuItem[text='save as']""".trim()) { toPrettyTree(cm).trim() }

    }

    /* TODO: uncomment when importing Grid stuff
    group("grid") {
        test("column headers") {
            val grid: Grid<String> = UI.getCurrent().grid<String> {
                addColumn(karibuDslI18n).setHeader("Hello!")
                setItems2(listOf())
            }
            expect("""
└── Grid[<String>, dataprovider='ListDataProvider2{0 items}']
    └── Column[header='Hello!']
""".trim()) { toPrettyTree(grid).trim() }
        }

        test("grid menu dump") {
            lateinit var cm: GridContextMenu<String>
            UI.getCurrent().grid<String> {
                cm = gridContextMenu {
                    item("menu") {
                        isEnabled = false
                        item("click me", { fail("shouldn't be called") })
                    }
                    item("save as")
                }
            }
            expect("""
└── GridContextMenu[]
    ├── GridMenuItem[DISABLED, text='menu']
    │   └── GridMenuItem[text='click me']
    └── GridMenuItem[text='save as']""".trim()) { toPrettyTree(cm).trim() }
        }

        // tests https://github.com/mvysny/karibu-testing/issues/37
        test("grid filters dump") {
            val grid: Grid<String> = UI.getCurrent().grid<String> {
                val col: Grid.Column<String> = addColumn(karibuDslI18n)
                appendHeaderRow().getCell(col).setComponent(TextField("Filter:"))
                setItems2(listOf())
            }
            expect("""
└── Grid[<String>, dataprovider='ListDataProvider2{0 items}']
    ├── TextField[label='Filter:', value='']
    └── Column[]""".trim()) { toPrettyTree(grid).trim() }
        }

        // tests https://github.com/mvysny/karibu-testing/issues/37
        test("grid filters dump for joined column") {
            val grid: Grid<String> = UI.getCurrent().grid<String> {
                val col1: Grid.Column<String> = addColumn(karibuDslI18n).setHeader("foo")
                val col2: Grid.Column<String> = addColumn(karibuDslI18n).setHeader("bar")
                appendHeaderRow()
                prependHeaderRow().join(col1, col2).setComponent(TextField("Filter:"))
                setItems2(listOf())
            }
            expect("""
└── Grid[<String>, dataprovider='ListDataProvider2{0 items}']
    ├── TextField[label='Filter:', value='']
    └── ColumnGroup[]
        ├── ColumnGroup[]
        │   └── Column[header='foo']
        └── ColumnGroup[]
            └── Column[header='bar']""".trim()) { toPrettyTree(grid).trim() }
        }
    }
         */

    group("toPrettyStringInternalServerError()") {
        fun createEvent(): BeforeEnterEvent {
            val router = VaadinService.getCurrent().router
            val navigationEvent = NavigationEvent(router, Location("helloworld"), UI.getCurrent(), NavigationTrigger.UI_NAVIGATE)
            val event = BeforeEnterEvent(navigationEvent, HelloWorldView::class.java, emptyList())
            return event
        }

        fun createErrorComponent(error: Exception?, message: String? = null): InternalServerError {
            val errorView = MockInternalSeverError()
            val errorParam = ErrorParameter(Exception::class.java, error, message)
            errorView.setErrorParameter(createEvent(), errorParam)
            return errorView;
        }
        test("no cause exception") {
            val error = createErrorComponent(RuntimeException("OOPS!"))
            val pretty = toPrettyString(error).trim()
            assertContains(pretty, "targetView='helloworld'")
            assertContains(pretty, "failureMessage='OOPS!'")
            assertContains(pretty, "exceptionType='java.lang.RuntimeException'")
        }
        test("custom message") {
            val error = createErrorComponent(RuntimeException("BOOM!"), "Something failed")
            val pretty = toPrettyString(error).trim()
            assertContains(pretty, "targetView='helloworld'")
            assertContains(pretty, "failureMessage='Something failed'")
            assertContains(pretty, "exceptionType='java.lang.RuntimeException'")
        }
    }

    test("Handle null properties") {
        val div = Div().apply {
            element.setProperty("null-property", null)
            element.setProperty("nonnull-property", "OK")
        }
        expect("""
└── Div[nonnull-property='OK']
""".trim()) { toPrettyTree(div).trim() }
    }

}

class MyComponentWithToString : Div() {
    override fun toString(): String = "my-div(25)"
}

/**
 * Inherits the private `href` field from [Anchor].
 */
private class MyAnchor(href: String) : Anchor(href)

/**
 * Declares `href` as a plain no-arg function, the way a record-style accessor would.
 */
private class ComponentWithHrefFunction : Div() {
    fun href(): String = "vaadin.com"
}

/**
 * Declares `href` as a Kotlin property, which compiles to an `href` field.
 */
private class ComponentWithHrefProperty : Div() {
    var href: String = "vaadin.com"
}

/**
 * Has a blank `href` field, but reports a real one through its getter.
 */
private class ComponentWithBlankHrefField : Anchor("") {
    override fun getHref(): String = "vaadin.com"
}

/**
 * Inherits `href` as a default method from an interface.
 */
private interface HasHrefFunction {
    fun href(): String = "vaadin.com"
}

private class ComponentWithHrefInterface : Div(), HasHrefFunction

/**
 * Declares `href` as a Kotlin property without a backing field, which compiles to a
 * bean getter, exactly like [com.vaadin.flow.router.RouterLink.getHref] is one.
 */
private class ComponentWithComputedHref : Div() {
    val href: String get() = "vaadin.com"
}

/**
 * Populates its contents with given html snippet. The advantage over [com.vaadin.flow.component.Html]
 * is that any html is accepted - it doesn't have to be wrapped in a single root element.
 *
 * Note that it is the developer's responsibility to sanitize and remove any
 * dangerous parts of the HTML before sending it to the user through this
 * component. Passing raw input data to the user will possibly lead to
 * cross-site scripting attacks.
 *
 * This component does not expand the HTML fragment into a server side DOM tree
 * so you cannot traverse or modify the HTML on the server. The root element can
 * be accessed through [element] and the inner HTML through
 * [innerHTML].
 * @param innerHTML the HTML snippet to populate the span with.
 */
private class HtmlSpan(@Language("html") innerHTML: String = "") : Span() {
    /**
     * Sets the inner html. Removes any children added via [add].
     */
    @get:Language("html")
    @set:Language("html")
    var innerHTML: String
        get() = element.getProperty("innerHTML", "") ?: ""
        set(value) {
            removeAll()
            element.setProperty("innerHTML", value)
        }

    init {
        this.innerHTML = innerHTML
    }
}
