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
package com.example.reload;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Shared body of the reload counter fixtures: a counter that is incremented by
 * clicking the {@code increment} button, plus a {@code self-reload} button
 * through which the view asks the browser to refresh itself. The subclasses
 * differ only in their route and in whether they are annotated with
 * {@link com.vaadin.flow.router.PreserveOnRefresh}, so a reload test compares
 * the two behaviors against identical views.
 */
public abstract class CounterViewBase extends VerticalLayout {

    private int count;
    private final Span label = new Span("0");

    protected CounterViewBase() {
        Button increment = new Button("Increment", e -> {
            count++;
            label.setText(String.valueOf(count));
        });
        increment.setId("increment");
        label.setId("count");
        Button selfReload = new Button("Reload",
                e -> getUI().ifPresent(ui -> ui.getPage().reload()));
        selfReload.setId("self-reload");
        add(label, increment, selfReload);
    }

    public int getCount() {
        return count;
    }
}
