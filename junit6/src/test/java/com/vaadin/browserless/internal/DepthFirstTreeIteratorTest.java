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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DepthFirstTreeIteratorTest {

    @Test
    void iterator_nestedNodes_visitsNodeThenDescendantsThenSibling() {
        DepthFirstTreeIterator<String> i = new DepthFirstTreeIterator<>("0",
                s -> s.length() > 2 ? List.of()
                        : List.of(s + "0", s + "1", s + "2"));
        List<String> actual = new ArrayList<>();
        while (i.hasNext()) {
            actual.add(i.next());
        }
        assertEquals(List.of("0", "00", "000", "001", "002", "01", "010", "011",
                "012", "02", "020", "021", "022"), actual);
    }

    @Test
    void walk_componentTree_visitsInDeclarationOrder() {
        List<Component> expected = new ArrayList<>();
        VerticalLayout root = new VerticalLayout();
        expected.add(root);

        Button button = new Button("Foo");
        root.add(button);
        expected.add(button);
        // In Vaadin 25.1, Button also has a text node
        button.getChildren().filter(c -> c instanceof Text).findFirst()
                .ifPresent(expected::add);

        HorizontalLayout horizontal = new HorizontalLayout();
        root.add(horizontal);
        expected.add(horizontal);
        Span span = new Span();
        horizontal.add(span);
        expected.add(span);

        VerticalLayout nested = new VerticalLayout();
        root.add(nested);
        expected.add(nested);

        List<Component> actual = new ArrayList<>();
        DepthFirstTreeIterator.walk(root).forEach(actual::add);
        assertEquals(expected, actual);
        assertSame(root, actual.get(0));
    }
}
