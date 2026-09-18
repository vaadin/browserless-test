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
package com.vaadin.browserless.trigger;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.clipboard.ClipboardPayload;
import com.vaadin.flow.component.clipboard.ClipboardSimulator;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;
import com.vaadin.flow.component.trigger.internal.ReadFromClipboardAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Built-in {@link ActionSimulator}s that back {@code Clipboard} against the
 * per-UI {@link ClipboardSimulator}: a write stores into it (and reports the
 * copied string), a read serves from it. Both honour the clipboard's denial
 * flags by delivering a {@code NotAllowedError} instead. Image writes have no
 * server-evaluable input and are rejected outright.
 */
final class ClipboardActionSimulators {

    private ClipboardActionSimulators() {
    }

    static void registerInto(Registrar registrar) {
        registrar.register(WriteToClipboardAction.class,
                ClipboardActionSimulators::simulateWrite);
        registrar.register(ReadFromClipboardAction.class,
                ClipboardActionSimulators::simulateRead);
    }

    /**
     * Bridges to {@link TriggerSimulation#register} without a static import.
     */
    @FunctionalInterface
    interface Registrar {
        <A extends Action> void register(Class<A> type,
                ActionSimulator<A> simulator);
    }

    private static void simulateWrite(WriteToClipboardAction action,
            Trigger trigger, SimulationContext context) {
        if (action.getImageInput() != null && action.getTextInput() == null
                && action.getHtmlInput() == null) {
            throw new UnsupportedOperationException(
                    "image clipboard is not supported in browserless tests");
        }
        ClipboardSimulator clipboard = ClipboardSimulator
                .forUI(context.getUI());
        if (clipboard.isWriteDenied()) {
            action.deliverError(trigger, notAllowed("write"));
            return;
        }
        String text = asString(context.evaluate(action.getTextInput()));
        String html = asString(context.evaluate(action.getHtmlInput()));
        clipboard.setContents(text, html);
        // onCopied receives text/plain if present, otherwise text/html.
        String copied = text != null ? text : html;
        action.deliverSuccess(trigger, copied == null ? JacksonUtils.nullNode()
                : JacksonUtils.writeValue(copied));
    }

    private static void simulateRead(ReadFromClipboardAction action,
            Trigger trigger, SimulationContext context) {
        ClipboardSimulator clipboard = ClipboardSimulator
                .forUI(context.getUI());
        if (clipboard.isReadDenied()) {
            action.deliverError(trigger, notAllowed("read"));
            return;
        }
        if (clipboard.isEmpty()) {
            // Empty clipboard => onPayload(null).
            action.deliverSuccess(trigger, JacksonUtils.nullNode());
            return;
        }
        action.deliverSuccess(trigger, JacksonUtils.writeValue(
                new ClipboardPayload(clipboard.text(), clipboard.html())));
    }

    private static Error notAllowed(String operation) {
        return new Error("NotAllowedError",
                "Clipboard " + operation + " denied in browserless test");
    }

    @Nullable
    private static String asString(@Nullable JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }
}
