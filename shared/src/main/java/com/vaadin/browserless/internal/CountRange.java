/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.internal;

import java.util.Objects;

/**
 * A tiny inclusive integer range, used by {@link SearchSpec#count} to express
 * the expected number of matches. Replaces the previous use of
 * {@code kotlin.ranges.IntRange}; this lets shared compile against pure JDK
 * without the Kotlin stdlib on the compile classpath.
 *
 * <p>Both bounds are inclusive, so {@code new CountRange(1, 1)} matches
 * exactly one component.
 */
public final class CountRange {

    /**
     * Range that accepts any non-negative count (0..Integer.MAX_VALUE).
     */
    public static final CountRange ANY = new CountRange(0, Integer.MAX_VALUE);

    /**
     * Range that accepts exactly zero matches.
     */
    public static final CountRange ZERO = new CountRange(0, 0);

    /**
     * Range that accepts exactly one match.
     */
    public static final CountRange ONE = new CountRange(1, 1);

    private final int start;
    private final int endInclusive;

    /**
     * @param start the lowest count that satisfies this range, inclusive.
     * @param endInclusive the highest count that satisfies this range, inclusive.
     */
    public CountRange(int start, int endInclusive) {
        this.start = start;
        this.endInclusive = endInclusive;
    }

    /**
     * Convenience factory for an exact-count range.
     */
    public static CountRange exactly(int count) {
        return new CountRange(count, count);
    }

    public int getStart() {
        return start;
    }

    public int getEndInclusive() {
        return endInclusive;
    }

    /**
     * @return true iff {@code value} lies in [{@link #getStart()}, {@link #getEndInclusive()}].
     */
    public boolean contains(int value) {
        return value >= start && value <= endInclusive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CountRange)) {
            return false;
        }
        CountRange that = (CountRange) o;
        return start == that.start && endInclusive == that.endInclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, endInclusive);
    }

    @Override
    public String toString() {
        return start + ".." + endInclusive;
    }
}
