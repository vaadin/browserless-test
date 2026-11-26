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

import java.security.Principal;
import java.util.List;

/**
 * A {@link Principal} with a fixed name and a fixed set of roles, for the
 * request mocks to report.
 */
record MockPrincipal(String name,
        List<String> allowedRoles) implements Principal {

    MockPrincipal(String name) {
        this(name, List.of());
    }

    @Override
    public String getName() {
        return name;
    }

    boolean isUserInRole(String role) {
        return allowedRoles.contains(role);
    }
}
