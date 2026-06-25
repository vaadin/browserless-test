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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.vaadin.browserless.internal.MockPage;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.locator.Locators;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * UI-level context for multi-user browserless testing.
 * <p>
 * Represents a single browser window (one {@link UI} instance). All DSL methods
 * ({@link #navigate}, {@link #find}, {@link #findInView}, {@link #test})
 * automatically call {@link #activate()} before executing, which transparently
 * switches the thread-local Vaadin state and security context to this window's
 * user.
 * <p>
 * Instances of this class are <strong>thread-affine</strong>: they must be
 * created, used, and closed on the same thread. The active context is held in a
 * {@link ThreadLocal} and is not visible to other threads. This class is not
 * safe for concurrent access from multiple threads.
 * <p>
 * This means you can freely interleave calls on different windows without
 * explicit context switching:
 *
 * <pre>
 * window1.navigate(ViewA.class);
 * window2.navigate(ViewB.class); // auto-switches to window2's user
 * window1.find(Button.class).first(); // auto-switches back to window1's user
 * </pre>
 *
 * @see BrowserlessUserContext#newWindow()
 * @see BrowserlessApplicationContext
 * @since 1.1
 */
public class BrowserlessUIContext
        implements TesterWrappers, Locators, AutoCloseable {

    private static final ThreadLocal<BrowserlessUIContext> activeContext = new ThreadLocal<>();

    private final BrowserlessUserContext user;
    private UI ui;
    private boolean closed;

    BrowserlessUIContext(BrowserlessUserContext user) {
        this.user = user;

        BrowserlessUIContext previous = activeContext.get();

        // Save outgoing user's security context on user switch — symmetric
        // with activate().
        if (previous != null && previous.user != this.user) {
            previous.user.saveSecurityContext();
        }

        // Install this user's Vaadin thread-locals (service/session/request/
        // response/security) so MockVaadin.createUI observes this user's
        // identity. UI is intentionally not touched here — createUI sets
        // UI.setCurrent itself after instantiating the UI, so leaving it
        // alone avoids clobbering a prior UI if createUI throws before
        // reaching its own UI.setCurrent. On same-user re-entry the security
        // snapshot is intentionally not restored, so the live thread state
        // (including any logout) is preserved for the new window.
        user.applySessionThreadLocals();

        try {
            MockVaadin.createUI(user.getApp().getUIFactory(),
                    user.getSession());
            this.ui = UI.getCurrent();
        } catch (RuntimeException ex) {
            // Roll back thread-local state: do not leak this half-built
            // context as the active context.
            if (previous != null && !previous.closed) {
                reactivateSurviving(previous);
            } else {
                user.clearThreadLocals();
            }
            throw ex;
        }

        // Publish as active only after the UI was fully constructed.
        activeContext.set(this);
    }

    /**
     * Activates this UI context on the current thread.
     * <p>
     * Sets all Vaadin thread-locals ({@link VaadinService},
     * {@link VaadinSession}, {@link UI}, {@link VaadinRequest},
     * {@link VaadinResponse}) to this window's values. If a
     * {@link SecurityContextHandler} is configured and the previous active
     * context belonged to a different user, the outgoing user's security
     * context is automatically saved and this user's context is restored.
     * <p>
     * The security context is a per-user snapshot, not per-window: switching
     * between windows of the same user does not save or restore security state,
     * so mutations made by one window are observed by sibling windows.
     * <p>
     * This method is called automatically by all DSL methods. You only need to
     * call it explicitly if you are accessing Vaadin APIs directly (e.g.
     * {@code UI.getCurrent()}).
     */
    public void activate() {
        if (closed) {
            throw new IllegalStateException(
                    "BrowserlessUIContext is already closed");
        }
        BrowserlessUIContext previous = activeContext.get();

        // Save outgoing user's security context on user switch
        if (previous != null && previous != this
                && previous.user != this.user) {
            previous.user.saveSecurityContext();
        }

        // Install this user's Vaadin thread-locals and UI, restoring the
        // user's security snapshot. On same-user re-entry the snapshot is
        // intentionally not restored, so a logout (or any security mutation)
        // made in a sibling window stays visible.
        user.applyUIThreadLocals(ui);

        activeContext.set(this);
    }

    /**
     * Navigates this window to the given view class.
     *
     * @param navigationTarget
     *            the view class to navigate to
     * @param <T>
     *            the view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(Class<T> navigationTarget) {
        activate();
        return BrowserlessDSL.navigate(ui, navigationTarget);
    }

    /**
     * Navigates this window to the given view class with a URL parameter.
     *
     * @param navigationTarget
     *            the view class to navigate to
     * @param parameter
     *            the URL parameter
     * @param <T>
     *            the view type
     * @param <C>
     *            the parameter type
     * @return the instantiated view
     */
    public <C, T extends Component & HasUrlParameter<C>> T navigate(
            Class<T> navigationTarget, C parameter) {
        activate();
        return BrowserlessDSL.navigate(ui, navigationTarget, parameter);
    }

    /**
     * Navigates this window to the given view class with route parameters.
     *
     * @param navigationTarget
     *            the view class to navigate to
     * @param parameters
     *            the route parameters
     * @param <T>
     *            the view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(Class<T> navigationTarget,
            Map<String, String> parameters) {
        activate();
        return BrowserlessDSL.navigate(ui, navigationTarget, parameters);
    }

    /**
     * Navigates this window to the given location and validates the resulting
     * view.
     *
     * @param location
     *            the navigation location string
     * @param expectedTarget
     *            the expected view class
     * @param <T>
     *            the view type
     * @return the instantiated view
     */
    public <T extends Component> T navigate(String location,
            Class<T> expectedTarget) {
        activate();
        return BrowserlessDSL.navigate(ui, location, expectedTarget);
    }

    /**
     * Shorthand for ad-hoc component testing of a single component — no
     * {@code @Route} view required. Builds a self-contained, route-free
     * application context via
     * {@link BrowserlessApplicationContext#forComponent(java.util.function.Supplier)},
     * opens one user and one window, and attaches the given component to that
     * window's UI. The returned window owns its bundled
     * {@link BrowserlessApplicationContext}, which is torn down when the
     * window's UI detaches, so a single try-with-resources is enough:
     *
     * <pre>
     * try (var window = BrowserlessUIContext.forComponent(new MyForm())) {
     *     window.findButton().withCaption("Save").click();
     * }
     * </pre>
     *
     * This is a <strong>single-window</strong> shorthand: it captures the given
     * instance, so opening further windows on the underlying context would
     * re-parent the same component. It also constructs the component
     * <em>before</em> any UI exists — if the component's constructor needs
     * {@code UI.getCurrent()} or the session, use
     * {@link #forComponent(Supplier)} instead. For multi-window, multi-user, or
     * routed tests use
     * {@link BrowserlessApplicationContext#forComponent(java.util.function.Supplier)}
     * with a fresh-instance factory, or the standard
     * {@code BrowserlessApplicationContext.create(...)} +
     * {@code newUser().newWindow()} chain.
     *
     * @param component
     *            the component to attach; must not be {@code null}
     * @return a window with the component attached
     */
    public static BrowserlessUIContext forComponent(Component component) {
        Objects.requireNonNull(component, "component must not be null");
        return forComponent(() -> component);
    }

    /**
     * Shorthand for ad-hoc component testing of a single component, taking a
     * factory rather than a ready instance — no {@code @Route} view required.
     * Builds a self-contained, route-free application context via
     * {@link BrowserlessApplicationContext#forComponent(java.util.function.Supplier)},
     * opens one user and one window, and attaches the produced component to
     * that window's UI:
     *
     * <pre>
     * try (var window = BrowserlessUIContext.forComponent(MyForm::new)) {
     *     window.findButton().withCaption("Save").click();
     * }
     * </pre>
     *
     * The factory runs from {@code UI.init()}, after the Vaadin thread-locals
     * ({@link UI}, session, security context) are installed, so a component
     * whose constructor reads {@code UI.getCurrent()} observes the live
     * environment — unlike {@link #forComponent(Component)}, which receives an
     * already-constructed instance. The returned window owns its bundled
     * {@link BrowserlessApplicationContext}, which is torn down when the
     * window's UI detaches.
     *
     * @param componentFactory
     *            supplies the component to attach; must not be {@code null}
     * @return a window with the produced component attached
     */
    public static BrowserlessUIContext forComponent(
            Supplier<Component> componentFactory) {
        Objects.requireNonNull(componentFactory,
                "componentFactory must not be null");
        // noinspection resource
        return BrowserlessApplicationContext.forComponent(componentFactory)
                .newUser().newWindow();
    }

    /**
     * Gets a query object for finding components of the given type in this
     * window's UI.
     *
     * @param componentType
     *            the type of component to search for
     * @param <T>
     *            the component type
     * @return a query object
     */
    public <T extends Component> ComponentQuery<T> find(
            Class<T> componentType) {
        activate();
        return BrowserlessDSL.find(ui, componentType);
    }

    /**
     * Gets a query object for finding components of the given type nested
     * inside the specified component.
     *
     * @param componentType
     *            the type of component to search for
     * @param fromThis
     *            the component to search within
     * @param <T>
     *            the component type
     * @return a query object
     */
    public <T extends Component> ComponentQuery<T> find(Class<T> componentType,
            Component fromThis) {
        activate();
        return BrowserlessDSL.find(ui, componentType, fromThis);
    }

    /**
     * Gets a query object for finding components of the given type inside the
     * current view.
     *
     * @param componentType
     *            the type of component to search for
     * @param <T>
     *            the component type
     * @return a query object
     */
    public <T extends Component> ComponentQuery<T> findInView(
            Class<T> componentType) {
        activate();
        return BrowserlessDSL.findView(ui, componentType);
    }

    /**
     * Wraps a component with the best matching {@link ComponentTester}. This
     * generic fallback is used for component types not covered by the specific
     * {@link TesterWrappers} defaults.
     *
     * @param component
     *            the component to wrap
     * @param <T>
     *            the tester type
     * @param <Y>
     *            the component type
     * @return the component wrapped in a tester
     */
    public <T extends ComponentTester<Y>, Y extends Component> T test(
            Y component) {
        activate();
        return BaseBrowserlessTest.internalWrap(component);
    }

    /**
     * Wraps a component in the given {@link ComponentTester}.
     *
     * @param tester
     *            test wrapper to use
     * @param component
     *            component to wrap
     * @param <T>
     *            the tester type
     * @param <Y>
     *            the component type
     * @return the initialized tester for the component
     */
    public <T extends ComponentTester<Y>, Y extends Component> T test(
            Class<T> tester, Y component) {
        activate();
        return BaseBrowserlessTest.internalWrap(tester, component);
    }

    /**
     * Gets the current view displayed in this window.
     *
     * @return the current view
     */
    public HasElement getCurrentView() {
        activate();
        return BrowserlessDSL.getCurrentView(ui);
    }

    /**
     * Simulates the user reloading this window (pressing F5): the window's UI
     * is detached and a fresh one is created in the same Vaadin session, then
     * the current location is rendered again. Session-scoped state (session
     * attributes, security context) survives, and sibling windows are
     * unaffected. Views annotated with
     * {@link com.vaadin.flow.router.PreserveOnRefresh @PreserveOnRefresh} keep
     * their component instance and state; other views are recreated.
     *
     * @return the view shown after the reload
     */
    public HasElement reload() {
        activate();
        HasElement view = BrowserlessDSL.reload(ui);
        // reload() swapped in a fresh UI; re-capture it so later operations on
        // this window act on the live UI rather than the detached one.
        this.ui = UI.getCurrent();
        return view;
    }

    /**
     * Simulates a page reload (see {@link #reload()}) and verifies the
     * resulting view is of the expected type.
     *
     * @param expectedTarget
     *            the expected view class after reload
     * @param <T>
     *            the view type
     * @return the view shown after the reload
     */
    public <T extends Component> T reload(Class<T> expectedTarget) {
        activate();
        T view = BrowserlessDSL.reload(ui, expectedTarget);
        this.ui = UI.getCurrent();
        return view;
    }

    /**
     * Simulates a server round-trip, flushing pending component changes.
     */
    public void roundTrip() {
        activate();
        BrowserlessDSL.roundTrip(ui);
    }

    /**
     * Simulates a keyboard shortcut performed on the browser.
     *
     * @param key
     *            primary key of the shortcut. This must not be a
     *            {@link KeyModifier}.
     * @param modifiers
     *            key modifiers. Can be empty.
     */
    public void fireShortcut(Key key, KeyModifier... modifiers) {
        activate();
        BrowserlessDSL.fireShortcut(ui, key, modifiers);
    }

    /**
     * Processes all pending Signals tasks with a default max wait time of 100
     * milliseconds. Convenience for tests that need to wait for asynchronous
     * Signal effects triggered from background threads or non-UI contexts.
     *
     * <p>
     * If this window's {@link VaadinSession} lock is held by the current
     * thread, it is temporarily released during the wait to allow background
     * threads to acquire the lock and enqueue tasks.
     *
     * @return {@code true} if any pending Signals tasks were processed
     * @see #runPendingSignalsTasks(long, TimeUnit)
     */
    public boolean runPendingSignalsTasks() {
        return runPendingSignalsTasks(100, TimeUnit.MILLISECONDS);
    }

    /**
     * Processes all pending Signals tasks, waiting up to the specified timeout
     * for the first task to arrive. Once the first task is found, all remaining
     * tasks in the queue are processed immediately without additional waiting.
     *
     * @param maxWaitTime
     *            the maximum time to wait for the first task to arrive in the
     *            given time unit. If &lt;= 0, returns immediately if no tasks
     *            are available.
     * @param unit
     *            the time unit of the timeout value
     * @return {@code true} if any pending Signals tasks were processed
     */
    public boolean runPendingSignalsTasks(long maxWaitTime, TimeUnit unit) {
        activate();
        return BrowserlessDSL.runPendingSignalsTasks(
                user.getApp().getSignalsTestEnvironment(), maxWaitTime, unit);
    }

    /**
     * Returns the URL from the last external navigation triggered by
     * {@link com.vaadin.flow.component.page.Page#setLocation(String)} or
     * {@link com.vaadin.flow.component.page.Page#open(String)}.
     * <p>
     * This captures URLs where the window name is {@code _self},
     * {@code _parent}, {@code _top}, empty, or {@code null} (all of which
     * navigate the current window). To query URLs opened in other windows, use
     * {@link #getExternalNavigationURL(String)} or {@link #getOpenedWindows()}.
     *
     * @return the external navigation URL, or {@code null} if no external
     *         navigation has occurred
     */
    public String getExternalNavigationURL() {
        activate();
        if (ui.getPage() instanceof MockPage mockPage) {
            return mockPage.getLastExternalNavigationURL();
        }
        return null;
    }

    /**
     * Returns the last URL opened with the given window name.
     * <p>
     * For {@code _blank}, returns the URL from the most recent call. For named
     * windows, returns the last URL that targeted that name.
     *
     * @param windowName
     *            the window name to look up
     * @return the last URL for the given window name, or {@code null} if none
     */
    public String getExternalNavigationURL(String windowName) {
        activate();
        if (ui.getPage() instanceof MockPage mockPage) {
            return mockPage.getExternalNavigationURL(windowName);
        }
        return null;
    }

    /**
     * Returns a map of all windows opened via
     * {@link com.vaadin.flow.component.page.Page#open(String)} or
     * {@link com.vaadin.flow.component.page.Page#open(String, String)},
     * excluding navigations that target the current window ({@code _self},
     * {@code _parent}, {@code _top}, empty, or {@code null}).
     * <p>
     * The map keys are window names, and values are lists of URLs opened under
     * that name. For {@code _blank}, the list contains all URLs (each call
     * opens a new window). For named windows, the list typically contains a
     * single entry (the last URL).
     *
     * @return an unmodifiable map of window names to URL lists
     */
    public Map<String, List<String>> getOpenedWindows() {
        activate();
        if (ui.getPage() instanceof MockPage mockPage) {
            return mockPage.getOpenedWindows();
        }
        return Map.of();
    }

    @Override
    public void activateLocatorContext() {
        activate();
    }

    /**
     * Returns the UI managed by this context.
     *
     * @return the UI instance
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Returns the user context that owns this window.
     *
     * @return the parent user context
     */
    public BrowserlessUserContext getUser() {
        return user;
    }

    /**
     * Closes this UI context and detaches the UI.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        BrowserlessUIContext stillActive = activeContext.get();
        boolean wasActive = stillActive == this;
        if (wasActive) {
            activeContext.remove();
        } else if (stillActive != null && stillActive.user != this.user) {
            // Cross-user non-active close: capture the active user's live
            // security state before the detach run displaces it, so the
            // re-activation below restores their up-to-date snapshot.
            stillActive.user.saveSecurityContext();
        }
        if (ui != null) {
            // Set thread-locals so detach listeners see this user's identity
            // (service/session/UI/request/response/security), not whatever
            // the thread happens to carry from another user's window.
            user.applyUIThreadLocals(ui);
            // Detaching the UI fires its detach listeners. For an ad-hoc
            // context (see BrowserlessApplicationContext.forComponent) this
            // cascades back into app.close() -> user.close() -> this.close();
            // re-entry is safe because the closed flag was already set above.
            MockVaadin.closeCurrentUI(true);
            // closeCurrentUI() records the closing UI's active view location
            // into MockVaadin's lastNavigation ThreadLocal so the single-user
            // reload/session-recreation flow can restore it on the next UI.
            // No createUI follows here, so the recording would otherwise leak
            // into the next unrelated newWindow() on this thread.
            MockVaadin.clearLastNavigation();
            ui = null;
        }
        // Re-establish thread-local coherence with activeContext. After a
        // non-active close with a surviving active sibling, re-activate it.
        // Otherwise (active close, or no surviving active context) clear the
        // closing user's thread-locals so user code reading e.g.
        // SecurityContextHolder / UI.getCurrent() between close() and the
        // next activate() does not observe stale state from the closed
        // window.
        if (!wasActive && stillActive != null && !stillActive.closed) {
            reactivateSurviving(stillActive);
        } else {
            user.clearThreadLocals();
        }
    }

    /**
     * Returns the currently active UI context for the calling thread, or
     * {@code null} if none is active.
     *
     * @return the active context, or {@code null}
     */
    static BrowserlessUIContext getActive() {
        return activeContext.get();
    }

    /**
     * Clears the active-context ThreadLocal without otherwise touching the
     * thread-local Vaadin state. Reserved for defensive test cleanup; lifecycle
     * code paths should prefer
     * {@link #reactivateSurviving(BrowserlessUIContext)} which both clears and
     * re-installs the surviving window's state.
     */
    static void clearActiveContext() {
        activeContext.remove();
    }

    /**
     * Re-establishes thread-local coherence with {@link #activeContext} by
     * re-activating the given surviving window. Clears {@code activeContext}
     * first so {@link #activate()} takes the full-restore branch
     * ({@code previous == null}) and re-installs the active user's security
     * snapshot. Used by lifecycle paths (constructor rollback, non-active
     * window close, cross-user session destroy) that temporarily displaced the
     * active window's thread-local state.
     */
    static void reactivateSurviving(BrowserlessUIContext stillActive) {
        activeContext.remove();
        stillActive.activate();
    }

    boolean isClosed() {
        return closed;
    }
}
