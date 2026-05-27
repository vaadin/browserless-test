/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.mocks;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

public class MockServletConfig implements ServletConfig {

    public final ServletContext context;

    public MockServletConfig(ServletContext context) {
        this.context = context;
    }

    /**
     * Per-servlet init parameters.
     */
    public Map<String, String> servletInitParams = new HashMap<>();

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
