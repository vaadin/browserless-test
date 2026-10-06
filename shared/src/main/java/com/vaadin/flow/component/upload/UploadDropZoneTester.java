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
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * Tester for UploadDropZone components.
 * <p>
 * Dropping files on the drop zone hands them to the {@link UploadManager} the
 * drop zone is linked to, which checks them against its
 * {@link UploadManager#setMaxFiles(int) maxFiles},
 * {@link UploadManager#setMaxFileSize(long) maxFileSize} and accepted file
 * types the same way the browser does: a file failing one of them fires a
 * {@link UploadManager.FileRejectedEvent} and never reaches the upload handler.
 * A refused file is no more of an error here than in the browser, so dropping
 * does not throw for it. Use {@link #getLastUploadStatus()} to see what became
 * of each file, or {@link #ensureUploaded()} to fail the test unless every file
 * went through.
 * <p>
 * The manager keeps the files in a file list shared by every component linked
 * to it, and {@code maxFiles} is checked against that list. As in the browser,
 * the drop zone is not usable while it is not linked to a manager, while the
 * manager is disabled, or once the file list holds {@code maxFiles} files.
 * <p>
 * When {@link UploadManager#setAutoUpload(boolean) auto upload} is turned off,
 * accepted files wait in the file list until they are started with
 * {@link UploadFileListTester#startUpload(String)}.
 *
 * @param <T>
 *            the component type.
 */
@Tests(UploadDropZone.class)
public class UploadDropZoneTester<T extends UploadDropZone>
        extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public UploadDropZoneTester(T component) {
        super(component);
    }

    /**
     * Simulates the user dropping the given files on the drop zone.
     * <p>
     * The content types are detected from the file names.
     *
     * @param files
     *            the files to drop
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalArgumentException
     *             if no file is given
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void drop(File... files) {
        drop(List.of(files));
    }

    /**
     * Simulates the user dropping the given files on the drop zone.
     * <p>
     * The content types are detected from the file names.
     *
     * @param files
     *            the files to drop
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalArgumentException
     *             if no file is given
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void drop(Collection<File> files) {
        // the files are converted once the component is known to be usable,
        // so that not being usable is reported first
        addFiles(() -> UploadTesterSupport.toItems(files));
    }

    /**
     * Simulates the user dropping a file with the given name, content type and
     * contents on the drop zone.
     *
     * @param fileName
     *            name of the file to drop
     * @param contentType
     *            content type of the file to drop
     * @param contents
     *            file contents as an array of bytes
     * @throws UncheckedIOException
     *             if the upload handler fails to handle the file contents
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void drop(String fileName, String contentType, byte[] contents) {
        addFiles(() -> List.of(new UploadTesterSupport.UploadItem(fileName,
                contentType, () -> contents)));
    }

    /**
     * Simulates the user dropping the given file on the drop zone and then
     * aborting its upload.
     * <p>
     * As in the browser, the aborted file is dropped from the file list and a
     * {@link UploadManager.FileRemovedEvent} is fired, freeing a slot when
     * {@link UploadManager#setMaxFiles(int)} is in use.
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link UploadManager#setAutoUpload(boolean) auto upload} is turned off.
     * The content type is detected from the file name, and the file is not
     * read.
     *
     * @param file
     *            the file to drop
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadAborted(File file) {
        uploadAborted(file.getName(),
                URLConnection.guessContentTypeFromName(file.getName()));
    }

    /**
     * Simulates the user dropping a file with the given name and content type
     * on the drop zone and then aborting its upload.
     * <p>
     * As in the browser, the aborted file is dropped from the file list and a
     * {@link UploadManager.FileRemovedEvent} is fired, freeing a slot when
     * {@link UploadManager#setMaxFiles(int)} is in use.
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link UploadManager#setAutoUpload(boolean) auto upload} is turned off.
     *
     * @param fileName
     *            name of the file to drop
     * @param contentType
     *            content type of the file to drop
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadAborted(String fileName, String contentType) {
        addFailedFile(fileName, contentType, true);
    }

    /**
     * Simulates the user dropping the given file on the drop zone and its
     * upload then failing.
     * <p>
     * As in the browser, a file whose upload failed stays in the file list.
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link UploadManager#setAutoUpload(boolean) auto upload} is turned off.
     * The content type is detected from the file name, and the file is not
     * read.
     *
     * @param file
     *            the file to drop
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadFailed(File file) {
        uploadFailed(file.getName(),
                URLConnection.guessContentTypeFromName(file.getName()));
    }

    /**
     * Simulates the user dropping a file with the given name and content type
     * on the drop zone and its upload then failing.
     * <p>
     * As in the browser, a file whose upload failed stays in the file list.
     * <p>
     * The file is added and its transfer started in one go, whether or not
     * {@link UploadManager#setAutoUpload(boolean) auto upload} is turned off.
     *
     * @param fileName
     *            name of the file to drop
     * @param contentType
     *            content type of the file to drop
     * @throws IllegalStateException
     *             if the component is not usable
     */
    public void uploadFailed(String fileName, String contentType) {
        addFailedFile(fileName, contentType, false);
    }

    /**
     * Returns what happened to each file the user last selected, dropped or
     * started through the {@link UploadManager} the drop zone is linked to, in
     * the order the files were given.
     * <p>
     * Files are reported as {@link UploadTester.UploadStatus#UPLOADED} once the
     * upload handler has consumed them, as
     * {@link UploadTester.UploadStatus#REJECTED} when the manager or Flow's
     * server-side accepted type validation refused them, as
     * {@link UploadTester.UploadStatus#FAILED} when the handler threw, and as
     * {@link UploadTester.UploadStatus#PENDING} while they wait in the file
     * list for auto upload being turned off. The error message of a file the
     * manager refused is the client-side error code: {@code tooManyFiles},
     * {@code fileIsTooBig} or {@code incorrectFileType}.
     *
     * @return the outcome of the last upload, one entry per file, or an empty
     *         list if nothing has been uploaded yet or the drop zone is not
     *         linked to a manager
     */
    public List<UploadTester.FileStatus> getLastUploadStatus() {
        return UploadManagerEmulation.getLastUploadStatus(getComponent());
    }

    /**
     * Checks that the last upload through the {@link UploadManager} the drop
     * zone is linked to delivered every one of its files, and fails otherwise.
     *
     * @throws IllegalStateException
     *             if no upload has been simulated, or if any file of the last
     *             upload was not uploaded
     * @see #getLastUploadStatus()
     */
    public void ensureUploaded() {
        UploadManagerEmulation.require(getComponent()).ensureUploaded();
    }

    /**
     * Checks that at least one file of the last upload through the
     * {@link UploadManager} the drop zone is linked to failed or was rejected,
     * and fails otherwise.
     * <p>
     * Counterpart of {@link #ensureUploaded()} for a test about the failure
     * case, such as an upload handler that throws or a file the manager
     * refuses. Use {@link #getLastUploadStatus()} to check which file failed
     * and why.
     * <p>
     * A file left {@link UploadTester.UploadStatus#PENDING} does not count as
     * failed: it was neither delivered nor refused, such as a file waiting
     * because auto upload is turned off.
     *
     * @throws IllegalStateException
     *             if no upload has been simulated, or if no file of the last
     *             upload failed or was rejected
     * @see #getLastUploadStatus()
     */
    public void ensureUploadFailed() {
        UploadManagerEmulation.require(getComponent()).ensureUploadFailed();
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

    private void addFailedFile(String fileName, String contentType,
            boolean abort) {
        ensureComponentIsUsable();
        // A round trip is necessary to ensure upload handler registration and
        // to pick up pending clearFileList() calls
        roundTrip();
        UploadManagerEmulation.require(getComponent()).addFailedFile(
                new UploadTesterSupport.UploadItem(fileName, contentType, null),
                abort);
    }
}
