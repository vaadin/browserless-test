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
import java.util.stream.Collectors;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.internal.DefaultErrorHandler;

/**
 * This route gets registered by default in {@link Routes}, so that Browserless
 * Testing can catch any navigation to a missing route and can respond with an
 * informative exception.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@Tag(Tag.DIV)
@DefaultErrorHandler
public class MockRouteNotFoundError extends RouteNotFoundError {

    private static final long serialVersionUID = -4006857065753712996L;

    private NotFoundException cause = null;

    /**
     * Returns the exception describing the failed navigation, set by
     * {@link #setErrorParameter(BeforeEnterEvent, ErrorParameter)}.
     *
     * @return the exception, or null if no navigation failed yet.
     */
    public final NotFoundException getCause() {
        return cause;
    }

    /**
     * Sets the exception describing the failed navigation.
     *
     * @param cause
     *            the exception, may be null.
     */
    public final void setCause(NotFoundException cause) {
        this.cause = cause;
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        StringBuilder message = new StringBuilder();
        String path = event.getLocation().getPath();
        message.append("No route found for '").append(path).append("'");
        if (parameter.hasCustomMessage()) {
            message.append(": ").append(parameter.getCustomMessage());
        }
        message.append("\nAvailable routes: ");
        List<RouteData> routes = event.getSource().getRegistry()
                .getRegisteredRoutes();
        message.append(
                routes.stream().map(MockRouteNotFoundError::toPrettyString)
                        .collect(Collectors.toList()));
        message.append(
                "\nIf you'd like to revert back to the original Vaadin RouteNotFoundError, please remove the "
                        + MockRouteNotFoundError.class
                        + " from Routes.errorRoutes");
        NotFoundException exception = new NotFoundException(message.toString());
        exception.initCause(parameter.getCaughtException());
        cause = exception;
        return super.setErrorParameter(event, parameter);
    }

    private static String toPrettyString(RouteData routeData) {
        String template = routeData.getTemplate();
        String path = Utils.isBlank(template) ? "<root>" : "/" + template;
        return routeData.getNavigationTarget().getSimpleName() + " at '" + path
                + "'";
    }

}
