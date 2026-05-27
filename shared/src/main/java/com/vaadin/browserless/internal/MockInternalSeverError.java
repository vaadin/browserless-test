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

import java.io.PrintWriter;
import java.io.StringWriter;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.internal.DefaultErrorHandler;

@DefaultErrorHandler
public class MockInternalSeverError extends InternalServerError {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        getElement().setProperty("targetView", event.getLocation().getPath());
        if (parameter.hasCustomMessage()) {
            getElement().setProperty("failureMessage", parameter.getCustomMessage());
        } else {
            getElement().setProperty("failureMessage", parameter.getException().getMessage());
        }
        getElement().setProperty("exceptionType", parameter.getException().getClass().getName());
        StringWriter sw = new StringWriter();
        parameter.getException().printStackTrace(new PrintWriter(sw));
        getElement().setProperty("stackTrace", sw.toString());
        return super.setErrorParameter(event, parameter);
    }
}
