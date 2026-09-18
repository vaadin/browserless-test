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
package com.vaadin.browserless.mocks;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;

/**
 * An {@link Instantiator} wrapping the one the mocked environment provides.
 * <p>
 * The wrapper does not mock anything any more: the {@link #getOrCreate} special
 * cases it was written for are long gone, so every member simply forwards to
 * {@link #delegate}. The mocked services therefore use the environment's own
 * instantiator directly, and this class is kept for source compatibility only.
 * <p>
 * The forwarding is spelled out by hand on purpose. A wrapper that leaves a
 * method out inherits Flow's {@code default} implementation instead of reaching
 * the delegate, which is how a Spring {@code PageTitleGenerator} bean used to
 * be dropped in browserless tests. Since a wrapper can always fall behind a
 * method added to {@link Instantiator} later, {@link #create(Instantiator)}
 * hands back the delegate itself instead of wrapping it.
 * <p>
 * The list of forwarders below is deliberately not guarded by a test: pinning
 * it would fail the build whenever Flow adds a method to {@link Instantiator},
 * which is not worth it for a class scheduled for removal and used by nothing
 * here. A method missing from the list is therefore silently dropped for
 * callers who construct this class themselves — one more reason not to.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated Wrapping the instantiator of the mocked environment has no
 *             effect; use that instantiator directly. Scheduled for removal.
 */
@Deprecated(forRemoval = true)
public class MockInstantiator implements Instantiator {

    private final Instantiator delegate;

    /**
     * Returns the instantiator every member of this class forwards to.
     *
     * @return the delegate
     */
    public Instantiator getDelegate() {
        return delegate;
    }

    /**
     * Creates a wrapper forwarding to the given instantiator.
     *
     * @param delegate
     *            the instantiator to forward to
     */
    public MockInstantiator(Instantiator delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        return delegate.getOrCreate(type);
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return delegate.getServiceInitListeners();
    }

    @Override
    public Stream<IndexHtmlRequestListener> getIndexHtmlRequestListeners(
            Stream<IndexHtmlRequestListener> indexHtmlRequestListeners) {
        return delegate.getIndexHtmlRequestListeners(indexHtmlRequestListeners);
    }

    @Override
    public Stream<DependencyFilter> getDependencyFilters(
            Stream<DependencyFilter> serviceInitFilters) {
        return delegate.getDependencyFilters(serviceInitFilters);
    }

    @Override
    public Class<?> getApplicationClass(Object instance) {
        return delegate.getApplicationClass(instance);
    }

    @Override
    public Class<?> getApplicationClass(Class<?> clazz) {
        return delegate.getApplicationClass(clazz);
    }

    @Override
    public <T extends HasElement> T createRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        return delegate.createRouteTarget(routeTargetType, event);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return delegate.createComponent(componentClass);
    }

    @Override
    public I18NProvider getI18NProvider() {
        return delegate.getI18NProvider();
    }

    @Override
    public MenuAccessControl getMenuAccessControl() {
        return delegate.getMenuAccessControl();
    }

    @Override
    public PageTitleGenerator getPageTitleGenerator() {
        return delegate.getPageTitleGenerator();
    }

    /**
     * Returns {@code delegate} as is: wrapping it changes nothing, and a
     * wrapper risks dropping methods added to {@link Instantiator} in the
     * future.
     *
     * @param delegate
     *            the instantiator to return
     * @return the given instantiator, unwrapped
     */
    public static Instantiator create(Instantiator delegate) {
        return delegate;
    }
}
