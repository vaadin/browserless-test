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
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.button.ButtonTester;

/**
 * Tester for UploadButton components.
 * <p>
 * Uploading through the button simulates the user picking files in the file
 * dialog the button opens. The files are handed to the {@link UploadManager}
 * the button is linked to, which checks them against its
 * {@link UploadManager#setMaxFiles(int) maxFiles},
 * {@link UploadManager#setMaxFileSize(long) maxFileSize} and accepted file
 * types the same way the browser does: a file failing one of them fires a
 * {@link UploadManager.FileRejectedEvent} and never reaches the upload handler.
 * A refused file is no more of an error here than in the browser, so uploading
 * does not throw for it. Use {@link #getLastUploadStatus()} to see what became
 * of each file, or {@link #ensureUploaded()} to fail the test unless every file
 * went through.
 * <p>
 * The manager keeps the files in a file list shared by every component linked
 * to it, and {@code maxFiles} is checked against that list. As in the browser,
 * the button is not usable while it is not linked to a manager, while the
 * manager is disabled, or once the file list holds {@code maxFiles} files.
 * Remove files with {@link UploadFileListTester#removeFile(String)} or
 * {@link UploadManager#clearFileList()} to make room.
 * <p>
 * When {@link UploadManager#setAutoUpload(boolean) auto upload} is turned off,
 * accepted files wait in the file list until they are started with
 * {@link UploadFileListTester#startUpload(String)}.
 *
 * @param <T>
 *            the component type.
 */
@Tests(UploadButton.class)
public class UploadButtonTester<T extends UploadButton>
        extends ButtonTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public UploadButtonTester(T component) {
        super(component);
    }

    /**
     * Simulates the user picking the given file in the file dialog.
     * <p>
     * The content type is detected from the file name.
     *
     * @param file
     *            the file to upload
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void upload(File file) {
        uploadAll(List.of(file));
    }

    /**
     * Simulates the user picking a file with the given name, content type and
     * contents in the file dialog.
     *
     * @param fileName
     *            name of the file to upload
     * @param contentType
     *            content type of the file to upload
     * @param contents
     *            file contents as an array of bytes
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void upload(String fileName, String contentType, byte[] contents) {
        addFiles(() -> List.of(new UploadTesterSupport.UploadItem(fileName,
                contentType, () -> contents)));
    }

    /**
     * Simulates the user picking several files at once in the file dialog.
     * <p>
     * The content types are detected from the file names.
     *
     * @param files
     *            the files to upload
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalArgumentException
     *             if no file is given
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadAll(File... files) {
        uploadAll(List.of(files));
    }

    /**
     * Simulates the user picking several files at once in the file dialog.
     * <p>
     * The content types are detected from the file names.
     *
     * @param files
     *            the files to upload
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalArgumentException
     *             if no file is given
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadAll(Collection<File> files) {
        // the files are converted once the component is known to be usable,
        // so that not being usable is reported first
        addFiles(() -> UploadTesterSupport.toItems(files));
    }

    /**
     * Returns what happened to each file the user last selected, dropped or
     * started through the {@link UploadManager} the button is linked to, in the
     * order the files were given.
     * <p>
     * Files are reported as {@link UploadTester.UploadStatus#UPLOADED} once the
     * upload handler has consumed them, as
     * {@link UploadTester.UploadStatus#REJECTED} when the manager or Flow's
     * server-side accepted type validation refused them, and as
     * {@link UploadTester.UploadStatus#PENDING} while they wait in the file
     * list for auto upload being turned off. The error message of a file the
     * manager refused is the client-side error code: {@code tooManyFiles},
     * {@code fileIsTooBig} or {@code incorrectFileType}.
     *
     * @return the outcome of the last upload, one entry per file, or an empty
     *         list if nothing has been uploaded yet or the button is not linked
     *         to a manager
     */
    public List<UploadTester.FileStatus> getLastUploadStatus() {
        return UploadManagerEmulation.lastUploadStatus(getComponent());
    }

    /**
     * Checks that the last upload through the {@link UploadManager} the button
     * is linked to delivered every one of its files, and fails otherwise.
     *
     * @throws IllegalStateException
     *             if no upload has been simulated, or if any file of the last
     *             upload was not uploaded
     * @see #getLastUploadStatus()
     */
    public void ensureUploaded() {
        UploadManagerEmulation.require(getComponent()).ensureUploaded();
    }

    @Override
    public boolean isUsable() {
        // Picks up pending clearFileList() calls, which make room in a full
        // file list
        roundTrip();
        return super.isUsable()
                && UploadManagerEmulation.isUsable(getComponent(), true);
    }

    @Override
    protected void notUsableReasons(Consumer<String> collector) {
        super.notUsableReasons(collector);
        UploadManagerEmulation.notUsableReasons(getComponent(), collector,
                true);
    }

    private void addFiles(
            Supplier<List<UploadTesterSupport.UploadItem>> items) {
        ensureComponentIsUsable();
        // A round trip is necessary to ensure upload handler registration and
        // to pick up pending clearFileList() calls
        roundTrip();
        UploadManagerEmulation.require(getComponent()).addFiles(items.get());
    }
}
