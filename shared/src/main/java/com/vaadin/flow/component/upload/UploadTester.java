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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.upload.UploadTesterSupport.UploadItem;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.server.communication.streaming.StreamingEndEventImpl;
import com.vaadin.flow.server.communication.streaming.StreamingErrorEventImpl;
import com.vaadin.flow.server.communication.streaming.StreamingStartEventImpl;
import com.vaadin.flow.server.streams.UploadHandler;

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
 * A refused file is no more of an error in the tester than it is in the
 * browser, so uploading does not throw for it. Use
 * {@link #getLastUploadStatus()} to see what became of each file, or
 * {@link #ensureUploaded()} to fail the test unless every file went through.
 * <p>
 * {@code maxFiles} is checked against an emulated file list, so files stay in
 * it between calls just like the entries the browser shows: uploading two files
 * to an {@code Upload} configured with a plain {@link Receiver} rejects the
 * second one, because such an {@code Upload} implicitly sets {@code maxFiles}
 * to one. Use {@link #removeFile(String)} or {@link Upload#clearFileList()} to
 * make room, as the user would.
 * <p>
 * Accepted files are uploaded right away unless
 * {@link Upload#setAutoUpload(boolean) auto upload} is turned off, in which
 * case they wait in the file list, {@link UploadStatus#PENDING}, until the test
 * starts them with {@link #startUpload(String)}, as the user would with the
 * start button of the entry.
 * <p>
 * As in the browser, the same file, or another file with the same name, can be
 * uploaded more than once: every upload adds an entry of its own to the file
 * list, is delivered on its own and counts towards {@code maxFiles}. When
 * several entries share a name, {@link #removeFile(String)} removes the most
 * recently added one, which is the first one the list shows, and
 * {@link #startUpload(String)} starts the most recently added one that still
 * waits to be started. To act on a specific entry, use {@link #removeFile(int)}
 * and {@link #startUpload(int)} with its position in {@link #getFiles()}.
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

    private static final String STATE_KEY = UploadTester.class.getName()
            + ".state";

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
     * the component. With {@link Upload#setAutoUpload(boolean) auto upload}
     * turned off, an accepted file waits in the file list until
     * {@link #startUpload(String)} is called.
     *
     * @param fileName
     *            name of the file to upload
     * @param contentType
     *            content type of the file to upload
     * @param contents
     *            file contents as an array of bytes
     * @throws UncheckedIOException
     *             if the upload component fails to handle file contents
     * @throws IllegalStateException
     *             if the component is not usable
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
     * the component. With {@link Upload#setAutoUpload(boolean) auto upload}
     * turned off, an accepted file waits in the file list until
     * {@link #startUpload(String)} is called.
     *
     * @param fileName
     *            name of the file to upload
     * @param contentType
     *            content type of the file to upload
     * @param contents
     *            file contents as an array of bytes
     * @throws UncheckedIOException
     *             if the upload component fails to handle file contents
     * @throws IllegalStateException
     *             if the component is not usable
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
     * the component. With {@link Upload#setAutoUpload(boolean) auto upload}
     * turned off, an accepted file waits in the file list until
     * {@link #startUpload(String)} is called.
     *
     * @param file
     *            the file to upload
     * @throws UncheckedIOException
     *             if the upload component fails to handle file contents
     * @throws IllegalStateException
     *             if the component is not usable
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
     * handler; the remaining files are uploaded, or wait in the file list when
     * {@link Upload#setAutoUpload(boolean) auto upload} is turned off.
     *
     * @param files
     *            files to upload
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadAll(File... files) {
        uploadAll(List.of(files));
    }

    /**
     * Simulates uploading multiple files at once.
     * <p>
     * Files violating one of the client-side constraints of the component are
     * rejected with a {@code FileRejectedEvent} and never reach the upload
     * handler; the remaining files are uploaded, or wait in the file list when
     * {@link Upload#setAutoUpload(boolean) auto upload} is turned off.
     *
     * @param files
     *            files to upload
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadAll(Collection<File> files) {
        ensureComponentIsUsable();
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
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link Upload#setAutoUpload(boolean) auto upload} is turned off.
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
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link Upload#setAutoUpload(boolean) auto upload} is turned off.
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
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link Upload#setAutoUpload(boolean) auto upload} is turned off.
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
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link Upload#setAutoUpload(boolean) auto upload} is turned off.
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
     * <p>
     * When several entries have the given name, the most recently added one is
     * removed.
     *
     * @param fileName
     *            name of the file to remove, as given when it was uploaded
     * @throws IllegalArgumentException
     *             if the file is not in the upload file list
     * @throws IllegalStateException
     *             if the component is not usable
     * @since 25.3
     */
    public void removeFile(String fileName) {
        UploadItem item = prepare().files.stream()
                .filter(file -> file.fileName.equals(fileName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File '"
                        + fileName
                        + "' is not in the upload file list. Files in the list: "
                        + describeFiles()));
        remove(item);
    }

    /**
     * Simulates the user removing a file from the upload file list, as if
     * clicking the remove button on the file entry.
     * <p>
     * A {@code FileRemovedEvent} is fired and the file stops counting towards
     * {@link Upload#setMaxFiles(int)}.
     * <p>
     * When several entries have the name of the given file, the most recently
     * added one is removed.
     *
     * @param file
     *            the file to remove
     * @throws IllegalArgumentException
     *             if the file is not in the upload file list
     * @throws IllegalStateException
     *             if the component is not usable
     * @since 25.3
     */
    public void removeFile(File file) {
        removeFile(file.getName());
    }

    /**
     * Simulates the user removing the file at the given position of the upload
     * file list, as if clicking the remove button on that entry. This reaches
     * one entry when several share a name.
     * <p>
     * A {@code FileRemovedEvent} is fired and the file stops counting towards
     * {@link Upload#setMaxFiles(int)}.
     *
     * @param index
     *            the position of the file in the list, in the order
     *            {@link #getFiles()} returns them: the most recently added file
     *            first
     * @throws IllegalArgumentException
     *             if there is no file at the position
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void removeFile(int index) {
        remove(getFileAt(prepare(), index));
    }

    /**
     * Simulates the user clicking the start button of a file that waits in the
     * upload file list because {@link Upload#setAutoUpload(boolean) auto
     * upload} is turned off, uploading it.
     * <p>
     * When several waiting entries have the given name, the most recently added
     * one is started.
     *
     * @param fileName
     *            name of the file to start, as given when it was added
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalArgumentException
     *             if no such file waits in the file list
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void startUpload(String fileName) {
        UploadItem item = prepare().files.stream()
                .filter(file -> file.fileName.equals(fileName)
                        && file.status == UploadStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File '"
                        + fileName
                        + "' is not waiting to be uploaded. Files in the list: "
                        + describeFiles()));
        start(item);
    }

    /**
     * Simulates the user clicking the start button of a file that waits in the
     * upload file list because {@link Upload#setAutoUpload(boolean) auto
     * upload} is turned off, uploading it.
     * <p>
     * When several waiting entries have the name of the given file, the most
     * recently added one is started.
     *
     * @param file
     *            the file to start
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalArgumentException
     *             if no such file waits in the file list
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void startUpload(File file) {
        startUpload(file.getName());
    }

    /**
     * Simulates the user clicking the start button of the file at the given
     * position of the upload file list, which waits because
     * {@link Upload#setAutoUpload(boolean) auto upload} is turned off,
     * uploading it. This reaches one entry when several share a name.
     *
     * @param index
     *            the position of the file in the list, in the order
     *            {@link #getFiles()} returns them: the most recently added file
     *            first
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalArgumentException
     *             if there is no file at the position, or it does not wait to
     *             be uploaded
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void startUpload(int index) {
        UploadItem item = getFileAt(prepare(), index);
        if (item.status != UploadStatus.PENDING) {
            throw new IllegalArgumentException("File '" + item.fileName
                    + "' at index " + index
                    + " is not waiting to be uploaded, it is " + item.status);
        }
        start(item);
    }

    /**
     * Gets the files the upload file list shows, in the order the browser shows
     * them: the most recently added file first.
     * <p>
     * A file is {@link UploadStatus#UPLOADED} once the upload handler or
     * receiver has consumed it, {@link UploadStatus#FAILED} or
     * {@link UploadStatus#REJECTED} when its transfer failed or the server
     * refused it, and {@link UploadStatus#PENDING} while it waits to be started
     * because auto upload is turned off. Files the client-side constraints
     * refused never enter the list.
     *
     * @return the files in the upload file list
     */
    public List<FileStatus> getFiles() {
        // Picks up pending clearFileList() calls
        roundTrip();
        return syncedState().files.stream().map(UploadItem::toStatus)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns what happened to each file of the upload last simulated on this
     * component, in the order the files were given, so that a test can check
     * the outcome the same way a user reads the upload file list in the
     * browser.
     * <p>
     * Files are reported as {@link UploadStatus#UPLOADED} once the upload
     * handler or receiver has consumed them, and as
     * {@link UploadStatus#REJECTED} both when the client-side gate refused them
     * and when Flow's server-side accepted type validation did, in which case
     * neither shows up as a thrown exception. A file waiting for
     * {@link #startUpload(String)} because auto upload is turned off is
     * {@link UploadStatus#PENDING}; once started, the last upload is that file.
     *
     * @return the outcome of the last simulated upload, one entry per file, or
     *         an empty list if no upload has been simulated yet
     * @since 25.3
     */
    public List<FileStatus> getLastUploadStatus() {
        return state().lastUpload;
    }

    /**
     * Checks that the upload last simulated on this component delivered every
     * one of its files, and fails otherwise.
     * <p>
     * Convenience for the common case where a test only wants to be sure the
     * files went through: an upload the browser or the server refused is
     * silent, so without this check it would be noticed only later, as a
     * missing side effect.
     *
     * @throws IllegalStateException
     *             if no upload has been simulated on the component, or if any
     *             file of the last upload was not uploaded
     * @see #getLastUploadStatus()
     * @since 25.3
     */
    public void ensureUploaded() {
        UploadTesterSupport.ensureUploaded(getLastUploadStatus(),
                "this Upload component");
    }

    /**
     * Checks that at least one file of the upload last simulated on this
     * component failed or was rejected, and fails otherwise.
     * <p>
     * Counterpart of {@link #ensureUploaded()} for a test about the failure
     * case, such as an upload handler that throws or a file the component
     * refuses. Use {@link #getLastUploadStatus()} to check which file failed
     * and why.
     * <p>
     * A file left {@link UploadTester.UploadStatus#PENDING} does not count as
     * failed: it was neither delivered nor refused, such as a file following
     * one whose upload threw or a file waiting because auto upload is turned
     * off.
     *
     * @throws IllegalStateException
     *             if no upload has been simulated on the component, or if no
     *             file of the last upload failed or was rejected
     * @see #getLastUploadStatus()
     */
    public void ensureUploadFailed() {
        UploadTesterSupport.ensureUploadFailed(getLastUploadStatus(),
                "this Upload component");
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
        List<UploadItem> items = List
                .of(new UploadItem(fileName, contentType, null));
        List<UploadItem> accepted = acceptFiles(items);
        if (accepted.isEmpty()) {
            // the file never entered the file list, so there is nothing to
            // fail and nothing to remove from it
            recordUploadStatus(items);
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
            // the upload is a simulated failure whatever the handler or the
            // receiver made of the broken stream
            accepted.forEach(item -> item.status = UploadStatus.FAILED);
            recordUploadStatus(items);
            if (removeFromFileList) {
                accepted.forEach(this::remove);
            }
        }
    }

    private void doUpload(List<UploadItem> items) {
        List<UploadItem> accepted = acceptFiles(items);
        try {
            if (getComponent().isAutoUpload() && !accepted.isEmpty()) {
                deliver(accepted);
            }
        } finally {
            recordUploadStatus(items);
        }
    }

    private void start(UploadItem item) {
        try {
            deliver(List.of(item));
        } finally {
            recordUploadStatus(List.of(item));
        }
    }

    private void remove(UploadItem item) {
        state().files.remove(item);
        fireFileRemoved(item.fileName);
    }

    /**
     * Prepares a user action on an entry of the file list.
     *
     * @return the emulated state, synchronized with pending
     *         {@link Upload#clearFileList()} calls
     */
    private UploadState prepare() {
        ensureComponentIsUsable();
        // Picks up pending clearFileList() calls
        roundTrip();
        return syncedState();
    }

    private UploadItem getFileAt(UploadState state, int index) {
        if (index < 0 || index >= state.files.size()) {
            throw new IllegalArgumentException("No file at index " + index
                    + ". Files in the list: " + describeFiles());
        }
        return state.files.get(index);
    }

    private String describeFiles() {
        return state().files.stream().map(file -> file.fileName)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private void recordUploadStatus(List<UploadItem> items) {
        state().lastUpload = items.stream().map(UploadItem::toStatus)
                .collect(Collectors.toUnmodifiableList());
        // Only a file waiting to be started needs its contents, release the
        // others so the file list does not keep every upload in memory
        items.stream().filter(item -> item.status != UploadStatus.PENDING)
                .forEach(item -> item.contentsProducer = null);
    }

    private void deliver(Collection<UploadItem> items) {
        if (useLegacyAPI()) {
            doLegacyUpload(items);
        } else {
            Element element = getComponent().getElement();
            UploadHandler uploadHandler = UploadTesterSupport
                    .uploadHandler(element);
            RuntimeException caughtException;
            try {
                items.forEach(item -> UploadTesterSupport.deliver(item,
                        uploadHandler, element));
            } finally {
                caughtException = UploadTesterSupport.runUIQueue();
                fireAllFinish();
            }
            if (caughtException != null) {
                throw caughtException;
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
    private List<UploadItem> acceptFiles(List<UploadItem> items) {
        ensureComponentIsUsable();
        // A round trip is necessary to ensure upload handler registration and
        // to pick up pending clearFileList() calls
        roundTrip();
        UploadState state = syncedState();
        // Read all contents before touching the file list, so that a file that
        // cannot be read leaves the list unchanged
        List<Long> sizes = new ArrayList<>();
        for (UploadItem item : items) {
            long size = 0;
            if (item.contentsProducer != null) {
                byte[] contents = UploadTesterSupport
                        .readContents(item.contentsProducer);
                size = contents.length;
                // Cache the contents, they are read again on delivery
                item.contentsProducer = () -> contents;
            }
            sizes.add(size);
        }
        List<UploadItem> accepted = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            UploadItem item = items.get(i);
            if (accept(state, item, sizes.get(i))) {
                // the web component shows the newest file first
                state.files.add(0, item);
                accepted.add(item);
            }
        }
        return accepted;
    }

    private boolean accept(UploadState state, UploadItem item, long size) {
        // A limit that has never been set is Infinity on the client, whereas
        // the Upload getters report it as zero, so the property itself decides
        // whether the limit applies. This keeps setMaxFiles(0) meaning "reject
        // everything", as it does in the browser.
        if (hasProperty("maxFiles")
                && state.files.size() >= getComponent().getMaxFiles()) {
            reject(item, errorMessage(UploadI18N.Error::getTooManyFiles,
                    DEFAULT_TOO_MANY_FILES));
            return false;
        }
        int maxFileSize = getComponent().getMaxFileSize();
        if (hasProperty("maxFileSize") && maxFileSize >= 0
                && size > maxFileSize) {
            reject(item, errorMessage(UploadI18N.Error::getFileIsTooBig,
                    DEFAULT_FILE_IS_TOO_BIG));
            return false;
        }
        if (!UploadTesterSupport.matchesAccept(acceptPattern(), item)) {
            reject(item, errorMessage(UploadI18N.Error::getIncorrectFileType,
                    DEFAULT_INCORRECT_FILE_TYPE));
            return false;
        }
        return true;
    }

    private void reject(UploadItem item, String errorMessage) {
        item.status = UploadStatus.REJECTED;
        item.errorMessage = errorMessage;
        fireFileRejected(item.fileName, errorMessage);
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
        return UploadTesterSupport.acceptPattern(
                getComponent().getElement().getProperty("accept"));
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
     * Returns the emulated client-side state of the wrapped component.
     * <p>
     * The state is stored on the component, not on the tester, because testers
     * and locators are short-lived wrappers around a component.
     *
     * @return the state of the wrapped component, never {@literal null}
     */
    private UploadState state() {
        UploadState state = (UploadState) ComponentUtil.getData(getComponent(),
                STATE_KEY);
        if (state == null) {
            state = new UploadState();
            ComponentUtil.setData(getComponent(), STATE_KEY, state);
        }
        return state;
    }

    /**
     * Returns the emulated client-side state, with the file list synchronized
     * with pending {@link Upload#clearFileList()} calls.
     *
     * @return the state of the wrapped component, never {@literal null}
     */
    private UploadState syncedState() {
        UploadState state = state();
        if (state.clearFileListCalls.hasNewInvocations(getComponent(),
                invocation -> invocation.getExpression()
                        .contains(CLEAR_FILE_LIST_EXPRESSION))) {
            state.files.clear();
        }
        return state;
    }

    private boolean useLegacyAPI() {
        return getComponent().getReceiver() != null;
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

        byte[] contents = UploadTesterSupport.readContents(contentsProducer);

        try {
            streamVariable.streamingStarted(new StreamingStartEventImpl(
                    fileName, contentType, contents.length));
            streamVariable.getOutputStream().write(contents);
            item.status = UploadStatus.UPLOADED;
            return Optional.of(new StreamingEndEventImpl(fileName, contentType,
                    contents.length));
        } catch (IOException ex) {
            item.status = UploadStatus.FAILED;
            item.errorMessage = ex.getMessage();
            errorHandler.accept(ex);
        } catch (Exception ex) {
            item.status = UploadStatus.FAILED;
            item.errorMessage = ex.getMessage();
            errorHandler.accept(ex);
            return Optional.of(new StreamingErrorEventImpl(fileName,
                    contentType, contents.length, 0, ex));
        }
        return Optional.empty();
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

    /**
     * Emulation of the file list the {@code vaadin-upload} web component keeps
     * on the client, against which {@link Upload#setMaxFiles(int)} is checked
     * and in which files wait while auto upload is turned off. The newest file
     * comes first, as the browser shows it.
     */
    private static class UploadState {
        private final List<UploadItem> files = new ArrayList<>();
        private final UploadTesterSupport.InvocationTracker clearFileListCalls = new UploadTesterSupport.InvocationTracker();
        private List<FileStatus> lastUpload = List.of();
    }

    /**
     * What became of a file handed to the tester for uploading.
     * 
     * @since 25.3
     */
    public enum UploadStatus {
        /**
         * The file was consumed by the upload handler or receiver.
         */
        UPLOADED,
        /**
         * The file never reached the upload handler or receiver, because either
         * the client-side constraints of the component or Flow's server-side
         * accepted type validation refused it. A {@code FileRejectedEvent} is
         * fired for the former.
         */
        REJECTED,
        /**
         * The file was accepted but its transfer failed.
         */
        FAILED,
        /**
         * The file was accepted but its transfer never concluded. That happens
         * when the upload itself threw, for instance because no upload handler
         * is configured, and for a file added with auto upload turned off,
         * which waits in the file list until the user starts it.
         */
        PENDING
    }

    /**
     * Outcome of a single file of an upload simulated by the tester.
     *
     * @param fileName
     *            name of the file
     * @param status
     *            what became of the file
     * @param errorMessage
     *            the rejection or failure message, {@literal null} for an
     *            uploaded file
     * @since 25.3
     */
    public record FileStatus(String fileName, UploadStatus status,
            String errorMessage) implements Serializable {

        String describe() {
            return fileName + " (" + status
                    + (errorMessage == null ? "" : ": " + errorMessage) + ")";
        }
    }

}
