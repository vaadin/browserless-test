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
package com.vaadin.browserless.dontscan.classnotfound;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.component.Component;

/**
 * Dummy tester for a component class that does not exist, like a commercial
 * component tester in an application that does not depend on that component.
 * This is used to verify that scanning for testers skips it quietly.
 */
@Tests(fqn = "com.vaadin.browserless.dontscan.classnotfound.MissingComponent")
public class MissingComponentTester extends ComponentTester<Component> {

    public MissingComponentTester(Component component) {
        super(component);
    }
}
