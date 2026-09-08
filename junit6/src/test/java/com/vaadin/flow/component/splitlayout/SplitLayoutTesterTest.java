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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.splitlayout.SplitLayout.SplitterDragEndEvent;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages
class SplitLayoutTesterTest extends BrowserlessTest {

    SplitLayoutView view;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(SplitLayoutView.class);
        view = navigate(SplitLayoutView.class);
    }

    @Test
    void dragSplitterTo_updatesPositionAndNotifiesListener() {
        List<SplitterDragEndEvent> events = new ArrayList<>();
        view.layout.addSplitterDragEndListener(events::add);

        test(view.layout).dragSplitterTo(30);

        assertEquals(30.0, test(view.layout).getSplitterPosition(),
                "Dragging the splitter should update the splitter position");
        assertEquals(1, events.size(),
                "Dragging the splitter should fire a single drag end event");
        assertTrue(events.get(0).isFromClient(),
                "A dragged splitter is a client-originated change");
    }

    @Test
    void dragSplitterTo_positionSetOnServer_isReplacedByTheDraggedPosition() {
        view.layout.setSplitterPosition(80);

        test(view.layout).dragSplitterTo(25.5);

        assertEquals(25.5, test(view.layout).getSplitterPosition());
    }

    @Test
    void dragSplitterTo_positionWithMoreThanTwoDecimals_isRoundedByComponent() {
        test(view.layout).dragSplitterTo(33.333);

        assertEquals(33.33, test(view.layout).getSplitterPosition(),
                "The component rounds a dragged position to two decimals");
    }

    @ParameterizedTest
    @ValueSource(doubles = { 0, 100 })
    void dragSplitterTo_positionAtRangeEnds_isAccepted(double position) {
        test(view.layout).dragSplitterTo(position);

        assertEquals(position, test(view.layout).getSplitterPosition(),
                "Collapsing a split fully is a legal drag");
    }

    @ParameterizedTest
    @ValueSource(doubles = { -0.5, 100.5 })
    void dragSplitterTo_positionOutsideRange_throws(double position) {
        assertThrows(IllegalArgumentException.class,
                () -> test(view.layout).dragSplitterTo(position));
    }

    @Test
    void dragSplitterTo_layoutHidden_throws() {
        view.layout.setVisible(false);
        assertThrows(IllegalStateException.class,
                () -> test(view.layout).dragSplitterTo(30));
    }

    @Test
    void getSplitterPosition_neverPositioned_returnsNull() {
        assertNull(test(view.layout).getSplitterPosition(),
                "Splitter position is unset until it is set or dragged");
    }

    @Test
    void getPrimaryAndSecondaryComponent_returnSplitContents() {
        assertSame(view.primary, test(view.layout).getPrimaryComponent());
        assertSame(view.secondary, test(view.layout).getSecondaryComponent());
    }

    @Test
    void getPrimaryComponent_emptyLayout_returnsNull() {
        view.layout.removeAll();

        assertNull(test(view.layout).getPrimaryComponent());
        assertNull(test(view.layout).getSecondaryComponent());
    }

    @Test
    void accessors_layoutHidden_throw() {
        view.layout.setVisible(false);

        assertThrows(IllegalStateException.class,
                test(view.layout)::getSplitterPosition);
        assertThrows(IllegalStateException.class,
                test(view.layout)::getPrimaryComponent);
        assertThrows(IllegalStateException.class,
                test(view.layout)::getSecondaryComponent);
    }
}
