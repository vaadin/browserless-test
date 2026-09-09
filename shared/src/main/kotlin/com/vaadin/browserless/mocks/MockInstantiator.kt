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

/**
 * An [Instantiator] wrapping the one the mocked environment provides.
 *
 * The wrapper does not mock anything any more: the [getOrCreate] special cases
 * it was written for are long gone, so every member simply forwards to
 * [delegate]. The mocked services therefore use the environment's own
 * instantiator directly, and this class is kept for source compatibility only.
 *
 * The forwarding is spelled out by hand on purpose: Kotlin interface delegation
 * only generates forwarders for the abstract members of [Instantiator], so a
 * method Flow declares as a Java `default` would otherwise resolve to that
 * default implementation and never reach the delegate — which is how a Spring
 * `PageTitleGenerator` bean used to be dropped in browserless tests. Since a
 * wrapper can always fall behind a method added to [Instantiator] later,
 * [create] hands back the delegate itself instead of wrapping it.
 *
 * The list of forwarders below is deliberately not guarded by a test: pinning
 * it would fail the build whenever Flow adds a method to [Instantiator], which
 * is not worth it for a class scheduled for removal and used by nothing here.
 * A method missing from the list is therefore silently dropped for callers who
 * construct this class themselves — one more reason not to.
 */
@Deprecated(
    "Wrapping the instantiator of the mocked environment has no effect; use that instantiator directly. Scheduled for removal."
)
open class MockInstantiator(val delegate: Instantiator) : Instantiator by delegate {

    override fun <T : Any?> getOrCreate(type: Class<T>): T = delegate.getOrCreate(type)

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
        /**
         * Returns [delegate] as is: wrapping it changes nothing, and a wrapper
         * risks dropping methods added to [Instantiator] in the future.
         */
        @JvmStatic
        fun create(delegate: Instantiator): Instantiator = delegate
    }
}
