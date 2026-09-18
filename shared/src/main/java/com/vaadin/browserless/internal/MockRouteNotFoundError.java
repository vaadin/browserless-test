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
package com.vaadin.browserless.internal;

import java.util.List;
import java.util.StringJoiner;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.internal.DefaultErrorHandler;

/**
 * Replaces Vaadin's {@code RouteNotFoundError} so that navigating to a route
 * that was never registered fails with a message naming the routes that were.
 * <p>
 * Registered by {@link Routes} by default. An application that handles
 * {@link com.vaadin.flow.router.NotFoundException} itself takes precedence: in
 * that case {@link Routes} drops this view, so the application's own error view
 * is what a test sees.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@Tag(Tag.DIV)
@DefaultErrorHandler
public class MockRouteNotFoundError extends RouteNotFoundError {

    /**
     * Creates the error view.
     */
    public MockRouteNotFoundError() {
    }

    /**
     * The exception the navigation failed with, recorded so the test can assert
     * on it.
     */
    private NotFoundException cause = null;

    /**
     * Returns the exception describing the failed navigation, built while
     * handling it.
     *
     * @return the cause, or {@code null} if this view has not handled a
     *         navigation yet
     */
    public NotFoundException getCause() {
        return cause;
    }

    /**
     * Sets the exception describing the failed navigation.
     *
     * @param cause
     *            the cause
     */
    public void setCause(NotFoundException cause) {
        this.cause = cause;
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        StringBuilder sb = new StringBuilder();
        String path = event.getLocation().getPath();
        sb.append("No route found for '").append(path).append("'");
        if (parameter.hasCustomMessage()) {
            sb.append(": ").append(parameter.getCustomMessage());
        }
        sb.append("\nAvailable routes: ");
        List<RouteData> routes = event.getSource().getRegistry()
                .getRegisteredRoutes();
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (RouteData rd : routes) {
            sj.add(toPrettyString(rd));
        }
        sb.append(sj.toString());
        sb.append(
                "\nIf you'd like to revert back to the original Vaadin RouteNotFoundError, please remove the ")
                .append(MockRouteNotFoundError.class)
                .append(" from Routes.errorRoutes");
        NotFoundException nfe = new NotFoundException(sb.toString());
        nfe.initCause(parameter.getCaughtException());
        cause = nfe;
        return super.setErrorParameter(event, parameter);
    }

    private static String toPrettyString(RouteData rd) {
        String template = rd.getTemplate();
        String path = (template == null || template.isBlank()) ? "<root>"
                : "/" + template;
        return rd.getNavigationTarget().getSimpleName() + " at '" + path + "'";
    }
}
