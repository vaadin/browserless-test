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
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for UploadFileList components.
 * <p>
 * The file list shows the files of the {@link UploadManager} it is linked to:
 * the files the user picked with an {@link UploadButton} or dropped on an
 * {@link UploadDropZone} linked to the same manager. The tester reads that list
 * and simulates the buttons on its entries. As in the browser, the file list is
 * not usable while it is not linked to a manager or while the manager is
 * disabled.
 * <p>
 * Entries are identified by file name. As in the browser, nothing prevents the
 * user from adding the same file, or another file with the same name, more than
 * once: every one becomes an entry of its own, is uploaded on its own and
 * counts towards {@link UploadManager#setMaxFiles(int)}. When several entries
 * share a name, {@link #removeFile(String)} removes the most recently added
 * one, which is the first one the list shows, and {@link #startUpload(String)}
 * starts the most recently added one that still waits to be started. Call them
 * again to reach the others.
 *
 * @param <T>
 *            the component type.
 */
@Tests(UploadFileList.class)
public class UploadFileListTester<T extends UploadFileList>
        extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public UploadFileListTester(T component) {
        super(component);
    }

    /**
     * Gets the files the file list shows, in the order the browser shows them:
     * the most recently added file first.
     * <p>
     * A file is {@link UploadTester.UploadStatus#UPLOADED} once the upload
     * handler has consumed it, {@link UploadTester.UploadStatus#FAILED} or
     * {@link UploadTester.UploadStatus#REJECTED} when its transfer failed or
     * the server refused it, and {@link UploadTester.UploadStatus#PENDING}
     * while it waits to be started because auto upload is turned off.
     *
     * @return the files in the file list, or an empty list if the file list is
     *         not linked to an upload manager
     */
    public List<UploadTester.FileStatus> getFiles() {
        // Picks up pending clearFileList() calls
        roundTrip();
        return UploadManagerEmulation.of(getComponent())
                .map(UploadManagerEmulation::getFiles).orElse(List.of());
    }

    /**
     * Returns what happened to each file the user last picked with an
     * {@link UploadButton}, dropped on an {@link UploadDropZone} or started
     * from this list, in the order the files were given.
     * <p>
     * A file is {@link UploadTester.UploadStatus#UPLOADED} once the upload
     * handler has consumed it, {@link UploadTester.UploadStatus#FAILED} when
     * the handler threw, and {@link UploadTester.UploadStatus#PENDING} while it
     * waits for {@link #startUpload(String)} because auto upload is turned off.
     * <p>
     * Unlike {@link #getFiles()}, this also reports the files the manager
     * refused, which never enter the list: they are
     * {@link UploadTester.UploadStatus#REJECTED} with the client-side error
     * code as the error message: {@code tooManyFiles}, {@code fileIsTooBig} or
     * {@code incorrectFileType}. A file Flow's server-side accepted type
     * validation refused is {@code REJECTED} as well.
     *
     * @return the outcome of the last upload, one entry per file, or an empty
     *         list if nothing has been uploaded yet or the file list is not
     *         linked to a manager
     */
    public List<UploadTester.FileStatus> getLastUploadStatus() {
        return UploadManagerEmulation.getLastUploadStatus(getComponent());
    }

    /**
     * Simulates the user clicking the remove button of a file in the list.
     * <p>
     * A {@link UploadManager.FileRemovedEvent} is fired and the file stops
     * counting towards {@link UploadManager#setMaxFiles(int)}.
     * <p>
     * When several entries have the given name, the most recently added one is
     * removed.
     *
     * @param fileName
     *            name of the file to remove, as given when it was uploaded
     * @throws IllegalArgumentException
     *             if the file is not in the file list
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void removeFile(String fileName) {
        prepare().removeFile(fileName);
    }

    /**
     * Simulates the user clicking the remove button of a file in the list.
     * <p>
     * A {@link UploadManager.FileRemovedEvent} is fired and the file stops
     * counting towards {@link UploadManager#setMaxFiles(int)}.
     * <p>
     * When several entries have the name of the given file, the most recently
     * added one is removed.
     *
     * @param file
     *            the file to remove
     * @throws IllegalArgumentException
     *             if the file is not in the file list
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void removeFile(File file) {
        removeFile(file.getName());
    }

    /**
     * Simulates the user clicking the start button of a file that waits in the
     * list because {@link UploadManager#setAutoUpload(boolean) auto upload} is
     * turned off, uploading it.
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
        prepare().startUpload(fileName);
    }

    /**
     * Simulates the user clicking the start button of a file that waits in the
     * list because {@link UploadManager#setAutoUpload(boolean) auto upload} is
     * turned off, uploading it.
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

    @Override
    public boolean isUsable() {
        return super.isUsable()
                && UploadManagerEmulation.isUsable(getComponent(), false);
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        super.notUsableReasons(collector);
        UploadManagerEmulation.notUsableReasons(getComponent(), collector,
                false);
    }

    private UploadManagerEmulation prepare() {
        ensureComponentIsUsable();
        // Picks up pending clearFileList() calls
        roundTrip();
        return UploadManagerEmulation.require(getComponent());
    }
}
