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

/**
 * Minimal {@link SecurityContextHandler} that stores a string snapshot in a
 * thread-local, so tests can observe save/restore behaviour without pulling in
 * Spring or Quarkus dependencies.
 */
class CapturingSecurityHandler implements SecurityContextHandler<String> {

    final ThreadLocal<String> live = new ThreadLocal<>();

    @Override
    public void setupAuthentication(String credentials) {
        live.set(credentials != null ? credentials : "<anon>");
    }

    @Override
    public Object saveContext() {
        return live.get();
    }

    @Override
    public void restoreContext(Object snapshot) {
        if (snapshot == null) {
            live.remove();
        } else if (snapshot instanceof String s) {
            live.set(s);
        } else {
            throw new IllegalArgumentException(
                    "Expected a String snapshot, got "
                            + snapshot.getClass().getName());
        }
    }

    @Override
    public void clearContext() {
        live.remove();
    }
}
