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
package com.vaadin.browserless.mocks;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.di.Instantiator;

@SuppressWarnings("deprecation")
class MockInstantiatorTest {

    @Test
    void create_returnsTheInstantiatorUnwrapped() {
        Instantiator instantiator = (Instantiator) Proxy.newProxyInstance(
                MockInstantiatorTest.class.getClassLoader(),
                new Class<?>[] { Instantiator.class },
                (proxy, method, args) -> null);

        Assertions.assertSame(instantiator,
                MockInstantiator.create(instantiator),
                "Wrapping the instantiator can only drop methods it does not forward, so create() must hand it back as is");
    }
}
