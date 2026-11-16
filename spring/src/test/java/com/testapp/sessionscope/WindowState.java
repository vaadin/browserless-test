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
package com.testapp.sessionscope;

import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.annotation.UIScope;

/**
 * A {@code @UIScope} bean: every window (UI) of a user gets its own instance.
 */
@Component
@UIScope
public class WindowState {
}
