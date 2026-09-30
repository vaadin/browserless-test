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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Serialization helpers for tests, replacing DynaTest's
 * {@code cloneBySerialization()}.
 */
public final class TestSerialization {

    private TestSerialization() {
    }

    /**
     * Serializes and deserializes the given object, so that a test can assert
     * it survives a round trip. A Vaadin session is serialized by a servlet
     * container, so anything reachable from one has to stay serializable.
     *
     * @param object
     *            the object to round-trip
     * @param <T>
     *            the object type
     * @return the deserialized copy
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T cloneBySerialization(T object) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(object);
            }
            try (ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bytes.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (Exception e) {
            throw new AssertionError(
                    "Failed to serialize " + object.getClass().getName(), e);
        }
    }
}
