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
package com.vaadin.browserless.mocks

import com.vaadin.flow.component.HasElement
import com.vaadin.flow.di.Instantiator
import com.vaadin.flow.i18n.I18NProvider
import com.vaadin.flow.router.NavigationEvent
import com.vaadin.flow.router.PageTitleGenerator
import com.vaadin.flow.server.DependencyFilter
import com.vaadin.flow.server.auth.MenuAccessControl
import com.vaadin.flow.server.communication.IndexHtmlRequestListener
import java.util.stream.Stream
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers

/**
 * Makes sure to load [MockNpmTemplateParser].
 *
 * Every [Instantiator] method is forwarded to [delegate] explicitly: Kotlin
 * interface delegation only generates forwarders for the abstract members of
 * [Instantiator], so a method with a Java `default` implementation would
 * silently resolve to that default instead of reaching the delegate. That
 * would hide whatever the real environment's instantiator does — a Spring
 * `PageTitleGenerator` bean, for instance, would never be applied in a test.
 * `MockInstantiatorDelegationTest` guards the forwarding.
 */
open class MockInstantiator(val delegate: Instantiator) : Instantiator by delegate {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getOrCreate(type: Class<T>): T = when (type) {
        /*
        LitTemplateParser.LitTemplateParserFactory::class.java ->
            MockLitTemplateParserFactory as T
        MockInstantiatorV18.classNpmTemplateParserFactory ->
            MockInstantiatorV18.classMockNpmTemplateParserFactory.getConstructor().newInstance() as T
         */
        else -> delegate.getOrCreate(type)
    }

    override fun getMenuAccessControl(): MenuAccessControl = delegate.menuAccessControl

    override fun getI18NProvider(): I18NProvider? = delegate.i18NProvider

    override fun getPageTitleGenerator(): PageTitleGenerator? = delegate.pageTitleGenerator

    override fun getIndexHtmlRequestListeners(
        indexHtmlRequestListeners: Stream<IndexHtmlRequestListener>
    ): Stream<IndexHtmlRequestListener> =
        delegate.getIndexHtmlRequestListeners(indexHtmlRequestListeners)

    override fun getDependencyFilters(
        serviceInitFilters: Stream<DependencyFilter>
    ): Stream<DependencyFilter> = delegate.getDependencyFilters(serviceInitFilters)

    override fun getApplicationClass(instance: Any): Class<*> =
        delegate.getApplicationClass(instance)

    override fun getApplicationClass(clazz: Class<*>): Class<*> =
        delegate.getApplicationClass(clazz)

    override fun <T : HasElement> createRouteTarget(
        routeTargetType: Class<T>,
        event: NavigationEvent
    ): T = delegate.createRouteTarget(routeTargetType, event)

    companion object {
        @JvmStatic
        fun create(delegate: Instantiator): Instantiator {
            return MockInstantiator(delegate)
        }
    }
}

private object ByteBuddyUtils {
    /**
     * Subclasses [baseClass] and overrides [methodName] which will now return [withResult].
     */
    fun overrideMethod(baseClass: Class<*>, methodName: String, withResult: () -> Any?): Class<*> {
        return ByteBuddy().subclass(baseClass)
                .method(ElementMatchers.named(methodName))
                .intercept(MethodCall.call(withResult))
                .make()
                .load(ByteBuddyUtils::class.java.classLoader)
                .loaded
    }
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