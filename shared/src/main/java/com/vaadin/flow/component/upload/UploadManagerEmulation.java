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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.upload.UploadTesterSupport.UploadItem;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.server.streams.UploadHandler;

/**
 * Emulation of the client-side upload manager an {@link UploadManager} drives
 * in the browser, shared by the testers of the components linked to it:
 * {@link UploadButton}, {@link UploadDropZone} and {@link UploadFileList}.
 * <p>
 * Files go through the same gate the client-side manager applies before it adds
 * a file to its file list: {@link UploadManager#setMaxFiles(int) maxFiles},
 * {@link UploadManager#setMaxFileSize(long) maxFileSize} and the accepted file
 * types are checked in that order, and a rejected file fires a
 * {@link UploadManager.FileRejectedEvent} instead of reaching the upload
 * handler. Accepted files are uploaded right away unless
 * {@link UploadManager#setAutoUpload(boolean) auto upload} is turned off, in
 * which case they wait in the file list until the user starts them.
 * <p>
 * The file list is stored on the manager's connector, not on a tester, because
 * every component linked to the manager shares it and testers are short-lived
 * wrappers.
 */
final class UploadManagerEmulation {

    /**
     * The client-side function {@link UploadManager#clearFileList()} calls.
     * Clearing the file list has no server side state, so the emulated file
     * list is synchronized by observing the pending JavaScript invocations.
     */
    private static final String CLEAR_FILE_LIST_FUNCTION = "clearFileList";

    // Error codes the client-side manager reports for a rejected file
    private static final String TOO_MANY_FILES = "tooManyFiles";
    private static final String FILE_IS_TOO_BIG = "fileIsTooBig";
    private static final String INCORRECT_FILE_TYPE = "incorrectFileType";

    private final UploadManager manager;
    private final Component connector;

    private UploadManagerEmulation(UploadManager manager) {
        this.manager = manager;
        this.connector = manager.getConnector();
    }

    /**
     * Gets the emulation of the upload manager linked to the given component.
     *
     * @param component
     *            a component linked to an upload manager
     * @return the emulation of the linked manager, or an empty optional if the
     *         component is not linked to any
     */
    static Optional<UploadManagerEmulation> of(HasUploadManager component) {
        return Optional.ofNullable(component.getUploadManager())
                .map(UploadManagerEmulation::new);
    }

    /**
     * Gets the emulation of the upload manager linked to the given component,
     * failing if there is none.
     *
     * @param component
     *            a component linked to an upload manager
     * @return the emulation of the linked manager
     * @throws IllegalStateException
     *             if the component is not linked to an upload manager
     */
    static UploadManagerEmulation require(HasUploadManager component) {
        return of(component).orElseThrow(() -> new IllegalStateException(
                component.getClass().getSimpleName()
                        + " is not linked to an UploadManager"));
    }

    /**
     * Whether the manager linked to the given component accepts files from it,
     * as the browser decides whether to disable the component.
     *
     * @param component
     *            a component linked to an upload manager
     * @param checkMaxFiles
     *            whether a file list holding the maximum number of files
     *            disables the component, as it does an upload button and a drop
     *            zone
     * @return {@code true} if the component is linked to a usable manager
     */
    static boolean isUsable(HasUploadManager component, boolean checkMaxFiles) {
        return of(component).filter(UploadManagerEmulation::isUsable).filter(
                manager -> !checkMaxFiles || !manager.isMaxFilesReached())
                .isPresent();
    }

    /**
     * Provides the reasons why the manager linked to the given component does
     * not accept files from it.
     *
     * @param component
     *            a component linked to an upload manager
     * @param collector
     *            the consumer of the reasons
     * @param checkMaxFiles
     *            whether reaching the maximum number of files counts as well
     * @see #isUsable(HasUploadManager, boolean)
     */
    static void notUsableReasons(HasUploadManager component,
            Consumer<String> collector, boolean checkMaxFiles) {
        of(component).ifPresentOrElse(
                manager -> manager.collectNotUsableReasons(collector,
                        checkMaxFiles),
                () -> collector.accept("not linked to an UploadManager"));
    }

    /**
     * Gets the outcome of the files last selected, dropped or started through
     * the manager linked to the given component.
     *
     * @param component
     *            a component linked to an upload manager
     * @return one entry per file, or an empty list if nothing has been uploaded
     *         yet or the component is not linked to a manager
     */
    static List<UploadTester.FileStatus> getLastUploadStatus(
            HasUploadManager component) {
        return of(component).map(manager -> manager.getLastUploadStatus())
                .orElse(List.of());
    }

