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
 * does not throw for it. Use {@link UploadFileListTester#getLastUploadStatus()}
 * to see what became of each file, or
 * {@link UploadFileListTester#ensureUploaded()} to fail the test unless every
 * file went through.
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
