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
package com.vaadin.flow.component.splitlayout;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
class SplitLayoutTesterTest extends BrowserlessTest {

    SplitLayoutView view;

    @BeforeEach
    public void registerView() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(SplitLayoutView.class);
        view = navigate(SplitLayoutView.class);
    }

    @Test
    void dragSplitterTo_updatesPositionAndNotifiesListener() {
        AtomicReference<SplitLayout.SplitterDragEndEvent> event = new AtomicReference<>();
        view.splitLayout.addSplitterDragEndListener(event::set);

        test(view.splitLayout).dragSplitterTo(30);

        Assertions.assertEquals(30d,
                test(view.splitLayout).getSplitterPosition());
        Assertions.assertNotNull(event.get(),
                "Dragging the splitter should notify the drag end listener");
        Assertions.assertTrue(event.get().isFromClient(),
                "Drag end event should be marked as coming from the client");
    }

    @Test
    void dragSplitterTo_positionOutsideRange_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.splitLayout).dragSplitterTo(-1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.splitLayout).dragSplitterTo(101));
        Assertions.assertEquals(50d,
                test(view.splitLayout).getSplitterPosition(),
                "A refused drag should not move the splitter");
    }

    @Test
    void dragSplitterTo_notUsable_throws() {
        view.splitLayout.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.splitLayout).dragSplitterTo(30));
    }

    @Test
    void getSplitComponents_returnSlottedComponents() {
        Assertions.assertSame(view.primary,
                test(view.splitLayout).getPrimaryComponent());
        Assertions.assertSame(view.secondary,
                test(view.splitLayout).getSecondaryComponent());
    }

    @Test
    void getters_notUsable_throw() {
        view.splitLayout.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.splitLayout)::getSplitterPosition);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.splitLayout)::getPrimaryComponent);
        Assertions.assertThrows(IllegalStateException.class,
                test(view.splitLayout)::getSecondaryComponent);
    }
}
