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
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.TransferUtil;
import com.vaadin.flow.server.communication.streaming.StreamingEndEventImpl;
import com.vaadin.flow.server.communication.streaming.StreamingErrorEventImpl;
import com.vaadin.flow.server.communication.streaming.StreamingStartEventImpl;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadResult;

/**
 * Tester for Upload components.
 * <p>
 * Files handed to {@link #upload(File)} and friends go through the same
 * client-side gate the {@code vaadin-upload} web component applies before it
 * sends anything to the server: {@link Upload#setMaxFiles(int) maxFiles},
 * {@link Upload#setMaxFileSize(int) maxFileSize} and the accepted file types
 * are checked in that order, and a file failing any of them is not delivered to
 * the upload handler or receiver. Instead a
 * {@link Upload#addFileRejectedListener(com.vaadin.flow.component.ComponentEventListener)
 * FileRejectedEvent} is fired, exactly as it would be in a browser.
 * <p>
 * {@code maxFiles} is checked against an emulated file list, so files stay in
 * it between calls just like the entries the browser shows: uploading two files
 * to an {@code Upload} configured with a plain {@link Receiver} rejects the
 * second one, because such an {@code Upload} implicitly sets {@code maxFiles}
 * to one. Use {@link #removeFile(String)} or {@link Upload#clearFileList()} to
 * make room, as the user would.
 * <p>
 * The accepted file types are checked the way the web component does, against
 * the file name or the content type. That is a laxer rule than the server side
 * validation Flow applies to {@link Upload#setAcceptedMimeTypes(String...)} and
 * {@link Upload#setAcceptedFileExtensions(String...)}, which the file has to
 * pass as well before it reaches the upload handler.
 *
 * @param <T>
 *            the component type.
 * @since 1.0
 */
@Tests(Upload.class)
public class UploadTester<T extends Upload> extends ComponentTester<T> {

    /**
     * Expression executed by {@link Upload#clearFileList()}. Clearing the file
     * list has no server side state, so the emulated file list is synchronized
     * by observing the pending JavaScript invocations.
     */
    private static final String CLEAR_FILE_LIST_EXPRESSION = "this.files = [];";

    private static final String FILE_LIST_KEY = UploadTester.class.getName()
            + ".fileList";

    // Defaults of the vaadin-upload web component, used when the application
    // has not provided an UploadI18N of its own.
    private static final String DEFAULT_TOO_MANY_FILES = "Too Many Files.";
    private static final String DEFAULT_FILE_IS_TOO_BIG = "File is Too Big.";
    private static final String DEFAULT_INCORRECT_FILE_TYPE = "Incorrect File Type.";

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public UploadTester(T component) {
        super(component);
    }

    /**
     * Send the file data to the Upload component as if it is uploaded in the
     * browser.
     * <p>
     * The file is rejected with a {@code FileRejectedEvent}, and never reaches
     * the upload handler, if it violates one of the client-side constraints of
     * the component.
     *
     * @param fileName
     *            name of the file to upload
     * @param contentType
     *            content type of the file to upload
     * @param contents
     *            file contents as an array of bytes
     * @throws UncheckedIOException
     *             if the upload component fails to handle file contents
     */
    public void upload(String fileName, String contentType,
            InputStream contents) {
        doUpload(List.of(
                new UploadItem(fileName, contentType, contents::readAllBytes)));
    }

    /**
     * Send the file data to the Upload component as if it is uploaded in the
     * browser.
     * <p>
     * The file is rejected with a {@code FileRejectedEvent}, and never reaches
     * the upload handler, if it violates one of the client-side constraints of
     * the component.
     *
     * @param fileName
     *            name of the file to upload
     * @param contentType
     *            content type of the file to upload
     * @param contents
     *            file contents as an array of bytes
     * @throws UncheckedIOException
     *             if the upload component fails to handle file contents
     */
    public void upload(String fileName, String contentType, byte[] contents) {
        doUpload(
                List.of(new UploadItem(fileName, contentType, () -> contents)));
    }

