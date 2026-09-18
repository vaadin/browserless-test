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
 * Replaces Vaadin's {@link InternalServerError} so that an unhandled exception
 * is visible to a test.
 * <p>
 * The stock view logs the exception and renders nothing that names it, which in
 * a browserless test means a failure whose cause is only in the log. This one
 * records the target view, the message, the exception type and the stack trace
 * as element properties, so a lookup that lands on it can report what actually
 * went wrong.
 * <p>
 * Registered in place of {@link InternalServerError} by {@link Routes}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@DefaultErrorHandler
public class MockInternalSeverError extends InternalServerError {

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
        StringWriter sw = new StringWriter();
        parameter.getException().printStackTrace(new PrintWriter(sw));
        getElement().setProperty("stackTrace", sw.toString());
        return super.setErrorParameter(event, parameter);
    }
}
