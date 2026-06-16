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
package com.vaadin.flow.component.masterdetaillayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Tag("div")
@Route(value = "master-detail-layout", registerAtStartup = false)
public class MasterDetailLayoutView extends Component implements HasComponents {

    MasterDetailLayout layout;
    Span master;
    Span placeholder;
    Span detail;

    public MasterDetailLayoutView() {
        layout = new MasterDetailLayout();
        master = new Span("Master content");
        placeholder = new Span("No detail selected");
        // Not added to the layout initially; tests set it as needed.
        detail = new Span("Detail content");
        layout.setMaster(master);
        layout.setDetailPlaceholder(placeholder);
        add(layout);
    }
}
