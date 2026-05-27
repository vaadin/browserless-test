/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.internal.DefaultErrorHandler;

/**
 * This route gets registered by default in [Routes], so that Karibu-Testing can catch
 * any navigation to a missing route and can respond with an informative exception.
 */
@Tag(Tag.DIV)
@DefaultErrorHandler
public class MockRouteNotFoundError extends RouteNotFoundError {

    public NotFoundException cause = null;

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        StringBuilder sb = new StringBuilder();
        String path = event.getLocation().getPath();
        sb.append("No route found for '").append(path).append("'");
        if (parameter.hasCustomMessage()) {
            sb.append(": ").append(parameter.getCustomMessage());
        }
        sb.append("\nAvailable routes: ");
        List<RouteData> routes = event.getSource().getRegistry().getRegisteredRoutes();
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (RouteData rd : routes) {
            sj.add(toPrettyString(rd));
        }
        sb.append(sj.toString());
        sb.append("\nIf you'd like to revert back to the original Vaadin RouteNotFoundError, please remove the ")
                .append(MockRouteNotFoundError.class)
                .append(" from Routes.errorRoutes");
        NotFoundException nfe = new NotFoundException(sb.toString());
        nfe.initCause(parameter.getCaughtException());
        cause = nfe;
        return super.setErrorParameter(event, parameter);
    }

    private static String toPrettyString(RouteData rd) {
        String template = rd.getTemplate();
        String path = (template == null || template.isBlank()) ? "<root>" : "/" + template;
        return rd.getNavigationTarget().getSimpleName() + " at '" + path + "'";
    }
}
