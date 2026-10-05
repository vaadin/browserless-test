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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;

@Tag("div")
@Route(value = "upload-manager", registerAtStartup = false)
public class UploadManagerView extends Component implements HasComponents {

    final UploadManager manager;
    final UploadButton button;
    final UploadDropZone dropZone;
    final UploadFileList fileList;
    // file name and contents of every file the upload handler received
    final List<String> received = new ArrayList<>();

    public UploadManagerView() {
        manager = new UploadManager(this, UploadHandler
                .inMemory((metadata, data) -> received.add(metadata.fileName()
                        + ":" + new String(data, StandardCharsets.UTF_8))));
        button = new UploadButton("Upload", manager);
        dropZone = new UploadDropZone(manager);
        fileList = new UploadFileList(manager);
        add(button, dropZone, fileList);
    }
}
