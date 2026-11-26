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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;

class CompositeTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void lookup_compositeWithVirtualChild_findsItExactlyOnce() {
        MyComposite comp = new MyComposite();
        Locator._expectOne(comp, Span.class,
                spec -> spec.setText("virtual child"));
    }

    static class MyComposite extends Composite<VirtualChildComponent> {
    }

    @Tag("my-test")
    public static class VirtualChildComponent extends Component {
        public VirtualChildComponent() {
            Span child = new Span("virtual child");
            getElement().appendVirtualChild(child.getElement());
        }
    }
}
