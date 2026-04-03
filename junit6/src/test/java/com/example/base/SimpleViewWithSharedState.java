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
package com.example.base;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route
public class SimpleViewWithSharedState extends VerticalLayout {

    @Service
    public static class MyService {
        private String state = "initial";

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public SimpleViewWithSharedState(MyService myService) {
        add(new Button("Check", e -> {
            add(new Paragraph("State:" + myService.getState()));
        }));
        add(new Button("Set", e -> {
            myService
                    .setState("New state at " + LocalDateTime.now().toString());
        }));
    }
}
