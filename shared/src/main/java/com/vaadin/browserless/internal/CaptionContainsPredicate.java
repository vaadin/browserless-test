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

import java.util.Objects;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;

/**
 * Matches components whose caption contains given substring.
 */
final class CaptionContainsPredicate<T extends Component>
        implements Predicate<T> {

    private final String substring;

    CaptionContainsPredicate(String substring) {
        this.substring = Objects.requireNonNull(substring, "substring");
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean test(T t) {
        return ComponentUtilsKt.getCaption(t).contains(substring);
    }

    @Override
    public String toString() {
        return "captionContains('" + substring + "')";
    }

    @Override
    public int hashCode() {
        return substring.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof CaptionContainsPredicate<?> that
                        && substring.equals(that.substring));
    }
}
