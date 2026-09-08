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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.upload.AssertingTransferProgressListener.UploadedData;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.streams.UploadHandler;

@ViewPackages
class UploadTesterTest extends BrowserlessTest {

    private static final String FIRST_FILE_CONTENTS = "First file";
    private static final String SECOND_FILE_CONTENTS = "Second file";
    private static final String THIRD_FILE_CONTENTS = "Third file";

    @TempDir
    private static Path tempDir;
    private static File file1;
    private static File file2;
    private static File file3;

    UploadView view;
    UploadTester<Upload> single_;
    UploadTester<Upload> multi_;
    final List<String> rejected = new ArrayList<>();
    final List<String> removed = new ArrayList<>();

    @BeforeAll
    static void setupTestFiles() throws IOException {
        file1 = tempDir.resolve("upload1.txt").toFile();
        Files.writeString(file1.toPath(), FIRST_FILE_CONTENTS);
        file2 = tempDir.resolve("file2.txt").toFile();
        Files.writeString(file2.toPath(), SECOND_FILE_CONTENTS);
        file3 = tempDir.resolve("third.txt").toFile();
        Files.writeString(file3.toPath(), THIRD_FILE_CONTENTS);
    }

    @BeforeEach
    void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(UploadView.class);
        view = navigate(UploadView.class);
        single_ = test(view.uploadSingle);
        multi_ = test(view.uploadMulti);
        Stream.of(view.uploadSingle, view.uploadMulti).forEach(upload -> {
            upload.addFileRejectedListener(ev -> rejected
                    .add(ev.getFileName() + ":" + ev.getErrorMessage()));
            upload.addFileRemovedListener(ev -> removed.add(ev.getFileName()));
        });
    }

    @Test
    void upload_notUsable_throws() {
        view.uploadSingle.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> single_.upload(file1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> single_.uploadAborted(file1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> single_.uploadFailed(file1));
    }

    @Test
    void upload_singleFile_succeeds() {
        AtomicBoolean allFinished = new AtomicBoolean();

        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.addAllFinishedListener(ev -> allFinished.set(true));

        single_.upload(file1);

        listener.assertStarted();
        listener.assertProgressCalled();
        listener.assertCompleted();
        listener.assertNotFailed();
        Assertions.assertTrue(allFinished.get(),
                "All Finished listener was not notified");

        UploadedData uploadedData = listener.assertFileReceived();
        Assertions.assertNotNull(uploadedData);
        Assertions.assertEquals(file1.getName(),
                uploadedData.metadata().fileName());
        Assertions.assertEquals("text/plain",
                uploadedData.metadata().contentType());
        Assertions.assertEquals(FIRST_FILE_CONTENTS,
                uploadedDataToString(uploadedData));
    }

    @Test
    void upload_singleFile_failure() {

        AtomicBoolean allFinished = new AtomicBoolean();
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(listener.asFailingHandler());
        view.uploadSingle.addAllFinishedListener(ev -> allFinished.set(true));

        Assertions.assertThrows(UncheckedIOException.class,
                () -> single_.upload(file1));

        listener.assertStarted();
        listener.assertFailed();
        listener.assertNotCompleted();
        Assertions.assertTrue(allFinished.get(),
                "All Finished listener was not notified");

    }

    @Test
    void upload_multipleFiles_succeeds() {

        AtomicInteger allFinished = new AtomicInteger();
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadMulti.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadMulti
                .addAllFinishedListener(ev -> allFinished.incrementAndGet());

        multi_.uploadAll(file1, file2, file3);

        listener.assertStarted(3);
        listener.assertCompleted(3);
        listener.assertNotFailed();
        Assertions.assertEquals(1, allFinished.get(),
                "All Finished should be invoked once");

        List<UploadedData> receivedFiles = listener.assertFilesReceived(3);
        Set<String> fileNames = receivedFiles.stream()
                .map(ud -> ud.metadata().fileName())
                .collect(Collectors.toSet());
        Assertions.assertEquals(
                Set.of(file1.getName(), file2.getName(), file3.getName()),
                fileNames);
    }

    @Test
    void uploadAll_noFiles_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> multi_.uploadAll());
    }

    @Test
    void uploadAborted_failureNotified() {
        assertFailedUpload(single_::uploadAborted);
    }

    @Test
    void uploadFailed_failureNotified() {
        assertFailedUpload(single_::uploadFailed);
    }

    @Test
    void upload_acceptedFileExtensions_disallowedExtensionRejected() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.setAcceptedFileExtensions(".txt");

        single_.upload("image.png", "image/png",
                "not a text file".getBytes(StandardCharsets.UTF_8));

        listener.assertNotStarted();
        Assertions.assertTrue(listener.uploadedData.isEmpty(),
                "Rejected file should not have been received by the handler");
        Assertions.assertEquals(List.of("image.png:Incorrect File Type."),
                rejected);
    }

    @Test
    void upload_acceptedFileExtensions_allowedExtensionAccepted() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.setAcceptedFileExtensions(".txt");

        single_.upload(file1);

        listener.assertStarted();
        listener.assertCompleted();
        UploadedData uploadedData = listener.assertFileReceived();
        Assertions.assertEquals(file1.getName(),
                uploadedData.metadata().fileName());
        Assertions.assertEquals(FIRST_FILE_CONTENTS,
                uploadedDataToString(uploadedData));
    }

    @Test
    void upload_acceptedMimeTypes_disallowedTypeRejected() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.setAcceptedMimeTypes("image/*");

        // text/plain detected from the file name does not match image/*
        single_.upload(file1);

        listener.assertNotStarted();
        Assertions.assertTrue(listener.uploadedData.isEmpty(),
                "Rejected file should not have been received by the handler");
        Assertions.assertEquals(
                List.of(file1.getName() + ":Incorrect File Type."), rejected);
    }

    @Test
    void upload_acceptedMimeTypes_allowedTypeAccepted() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.setAcceptedMimeTypes("image/*");

        single_.upload("photo.png", "image/png",
                "fake image data".getBytes(StandardCharsets.UTF_8));

        listener.assertStarted();
        listener.assertCompleted();
        UploadedData uploadedData = listener.assertFileReceived();
        Assertions.assertEquals("photo.png",
                uploadedData.metadata().fileName());
    }

    @Test
    void uploadAll_fileCountExceeded_extraFilesRejected() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadMulti.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadMulti.setMaxFiles(2);

        multi_.uploadAll(file1, file2, file3);

        Assertions.assertEquals(List.of(file3.getName() + ":Too Many Files."),
                rejected,
                "The file exceeding maxFiles should have been rejected");
        Assertions.assertEquals(Set.of(file1.getName(), file2.getName()),
                listener.assertFilesReceived(2).stream()
                        .map(ud -> ud.metadata().fileName())
                        .collect(Collectors.toSet()));
    }

    @Test
    void upload_fileCountExceededOverSeparateUploads_extraFilesRejected() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadMulti.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadMulti.setMaxFiles(2);

        multi_.upload(file1);
        multi_.upload(file2);
        multi_.upload(file3);

        Assertions.assertEquals(List.of(file3.getName() + ":Too Many Files."),
                rejected,
                "Files already in the file list should count towards maxFiles");
        listener.assertFilesReceived(2);
    }

    @Test
    void upload_exceedsMaxFileSize_rejected() {
        AtomicBoolean allFinished = new AtomicBoolean();
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.addAllFinishedListener(ev -> allFinished.set(true));
        view.uploadSingle.setMaxFileSize(5);

        single_.upload("big.txt", "text/plain",
                "0123456789".getBytes(StandardCharsets.UTF_8));

        listener.assertNotStarted();
        Assertions.assertEquals(List.of("big.txt:File is Too Big."), rejected);
        Assertions.assertFalse(allFinished.get(),
                "All Finished should not be notified when nothing is uploaded");

        // A file of exactly the maximum size still goes through
        single_.upload("small.txt", "text/plain",
                "01234".getBytes(StandardCharsets.UTF_8));

        listener.assertStarted();
        Assertions.assertEquals("small.txt",
                listener.assertFileReceived().metadata().fileName());
    }

    @Test
    void upload_acceptedFileTypes_onlyMatchingFilesAccepted() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadMulti.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadMulti.setAcceptedFileTypes(".txt", "image/*");

        multi_.upload("report.pdf", "application/pdf",
                "not accepted".getBytes(StandardCharsets.UTF_8));
        // matches by mime type wildcard
        multi_.upload("photo.png", "image/png",
                "fake image".getBytes(StandardCharsets.UTF_8));
        // matches by file name extension
        multi_.upload(file1);

        Assertions.assertEquals(List.of("report.pdf:Incorrect File Type."),
                rejected);
        Assertions.assertEquals(Set.of("photo.png", file1.getName()),
                listener.assertFilesReceived(2).stream()
                        .map(ud -> ud.metadata().fileName())
                        .collect(Collectors.toSet()));
    }

    @Test
    void upload_customI18n_rejectionUsesConfiguredMessage() {
        view.uploadSingle.setUploadHandler(UploadHandler.inMemory((m, d) -> {
        }));
        view.uploadSingle
                .setI18n(new UploadI18N().setError(new UploadI18N.Error()
                        .setFileIsTooBig("Tiedosto on liian iso")));
        view.uploadSingle.setMaxFileSize(1);

        single_.upload("big.txt", "text/plain",
                "0123456789".getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(List.of("big.txt:Tiedosto on liian iso"),
                rejected);
    }

    @Test
    void removeFile_fileRemovedNotifiedAndSlotFreed() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.setMaxFiles(1);

        single_.upload(file1);
        single_.upload(file2);
        Assertions.assertEquals(List.of(file2.getName() + ":Too Many Files."),
                rejected, "The file list should be full");

        single_.removeFile(file1);

        Assertions.assertEquals(List.of(file1.getName()), removed);

        single_.upload(file2);

        Assertions.assertEquals(Set.of(file1.getName(), file2.getName()),
                listener.assertFilesReceived(2).stream()
                        .map(ud -> ud.metadata().fileName())
                        .collect(Collectors.toSet()));
    }

    @Test
    void removeFile_fileNotInFileList_throws() {
        view.uploadSingle.setUploadHandler(UploadHandler.inMemory((m, d) -> {
        }));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> single_.removeFile(file1));
    }

    @Test
    void clearFileList_slotsFreed() {
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));
        view.uploadSingle.setMaxFiles(1);

        single_.upload(file1);
        view.uploadSingle.clearFileList();
        single_.upload(file2);

        Assertions.assertTrue(rejected.isEmpty(),
                "No file should have been rejected, but got " + rejected);
        listener.assertFilesReceived(2);
    }

    @Test
    void uploadAborted_fileRemovedFromFileList() {
        view.uploadSingle.setUploadHandler(UploadHandler.inMemory((m, d) -> {
        }));
        view.uploadSingle.setMaxFiles(1);

        single_.uploadAborted(file1);

        Assertions.assertEquals(List.of(file1.getName()), removed,
                "An aborted file should be removed from the file list");

        single_.upload(file2);

        Assertions.assertTrue(rejected.isEmpty(),
                "The aborted file should not occupy a slot, but got "
                        + rejected);
    }

    @Test
    void uploadFailed_fileKeptInFileList() {
        view.uploadSingle.setUploadHandler(
                new AssertingTransferProgressListener().asFailingHandler());
        view.uploadSingle.setMaxFiles(1);

        single_.uploadFailed(file1);

        Assertions.assertTrue(removed.isEmpty(),
                "A failed file should stay in the file list");

        single_.upload(file2);

        Assertions.assertEquals(List.of(file2.getName() + ":Too Many Files."),
                rejected);
    }

    void assertFailedUpload(BiConsumer<String, String> wrapperAction) {

        AtomicBoolean allFinished = new AtomicBoolean();
        AssertingTransferProgressListener listener = new AssertingTransferProgressListener();
        view.uploadSingle.setUploadHandler(
                UploadHandler.inMemory(listener::fileUploaded, listener));

        view.uploadSingle.addAllFinishedListener(ev -> allFinished.set(true));

        wrapperAction.accept(file1.getName(), "text/plain");

        listener.assertStarted();
        listener.assertNotCompleted();
        listener.assertFailed();
        Assertions.assertTrue(allFinished.get(),
                "All Finished listener was not notified");
    }

    private String uploadedDataToString(UploadedData uploadedData) {
        return new String(uploadedData.data(), StandardCharsets.UTF_8);
    }
}
