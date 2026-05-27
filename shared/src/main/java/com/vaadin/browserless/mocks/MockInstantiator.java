/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.mocks;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;

/**
 * Makes sure to load [MockNpmTemplateParser].
 */
@SuppressWarnings({ "OverridingDeprecatedMember", "deprecation" })
public class MockInstantiator implements Instantiator {

    public final Instantiator delegate;

    public MockInstantiator(Instantiator delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        /*
        LitTemplateParser.LitTemplateParserFactory::class.java ->
            MockLitTemplateParserFactory as T
        MockInstantiatorV18.classNpmTemplateParserFactory ->
            MockInstantiatorV18.classMockNpmTemplateParserFactory.getConstructor().newInstance() as T
         */
        return delegate.getOrCreate(type);
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return delegate.getServiceInitListeners();
    }

    @Override
    public Stream<IndexHtmlRequestListener> getIndexHtmlRequestListeners(Stream<IndexHtmlRequestListener> listeners) {
        return delegate.getIndexHtmlRequestListeners(listeners);
    }

    @Override
    public Stream<DependencyFilter> getDependencyFilters(Stream<DependencyFilter> filters) {
        return delegate.getDependencyFilters(filters);
    }

    @Override
    public Class<?> getApplicationClass(Object instance) {
        return delegate.getApplicationClass(instance);
    }

    @Override
    public Class<?> getApplicationClass(Class<?> instanceClass) {
        return delegate.getApplicationClass(instanceClass);
    }

    @Override
    public <T extends HasElement> T createRouteTarget(Class<T> routeTargetType, NavigationEvent event) {
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

    public static Instantiator create(Instantiator delegate) {
        return new MockInstantiator(delegate);
    }

    /*
    private object MockLitTemplateParserImpl : LitTemplateParserImpl() {
        override fun getSourcesFromTemplate(tag: String, url: String): String =
                MockNpmTemplateParser.mockGetSourcesFromTemplate(tag, url)

        // Vaadin 22.0.0.beta2+ adds a new `service` parameter, need to override that function as well.
        open fun getSourcesFromTemplate(service: VaadinService, tag: String, url: String): String =
                MockNpmTemplateParser.mockGetSourcesFromTemplate(tag, url)
    }

    private object MockLitTemplateParserFactory : LitTemplateParser.LitTemplateParserFactory() {
        override fun createParser() = MockLitTemplateParserImpl
    }

    */
}
