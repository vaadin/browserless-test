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
package com.vaadin.browserless;

import java.io.Serializable;

/**
 * Plays the browser for JavaScript that an application sends and that
 * Browserless Test does not simulate on its own, such as reading a value that
 * only the browser knows.
 * <p>
 * A handler is registered for the whole test environment, with
 * {@link BrowserlessTestConfig#javaScriptHandlers()} or
 * {@link BrowserlessConfiguration.Builder#withJavaScriptHandlers(Class[])}.
 * Every UI the environment creates, also on a reload or in a new window, gets
 * its own instance, created with the public no-argument constructor before the
 * UI is initialized. The handler then sees each piece of JavaScript at the
 * moment it is sent to the simulated browser, in order, also while a view is
 * being navigated to, and answers it the way the browser would, so the
 * application behaves as it does in a browser without the test doing anything:
 *
 * <pre>
 * public class BrowserTimeZone implements JavaScriptHandler {
 *     &#64;Override
 *     public void handle(ExecutedJavaScript.Execution execution) {
 *         if (execution.isWaitingForResult() &amp;&amp; execution.getExpression()
 *                 .contains("resolvedOptions().timeZone")) {
 *             execution.resolve("Europe/Helsinki");
 *         }
 *     }
 * }
 *
 * &#64;BrowserlessTestConfig(javaScriptHandlers = BrowserTimeZone.class)
 * class ScheduleViewTest extends BrowserlessTest {
 * }
 * </pre>
 * <p>
 * A handler is offered every piece of JavaScript and ignores what it does not
 * simulate. Handlers run after the simulations Browserless Test ships with, in
 * the order they are registered. JavaScript that a handler makes the
 * application schedule, from a result handler for example, is sent at the next
 * client boundary, as with a browser. To check afterwards what was sent, use
 * {@link ExecutedJavaScript#capture()} instead.
 *
 * @see ExecutedJavaScript
 */
@FunctionalInterface
public interface JavaScriptHandler extends Serializable {

    /**
     * Reacts to a piece of JavaScript that the simulated browser has received.
     *
     * @param execution
     *            the JavaScript the browser received, not null
     */
    void handle(ExecutedJavaScript.Execution execution);
}
