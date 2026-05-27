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

import static com.vaadin.browserless.mocks.MockUtils.putOrRemove;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

/**
 * A standalone implementation of the [HttpSession] interface.
 */
public class MockHttpSession implements HttpSession, Serializable {

    private final String sessionId;
    private final ServletContext servletContext;
    private final long creationTime;
    private int maxInactiveInterval;

    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public MockHttpSession(String sessionId, ServletContext servletContext, long creationTime, int maxInactiveInterval) {
        this.sessionId = sessionId;
        this.servletContext = servletContext;
        this.creationTime = creationTime;
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public MockHttpSession(HttpSession session) {
        this(session.getId(), session.getServletContext(), session.getLastAccessedTime(), session.getMaxInactiveInterval());
        copyAttributes(session);
    }

    public boolean isValid() {
        return valid.get();
    }

    public void destroy() {
        attributes.clear();
    }

    @Override
    public long getCreationTime() {
        checkValid();
        return creationTime;
    }

    @Override
    public String getId() {
        return sessionId;
    }

    @Override
    public long getLastAccessedTime() {
        checkValid();
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public Object getAttribute(String name) {
        checkValid();
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkValid();
        return attributes.keys();
    }

    @Override
    public void setAttribute(String name, Object value) {
        checkValid();
        putOrRemove(attributes, name, value);
    }

    @Override
    public void removeAttribute(String name) {
        checkValid();
        attributes.remove(name);
    }

    public MockHttpSession copyAttributes(HttpSession httpSession) {
        for (String name : Collections.list(httpSession.getAttributeNames())) {
            attributes.put(name, httpSession.getAttribute(name));
        }
        return this;
    }

    @Override
    public void invalidate() {
        checkValid();
        valid.set(false);
    }

    @Override
    public boolean isNew() {
        checkValid();
        return false;
    }

    private void checkValid() {
        if (!isValid()) {
            throw new IllegalStateException("invalidated: " + this);
        }
    }

    @Override
    public String toString() {
        return "MockHttpSession(sessionId='" + sessionId + "', creationTime=" + creationTime
                + ", maxInactiveInterval=" + maxInactiveInterval + ", attributes=" + attributes
                + ", isValid=" + isValid() + ")";
    }

    private static final AtomicInteger sessionIdGenerator = new AtomicInteger();

    public static MockHttpSession create(ServletContext ctx) {
        return new MockHttpSession(
                Integer.toString(sessionIdGenerator.incrementAndGet()),
                ctx,
                System.currentTimeMillis(),
                30);
    }
}
