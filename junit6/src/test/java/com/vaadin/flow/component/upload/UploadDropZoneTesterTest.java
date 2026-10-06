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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.upload.UploadManager.FileRejectionReason;
import com.vaadin.flow.component.upload.UploadTester.UploadStatus;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class UploadDropZoneTesterTest extends BrowserlessTest {

    @TempDir
    Path tempDir;

    UploadManagerView view;
    UploadDropZoneTester<UploadDropZone> dropZone_;

    @BeforeEach
    void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(UploadManagerView.class);
        view = navigate(UploadManagerView.class);
        dropZone_ = test(view.dropZone);
    }

    @Test
    void drop_filesDeliveredToUploadHandler() throws IOException {
        Assertions.assertThrows(IllegalStateException.class,
                dropZone_::ensureUploaded, "Nothing has been uploaded yet");
        Assertions.assertThrows(IllegalStateException.class,
                dropZone_::ensureUploadFailed, "Nothing has been uploaded yet");

        dropZone_.drop(file("a.txt", "a"), file("b.txt", "b"));

        Assertions.assertEquals(List.of("a.txt:a", "b.txt:b"), view.received);
        Assertions.assertEquals(
                List.of(UploadStatus.UPLOADED, UploadStatus.UPLOADED),
                dropZone_.getLastUploadStatus().stream()
                        .map(UploadTester.FileStatus::status).toList());
        dropZone_.ensureUploaded();
        Assertions.assertThrows(IllegalStateException.class,
                dropZone_::ensureUploadFailed, "Every file was uploaded");
    }

    @Test
    void drop_filesCheckedAgainstTheManagerConstraints() throws IOException {
        List<String> rejected = new ArrayList<>();
        view.manager.addFileRejectedListener(ev -> {
            Assertions.assertTrue(ev.isFromClient());
            rejected.add(ev.getFileName() + ":" + ev.getReason());
        });
        view.manager.setMaxFiles(1);
        view.manager.setMaxFileSize(5);
        view.manager.setAcceptedMimeTypes("text/plain");

        dropZone_.drop(file("big.txt", "too big"), file("image.png", "b"),
                file("a.txt", "a"), file("c.txt", "c"));

        Assertions.assertEquals(List.of("a.txt:a"), view.received);
        Assertions.assertEquals(
                List.of("big.txt:" + FileRejectionReason.FILE_TOO_LARGE,
                        "image.png:" + FileRejectionReason.INCORRECT_FILE_TYPE,
                        "c.txt:" + FileRejectionReason.TOO_MANY_FILES),
                rejected);
        Assertions.assertEquals(
                List.of(UploadStatus.REJECTED, UploadStatus.REJECTED,
                        UploadStatus.UPLOADED, UploadStatus.REJECTED),
                dropZone_.getLastUploadStatus().stream()
                        .map(UploadTester.FileStatus::status).toList());
        dropZone_.ensureUploadFailed();
    }

    @Test
    void uploadFailedAndAborted_failedFileKeptAbortedFileRemoved()
            throws IOException {
        List<String> removed = new ArrayList<>();
        view.manager.addFileRemovedListener(ev -> {
            Assertions.assertTrue(ev.isFromClient());
            removed.add(ev.getFileName());
        });

        dropZone_.uploadFailed(file("failed.txt", "f"));

        Assertions.assertEquals(UploadStatus.FAILED,
                dropZone_.getLastUploadStatus().get(0).status());
        dropZone_.ensureUploadFailed();

        dropZone_.uploadAborted("aborted.txt", "text/plain");

        Assertions.assertEquals(UploadStatus.FAILED,
                dropZone_.getLastUploadStatus().get(0).status());
        Assertions.assertEquals(List.of("aborted.txt"), removed);
        Assertions.assertEquals(List.of("failed.txt"),
                test(view.fileList).getFiles().stream()
                        .map(UploadTester.FileStatus::fileName).toList());
        Assertions.assertEquals(List.of(), view.received);
    }

    @Test
    void uploadFailedAndAborted_notUsable_throws() {
        view.dropZone.setEnabled(false);

        Assertions.assertThrows(IllegalStateException.class,
                () -> dropZone_.uploadFailed("a.txt", "text/plain"));
        Assertions.assertThrows(IllegalStateException.class,
                () -> dropZone_.uploadAborted("a.txt", "text/plain"));
        Assertions.assertEquals(List.of(), dropZone_.getLastUploadStatus());
    }

    @Test
    void drop_notUsable_throws() throws IOException {
        File file = file("a.txt", "a");

        UploadDropZone unlinked = new UploadDropZone();
        view.add(unlinked);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(unlinked).drop(file));

        // the file list is shared with the button linked to the same manager
        view.manager.setMaxFiles(1);
        test(view.button).upload(file);
        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class, () -> dropZone_.drop(file));
        Assertions.assertTrue(
                exception.getMessage().contains("maximum number of files"),
                exception.getMessage());

        view.manager.setMaxFiles(0);
        view.dropZone.setEnabled(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> dropZone_.drop(file));
        Assertions.assertEquals(List.of("a.txt:a"), view.received);
    }

    private File file(String name, String contents) throws IOException {
        Path path = tempDir.resolve(name);
        Files.writeString(path, contents);
        return path.toFile();
    }
}
