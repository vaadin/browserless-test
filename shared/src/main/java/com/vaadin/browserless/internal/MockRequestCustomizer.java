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

import com.vaadin.browserless.mocks.MockRequest;

/**
 * Customizes the {@link MockRequest} created for every new mocked session.
 * Looked up from the Vaadin {@link com.vaadin.flow.di.Lookup}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@FunctionalInterface
public interface MockRequestCustomizer {

    /**
     * Customizes given request.
     *
     * @param request
     *            the mock request, not null.
     */
    void apply(MockRequest request);
}
