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
package com.example.adhoc;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Tiny reusable widget used to exercise the ad-hoc component testing path —
 * deliberately not a {@code @Route} view.
 */
public class CounterWidget extends Composite<VerticalLayout> {

    private int count;
    private final Span counter = new Span("0");
    private final Button increment;

    public CounterWidget() {
        increment = new Button("Increment", e -> {
            count++;
            counter.setText(String.valueOf(count));
        });
        increment.setId("increment");
        counter.setId("counter");
        getContent().add(counter, increment);
    }

    public int getCount() {
        return count;
    }
}
