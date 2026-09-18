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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vaadin.browserless.mocks.MockUtils.putOrRemove;

/**
 * A standalone {@link HttpSession}, backed by an in-memory attribute map.
 * <p>
 * Attribute storage, invalidation and
 * {@link jakarta.servlet.http.HttpServletRequest#changeSessionId()} behave the
 * way a servlet container implements them, including the failure modes: every
 * accessor throws {@link IllegalStateException} once the session has been
 * invalidated. Ids come from a sequential counter rather than being random, so
 * test output stays readable.
 * <p>
 * What is not modeled: the session is never expired on its own, so
 * {@link #getMaxInactiveInterval()} is stored and reported but never acted on,
 * and {@link #getLastAccessedTime()} always reports {@code 0}. There is no
 * request loop to update it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MockHttpSession implements HttpSession, Serializable {

    private volatile String sessionId;
    private final ServletContext servletContext;
    private final long creationTime;
    private int maxInactiveInterval;

    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
    private final AtomicBoolean valid = new AtomicBoolean(true);

    /**
     * Creates a session with the given id and settings.
     *
     * @param sessionId
     *            the session id
     * @param servletContext
     *            the context this session belongs to
     * @param creationTime
     *            the creation timestamp reported by {@link #getCreationTime()}
     * @param maxInactiveInterval
     *            the inactive interval, in seconds; stored and reported, never
     *            acted on
     */
    public MockHttpSession(String sessionId, ServletContext servletContext,
            long creationTime, int maxInactiveInterval) {
        this.sessionId = sessionId;
        this.servletContext = servletContext;
        this.creationTime = creationTime;
        this.maxInactiveInterval = maxInactiveInterval;
    }

    /**
     * Creates a copy of the given session, carrying over its id, context,
     * settings and all of its attributes.
     *
     * @param session
     *            the session to copy
     */
    public MockHttpSession(HttpSession session) {
        this(session.getId(), session.getServletContext(),
                session.getLastAccessedTime(),
                session.getMaxInactiveInterval());
        copyAttributes(session);
    }

    /**
     * Tells whether this session is still usable, i.e. has not been
     * {@link #invalidate() invalidated}.
     *
     * @return {@code true} while the session is valid
     */
    public boolean isValid() {
        return valid.get();
    }

    /**
     * Drops all attributes, leaving the session itself valid.
     */
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

    /**
     * Copies all attributes of the given session into this one, keeping the
     * attributes already present.
     *
     * @param httpSession
     *            the session to copy the attributes from
     * @return this session
     */
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

    /**
     * Assigns a new ID to this session, keeping its identity and all of its
     * attributes intact, and returns the new ID.
     * <p>
     * This mirrors what a servlet container does for
     * {@link jakarta.servlet.http.HttpServletRequest#changeSessionId()}: apps
     * which implement session-fixation protection themselves (vanilla and Java
     * EE apps, i.e. those not relying on Spring Security) call it after a
     * successful login.
     *
     * @return the new session id
     * @throws IllegalStateException
     *             if the session has been invalidated
     */
    public String changeSessionId() {
        checkValid();
        sessionId = newSessionId();
        return sessionId;
    }

    @Override
    public String toString() {
        return "MockHttpSession(sessionId='" + sessionId + "', creationTime="
                + creationTime + ", maxInactiveInterval=" + maxInactiveInterval
                + ", attributes=" + attributes + ", isValid=" + isValid() + ")";
    }

    private static final AtomicInteger sessionIdGenerator = new AtomicInteger();

    private static String newSessionId() {
        return Integer.toString(sessionIdGenerator.incrementAndGet());
    }

    /**
     * Creates a session in the given context, with a fresh sequential id and a
     * 30 second inactive interval.
     *
     * @param ctx
     *            the context the session belongs to
     * @return the new session
     */
    public static MockHttpSession create(ServletContext ctx) {
        return new MockHttpSession(newSessionId(), ctx,
                System.currentTimeMillis(), 30);
    }
}
