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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.upload.UploadManager.FileRejectionReason;
import com.vaadin.flow.component.upload.UploadTester.FileStatus;
import com.vaadin.flow.component.upload.UploadTester.UploadStatus;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class UploadButtonTesterTest extends BrowserlessTest {

    @TempDir
    Path tempDir;

    UploadManagerView view;
    UploadButtonTester<UploadButton> button_;
    final List<String> rejected = new ArrayList<>();

    @BeforeEach
    void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(UploadManagerView.class);
        view = navigate(UploadManagerView.class);
        button_ = test(view.button);
        view.manager.addFileRejectedListener(ev -> {
            Assertions.assertTrue(ev.isFromClient());
            rejected.add(ev.getFileName() + ":" + ev.getReason());
        });
    }

    @Test
    void upload_fileDeliveredToUploadHandler() throws IOException {
        Assertions.assertThrows(IllegalStateException.class,
                button_::ensureUploaded, "Nothing has been uploaded yet");
        Assertions.assertThrows(IllegalStateException.class,
                button_::ensureUploadFailed, "Nothing has been uploaded yet");

        AtomicInteger allFinished = new AtomicInteger();
        view.manager
                .addAllFinishedListener(ev -> allFinished.incrementAndGet());

        button_.upload(file("notes.txt", "Some notes"));

        Assertions.assertEquals(List.of("notes.txt:Some notes"), view.received);
        Assertions.assertEquals(1, allFinished.get());
        button_.ensureUploaded();
        Assertions.assertThrows(IllegalStateException.class,
                button_::ensureUploadFailed, "Every file was uploaded");
    }

    @Test
    void uploadAll_filesCheckedAgainstTheManagerConstraints()
            throws IOException {
        view.manager.setMaxFiles(2);
        view.manager.setMaxFileSize(5);
        view.manager.setAcceptedFileExtensions(".txt");

        button_.uploadAll(file("a.txt", "a"), file("big.txt", "too big"),
                file("image.png", "b"), file("c.txt", "c"), file("d.txt", "d"));

        Assertions.assertEquals(List.of("a.txt:a", "c.txt:c"), view.received);
        Assertions.assertEquals(
                List.of("big.txt:" + FileRejectionReason.FILE_TOO_LARGE,
                        "image.png:" + FileRejectionReason.INCORRECT_FILE_TYPE,
                        "d.txt:" + FileRejectionReason.TOO_MANY_FILES),
                rejected);
        Assertions.assertEquals(
                List.of(new FileStatus("a.txt", UploadStatus.UPLOADED, null),
                        new FileStatus("big.txt", UploadStatus.REJECTED,
                                "fileIsTooBig"),
                        new FileStatus("image.png", UploadStatus.REJECTED,
                                "incorrectFileType"),
                        new FileStatus("c.txt", UploadStatus.UPLOADED, null),
                        new FileStatus("d.txt", UploadStatus.REJECTED,
                                "tooManyFiles")),
                button_.getLastUploadStatus());
        Assertions.assertThrows(IllegalStateException.class,
                button_::ensureUploaded);
        button_.ensureUploadFailed();

        // the browser disables the button once the file list is full
        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class, () -> button_.upload("e.txt",
                        "text/plain", "e".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertTrue(
                exception.getMessage().contains("maximum number of files"),
                exception.getMessage());

        view.manager.clearFileList();
        button_.upload("e.txt", "text/plain",
                "e".getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(List.of("a.txt:a", "c.txt:c", "e.txt:e"),
                view.received);
    }

    @Test
    void upload_sameFileNameTwice_bothUploaded() {
        view.manager.setMaxFiles(2);

        button_.upload("a.txt", "text/plain",
                "first".getBytes(StandardCharsets.UTF_8));
        button_.upload("a.txt", "text/plain",
                "second".getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(List.of("a.txt:first", "a.txt:second"),
                view.received);
        Assertions.assertTrue(rejected.isEmpty(), "Got " + rejected);
        Assertions.assertFalse(button_.isUsable(),
                "Both files should count towards maxFiles");
    }

    @Test
    void ensureUploadFailed_autoUploadOff_pendingFileNotFailed() {
        view.manager.setAutoUpload(false);

        button_.upload("a.txt", "text/plain",
                "a".getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(UploadStatus.PENDING,
                button_.getLastUploadStatus().get(0).status());
        Assertions.assertThrows(IllegalStateException.class,
                button_::ensureUploadFailed,
                "A file waiting to be started has not failed");
    }

    @Test
    void upload_notUsable_throws() throws IOException {
        File file = file("notes.txt", "Some notes");

        UploadButton unlinked = new UploadButton();
        view.add(unlinked);
        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class, () -> test(unlinked).upload(file));
        Assertions.assertTrue(
                exception.getMessage()
                        .contains("not linked to an UploadManager"),
                exception.getMessage());

        view.manager.setEnabled(false);
        exception = Assertions.assertThrows(IllegalStateException.class,
                () -> button_.upload(file));
        Assertions.assertTrue(
                exception.getMessage().contains("disabled UploadManager"),
                exception.getMessage());

        view.manager.setEnabled(true);
        view.button.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> button_.upload(file));
        Assertions.assertTrue(view.received.isEmpty());
    }

    private File file(String name, String contents) throws IOException {
        Path path = tempDir.resolve(name);
        Files.writeString(path, contents);
        return path.toFile();
    }
}
