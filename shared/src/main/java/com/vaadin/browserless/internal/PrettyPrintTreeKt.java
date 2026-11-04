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

/**
 * Forwards to {@link PrettyPrintTree}, for code compiled against the former
 * Kotlin file facade of the same name.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @deprecated use {@link PrettyPrintTree} instead.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class PrettyPrintTreeKt {

    private PrettyPrintTreeKt() {
    }

    public static boolean getPrettyPrintUseAscii() {
        return PrettyPrintTree.getPrettyPrintUseAscii();
    }

    public static void setPrettyPrintUseAscii(boolean p0) {
        PrettyPrintTree.setPrettyPrintUseAscii(p0);
    }

    public static java.lang.String toPrettyTree(
            com.vaadin.flow.component.Component p0) {
        return PrettyPrintTree.toPrettyTree(p0);
    }

    public static java.lang.String toPrettyString(
            com.vaadin.flow.component.Component p0) {
        return PrettyPrintTree.toPrettyString(p0);
    }

    public static kotlin.jvm.functions.Function2<com.vaadin.flow.component.Component, java.util.LinkedList<java.lang.String>, kotlin.Unit> getPrettyStringHook() {
        return PrettyPrintTree.getPrettyStringHook();
    }

    public static void setPrettyStringHook(
            kotlin.jvm.functions.Function2<? super com.vaadin.flow.component.Component, ? super java.util.LinkedList<java.lang.String>, kotlin.Unit> p0) {
        PrettyPrintTree.setPrettyStringHook(p0);
    }

    public static java.util.Set<java.lang.String> getDontDumpAttributes() {
        return PrettyPrintTree.getDontDumpAttributes();
    }

    public static void setDontDumpAttributes(
            java.util.Set<java.lang.String> p0) {
        PrettyPrintTree.setDontDumpAttributes(p0);
    }
}
