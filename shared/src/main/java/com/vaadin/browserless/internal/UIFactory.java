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
package com.vaadin.browserless.internal;

import java.io.Serializable;

import kotlin.jvm.functions.Function0;

import com.vaadin.flow.component.UI;

/**
 * Produces {@link UI} instances. The factory must provide a new, fresh instance
 * of the UI on every invocation.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@FunctionalInterface
public interface UIFactory extends Function0<UI>, Serializable {
}
