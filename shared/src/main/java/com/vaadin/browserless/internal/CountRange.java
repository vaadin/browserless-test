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
package com.vaadin.browserless.internal;

import java.io.Serializable;
import java.util.Objects;

/**
 * An inclusive range of component counts a lookup will accept.
 * <p>
 * Replaces the {@code kotlin.ranges.IntRange} the Kotlin locator used, so that
 * nothing on the published API carries a Kotlin type.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class CountRange implements Serializable {

    /**
     * Accepts any number of components, which is what a lookup expects unless
     * it is told otherwise.
     */
    public static final CountRange ANY = new CountRange(0, Integer.MAX_VALUE);

    /**
     * Accepts exactly one component.
     */
    public static final CountRange ONE = new CountRange(1, 1);

    /**
     * The smallest accepted count, inclusive.
     */
    private final int start;

    /**
     * The largest accepted count, inclusive.
     */
    private final int endInclusive;

    /**
     * Creates a range. An empty range, i.e. one whose end is below its start,
     * accepts nothing.
     *
     * @param start
     *            the lowest accepted count
     * @param endInclusive
     *            the highest accepted count
     */
    public CountRange(int start, int endInclusive) {
        this.start = start;
        this.endInclusive = endInclusive;
    }

    /**
     * Creates a range accepting exactly the given count.
     *
     * @param count
     *            the only accepted count
     * @return the range
     */
    public static CountRange exactly(int count) {
        return new CountRange(count, count);
    }

    /**
     * Returns the lowest accepted count.
     *
     * @return the start of the range
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the highest accepted count.
     *
     * @return the end of the range, inclusive
     */
    public int getEndInclusive() {
        return endInclusive;
    }

    /**
     * Tells whether the given count falls in this range.
     *
     * @param count
     *            the count to check
     * @return {@code true} if the count is accepted
     */
    public boolean contains(int count) {
        return count >= start && count <= endInclusive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CountRange other)) {
            return false;
        }
        return start == other.start && endInclusive == other.endInclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, endInclusive);
    }

    /**
     * Formats the range the way the Kotlin one did, e.g. {@code 1..5}, since
     * lookup failure messages quote it.
     */
    @Override
    public String toString() {
        return start + ".." + endInclusive;
    }
}
