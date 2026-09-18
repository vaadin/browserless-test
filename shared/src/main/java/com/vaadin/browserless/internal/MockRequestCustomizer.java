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
 * Applies last-minute customisations to a freshly created {@link MockRequest},
 * before it is wrapped in a {@code VaadinRequest} and the session is created.
 * <p>
 * The environment resolves an implementation through the Vaadin {@code Lookup},
 * so registering one as a lookup service is how a request gains details the
 * mock cannot know by itself. That is how the Spring and Quarkus integrations
 * inject the authenticated principal and its roles, and it is the extension
 * point to implement for a container the framework does not cover.
 */
@FunctionalInterface
public interface MockRequestCustomizer {
    void apply(MockRequest request);
}
