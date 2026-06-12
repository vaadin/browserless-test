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
package com.vaadin.flow.component.masterdetaillayout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages
class MasterDetailLayoutTesterTest extends BrowserlessTest {

    MasterDetailLayoutView view;

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(MasterDetailLayoutView.class);
        view = navigate(MasterDetailLayoutView.class);
    }

    @Test
    void getMaster_returnsMasterComponent() {
        assertSame(view.master, test(view.layout).getMaster(),
                "Master accessor should return the component in the master area");
    }

    @Test
    void getMaster_layoutHidden_throws() {
        view.layout.setVisible(false);
        assertThrows(IllegalStateException.class, test(view.layout)::getMaster);
    }

    @Test
    void getDetail_noDetailSet_returnsNull() {
        assertNull(test(view.layout).getDetail(),
                "Detail accessor should return null when no detail is set");
    }

    @Test
    void getDetail_detailSet_returnsDetail() {
        view.layout.setDetail(view.detail);

        assertSame(view.detail, test(view.layout).getDetail(),
                "Detail accessor should return the component in the detail area");
    }

    @Test
    void getDetail_detailCleared_returnsNull() {
        view.layout.setDetail(view.detail);
        view.layout.setDetail(null);

        assertNull(test(view.layout).getDetail(),
                "Detail accessor should return null after detail is cleared");
    }

    @Test
    void getDetail_layoutHidden_throws() {
        view.layout.setVisible(false);
        assertThrows(IllegalStateException.class, test(view.layout)::getDetail);
    }

    @Test
    void getDetailPlaceholder_noDetailSet_returnsPlaceholder() {
        assertSame(view.placeholder, test(view.layout).getDetailPlaceholder(),
                "Placeholder accessor should return the placeholder when no detail is set");
    }

    @Test
    void getDetailPlaceholder_noPlaceholderSet_returnsNull() {
        view.layout.setDetailPlaceholder(null);

        assertNull(test(view.layout).getDetailPlaceholder(),
                "Placeholder accessor should return null when no placeholder is set");
    }

    @Test
    void getDetailPlaceholder_detailSet_throws() {
        view.layout.setDetail(view.detail);

        assertThrows(IllegalStateException.class,
                test(view.layout)::getDetailPlaceholder,
                "Placeholder is hidden while detail content is shown");
    }

    @Test
    void getDetailPlaceholder_layoutHidden_throws() {
        view.layout.setVisible(false);
        assertThrows(IllegalStateException.class,
                test(view.layout)::getDetailPlaceholder);
    }

    @Test
    void isDetailPlaceholderVisible_noDetailSet_true() {
        assertTrue(test(view.layout).isDetailPlaceholderVisible(),
                "Placeholder should be visible when set and no detail is shown");
    }

    @Test
    void isDetailPlaceholderVisible_detailSet_false() {
        view.layout.setDetail(view.detail);

        assertFalse(test(view.layout).isDetailPlaceholderVisible(),
                "Placeholder should be hidden while detail content is shown");
    }

    @Test
    void isDetailPlaceholderVisible_detailCleared_trueAgain() {
        view.layout.setDetail(view.detail);
        view.layout.setDetail(null);

        assertTrue(test(view.layout).isDetailPlaceholderVisible(),
                "Placeholder should be visible again after detail is cleared");
    }

    @Test
    void isDetailPlaceholderVisible_noPlaceholderSet_false() {
        view.layout.setDetailPlaceholder(null);

        assertFalse(test(view.layout).isDetailPlaceholderVisible(),
                "Placeholder cannot be visible when none is set");
    }

    @Test
    void isDetailPlaceholderVisible_layoutHidden_throws() {
        view.layout.setVisible(false);
        assertThrows(IllegalStateException.class,
                test(view.layout)::isDetailPlaceholderVisible);
    }
}
