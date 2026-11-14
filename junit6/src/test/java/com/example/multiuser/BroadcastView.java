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
package com.example.multiuser;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

/**
 * A view that broadcasts a message to every attached instance, in every
 * session, used for testing cross-session UI updates. "Broadcast" updates the
 * other instances directly, which needs their session lock; "Broadcast with
 * access" goes through {@code UI.access()}.
 */
@Route("broadcast")
public class BroadcastView extends VerticalLayout {

    private static final Set<BroadcastView> attached = new CopyOnWriteArraySet<>();

    private final Paragraph message = new Paragraph("No message");
    private VaadinSession updatedInSession;

    public BroadcastView() {
        Button broadcast = new Button("Broadcast",
                e -> attached.forEach(view -> view.show("Hello")));
        Button broadcastWithAccess = new Button("Broadcast with access",
                e -> attached.forEach(view -> view.getUI().orElseThrow()
                        .access(() -> view.show("Hello"))));
        add(message, broadcast, broadcastWithAccess);
    }

    private void show(String text) {
        message.setText(text);
        updatedInSession = VaadinSession.getCurrent();
    }

    public String getMessage() {
        return message.getText();
    }

    /**
     * Returns the session that was current when the message was last shown.
     *
     * @return the session current during the last update, or {@code null}
     */
    public VaadinSession getUpdatedInSession() {
        return updatedInSession;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        attached.add(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        attached.remove(this);
    }
}
