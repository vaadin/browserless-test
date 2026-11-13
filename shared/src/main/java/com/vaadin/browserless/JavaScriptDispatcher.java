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
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.shared.Registration;

/**
 * Plays the browser for the JavaScript the server sends to a UI during a
 * browserless test.
 * <p>
 * At each client boundary - a round trip, the end of a simulated user
 * interaction - the whole pending JavaScript queue of the UI is taken, in
 * order, the way a server response would send it. Each invocation is offered to
 * the registered handlers: the ones that simulate what the browser would do
 * with it, and each {@link ExecutedJavaScript} capture a test has started.
 * <p>
 * Taking the queue is what lets the framework react to scheduled JavaScript
 * without hiding it from the application or from the test: this is the only
 * place that consumes the queue, and every handler sees every invocation, so
 * the simulation and any number of captures never take calls from each other.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class JavaScriptDispatcher implements Serializable {

    /**
     * Receives each piece of JavaScript sent to the simulated client, to
     * simulate what the browser does with it or to capture it for a test.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     */
    @FunctionalInterface
    public interface Handler extends Serializable {

        /**
         * Reacts to an invocation that the client has received. Called for
         * every invocation, in the order the server scheduled them; a handler
         * ignores the ones it is not interested in.
         *
         * @param invocation
         *            the invocation the client received, not null
         */
        void handle(PendingJavaScriptInvocation invocation);
    }

    private final UI ui;

    private final List<Handler> handlers = new ArrayList<>();

    private final Set<PendingJavaScriptInvocation> answered = Collections
            .newSetFromMap(new IdentityHashMap<>());

    private boolean dispatching;

    private JavaScriptDispatcher(UI ui) {
        this.ui = ui;
        // Installed with the dispatcher rather than when focus is first
        // tracked, so a focus call sent before any tester interaction, by a
        // lookup for example, is simulated too
        handlers.add(FocusTracker.handlerFor(ui));
    }

    /**
     * Gets the dispatcher of the given UI, creating it on first use.
     *
     * @param ui
     *            the UI, not null
     * @return the dispatcher of the UI, not null
     */
    public static JavaScriptDispatcher forUI(UI ui) {
        JavaScriptDispatcher dispatcher = ComponentUtil.getData(ui,
                JavaScriptDispatcher.class);
        if (dispatcher == null) {
            dispatcher = new JavaScriptDispatcher(ui);
            ComponentUtil.setData(ui, JavaScriptDispatcher.class, dispatcher);
        }
        return dispatcher;
    }

    /**
     * Sends the pending JavaScript of the given UI to the simulated client:
     * runs the tasks scheduled with {@code UI.beforeClientResponse}, then takes
     * the whole queue and offers each invocation to the handlers.
     *
     * @param ui
     *            the UI whose pending JavaScript to send, or null to do nothing
     */
    public static void dispatch(UI ui) {
        if (ui != null) {
            forUI(ui).dispatch();
        }
    }

    /**
     * Registers a handler that is offered every invocation sent from now on.
     *
     * @param handler
     *            the handler, not null
     * @return a handle for removing the handler, not null
     */
    public Registration addHandler(Handler handler) {
        return Registration.addAndRemove(handlers, handler);
    }

    private void dispatch() {
        // Like a server response, run the tasks scheduled with
        // beforeClientResponse first; executeJs calls are queued that way.
        // Also done by a round trip that a handler's listener triggers.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        // A handler that fires events may make the application schedule more
        // JavaScript, which goes out with the next response, as in a browser
        if (dispatching) {
            return;
        }
        dispatching = true;
        try {
            for (PendingJavaScriptInvocation invocation : ui.getInternals()
                    .dumpPendingJavaScriptInvocations()) {
                for (Handler handler : List.copyOf(handlers)) {
                    handler.handle(invocation);
                }
            }
        } finally {
            dispatching = false;
        }
    }

    /**
     * Marks an invocation as answered by the simulated client, which answers
     * each invocation at most once, like a browser.
     *
     * @param invocation
     *            the invocation, not null
     * @return {@literal true} if the invocation was not answered before
     */
    public boolean markAnswered(PendingJavaScriptInvocation invocation) {
        return answered.add(invocation);
    }
}
