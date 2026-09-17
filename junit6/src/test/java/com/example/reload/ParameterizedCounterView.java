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

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

/**
 * A plain counter view reached through a URL parameter and a query string. A
 * reload must render the very same location again, so the fresh instance is
 * expected to see the same route parameter and query parameters.
 */
@Route("param-counter")
public class ParameterizedCounterView extends CounterViewBase
        implements HasUrlParameter<String> {

    private String parameter;
    private QueryParameters queryParameters;

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.parameter = parameter;
        this.queryParameters = event.getLocation().getQueryParameters();
    }

    public String getParameter() {
        return parameter;
    }

    public QueryParameters getQueryParameters() {
        return queryParameters;
    }
}
