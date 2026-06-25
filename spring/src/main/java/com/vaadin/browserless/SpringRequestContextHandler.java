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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.SessionScope;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;

/**
 * Spring implementation of {@link RequestContextHandler}.
 * <p>
 * Binds each browserless user's request to Spring's
 * {@link RequestContextHolder}, so that {@code @SessionScope} beans resolve
 * against the active user's own {@code HttpSession} (each user has its own
 * {@code MockHttpSession}). Without this, a single request is bound to the test
 * thread for the whole test method, and every user reuses the first user's
 * session-scoped instances — see
 * <a href="https://github.com/vaadin/browserless-test/issues/110">#110</a>.
 * <p>
 * Also registers the standard {@code session} web scope on the bean factory
 * when it is missing, so {@code @SessionScope} beans can be resolved in the
 * mock environment even when the Spring test context is not a
 * {@code WebApplicationContext}.
 *
 * @see RequestContextHandler
 * @see SpringBrowserlessApplicationContext
 * @since 1.1
 */
class SpringRequestContextHandler implements RequestContextHandler {

    SpringRequestContextHandler(ApplicationContext applicationContext) {
        registerSessionScopeIfMissing(applicationContext);
    }

    private static void registerSessionScopeIfMissing(ApplicationContext ctx) {
        if (!(ctx instanceof ConfigurableApplicationContext configurable)) {
            return;
        }
        ConfigurableListableBeanFactory beanFactory = configurable
                .getBeanFactory();
        // A real WebApplicationContext already registers this during refresh;
        // only fill the gap for non-web test contexts to avoid replacing the
        // container's own scope instance.
        if (beanFactory.getRegisteredScope(
                WebApplicationContext.SCOPE_SESSION) == null) {
            beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION,
                    new SessionScope());
        }
    }

    @Override
    public Object saveContext() {
        return RequestContextHolder.getRequestAttributes();
    }

    @Override
    public void restoreContext(Object snapshot) {
        // setRequestAttributes(null) resets the binding, matching the
        // null-clears contract.
        RequestContextHolder.setRequestAttributes((RequestAttributes) snapshot);
    }

    @Override
    public void bind(VaadinRequest request) {
        // In the Spring integration the request is always a
        // VaadinServletRequest, which is itself an HttpServletRequest backed by
        // the user's MockHttpSession.
        if (request instanceof VaadinServletRequest servletRequest) {
            RequestContextHolder.setRequestAttributes(
                    new ServletRequestAttributes(servletRequest));
        }
    }

    @Override
    public void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }
}
