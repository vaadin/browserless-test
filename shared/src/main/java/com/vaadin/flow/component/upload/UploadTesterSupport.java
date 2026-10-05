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
package com.vaadin.flow.component.upload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.TransferUtil;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadResult;

/**
 * Upload simulation shared by the testers of the upload components: the
 * client-side accept rule, and the delivery of a file to the
 * {@link UploadHandler} registered as the upload target of an element.
 */
final class UploadTesterSupport {

    private UploadTesterSupport() {
    }

    /**
     * Builds the same regular expression the web components derive from the
     * {@code accept} property.
     *
     * @param accept
     *            the value of the {@code accept} property, may be
     *            {@literal null}
     * @return the accept pattern, or {@code null} if no file types are
     *         configured
     */
    static Pattern acceptPattern(String accept) {
        if (accept == null || accept.isBlank()) {
            return null;
        }
        String alternatives = Stream.of(accept.split(",")).map(token -> {
            // Escape regex operators common to mime types
            String processed = token.trim().replaceAll("([+.])", "\\\\$1");
            // Make extension patterns match the end of the file name
            if (processed.startsWith("\\.")) {
                processed = ".*" + processed + "$";
            }
            // Handle star (*) wildcards
            return processed.replace("/*", "/.*");
        }).collect(Collectors.joining("|"));
        return Pattern.compile("^(" + alternatives + ")$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * Checks a file against the accept pattern the way the web components do,
     * matching either the content type or the file name.
     *
     * @param acceptPattern
     *            the accept pattern, {@literal null} to accept every file
     * @param item
     *            the file to check
     * @return whether the file is accepted
     */
    static boolean matchesAccept(Pattern acceptPattern, UploadItem item) {
        return acceptPattern == null
                || acceptPattern.matcher(Objects.toString(item.contentType, ""))
                        .matches()
                || acceptPattern.matcher(item.fileName).matches();
    }

    /**
     * Looks up the upload handler registered as the {@code target} of the given
     * element.
     *
     * @param element
     *            the element owning the upload target
     * @return the upload handler, never {@literal null}
     * @throws IllegalStateException
     *             if no upload handler is registered for the element
     */
    static UploadHandler uploadHandler(Element element) {
        String target = element.getAttribute("target");
        if (target == null) {
            throw new IllegalStateException("Upload handler is not registered");
        }
        StreamResourceRegistry.ElementStreamResource resource = VaadinSession
                .getCurrent().getResourceRegistry()
                .getResource(StreamResourceRegistry.ElementStreamResource.class,
                        URI.create(target))
                .orElseThrow(() -> new IllegalStateException(
                        "Upload handler is not registered"));
        if (resource
                .getElementRequestHandler() instanceof UploadHandler uploadHandler) {
            return uploadHandler;
        }
        throw new IllegalStateException("Invalid or null upload handler "
                + resource.getElementRequestHandler());
    }

    /**
     * Hands a single file to the upload handler the way Flow does for an upload
     * request, updating the status of the item with the outcome.
     *
     * @param item
     *            the file to upload; a file without contents simulates a
     *            transfer that fails while reading
     * @param uploadHandler
     *            the handler to deliver the file to
     * @param owner
     *            the element the upload target belongs to
     * @throws UncheckedIOException
     *             if the handler fails to handle the file contents
     */
    static void deliver(UploadItem item, UploadHandler uploadHandler,
            Element owner) {
        long contentLength;
        InputStream inputStream;
        if (item.contentsProducer == null) {
            contentLength = 0L;
            inputStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Simulated upload failure");
                }
            };
        } else {
            byte[] content = readContents(item.contentsProducer);
            contentLength = content.length;
            inputStream = new ByteArrayInputStream(content);
        }

        UploadEvent event = new UploadEvent(VaadinRequest.getCurrent(),
                VaadinResponse.getCurrent(), VaadinSession.getCurrent(),
                item.fileName, contentLength, item.contentType, owner, null) {
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
            method.invoke(null, uploadHandler, event);
            // Flow validates the accepted mime types and file extensions on
            // the server as well, rejecting the request instead of throwing
            if (event.isRejected()) {
                item.status = UploadTester.UploadStatus.REJECTED;
                item.errorMessage = event.getRejectionMessage();
            } else {
                item.status = UploadTester.UploadStatus.UPLOADED;
            }
            uploadHandler.responseHandled(
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
            item.status = UploadTester.UploadStatus.FAILED;
            item.errorMessage = cause.getMessage();
            uploadHandler.responseHandled(new UploadResult(false,
                    VaadinResponse.getCurrent(), cause));
            throw cause;
        }
    }

    /**
     * Runs the pending {@code UI.access} tasks, in which the upload callbacks
     * are executed.
     *
     * @return the exception thrown by a task, or {@literal null}
     */
    static RuntimeException runUIQueue() {
        try {
            MockVaadin.runUIQueue();
        } catch (RuntimeException ex) {
            return ex;
        } catch (Exception ex) {
            // upload callbacks are executed in UI.access blocks.
            // we need to purge the queue to ensure listeners are
            // invoked
            // runUIQueue throws ExecutionException in case of failure
            // but the method does not declare any thrown exception
            // (kotlin magic)
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

    static byte[] readContents(Callable<byte[]> contentsProducer) {
        byte[] contents;
        try {
            contents = contentsProducer.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (Exception ex) {
            throw new UncheckedIOException(new IOException(ex));
        }
        Objects.requireNonNull(contents, "file contents cannot be null");
        return contents;
    }

    /**
     * Converts the given files into upload items, detecting the content types
     * from the file names.
     *
     * @param files
     *            the files to upload
     * @return the upload items, in the order the files were given
     * @throws IllegalArgumentException
     *             if no file is given
     */
    static List<UploadItem> toItems(Collection<File> files) {
        if (files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one file must be provided");
        }
        return files.stream()
                .map(file -> new UploadItem(file.getName(),
                        URLConnection.guessContentTypeFromName(file.getName()),
                        () -> Files.readAllBytes(file.toPath())))
                .collect(Collectors.toList());
    }

    /**
     * Counts the JavaScript invocations scheduled so far for the given
     * component that match the given condition.
     * <p>
     * Clearing the file list of an upload component has no server side state,
     * so the testers learn about it by observing the invocations.
     * {@code UIInternals} has no public accessor for them, which is why it is
     * read reflectively.
     *
     * @param component
     *            the component the invocations are scheduled for
     * @param condition
     *            the condition the invocation matches
     * @return the number of matching invocations
     */
    @SuppressWarnings("unchecked")
    static int countPendingInvocations(Component component,
            Predicate<JavaScriptInvocation> condition) {
        UI ui = component.getUI().orElse(null);
        if (ui == null) {
            return 0;
        }
        StateNode node = component.getElement().getNode();
        try {
            Method method = UIInternals.class
                    .getDeclaredMethod("getPendingJavaScriptInvocations");
            method.setAccessible(true);
            return (int) ((Stream<PendingJavaScriptInvocation>) method
                    .invoke(ui.getInternals()))
                    .filter(invocation -> invocation.getOwner() == node)
                    .map(PendingJavaScriptInvocation::getInvocation)
                    .filter(condition).count();
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A file handed to a tester for uploading, and what became of it.
     */
    static class UploadItem {
        final String fileName;
        final String contentType;
        Callable<byte[]> contentsProducer;
        UploadTester.UploadStatus status = UploadTester.UploadStatus.PENDING;
        String errorMessage;

        UploadItem(String fileName, String contentType,
                Callable<byte[]> contentsProducer) {
            this.fileName = Objects.requireNonNull(fileName,
                    "fileName cannot be null");
            this.contentType = contentType;
            this.contentsProducer = contentsProducer;
        }

        UploadTester.FileStatus toStatus() {
            return new UploadTester.FileStatus(fileName, status, errorMessage);
        }
    }
}