    /**
     * Send the file data to the Upload component as if it is uploaded in the
     * browser.
     *
     * The content type is detected from file name.
     * <p>
     * The file is rejected with a {@code FileRejectedEvent}, and never reaches
     * the upload handler, if it violates one of the client-side constraints of
     * the component.
     *
     * @param file
     *            the file to upload
     * @throws UncheckedIOException
     *             if the upload component fails to handle file contents
     */
    public void upload(File file) {
        doUpload(List.of(new UploadItem(file.getName(),
                URLConnection.guessContentTypeFromName(file.getName()),
                () -> Files.readAllBytes(file.toPath()))));
    }

    /**
     * Simulates uploading multiple files at once.
     * <p>
     * Files violating one of the client-side constraints of the component are
     * rejected with a {@code FileRejectedEvent} and never reach the upload
     * handler; the remaining files are uploaded.
     *
     * @param files
     *            files to upload
     */
    public void uploadAll(File... files) {
        uploadAll(List.of(files));
    }

    /**
     * Simulates uploading multiple files at once.
     * <p>
     * Files violating one of the client-side constraints of the component are
     * rejected with a {@code FileRejectedEvent} and never reach the upload
     * handler; the remaining files are uploaded.
     *
     * @param files
     *            files to upload
     */
    public void uploadAll(Collection<File> files) {
        Receiver receiver = getComponent().getReceiver();
        if (receiver != null && !(receiver instanceof MultiFileReceiver)) {
            throw new IllegalStateException(
                    "Upload component is not configured with a MultiFileReceiver");
        }
        if (files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one file must be provided");
        }
        doUpload(files.stream()
                .map(f -> new UploadItem(f.getName(),
                        URLConnection.guessContentTypeFromName(f.getName()),
                        () -> Files.readAllBytes(f.toPath())))
                .collect(Collectors.toList()));
    }

    /**
     * Simulates upload interruption by user on browser.
     * <p>
     * As in the browser, the interrupted file is dropped from the file list and
     * a {@code FileRemovedEvent} is fired, freeing a slot when
     * {@link Upload#setMaxFiles(int)} is in use.
     *
     * @param fileName
     *            name of uploading file
     * @param contentType
     *            content type of the uploading file
     */
    public void uploadAborted(String fileName, String contentType) {
        doFailUpload(fileName, contentType, true);
    }

    /**
     * Simulates upload interruption by user on browser.
     * <p>
     * As in the browser, the interrupted file is dropped from the file list and
     * a {@code FileRemovedEvent} is fired, freeing a slot when
     * {@link Upload#setMaxFiles(int)} is in use.
     *
     * @param file
     *            uploading file
     */
    public void uploadAborted(File file) {
        uploadAborted(file.getName(),
                URLConnection.guessContentTypeFromName(file.getName()));
    }

    /**
     * Simulates a failure during file upload.
     * <p>
     * As in the browser, a file whose upload failed stays in the file list.
     *
     * @param file
     *            uploading file
     */
    public void uploadFailed(File file) {
        uploadFailed(file.getName(),
                URLConnection.guessContentTypeFromName(file.getName()));
    }

    /**
     * Simulates a failure during file upload.
     * <p>
     * As in the browser, a file whose upload failed stays in the file list.
     *
     * @param fileName
     *            name of uploading file
     * @param contentType
     *            content type of the uploading file
     */
    public void uploadFailed(String fileName, String contentType) {
        doFailUpload(fileName, contentType, false);
    }

    /**
     * Simulates the user removing a file from the upload file list, as if
     * clicking the remove button on the file entry.
     * <p>
     * A {@code FileRemovedEvent} is fired and the file stops counting towards
     * {@link Upload#setMaxFiles(int)}.
     *
     * @param fileName
     *            name of the file to remove, as given when it was uploaded
     * @throws IllegalArgumentException
     *             if the file is not in the upload file list
     */
    public void removeFile(String fileName) {
        ensureComponentIsUsable();
        // Flushes a potential pending clearFileList() call
        roundTrip();
        FileList fileList = fileList();
        if (!fileList.fileNames.remove(fileName)) {
            throw new IllegalArgumentException("File '" + fileName
                    + "' is not in the upload file list. Files in the list: "
                    + fileList.fileNames);
        }
        fireFileRemoved(fileName);
    }

