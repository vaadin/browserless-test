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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.upload.UploadTester.FileStatus;
import com.vaadin.flow.component.upload.UploadTester.UploadStatus;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class UploadFileListTesterTest extends BrowserlessTest {

    UploadManagerView view;
    UploadButtonTester<UploadButton> button_;
    UploadFileListTester<UploadFileList> fileList_;
    final List<String> removed = new ArrayList<>();

    @BeforeEach
    void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(UploadManagerView.class);
        view = navigate(UploadManagerView.class);
        button_ = test(view.button);
        fileList_ = test(view.fileList);
        view.manager.addFileRemovedListener(ev -> {
            Assertions.assertTrue(ev.isFromClient());
            removed.add(ev.getFileName());
        });
    }

    @Test
    void getFiles_newestFileFirst() {
        upload("a.txt");
        test(view.dropZone).drop("b.txt", "text/plain", bytes("b.txt"));

        Assertions.assertEquals(
                List.of(new FileStatus("b.txt", UploadStatus.UPLOADED, null),
                        new FileStatus("a.txt", UploadStatus.UPLOADED, null)),
                fileList_.getFiles());

        view.manager.clearFileList();

        Assertions.assertEquals(List.of(), fileList_.getFiles());
    }

    @Test
    void removeFile_fileRemovedAndSlotFreed() {
        view.manager.setMaxFiles(1);
        upload("a.txt");
        Assertions.assertFalse(button_.isUsable(),
                "The file list should be full");

        fileList_.removeFile("a.txt");

        Assertions.assertEquals(List.of("a.txt"), removed);
        Assertions.assertEquals(List.of(), fileList_.getFiles());
        upload("b.txt");
        Assertions.assertEquals(List.of("a.txt:a.txt", "b.txt:b.txt"),
                view.received);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> fileList_.removeFile("a.txt"));
    }

    @Test
    void startUpload_autoUploadOff_fileUploadedOnlyWhenStarted() {
        view.manager.setAutoUpload(false);

        upload("a.txt");

        Assertions.assertTrue(view.received.isEmpty());
        Assertions.assertEquals(
                List.of(new FileStatus("a.txt", UploadStatus.PENDING, null)),
                fileList_.getFiles());

        fileList_.startUpload(new File("a.txt"));

        Assertions.assertEquals(List.of("a.txt:a.txt"), view.received);
        Assertions.assertEquals(
                List.of(new FileStatus("a.txt", UploadStatus.UPLOADED, null)),
                fileList_.getLastUploadStatus());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> fileList_.startUpload("a.txt"),
                "An uploaded file cannot be started again");
    }

    @Test
    void upload_handlerFails_fileListedAsFailed() {
        AtomicInteger allFinished = new AtomicInteger();
        view.manager
                .addAllFinishedListener(ev -> allFinished.incrementAndGet());
        view.manager.setUploadHandler(event -> {
            throw new IOException("Disk full");
        });
        view.manager.setAutoUpload(false);
        upload("a.txt");

        UncheckedIOException exception = Assertions.assertThrows(
                UncheckedIOException.class,
                () -> fileList_.startUpload("a.txt"));

        Assertions.assertEquals("Disk full", exception.getCause().getMessage());
        Assertions.assertEquals(1, allFinished.get());
        Assertions.assertEquals(UploadStatus.FAILED,
                fileList_.getLastUploadStatus().get(0).status());
        // the failed file stays in the file list, as it does in the browser
        Assertions.assertEquals(List.of(UploadStatus.FAILED),
                fileList_.getFiles().stream().map(FileStatus::status).toList());
    }

    @Test
    void getLastUploadStatus_noUpload_empty() {
        Assertions.assertEquals(List.of(), fileList_.getLastUploadStatus());

        UploadFileList unlinked = new UploadFileList();
        view.add(unlinked);
        Assertions.assertEquals(List.of(),
                test(unlinked).getLastUploadStatus());
    }

    @Test
    void removeFile_managerDisabled_throws() {
        upload("a.txt");
        view.manager.setEnabled(false);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> fileList_.removeFile("a.txt"));
        Assertions.assertTrue(
                exception.getMessage().contains("disabled UploadManager"),
                exception.getMessage());
        Assertions.assertTrue(removed.isEmpty());
    }

    private void upload(String fileName) {
        button_.upload(fileName, "text/plain", bytes(fileName));
    }

    private static byte[] bytes(String contents) {
        return contents.getBytes(StandardCharsets.UTF_8);
    }
}
