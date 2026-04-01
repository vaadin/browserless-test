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

import com.example.TestApplication;
import com.example.base.SimpleViewWithSharedState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {TestApplication.class, BrowserlessTestConfiguration.class})
class MultiTabTest {

    @Autowired
    VaadinTestApplicationContext app;

    @Test
    void user1SetsState_user2SeesChangedState() {
        VaadinTestUiContext ui1 = app.newUser().newWindow();
        VaadinTestUiContext ui2 = app.newUser().newWindow();

        // Both users navigate to the view
        var view1 = ui1
                .navigate(SimpleViewWithSharedState.class);
        var view2 = ui2
                .navigate(SimpleViewWithSharedState.class);
        Assertions.assertNotSame(view1, view2);

        // User 2 clicks "Check" - state should be initial
        ui2.test(ui2.get(Button.class).withText("Check").single()).click();
        Assertions.assertEquals("State:initial",
                ui2.get(Paragraph.class).last().getText());

        // User 1 clicks "Set" to change the shared state
        ui1.test(ui1.get(Button.class).withText("Set").single()).click();

        // User 2 clicks "Check" again - state should be updated
        ui2.test(ui2.get(Button.class).withText("Check").single()).click();

        Paragraph stateParagraph = ui2.get(Paragraph.class).last();
        String text = stateParagraph.getText();
        Assertions.assertTrue(text.startsWith("State:New state at"),
                "Expected state to be changed by user 1, but got: " + text);
    }
}