    /**
     * Simulates the user removing a file from the upload file list, as if
     * clicking the remove button on the file entry.
     * <p>
     * A {@code FileRemovedEvent} is fired and the file stops counting towards
     * {@link Upload#setMaxFiles(int)}.
     *
     * @param file
     *            the file to remove
     * @throws IllegalArgumentException
     *             if the file is not in the upload file list
     */
    public void removeFile(File file) {
        removeFile(file.getName());
    }

    private void fireAllFinish() {
        try {
            getMethod("fireAllFinish").invoke(getComponent());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void doFailUpload(String fileName, String contentType,
            boolean removeFromFileList) {
        List<UploadItem> accepted = acceptFiles(
                List.of(new UploadItem(fileName, contentType, null)));
        if (accepted.isEmpty()) {
            return;
        }
        try {
            if (useLegacyAPI()) {
                StreamVariable streamVariable = getGetStreamVariable();
                try {
                    streamVariable.streamingStarted(new StreamingStartEventImpl(
                            fileName, contentType, 0));
                    streamVariable.streamingFailed(new StreamingErrorEventImpl(
                            fileName, contentType, 0, 0, null));
                } finally {
                    fireAllFinish();
                }
            } else {
                try {
                    deliver(accepted);
                } catch (UncheckedIOException ex) {
                    // an exception is expected since we are simulating an error
                }
            }
        } finally {
            if (removeFromFileList && fileList().fileNames.remove(fileName)) {
                fireFileRemoved(fileName);
            }
        }
    }

    private void doUpload(Collection<UploadItem> items) {
        List<UploadItem> accepted = acceptFiles(items);
        if (!accepted.isEmpty()) {
            deliver(accepted);
        }
    }

    private void deliver(Collection<UploadItem> items) {
        if (useLegacyAPI()) {
            doLegacyUpload(items);
        } else {
            var target = getComponent().getElement().getAttribute("target");
            StreamResourceRegistry.ElementStreamResource resource = VaadinSession
                    .getCurrent().getResourceRegistry()
                    .getResource(
                            StreamResourceRegistry.ElementStreamResource.class,
                            URI.create(target))
                    .orElseThrow(() -> new IllegalStateException(
                            "Upload handler is not registered"));
            if (resource
                    .getElementRequestHandler() instanceof UploadHandler uploadHandler) {
                RuntimeException caughtException;
                try {
                    items.forEach(item -> doUpload(item, uploadHandler));
                } finally {
                    caughtException = runUIQueue();
                    fireAllFinish();
                }
                if (caughtException != null) {
                    throw caughtException;
                }
            } else {
                throw new IllegalStateException(
                        "Invalid or null upload handler "
                                + resource.getElementRequestHandler());
            }
        }
    }

    /**
     * Applies the constraints the {@code vaadin-upload} web component checks
     * before a file is added to the file list, firing a
     * {@code FileRejectedEvent} for every rejected file.
     *
     * @param items
     *            the files the test wants to upload
     * @return the files that passed the client-side gate, with their contents
     *         already resolved
     */
    private List<UploadItem> acceptFiles(Collection<UploadItem> items) {
        ensureComponentIsUsable();
        // A round trip is necessary to ensure upload handler registration and
        // to pick up pending clearFileList() calls
        roundTrip();
        FileList fileList = fileList();
        List<UploadItem> accepted = new ArrayList<>();
        for (UploadItem item : items) {
            UploadItem resolved = item;
            long size = 0;
            if (item.contentsProducer != null) {
                byte[] contents = getUploadedItemContent(item.contentsProducer);
                size = contents.length;
                resolved = new UploadItem(item.fileName, item.contentType,
                        () -> contents);
            }
            if (accept(fileList, resolved, size)) {
                fileList.fileNames.add(resolved.fileName);
                accepted.add(resolved);
            }
        }
        return accepted;
    }

    private boolean accept(FileList fileList, UploadItem item, long size) {
        // A limit that has never been set is Infinity on the client, whereas
        // the Upload getters report it as zero, so the property itself decides
        // whether the limit applies. This keeps setMaxFiles(0) meaning "reject
        // everything", as it does in the browser.
        if (hasProperty("maxFiles")
                && fileList.fileNames.size() >= getComponent().getMaxFiles()) {
            fireFileRejected(item.fileName, errorMessage(
                    UploadI18N.Error::getTooManyFiles, DEFAULT_TOO_MANY_FILES));
            return false;
        }
        int maxFileSize = getComponent().getMaxFileSize();
        if (hasProperty("maxFileSize") && maxFileSize >= 0
                && size > maxFileSize) {
            fireFileRejected(item.fileName,
                    errorMessage(UploadI18N.Error::getFileIsTooBig,
                            DEFAULT_FILE_IS_TOO_BIG));
            return false;
        }
        Pattern acceptPattern = acceptPattern();
        if (acceptPattern != null && !acceptPattern
                .matcher(Objects.toString(item.contentType, "")).matches()
                && !acceptPattern.matcher(item.fileName).matches()) {
            fireFileRejected(item.fileName,
                    errorMessage(UploadI18N.Error::getIncorrectFileType,
                            DEFAULT_INCORRECT_FILE_TYPE));
            return false;
        }
        return true;
    }

    private boolean hasProperty(String name) {
        return getComponent().getElement().hasProperty(name);
    }

    /**
     * Builds the same regular expression the web component derives from the
     * {@code accept} property, which is where
     * {@link Upload#setAcceptedFileTypes(String...)},
     * {@link Upload#setAcceptedMimeTypes(String...)} and
     * {@link Upload#setAcceptedFileExtensions(String...)} all end up.
     *
     * @return the accept pattern, or {@code null} if no file types are
     *         configured
     */
    private Pattern acceptPattern() {
        String accept = getComponent().getElement().getProperty("accept");
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

    private String errorMessage(Function<UploadI18N.Error, String> getter,
            String defaultMessage) {
        UploadI18N i18n = getComponent().getI18n();
        if (i18n != null && i18n.getError() != null) {
            String message = getter.apply(i18n.getError());
            if (message != null) {
                return message;
            }
        }
        return defaultMessage;
    }

    private void fireFileRejected(String fileName, String errorMessage) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.error", errorMessage);
        eventData.put("event.detail.file.name", fileName);
        fireDomEvent("file-reject", eventData);
    }

    private void fireFileRemoved(String fileName) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.file.name", fileName);
        fireDomEvent("file-remove", eventData);
    }

    /**
     * Returns the emulated client-side file list, synchronized with pending
     * {@link Upload#clearFileList()} calls.
     * <p>
     * The list is stored on the component, not on the tester, because testers
     * and locators are short-lived wrappers around a component.
     *
     * @return the file list of the wrapped component, never {@literal null}
     */
    private FileList fileList() {
        FileList fileList = (FileList) ComponentUtil.getData(getComponent(),
                FILE_LIST_KEY);
        if (fileList == null) {
            fileList = new FileList();
            ComponentUtil.setData(getComponent(), FILE_LIST_KEY, fileList);
        }
        int clearCount = countClearFileListInvocations();
        if (clearCount > fileList.observedClearCount) {
            fileList.fileNames.clear();
        }
        fileList.observedClearCount = clearCount;
        return fileList;
    }

    @SuppressWarnings("unchecked")
    private int countClearFileListInvocations() {
        UI ui = getComponent().getUI().orElse(null);
        if (ui == null) {
            return 0;
        }
        StateNode node = getComponent().getElement().getNode();
        Method method = getMethod(UIInternals.class,
                "getPendingJavaScriptInvocations");
        try {
            return (int) ((Stream<PendingJavaScriptInvocation>) method
                    .invoke(ui.getInternals()))
                    .filter(invocation -> invocation.getOwner() == node)
                    .filter(invocation -> invocation.getInvocation()
                            .getExpression()
                            .contains(CLEAR_FILE_LIST_EXPRESSION))
                    .count();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean useLegacyAPI() {
        return getComponent().getReceiver() != null;
    }

    private RuntimeException runUIQueue() {
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

    private void doUpload(UploadItem item, UploadHandler uploadHandler) {
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
            byte[] content = getUploadedItemContent(item.contentsProducer);
            contentLength = content.length;
            inputStream = new ByteArrayInputStream(content);
        }

        UploadEvent event = new UploadEvent(VaadinRequest.getCurrent(),
                VaadinResponse.getCurrent(), VaadinSession.getCurrent(),
                item.fileName, contentLength, item.contentType,
                getComponent().getElement(), null) {
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
            uploadHandler.responseHandled(new UploadResult(false,
                    VaadinResponse.getCurrent(), cause));
            throw cause;
        }
    }

    private void doLegacyUpload(Collection<UploadItem> items) {
        try {
            StreamVariable streamVariable = getGetStreamVariable();
            AtomicReference<Exception> errorCollector = new AtomicReference<>();
            // Collect all upload related events. They will be fired after all
            // items completed because otherwise the current uploading count is
            // decremented too early and check with max file size may fail
            List<StreamVariable.StreamingEvent> events = items.stream()
                    .map(item -> doUpload(streamVariable, item,
                            ex -> errorCollector.compareAndSet(null, ex)))
                    .filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());

            events.forEach(ev -> handleUploadResult(streamVariable, ev));

            Exception ex = errorCollector.get();
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else if (ex instanceof IOException) {
                throw new UncheckedIOException((IOException) ex);
            } else if (ex != null) {
                throw new RuntimeException(ex);
            }
        } finally {
            fireAllFinish();
        }
    }

    private Optional<StreamVariable.StreamingEvent> doUpload(
            StreamVariable streamVariable, UploadItem item,
            Consumer<Exception> errorHandler) {
        String fileName = item.fileName;
        String contentType = item.contentType;
        Callable<byte[]> contentsProducer = item.contentsProducer;

        byte[] contents = getUploadedItemContent(contentsProducer);

        try {
            streamVariable.streamingStarted(new StreamingStartEventImpl(
                    fileName, contentType, contents.length));
            streamVariable.getOutputStream().write(contents);
            return Optional.of(new StreamingEndEventImpl(fileName, contentType,
                    contents.length));
        } catch (IOException ex) {
            errorHandler.accept(ex);
        } catch (Exception ex) {
            errorHandler.accept(ex);
            return Optional.of(new StreamingErrorEventImpl(fileName,
                    contentType, contents.length, 0, ex));
        }
        return Optional.empty();
    }

    private static byte[] getUploadedItemContent(
            Callable<byte[]> contentsProducer) {
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

    private void handleUploadResult(StreamVariable streamVariable,
            StreamVariable.StreamingEvent event) {
        if (event instanceof StreamVariable.StreamingErrorEvent) {
            streamVariable.streamingFailed(
                    (StreamVariable.StreamingErrorEvent) event);
        } else if (event instanceof StreamVariable.StreamingEndEvent) {
            streamVariable.streamingFinished(
                    (StreamVariable.StreamingEndEvent) event);
        }
    }

    private StreamVariable getGetStreamVariable() {
        try {
            return (StreamVariable) getMethod("getStreamVariable")
                    .invoke(getComponent());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static class UploadItem {
        private final String fileName;
        private final String contentType;
        private final Callable<byte[]> contentsProducer;

        UploadItem(String fileName, String contentType,
                Callable<byte[]> contentsProducer) {
            this.fileName = Objects.requireNonNull(fileName,
                    "fileName cannot be null");
            this.contentType = contentType;
            this.contentsProducer = contentsProducer;
        }
    }

    /**
     * Emulation of the file list the {@code vaadin-upload} web component keeps
     * on the client, against which {@link Upload#setMaxFiles(int)} is checked.
     */
    private static class FileList implements Serializable {
        private final List<String> fileNames = new ArrayList<>();
        private int observedClearCount;
    }

}
