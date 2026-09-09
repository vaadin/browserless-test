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

import com.vaadin.flow.di.Instantiator
import com.vaadin.flow.i18n.I18NProvider
import com.vaadin.flow.server.auth.MenuAccessControl
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers

/**
 * Makes sure to load [MockNpmTemplateParser].
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