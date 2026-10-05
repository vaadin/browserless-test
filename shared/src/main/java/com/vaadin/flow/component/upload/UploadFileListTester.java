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
     * Simulates the user clicking the remove button of a file in the list.
     * <p>
     * A {@link UploadManager.FileRemovedEvent} is fired and the file stops
     * counting towards {@link UploadManager#setMaxFiles(int)}.
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

    @Override
    public boolean isUsable() {
        return super.isUsable() && UploadManagerEmulation.of(getComponent())
                .filter(UploadManagerEmulation::isUsable).isPresent();
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        super.notUsableReasons(collector);
        UploadManagerEmulation.of(getComponent()).ifPresentOrElse(
                manager -> manager.notUsableReasons(collector, false),
                () -> collector.accept("not linked to an UploadManager"));
    }

    private UploadManagerEmulation prepare() {
        ensureComponentIsUsable();
        // Picks up pending clearFileList() calls
        roundTrip();
        return UploadManagerEmulation.of(getComponent()).orElseThrow();
    }
}
