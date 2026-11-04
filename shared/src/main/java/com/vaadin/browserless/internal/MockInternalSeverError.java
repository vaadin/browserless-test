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

import java.io.PrintWriter;
import java.io.StringWriter;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.internal.DefaultErrorHandler;

/**
 * Replaces the default {@link InternalServerError} exception handler with an
 * implementation that exposes the error details as element properties, so that
 * they show up in the pretty-printed component tree.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@DefaultErrorHandler
public class MockInternalSeverError extends InternalServerError {

    private static final long serialVersionUID = -5950962994914319392L;

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<Exception> parameter) {
        getElement().setProperty("targetView", event.getLocation().getPath());
        if (parameter.hasCustomMessage()) {
            getElement().setProperty("failureMessage",
                    parameter.getCustomMessage());
        } else {
            getElement().setProperty("failureMessage",
                    parameter.getException().getMessage());
        }
        getElement().setProperty("exceptionType",
                parameter.getException().getClass().getName());
        getElement().setProperty("stackTrace",
                stackTraceToString(parameter.getException()));
        return super.setErrorParameter(event, parameter);
    }

    private static String stackTraceToString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
