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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jspecify.annotations.Nullable;

import com.vaadin.browserless.mocks.MockRequest;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.TransferUtil;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadResult;

/**
 * Drives an {@link UploadHandler} headlessly, reproducing what the browser does
 * on a file upload. Shared by the upload component tester and the clipboard
 * file-paste simulation, both of which POST files to a stream-resource URL in a
 * real browser.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.1
 */
public final class UploadSimulation {

    private UploadSimulation() {
    }

    /**
     * Resolves the {@link UploadHandler} registered as the stream resource at
     * the given URL (typically the value of a component's upload-target
     * attribute).
     *
     * @param url
     *            the stream-resource URL, not {@code null}
     * @return the registered upload handler, never {@code null}
     * @throws IllegalStateException
     *             if no upload handler is registered at the URL
     */
    public static UploadHandler resolveUploadHandler(String url) {
        StreamResourceRegistry.ElementStreamResource resource = VaadinSession
                .getCurrent().getResourceRegistry()
                .getResource(StreamResourceRegistry.ElementStreamResource.class,
                        URI.create(url))
                .orElseThrow(() -> new IllegalStateException(
                        "Upload handler is not registered"));
        if (resource
                .getElementRequestHandler() instanceof UploadHandler handler) {
            return handler;
        }
        throw new IllegalStateException("Invalid or null upload handler "
                + resource.getElementRequestHandler());
    }

    /**
     * Invokes the handler once for a single file, as if the browser had POSTed
     * it. A {@code null} {@code content} simulates a failed/aborted upload (the
     * handler's stream throws on read).
     *
     * @param handler
     *            the upload handler to invoke, not {@code null}
     * @param element
     *            the element the upload targets, not {@code null}
     * @param request
     *            the request to expose to the handler (carries any
     *            upload-specific headers), not {@code null}
     * @param fileName
     *            the uploaded file name, not {@code null}
     * @param contentType
     *            the uploaded content type, may be {@code null}
     * @param content
     *            the file bytes, or {@code null} to simulate a failure
     * @throws RuntimeException
     *             propagated from the handler; a failed upload is reported to
     *             the handler via {@link UploadHandler#responseHandled} before
     *             the exception is rethrown
     */
    public static void invokeUpload(UploadHandler handler, Element element,
            VaadinRequest request, String fileName,
            @Nullable String contentType, byte @Nullable [] content) {
        long contentLength;
        InputStream inputStream;
        if (content == null) {
            contentLength = 0L;
            inputStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Simulated upload failure");
                }
            };
        } else {
            contentLength = content.length;
            inputStream = new ByteArrayInputStream(content);
        }

        UploadEvent event = new UploadEvent(request,
                VaadinResponse.getCurrent(), VaadinSession.getCurrent(),
                fileName, contentLength, contentType, element, null) {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }
        };
        try {
            Method method = TransferUtil.class.getDeclaredMethod(
                    "handleUploadRequest", UploadHandler.class,
                    UploadEvent.class);
            method.setAccessible(true);
            method.invoke(null, handler, event);
            handler.responseHandled(
                    new UploadResult(true, VaadinResponse.getCurrent()));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot handle upload request", e);
        } catch (InvocationTargetException e) {
            RuntimeException cause;
            if (e.getCause() instanceof RuntimeException re) {
                cause = re;
            } else if (e.getCause() instanceof IOException ioe) {
                cause = new UncheckedIOException(ioe);
            } else {
                cause = new UncheckedIOException(new IOException(e));
            }
            handler.responseHandled(new UploadResult(false,
                    VaadinResponse.getCurrent(), cause));
            throw cause;
        }
    }

    /**
     * Runs {@code action} with the given request headers temporarily added to
     * the current request, then removes them. Lets callers expose
     * upload-specific headers (e.g. the clipboard paste id and file count) to
     * the {@link UploadHandler}, which reads them via
     * {@code event.getRequest().getHeader(...)}.
     *
     * @param headers
     *            header name/value pairs to add for the duration, not
     *            {@code null}
     * @param action
     *            the action to run with the headers in place, not {@code null}
     */
    public static void withRequestHeaders(Map<String, String> headers,
            Runnable action) {
        MockRequest request = (MockRequest) ((VaadinServletRequest) VaadinRequest
                .getCurrent()).getRequest();
        Map<String, List<String>> requestHeaders = request.getHeaders();
        headers.forEach(
                (name, value) -> requestHeaders.put(name, List.of(value)));
        try {
            action.run();
        } finally {
            headers.keySet().forEach(requestHeaders::remove);
        }
    }

    /**
     * Drains the UI's access queue so upload callbacks running inside
     * {@code UI.access} blocks are executed.
     *
     * @return a {@link RuntimeException} caught while draining, or {@code null}
     *         if none
     */
    public static @Nullable RuntimeException runUIQueue() {
        try {
            MockVaadin.runUIQueue();
        } catch (RuntimeException ex) {
            return ex;
        } catch (Exception ex) {
            // Upload callbacks run in UI.access blocks; purge the queue to
            // ensure listeners are invoked. runUIQueue throws
            // ExecutionException on failure but does not declare it (Kotlin).
            if (ex instanceof ExecutionException) {
                if (ex.getCause() instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new RuntimeException(ex.getCause());
                }
            }
            return new RuntimeException(ex);
        }
        return null;
    }
}
