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
package com.vaadin.flow.component.clipboard;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A single file to deliver through
 * {@link ClipboardSimulator#pasteFilesInto(com.vaadin.flow.component.Component, PastedFile...)}
 * when simulating a file paste in a browserless test.
 *
 * @since 1.1
 */
public final class PastedFile {

    private final String fileName;
    private final @Nullable String contentType;
    private final byte[] content;

    private PastedFile(String fileName, @Nullable String contentType,
            byte[] content) {
        this.fileName = Objects.requireNonNull(fileName,
                "fileName must not be null");
        this.contentType = contentType;
        this.content = Objects.requireNonNull(content,
                "content must not be null");
    }

    /**
     * Creates a pasted file with the given name, content type and bytes.
     *
     * @param fileName
     *            the file name, not {@code null}
     * @param contentType
     *            the MIME type, or {@code null}
     * @param content
     *            the file bytes, not {@code null}
     * @return a new {@link PastedFile}
     */
    public static PastedFile of(String fileName, @Nullable String contentType,
            byte[] content) {
        return new PastedFile(fileName, contentType, content);
    }

    /**
     * Creates a pasted file from a file on disk; the content type is guessed
     * from the file name.
     *
     * @param file
     *            the file to read, not {@code null}
     * @return a new {@link PastedFile}
     * @throws UncheckedIOException
     *             if the file cannot be read
     */
    public static PastedFile of(File file) {
        Objects.requireNonNull(file, "file must not be null");
        return of(file, URLConnection.guessContentTypeFromName(file.getName()));
    }

    /**
     * Creates a pasted file from a file on disk with an explicit content type.
     *
     * @param file
     *            the file to read, not {@code null}
     * @param contentType
     *            the MIME type, or {@code null}
     * @return a new {@link PastedFile}
     * @throws UncheckedIOException
     *             if the file cannot be read
     */
    public static PastedFile of(File file, @Nullable String contentType) {
        Objects.requireNonNull(file, "file must not be null");
        try {
            return new PastedFile(file.getName(), contentType,
                    Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String fileName() {
        return fileName;
    }

    @Nullable
    String contentType() {
        return contentType;
    }

    byte[] content() {
        return content;
    }
}
