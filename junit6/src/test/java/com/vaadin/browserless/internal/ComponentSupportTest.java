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

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;

/**
 * Verifies that the mocked environment consults every {@link ComponentSupport}
 * registered with {@link java.util.ServiceLoader}, using one registered by this
 * test module for a component of its own.
 */
class ComponentSupportTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void lookup_findsChildrenReportedBySupport() {
        Span hidden = new Span("hidden");
        UI.getCurrent().add(new PanelWithHiddenContent(hidden));

        Assertions.assertSame(hidden,
                LocatorKt._get(Span.class, spec -> kotlin.Unit.INSTANCE));
    }

    @Test
    void label_comesFromSupport() {
        PanelWithHiddenContent panel = new PanelWithHiddenContent(new Span());

        Assertions.assertEquals("panel title",
                ComponentUtilsKt.getLabel(panel));
        ComponentUtilsKt.setLabel(panel, "renamed");
        Assertions.assertEquals("renamed", ComponentUtilsKt.getLabel(panel));
    }

    @SuppressWarnings("deprecation")
    @Test
    void caption_fallsBackToTheLabelFromSupport() {
        PanelWithHiddenContent panel = new PanelWithHiddenContent(new Span());

        Assertions.assertEquals("panel title",
                ComponentUtilsKt.getCaption(panel));
        ComponentUtilsKt.setCaption(panel, "renamed");
        Assertions.assertEquals("renamed", ComponentUtilsKt.getLabel(panel));
    }

    @Test
    void label_ofHasLabelComponent_isNotAskedFromSupport() {
        LabelledPanel panel = new LabelledPanel();
        panel.setLabel("own label");

        Assertions.assertEquals("own label", ComponentUtilsKt.getLabel(panel));
    }

    @Test
    void dialogTypesAndClosedDialogs_comeFromSupport() {
        TestDialog dialog = new TestDialog();
        UI.getCurrent().add(dialog);

        Assertions.assertThrows(AssertionError.class,
                LocatorKt::_expectNoDialogs);

        dialog.opened = false;
        MockVaadin.clientRoundtrip();

        Assertions.assertFalse(dialog.isAttached(),
                "A dialog the support reports as closed should be removed");
        LocatorKt._expectNoDialogs();
    }

    @Test
    void prettyString_includesDetailsAddedBySupport() {
        PanelWithHiddenContent panel = new PanelWithHiddenContent(new Span());

        Assertions.assertTrue(
                PrettyPrintTreeKt.toPrettyString(panel).contains("hidden=1"),
                PrettyPrintTreeKt.toPrettyString(panel));
    }

    /**
     * Keeps its content out of the element tree, so only its
     * {@link ComponentSupport} can report it.
     */
    @Tag("test-panel")
    static class PanelWithHiddenContent extends Component {
        private final Component content;
        private String title = "panel title";

        PanelWithHiddenContent(Component content) {
            this.content = content;
        }
    }

    @Tag("test-labelled-panel")
    static class LabelledPanel extends Component implements HasLabel {
    }

    @Tag("test-dialog")
    static class TestDialog extends Component {
        private boolean opened = true;
    }

    public static class TestComponentSupport implements ComponentSupport {

        @Override
        public boolean isClosedDialog(Component component) {
            return component instanceof TestDialog dialog && !dialog.opened;
        }

        @Override
        public Collection<Class<? extends Component>> getDialogTypes() {
            return List.of(TestDialog.class);
        }

        @Override
        public List<Component> getAllChildren(Component component) {
            if (component instanceof PanelWithHiddenContent panel) {
                return List.of(panel.content);
            }
            return null;
        }

        @Override
        public void describe(Component component, List<String> description) {
            if (component instanceof PanelWithHiddenContent) {
                description.add("hidden=1");
            }
        }

        @Override
        public String getLabel(Component component) {
            if (component instanceof PanelWithHiddenContent panel) {
                return panel.title;
            }
            if (component instanceof LabelledPanel) {
                return "label from support";
            }
            return null;
        }

        @Override
        public boolean setLabel(Component component, String label) {
            if (component instanceof PanelWithHiddenContent panel) {
                panel.title = label;
                return true;
            }
            return false;
        }
    }
}
