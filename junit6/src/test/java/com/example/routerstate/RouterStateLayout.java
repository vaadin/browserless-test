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
package com.example.routerstate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;

/**
 * Reproducer for vaadin/flow#24471: a parent layout that binds text to the
 * router state signal's navigation target. The bindText probe runs during the
 * constructor (before the element is attached), so it reads the initial router
 * state whose navigationTarget is {@code null} and the mapper NPEs.
 */
@Layout
public class RouterStateLayout extends VerticalLayout implements RouterLayout {

    public RouterStateLayout() {
        H3 title = new H3();
        title.bindText(UI.getCurrentOrThrow().routerStateSignal()
                .map(state -> state.navigationTarget().getSimpleName()));
        add(title);
    }
}
