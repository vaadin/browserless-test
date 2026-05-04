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

import java.util.ServiceConfigurationError;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RouteDiscoveryFailureMessageTest {

    @Test
    void packagedScan_messageNamesPackagesAndCause() {
        ServiceConfigurationError cause = new ServiceConfigurationError(
                "com.example.MyProvider not a subtype");

        String msg = BaseBrowserlessTest.routeDiscoveryFailureMessage(
                Set.of("com.example.views"), cause);

        Assertions.assertTrue(msg.contains("com.example.views"),
                "message should name the scanned package, was:\n" + msg);
        Assertions.assertTrue(msg.contains("ServiceConfigurationError"),
                "message should name the underlying error type, was:\n" + msg);
        Assertions.assertTrue(
                msg.contains("com.example.MyProvider not a subtype"),
                "message should include the original cause message, was:\n"
                        + msg);
    }

    @Test
    void wholeClasspathScan_messageDescribesFullScan() {
        String msg = BaseBrowserlessTest.routeDiscoveryFailureMessage(
                Set.of(""), new ServiceConfigurationError("boom"));

        Assertions.assertTrue(msg.contains("the whole classpath"),
                "empty package set should be described as a full classpath "
                        + "scan, was:\n" + msg);
    }

    @Test
    void message_pointsAtDiscoverRoutesOverrideAndViewPackages() {
        String msg = BaseBrowserlessTest.routeDiscoveryFailureMessage(
                Set.of("com.example"), new ServiceConfigurationError("boom"));

        Assertions.assertTrue(msg.contains("@ViewPackages"),
                "message should suggest @ViewPackages, was:\n" + msg);
        Assertions.assertTrue(msg.contains("protected Routes discoverRoutes()"),
                "message should show a discoverRoutes() override snippet, "
                        + "was:\n" + msg);
        Assertions.assertTrue(
                msg.contains("new Routes()") && msg.contains("getRoutes().add"),
                "message should show how to populate Routes, was:\n" + msg);
    }
}
