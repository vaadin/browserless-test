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
package com.vaadin.browserless.internal

import com.github.mvysny.dynatest.DynaTest
import java.net.URL
import java.util.*
import kotlin.test.expect
import com.vaadin.flow.component.button.Button

class AllTests : DynaTest({

    beforeEach {
        // make sure that Validator produces messages in English
        Locale.setDefault(Locale.ENGLISH)
    }

    test("flow-build-info.json doesn't exist") {
        val res: URL? = Thread.currentThread().contextClassLoader.getResource("META-INF/VAADIN/config/flow-build-info.json")
        expect(null, "flow-build-info.json exists on the classpath!") { res }
    }

    group("Depth First Tree Iterator") {
        depthFirstTreeIteratorTests()
    }

    group("basic utils") {
        basicUtilsTestbatch()
    }

    group("Element Utils") {
        elementUtilsTests()
    }

    group("Component Utils") {
        componentUtilsTests()
    }

    group("routes test") {
        routesTestBatch()
    }

    group("mock vaadin") {
        mockVaadinTest()
    }

    group("pretty print tree") {
        prettyPrintTreeTest()
    }

    group("locator") {
        group("with lifecycle hook testing") {
            locatorTest()
        }
        group("no lifecycle hook testing") {
            locatorTest2()
        }
    }

    group("Composite") {
        compositeTests()
    }

    group("search spec") {
        searchSpecTest()
    }

    group("shortcuts") {
        shortcutsTestBatch()
    }

    test("Component.isTemplate does not fail without polymer templates dependency") {
        expect(false) { Button("foo").isTemplate }
    }


})
