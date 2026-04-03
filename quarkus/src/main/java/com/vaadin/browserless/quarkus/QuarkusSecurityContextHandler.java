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
package com.vaadin.browserless.quarkus;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

import com.vaadin.browserless.SecurityContextHandler;

/**
 * Quarkus Security implementation of {@link SecurityContextHandler}.
 * <p>
 * Manages the {@link SecurityIdentity} via
 * {@link CurrentIdentityAssociation} for multi-user test isolation.
 * <p>
 * The {@link #setupAuthentication(Object)} method expects a
 * {@link SecurityIdentity} instance as the credentials parameter.
 *
 * @see SecurityContextHandler
 * @see QuarkusBrowserlessApplicationContext
 */
public class QuarkusSecurityContextHandler
        implements SecurityContextHandler<SecurityIdentity> {

    @Override
    public void setupAuthentication(SecurityIdentity credentials) {
        if (credentials != null) {
            getIdentityAssociation().setIdentity(credentials);
        }
    }

    @Override
    public Object saveContext() {
        try {
            return CurrentIdentityAssociation.current();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void restoreContext(Object snapshot) {
        if (snapshot instanceof SecurityIdentity identity) {
            getIdentityAssociation().setIdentity(identity);
        } else {
            clearContext();
        }
    }

    @Override
    public void clearContext() {
        getIdentityAssociation().setIdentity(
                QuarkusSecurityIdentity.builder()
                        .setAnonymous(true).build());
    }

    private CurrentIdentityAssociation getIdentityAssociation() {
        return CDI.current().select(CurrentIdentityAssociation.class).get();
    }
}