    /**
     * Whether the client-side manager accepts files at all. The browser
     * disables the linked components otherwise.
     *
     * @return {@code true} if the manager is attached and enabled
     */
    private boolean isUsable() {
        return connector.isAttached() && connector.getElement().isEnabled();
    }

    /**
     * Whether the file list holds as many files as the manager allows. The
     * browser disables an upload button and a drop zone in that case.
     *
     * @return {@code true} if no more files can be added
     */
    private boolean isMaxFilesReached() {
        int maxFiles = manager.getMaxFiles();
        return maxFiles > 0 && syncedState().files.size() >= maxFiles;
    }

    /**
     * Provides the reasons why the manager does not accept files.
     *
     * @param collector
     *            the consumer of the reasons
     * @param checkMaxFiles
     *            whether reaching the maximum number of files counts as well
     */
    private void collectNotUsableReasons(Consumer<String> collector,
            boolean checkMaxFiles) {
        if (!connector.isAttached()) {
            collector.accept("linked to an UploadManager whose owner is not "
                    + "attached");
        } else if (!connector.getElement().isEnabled()) {
            collector.accept("linked to a disabled UploadManager");
        }
        if (checkMaxFiles && isMaxFilesReached()) {
            collector.accept("linked to an UploadManager that reached its "
                    + "maximum number of files (" + manager.getMaxFiles()
                    + ")");
        }
    }

    /**
     * Adds the given files to the file list as the client-side manager does
     * when the user selects or drops them, and uploads the accepted ones unless
     * auto upload is turned off.
     *
     * @param items
     *            the files the user selected or dropped
     */
    void addFiles(List<UploadItem> items) {
        State state = syncedState();
        List<UploadItem> accepted = new ArrayList<>();
        try {
            // Read all contents before touching the file list, so that a file
            // that cannot be read leaves the list unchanged
            List<byte[]> allContents = new ArrayList<>();
            for (UploadItem item : items) {
                allContents.add(UploadTesterSupport
                        .readContents(item.contentsProducer));
            }
            for (int i = 0; i < items.size(); i++) {
                UploadItem item = items.get(i);
                byte[] contents = allContents.get(i);
                // Cache the contents, they are read again on delivery
                item.contentsProducer = () -> contents;
                String error = validate(state, item, contents.length);
                if (error == null) {
                    // the client-side manager shows the newest file first
                    state.files.add(0, item);
                    accepted.add(item);
                } else {
                    item.status = UploadTester.UploadStatus.REJECTED;
                    item.errorMessage = error;
                    fireFileRejected(item.fileName, error);
                }
            }
            if (manager.isAutoUpload() && !accepted.isEmpty()) {
                deliver(accepted);
            }
        } finally {
            recordLastUpload(items);
        }
    }

