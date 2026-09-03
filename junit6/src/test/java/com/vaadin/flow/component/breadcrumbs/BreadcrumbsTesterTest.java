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
package com.vaadin.flow.component.breadcrumbs;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
@BrowserlessTestConfig(featureFlags = "breadcrumbsComponent")
class BreadcrumbsTesterTest extends BrowserlessTest {

    BreadcrumbsView view;

    @BeforeEach
    void init() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forApplicationScope();
        routeConfiguration.setAnnotatedRoute(BreadcrumbsView.class);
        routeConfiguration.setAnnotatedRoute(TargetView.class);
        view = navigate(BreadcrumbsView.class);
    }

    @Test
    void getItemTexts_returnsTrailLabels() {
        Assertions.assertEquals(List.of("Home", "Docs", "Current"),
                test(view.breadcrumbs).getItemTexts());
    }

    @Test
    void clickItem_byLabel_navigatesToItemPath() {
        test(view.breadcrumbs).clickItem("Home");
        Assertions.assertEquals("breadcrumbs-target", currentPath());
    }

    @Test
    void clickItem_byIndex_navigatesToItemPath() {
        test(view.breadcrumbs).clickItem(1);
        Assertions.assertEquals("breadcrumbs-target", currentPath());
    }

    @Test
    void getItemPaths_returnsResolvedHrefs() {
        Assertions.assertEquals(
                Arrays.asList("breadcrumbs-target", "breadcrumbs-target", null),
                test(view.breadcrumbs).getItemPaths());
    }

    @Test
    void getItemPaths_skipsHiddenItems() {
        hideItem("Docs");
        Assertions.assertEquals(Arrays.asList("breadcrumbs-target", null),
                test(view.breadcrumbs).getItemPaths());
    }

    @Test
    void getItemTexts_skipsHiddenItems() {
        hideItem("Docs");
        Assertions.assertEquals(List.of("Home", "Current"),
                test(view.breadcrumbs).getItemTexts());
    }

    @Test
    void clickItem_byIndex_skipsHiddenItems() {
        hideItem("Home");
        test(view.breadcrumbs).clickItem(0);
        Assertions.assertEquals("breadcrumbs-target", currentPath());
    }

    @Test
    void clickItem_currentItemWithoutPath_throws() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.breadcrumbs).clickItem("Current"));
    }

    @Test
    void clickItem_unknownLabel_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> test(view.breadcrumbs).clickItem("Nope"));
    }

    @Test
    void notVisible_throws() {
        view.breadcrumbs.setVisible(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.breadcrumbs).clickItem("Home"));
    }

    @Test
    void notAttached_throws() {
        view.breadcrumbs.removeFromParent();
        Assertions.assertThrows(IllegalStateException.class,
                () -> test(view.breadcrumbs).getItemTexts());
    }

    private void hideItem(String text) {
        view.breadcrumbs.getChildren()
                .filter(child -> child instanceof BreadcrumbsItem item
                        && text.equals(item.getText()))
                .forEach(child -> child.setVisible(false));
    }

    private static String currentPath() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }
}
