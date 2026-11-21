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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
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

    /**
     * The UI whose pending JavaScript is dispatched.
     */
    private final UI ui;

    /**
     * The handlers the pending JavaScript is passed to, in registration order.
     */
    private final List<Handler> handlers = new ArrayList<>();

    /**
     * The invocations already answered, so that none is answered twice.
     */
    private final Set<PendingJavaScriptInvocation> answered = Collections
            .newSetFromMap(new IdentityHashMap<>());

    /**
     * Whether a dispatch is running, so that a nested one is skipped.
     */
    private boolean dispatching;

    /**
     * Whether the simulated client has been set up for the UI.
     */
    private boolean simulatedClientSetUp;

    /**
     * When the queue was last taken by this dispatcher. Flow updates the
     * timestamp each time the queue is taken, even when it is empty, so a
     * different value means something else took it. A clock too coarse to tell
     * two takes apart only means such a take goes unnoticed.
     */
    private Instant lastTaken;

    private JavaScriptDispatcher(UI ui) {
        this.ui = ui;
        lastTaken = ui.getInternals().getLastUpdateSentTimestamp();
    }

    /**
     * Sets up the simulated client of a UI that the mocked environment has just
     * created, before the UI is initialized: installs the handlers that
     * simulate what a browser does with the JavaScript Browserless Test knows
     * how to simulate, such as server-side {@code Focusable.focus()} and
     * {@code blur()} calls, followed by a new instance of each
     * {@link JavaScriptHandler} configured for the environment with
     * {@link #setJavaScriptHandlers(VaadinService, Collection)}. Installing
     * them up front means a call sent before any tester interaction, while the
     * initial view is built or by a lookup, is handled too. Setting up the same
     * UI again does nothing.
     *
     * @param ui
     *            the UI, not null
     * @throws BrowserlessTestSetupException
     *             if a configured handler cannot be instantiated
     */
    public static void setUpSimulatedClient(UI ui) {
        JavaScriptDispatcher dispatcher = forUI(ui);
        if (dispatcher.simulatedClientSetUp) {
            return;
        }
        dispatcher.simulatedClientSetUp = true;
        dispatcher.addHandler(FocusTracker.HANDLER);
        VaadinSession session = ui.getSession();
        ConfiguredHandlers configured = session == null ? null
                : session.getService().getContext()
                        .getAttribute(ConfiguredHandlers.class);
        if (configured != null) {
            for (Class<? extends JavaScriptHandler> type : configured.types()) {
                JavaScriptHandler handler = instantiate(type);
                dispatcher.addHandler(invocation -> handler
                        .handle(new ExecutedJavaScript.Execution(dispatcher,
                                invocation)));
            }
        }
    }

    /**
     * Sets the {@link JavaScriptHandler} classes that every UI created from now
     * on for the given service gets an instance of, replacing the ones set
     * before.
     *
     * @param service
     *            the service of the mocked environment, not null
     * @param handlers
     *            the handler classes, in the order they run, not null
     */
    public static void setJavaScriptHandlers(VaadinService service,
            Collection<Class<? extends JavaScriptHandler>> handlers) {
        service.getContext().setAttribute(ConfiguredHandlers.class,
                new ConfiguredHandlers(List.copyOf(handlers)));
    }

    private static JavaScriptHandler instantiate(
            Class<? extends JavaScriptHandler> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new BrowserlessTestSetupException("Cannot create the "
                    + "JavaScript handler " + type.getName()
                    + ". A JavaScript handler must be a public class with a "
                    + "public no-argument constructor.", e);
        }
    }

    private record ConfiguredHandlers(
            List<Class<? extends JavaScriptHandler>> types)
            implements
                Serializable {
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
     * <p>
     * Does nothing for a UI that is closed and removed from its session, as
     * there is no client left to send to.
     *
     * @param ui
     *            the UI whose pending JavaScript to send, or null to do nothing
     * @throws IllegalStateException
     *             if something else, such as a test calling
     *             {@code UIInternals.dumpPendingJavaScriptInvocations()}, took
     *             the pending JavaScript queue of the UI since the previous
     *             dispatch; any JavaScript taken that way reached neither the
     *             simulation nor any capture, so the test would no longer check
     *             what a browser gets
     */
    public static void dispatch(UI ui) {
        if (ui != null && ui.getSession() != null) {
            forUI(ui).dispatch();
        }
    }

    UI getUI() {
        return ui;
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
        Instant taken = ui.getInternals().getLastUpdateSentTimestamp();
        if (!taken.equals(lastTaken)) {
            lastTaken = taken;
            throw new IllegalStateException("The pending JavaScript queue of "
                    + "the UI was taken outside Browserless Test, for example "
                    + "with UIInternals.dumpPendingJavaScriptInvocations(). "
                    + "Browserless Test sends the pending JavaScript to its "
                    + "simulated client itself. Any JavaScript taken "
                    + "elsewhere is not simulated, such as a focus() call, "
                    + "and reaches no JavaScript handler or capture. Use "
                    + "ExecutedJavaScript.capture() to check which "
                    + "JavaScript was sent.");
        }
        dispatching = true;
        try {
            List<PendingJavaScriptInvocation> invocations = ui.getInternals()
                    .dumpPendingJavaScriptInvocations();
            lastTaken = ui.getInternals().getLastUpdateSentTimestamp();
            for (PendingJavaScriptInvocation invocation : invocations) {
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

    /**
     * Checks whether the simulated client has answered an invocation.
     *
     * @param invocation
     *            the invocation, not null
     * @return {@literal true} if the invocation was answered
     */
    public boolean isAnswered(PendingJavaScriptInvocation invocation) {
        return answered.contains(invocation);
    }
}
