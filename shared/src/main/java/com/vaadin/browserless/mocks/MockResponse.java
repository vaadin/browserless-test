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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class MockResponse implements HttpServletResponse {

    @Override
    public String encodeURL(String url) {
        return url;
    }

    public final ConcurrentHashMap<String, String[]> headers = new ConcurrentHashMap<>();

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, Integer.toString(value));
    }

    public final CopyOnWriteArrayList<Cookie> cookies = new CopyOnWriteArrayList<>();

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public Cookie getCookie(String name) {
        Cookie c = findCookie(name);
        if (c == null) {
            String avail = cookies.stream()
                    .map(it -> it.getName() + "=" + it.getValue())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("no such cookie with name " + name + ". Available cookies: " + avail);
        }
        return c;
    }

    public Cookie findCookie(String name) {
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public void flushBuffer() {
        // not needed at the moment
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public void sendRedirect(String location) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) {
        throw new UnsupportedOperationException("not implemented");
    }

    private int bufferSize = 4096;

    @Override
    public void setBufferSize(int size) {
        this.bufferSize = size;
    }

    private Locale locale = Locale.US;

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        throw new IOException("The app requests a failure: " + sc + " " + msg);
    }

    @Override
    public void sendError(int sc) throws IOException {
        throw new IOException("The app requests a failure: " + sc);
    }

    @Override
    public void setContentLengthLong(long len) {
        // not needed at the moment
    }

    private String characterEncoding = "ISO-8859-1";

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, Long.toString(date));
    }

    @Override
    public void setLocale(Locale loc) {
        this.locale = loc;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        String[] h = headers.get(name);
        return h == null ? Collections.emptyList() : Arrays.asList(h);
    }

    @Override
    public void addHeader(String name, String value) {
        headers.compute(name, (k, v) -> {
            if (v == null) {
                return new String[] { value };
            }
            String[] copy = Arrays.copyOf(v, v.length + 1);
            copy[v.length] = value;
            return copy;
        });
    }

    @Override
    public void setContentLength(int len) {
        // not needed at the moment
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, Long.toString(date));
    }

    private int status = 200;

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
    }

    @Override
    public String getHeader(String name) {
        String[] h = headers.get(name);
        return h == null ? null : h[0];
    }

    private String contentType = null;

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public PrintWriter getWriter() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, Integer.toString(value));
    }

    @Override
    public Collection<String> getHeaderNames() {
        return new LinkedHashSet<>(headers.keySet());
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, new String[] { value });
    }

    @Override
    public ServletOutputStream getOutputStream() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }
}
