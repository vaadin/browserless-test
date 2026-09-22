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
package com.vaadin.browserless.mocks;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link ServletConfig} the mocked servlet is initialized with.
 * <p>
 * It carries the per-servlet init parameters and the {@link ServletContext},
 * which is the whole of what a Vaadin servlet reads from its config. The
 * servlet name is fixed at {@code "Vaadin Servlet"}.
 * <p>
 * Init parameters are read while the servlet initializes, so they have to be in
 * {@link #getServletInitParams()} before {@code Servlet.init(ServletConfig)} is
 * called; setting one afterwards has no effect.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockServletConfig implements ServletConfig {

    private final ServletContext context;

    /**
     * Creates a config for a servlet in the given context, with no init
     * parameters.
     *
     * @param context
     *            the context the servlet belongs to
     */
    public MockServletConfig(ServletContext context) {
        this.context = context;
    }

    /**
     * Returns the context the configured servlet belongs to. Same value as
     * {@link #getServletContext()}.
     *
     * @return the servlet context
     */
    public ServletContext getContext() {
        return context;
    }

    private Map<String, String> servletInitParams = new HashMap<>();

    /**
     * Returns the per-servlet init parameters, live and directly modifiable.
     * Populate before the servlet is initialized.
     *
     * @return the init parameters
     */
    public Map<String, String> getServletInitParams() {
        return servletInitParams;
    }

    /**
     * Replaces the per-servlet init parameters.
     *
     * @param servletInitParams
     *            the init parameters
     */
    public void setServletInitParams(Map<String, String> servletInitParams) {
        this.servletInitParams = servletInitParams;
    }

    @Override
    public String getInitParameter(String name) {
        return servletInitParams.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(servletInitParams.keySet());
    }

    @Override
    public String getServletName() {
        return "Vaadin Servlet";
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }
}