    /**
     * Uploads a file that waits in the file list, as clicking its start button
     * does.
     *
     * @param fileName
     *            the name of the file to start
     * @throws IllegalArgumentException
     *             if no such file waits in the file list
     */
    void startUpload(String fileName) {
        UploadItem item = syncedState().files.stream()
                .filter(file -> file.fileName.equals(fileName)
                        && file.status == UploadTester.UploadStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File '"
                        + fileName
                        + "' is not waiting to be uploaded. Files in the list: "
                        + describeFiles()));
        start(item);
    }

    /**
     * Uploads the file at the given position of the file list, as clicking its
     * start button does.
     *
     * @param index
     *            the position of the file in the list, in the order
     *            {@link #getFiles()} returns them
     * @throws IllegalArgumentException
     *             if there is no file at the position, or it does not wait to
     *             be uploaded
     */
    void startUpload(int index) {
        UploadItem item = getFileAt(index);
        if (item.status != UploadTester.UploadStatus.PENDING) {
            throw new IllegalArgumentException("File '" + item.fileName
                    + "' at index " + index
                    + " is not waiting to be uploaded, it is " + item.status);
        }
        start(item);
    }

    /**
     * Removes a file from the file list, as clicking its remove button does.
     *
     * @param fileName
     *            the name of the file to remove
     * @throws IllegalArgumentException
     *             if the file is not in the file list
     */
    void removeFile(String fileName) {
        State state = syncedState();
        UploadItem item = state.files.stream()
                .filter(file -> file.fileName.equals(fileName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File '"
                        + fileName
                        + "' is not in the upload file list. Files in the list: "
                        + describeFiles()));
        remove(item);
    }

    /**
     * Removes the file at the given position of the file list, as clicking its
     * remove button does.
     *
     * @param index
     *            the position of the file in the list, in the order
     *            {@link #getFiles()} returns them
     * @throws IllegalArgumentException
     *             if there is no file at the position
     */
    void removeFile(int index) {
        remove(getFileAt(index));
    }

    private UploadItem getFileAt(int index) {
        List<UploadItem> files = syncedState().files;
        if (index < 0 || index >= files.size()) {
            throw new IllegalArgumentException("No file at index " + index
                    + ". Files in the list: " + describeFiles());
        }
        return files.get(index);
    }

    private void start(UploadItem item) {
        try {
            deliver(List.of(item));
        } finally {
            recordLastUpload(List.of(item));
        }
    }

    private void remove(UploadItem item) {
        state().files.remove(item);
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.fileName", item.fileName);
        fireDomEvent("file-remove", eventData);
    }

    /**
     * Gets the files in the file list, in the order the browser shows them,
     * newest first.
     *
     * @return the files in the file list
     */
    List<UploadTester.FileStatus> getFiles() {
        return syncedState().files.stream().map(UploadItem::toStatus)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets the outcome of the files the user last selected, dropped or started.
     *
     * @return one entry per file, in the order the files were given
     */
    List<UploadTester.FileStatus> getLastUploadStatus() {
        return state().lastUpload;
    }

    /**
     * Fails unless every file of the last upload was uploaded.
     *
     * @throws IllegalStateException
     *             if nothing has been uploaded, or a file of the last upload
     *             was not uploaded
     */
    void ensureUploaded() {
        UploadTesterSupport.ensureUploaded(getLastUploadStatus(),
                "this UploadManager");
    }

    /**
     * Fails unless a file of the last upload failed or was rejected.
     *
     * @throws IllegalStateException
     *             if nothing has been uploaded, or no file of the last upload
     *             failed or was rejected
     */
    void ensureUploadFailed() {
        UploadTesterSupport.ensureUploadFailed(getLastUploadStatus(),
                "this UploadManager");
    }

    private String validate(State state, UploadItem item, long size) {
        int maxFiles = manager.getMaxFiles();
        if (maxFiles > 0 && state.files.size() >= maxFiles) {
            return TOO_MANY_FILES;
        }
        long maxFileSize = manager.getMaxFileSize();
        if (maxFileSize > 0 && size > maxFileSize) {
            return FILE_IS_TOO_BIG;
        }
        if (!UploadTesterSupport
                .matchesAccept(
                        UploadTesterSupport.acceptPattern(
                                connector.getElement().getProperty("accept")),
                        item)) {
            return INCORRECT_FILE_TYPE;
        }
        return null;
    }

    private void deliver(List<UploadItem> items) {
        Element element = connector.getElement();
        UploadHandler uploadHandler = UploadTesterSupport
                .uploadHandler(element);
        RuntimeException caughtException;
        try {
            items.forEach(item -> UploadTesterSupport.deliver(item,
                    uploadHandler, element));
        } finally {
            caughtException = UploadTesterSupport.runUIQueue();
            fireDomEvent("all-finished", JacksonUtils.createObjectNode());
        }
        if (caughtException != null) {
            throw caughtException;
        }
    }

    private void recordLastUpload(List<UploadItem> items) {
        state().lastUpload = items.stream().map(UploadItem::toStatus)
                .collect(Collectors.toUnmodifiableList());
    }

    private void fireFileRejected(String fileName, String error) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.fileName", fileName);
        eventData.put("event.detail.errorMessage", error);
        fireDomEvent("file-reject", eventData);
    }

    private void fireDomEvent(String eventType, ObjectNode eventData) {
        DomEvent event = new DomEvent(connector.getElement(), eventType,
                eventData);
        connector.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(event);
    }

    private String describeFiles() {
        return syncedState().files.stream().map(file -> file.fileName)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private State state() {
        State state = ComponentUtil.getData(connector, State.class);
        if (state == null) {
            state = new State();
            ComponentUtil.setData(connector, State.class, state);
        }
        return state;
    }

    private State syncedState() {
        State state = state();
        // the function is called through a generic expression that takes the
        // function name as its first parameter
        if (state.clearFileListCalls.hasNewInvocations(connector,
                invocation -> !invocation.getParameters().isEmpty()
                        && CLEAR_FILE_LIST_FUNCTION
                                .equals(invocation.getParameters().get(0)))) {
            state.files.clear();
        }
        return state;
    }

    /**
     * The emulated client-side state of an upload manager.
     */
    private static class State {
        private final List<UploadItem> files = new ArrayList<>();
        private final UploadTesterSupport.InvocationTracker clearFileListCalls = new UploadTesterSupport.InvocationTracker();
        private List<UploadTester.FileStatus> lastUpload = List.of();
    }
}
