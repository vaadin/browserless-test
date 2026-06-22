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
package com.vaadin.browserless;

import com.vaadin.flow.component.dialog.DialogTester;

/**
 * A custom tester extending the concrete {@link DialogTester} (which binds the
 * generic component type to {@code Dialog}) while wrapping a {@code Dialog}
 * subclass. Its constructor declares the narrower {@link CustomDialog} type,
 * which previously caused instantiation to fail because the constructor was
 * looked up using the generic-resolved {@code Dialog} type.
 */
@Tests(CustomDialog.class)
public class CustomDialogTester extends DialogTester {

    public CustomDialogTester(CustomDialog component) {
        super(component);
    }
}
