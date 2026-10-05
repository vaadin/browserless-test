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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
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
        dropZone_.drop(file("a.txt", "a"), file("b.txt", "b"));

        Assertions.assertEquals(List.of("a.txt:a", "b.txt:b"), view.received);
        Assertions.assertEquals(
                List.of(UploadStatus.UPLOADED, UploadStatus.UPLOADED),
                dropZone_.getLastUploadStatus().stream()
                        .map(UploadTester.FileStatus::status).toList());
        dropZone_.ensureUploaded();
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
