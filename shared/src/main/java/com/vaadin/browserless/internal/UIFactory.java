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

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Creates the {@link UI} of a Vaadin session.
 * <p>
 * The factory <em>must</em> return a fresh instance every time. A UI already
 * attached to a session carries that session's state into the next test, which
 * is why {@code MockVaadin.createUI} refuses one.
 * <p>
 * Pass an implementation to
 * {@code BrowserlessApplicationContext.Builder.withUIFactory(UIFactory)} to
 * have tests run against a {@link UI} subclass of your own, or to
 * {@code withServletFactory(…)} alongside the routes.
 * <p>
 * The single abstract method is named {@code invoke()} to match the bytecode
 * shape of the Kotlin {@code () -> UI} interface it replaces, so that existing
 * lambdas and method references such as {@code MockedUI::new} keep binding.
 */
@FunctionalInterface
public interface UIFactory extends Serializable {
    UI invoke();
}
