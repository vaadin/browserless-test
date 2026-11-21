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
package com.example.javascript;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * A view that shows times in the user's time zone, which only the browser
 * knows, so the view asks the browser for it while it is built.
 */
@Route("schedule")
public class ScheduleView extends Div {

    public static final String TIME_ZONE_EXPRESSION = "return Intl.DateTimeFormat().resolvedOptions().timeZone";

    private final Span timeZone = new Span("Times shown in UTC");

    public ScheduleView() {
        add(timeZone);
        getElement().executeJs(TIME_ZONE_EXPRESSION).then(String.class,
                zone -> timeZone.setText("Times shown in " + zone));
    }

    public Span getTimeZone() {
        return timeZone;
    }
}
