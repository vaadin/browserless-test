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

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({ "deprecation", "removal" })
public class IsTemplateTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void isTemplate_litAndPolymerTemplates_trueOnlyForTemplates() {
        assertFalse(TestingLifecycleHook.isTemplate(new Button("foo")));
        assertTrue(TestingLifecycleHook.isTemplate(new MyLitTemplate()));
        assertTrue(TestingLifecycleHook.isTemplate(new MyPolymerTemplate()));
    }

    public interface MyModel extends TemplateModel {
    }

    public static class MyTemplateParser implements TemplateParser {
        @Override
        public TemplateData getTemplateContent(
                Class<? extends PolymerTemplate<?>> clazz, String tag,
                VaadinService service) {
            return new TemplateData("", new Element(tag));
        }
    }

    @Tag("my-polymer")
    public static class MyPolymerTemplate extends PolymerTemplate<MyModel> {
        public MyPolymerTemplate() {
            super(new MyTemplateParser());
        }
    }

    @Tag("my-lit")
    public static class MyLitTemplate extends LitTemplate {
    }
}
