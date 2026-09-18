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
package com.vaadin.browserless.internal;

import java.util.List;

import com.example.base.HelloWorldView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;

import static com.vaadin.browserless.internal.PrettyPrintTree.toPrettyString;
import static com.vaadin.browserless.internal.PrettyPrintTree.toPrettyTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PrettyPrintTreeTest {

    private static Routes routes;

    @BeforeAll
    static void discoverRoutes() {
        routes = new Routes().autoDiscoverViews("com.example.base");
    }

    @BeforeEach
    void setUp() {
        MockVaadin.setup(routes);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void toPrettyTree_divWithText_rendersBothLevels() {
        Div div = new Div();
        div.add(new Text("Foo"));
        assertEquals("""
                └── Div[text='Foo']
                    └── Text[text='Foo']""", toPrettyTree(div).trim());
    }

    @Test
    void toPrettyString_htmlComponents_dumpTextVisibilityAndSlot() {
        assertEquals("Text[text='foo']", toPrettyString(new Text("foo")));

        Div invisible = new Div();
        invisible.setVisible(false);
        assertEquals("Div[INVIS]", toPrettyString(invisible));

        assertEquals("Html[<b>foo bar baz <i>foobar</i></b>]", toPrettyString(
                new Html("\n    <b>foo\nbar\n    baz\n<i>foobar</i></b>")));

        assertEquals("HtmlSpan[innerHTML='aaa<b>bbbb</b>ccc']",
                toPrettyString(new HtmlSpan("aaa<b>bbbb</b>ccc")));

        Div tooltipped = new Div();
        ComponentUtils.tooltip(tooltipped, "foobar");
        assertEquals("Div[@title='foobar']", toPrettyString(tooltipped));

        Span testSpan = new Span("hi");
        new TextField().setPrefixComponent(testSpan);
        assertEquals("Span[text='hi', @slot='prefix']",
                toPrettyString(testSpan));
    }

    @Test
    void toPrettyString_textFields_dumpIdLabelValueAndErrorMessage() {
        TextField withId = new TextField();
        BasicUtils.id_(withId, "25");
        assertMatches(toPrettyString(withId), "TextField\\[#25, value=''.*]");

        TextArea area = new TextArea("label");
        area.setValue("some text");
        assertMatches(toPrettyString(area),
                "TextArea\\[label='label', value='some text'.*]");

        TextField invalid = new TextField();
        BasicUtils.id_(invalid, "25");
        invalid.setErrorMessage("failed validation");
        assertMatches(toPrettyString(invalid),
                "TextField\\[#25, value='', errorMessage='failed validation'.*]");

        assertMatches(toPrettyString(new TextField("foobar")),
                "TextField\\[label='foobar', value=''.*]");
    }

    @Test
    void toPrettyString_button_dumpsCaptionAndIcon() {
        assertEquals("Button[caption='click me']",
                toPrettyString(new Button("click me")));
        assertEquals("Button[icon='vaadin:abacus', @theme='icon']",
                toPrettyString(new Button(VaadinIcon.ABACUS.create())));
    }

    @Test
    void toPrettyString_anchor_dumpsHrefHoweverItIsDeclared() {
        assertEquals("Anchor[]", toPrettyString(new Anchor()));
        assertEquals("Anchor[href='vaadin.com']",
                toPrettyString(new Anchor("vaadin.com")));
        // the href is dumped for any component declaring it, also when it is
        // inherited or declared as a plain href() method
        assertEquals("MyAnchor[href='vaadin.com']",
                toPrettyString(new MyAnchor("vaadin.com")));
        assertEquals("ComponentWithHrefFunction[href='vaadin.com']",
                toPrettyString(new ComponentWithHrefFunction()));
        assertEquals("ComponentWithHrefField[href='vaadin.com']",
                toPrettyString(new ComponentWithHrefField()));
        assertEquals("ComponentWithHrefInterface[href='vaadin.com']",
                toPrettyString(new ComponentWithHrefInterface()));
        // a bean getter is consulted as well
        assertEquals("ComponentWithComputedHref[href='vaadin.com']",
                toPrettyString(new ComponentWithComputedHref()));
        // a blank member doesn't end the lookup, the getter still gets its turn
        assertEquals("ComponentWithBlankHrefField[href='vaadin.com']",
                toPrettyString(new ComponentWithBlankHrefField()));
    }

    @Test
    void toPrettyString_routerLink_blankHrefIsNotDumped() {
        // getHref() reports a missing href as an empty string, which is not
        // dumped
        assertEquals("RouterLink[]", toPrettyString(new RouterLink()));
        assertEquals("RouterLink[text='Hello', href='helloworld']",
                toPrettyString(new RouterLink("Hello", HelloWorldView.class)));
    }

    @Test
    void toPrettyString_image_dumpsSrc() {
        assertEquals("Image[]", toPrettyString(new Image()));
        assertEquals("Image[@src='vaadin.com']",
                toPrettyString(new Image("vaadin.com", "")));
    }

    @Test
    void toPrettyString_icon_dumpsTheIconName() {
        assertEquals("Icon[@icon='vaadin:abacus']",
                toPrettyString(VaadinIcon.ABACUS.create()));
    }

    @Test
    void toPrettyString_formItem_dumpsItsLabel() {
        assertEquals("FormItem[label='foo']", toPrettyString(
                new FormLayout().addFormItem(new TextField(), "foo")));
    }

    @Test
    void toPrettyString_componentWithCustomToString_usesIt() {
        assertEquals("MyComponentWithToString[my-div(25)]",
                toPrettyString(new MyComponentWithToString()));
    }

    @Test
    void toPrettyString_styles_areNotDuplicated() {
        Div div = new Div();
        assertEquals("Div[]", toPrettyString(div));
        div.setWidthFull();
        assertEquals("Div[@style='width:100%']", toPrettyString(div));
        div.getStyle().set("flex-shrink", "1");
        assertEquals("Div[@style='width:100%;flex-shrink:1']",
                toPrettyString(div));
    }

    @Test
    void toPrettyTree_contextMenu_rendersItemsAndSubItems() {
        Div div = new Div();
        UI.getCurrent().add(div);
        ContextMenu cm = new ContextMenu(div);
        var menu = cm.addItem("menu");
        menu.setEnabled(false);
        menu.getSubMenu().addItem("click me", e -> fail("shouldn't be called"));
        cm.addItem("save as");

        assertEquals("""
                └── ContextMenu[opened='false']
                    ├── MenuItem[DISABLED, text='menu']
                    │   └── MenuItem[text='click me']
                    └── MenuItem[text='save as']""", toPrettyTree(cm).trim());
    }

    @Nested
    class ToPrettyStringInternalServerError {

        private BeforeEnterEvent createEvent() {
            var router = VaadinService.getCurrent().getRouter();
            NavigationEvent navigationEvent = new NavigationEvent(router,
                    new Location("helloworld"), UI.getCurrent(),
                    NavigationTrigger.UI_NAVIGATE);
            return new BeforeEnterEvent(navigationEvent, HelloWorldView.class,
                    List.of());
        }

        private InternalServerError createErrorComponent(Exception error,
                String message) {
            MockInternalSeverError errorView = new MockInternalSeverError();
            ErrorParameter<Exception> errorParam = new ErrorParameter<>(
                    Exception.class, error, message);
            errorView.setErrorParameter(createEvent(), errorParam);
            return errorView;
        }

        @Test
        void noCauseException_dumpsTargetViewMessageAndType() {
            String pretty = toPrettyString(
                    createErrorComponent(new RuntimeException("OOPS!"), null))
                    .trim();
            assertTrue(pretty.contains("targetView='helloworld'"));
            assertTrue(pretty.contains("failureMessage='OOPS!'"));
            assertTrue(pretty
                    .contains("exceptionType='java.lang.RuntimeException'"));
        }

        @Test
        void customMessage_isDumpedInsteadOfTheExceptionMessage() {
            String pretty = toPrettyString(createErrorComponent(
                    new RuntimeException("BOOM!"), "Something failed")).trim();
            assertTrue(pretty.contains("targetView='helloworld'"));
            assertTrue(pretty.contains("failureMessage='Something failed'"));
            assertTrue(pretty
                    .contains("exceptionType='java.lang.RuntimeException'"));
        }
    }

    @Test
    void toPrettyTree_nullProperty_isNotDumped() {
        Div div = new Div();
        div.getElement().setProperty("null-property", (String) null);
        div.getElement().setProperty("nonnull-property", "OK");
        assertEquals("└── Div[nonnull-property='OK']",
                toPrettyTree(div).trim());
    }

    private static void assertMatches(String actual, String regex) {
        assertTrue(actual.matches(regex),
                "Expected '" + actual + "' to match '" + regex + "'");
    }

    static class MyComponentWithToString extends Div {
        @Override
        public String toString() {
            return "my-div(25)";
        }
    }

    /** Inherits the private {@code href} field from {@link Anchor}. */
    static class MyAnchor extends Anchor {
        MyAnchor(String href) {
            super(href);
        }
    }

    /** Declares {@code href} as a plain no-arg method. */
    static class ComponentWithHrefFunction extends Div {
        public String href() {
            return "vaadin.com";
        }
    }

    /** Declares {@code href} as a field. */
    static class ComponentWithHrefField extends Div {
        public String href = "vaadin.com";
    }

    /** Has a blank {@code href} field, but a real one through its getter. */
    static class ComponentWithBlankHrefField extends Anchor {
        ComponentWithBlankHrefField() {
            super("");
        }

        @Override
        public String getHref() {
            return "vaadin.com";
        }
    }

    /** Inherits {@code href} as a default method from an interface. */
    interface HasHrefFunction {
        default String href() {
            return "vaadin.com";
        }
    }

    static class ComponentWithHrefInterface extends Div
            implements HasHrefFunction {
    }

    /** Declares {@code href} through a bean getter only. */
    static class ComponentWithComputedHref extends Div {
        public String getHref() {
            return "vaadin.com";
        }
    }

    /**
     * Populates its contents with a given html snippet, without wrapping it in
     * a single root element the way {@link Html} requires.
     */
    static class HtmlSpan extends Span {
        HtmlSpan(String innerHTML) {
            removeAll();
            getElement().setProperty("innerHTML", innerHTML);
        }
    }
}
