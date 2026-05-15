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
package com.vaadin.flow.component.grid;

import com.vaadin.browserless.locator.Locator;

/**
 * Locator/tester for {@link Grid}. The constructor takes the value type as a
 * witness, so the locator and its row accessors are typed:
 *
 * <pre>
 * getGrid(User.class).clickRow(0);
 * User row = getGrid(User.class).getRow(0);
 * </pre>
 *
 * @param <V>
 *            the grid's item type
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GridLocator<V> extends Locator<Grid<V>, GridLocator<V>> {

    /**
     * Creates a locator searching for a {@code Grid<V>} from the UI root.
     *
     * @param valueType
     *            value type witness; only used for static typing
     */
    public GridLocator(Class<V> valueType) {
        super((Class) Grid.class);
    }

    /** Returns the number of items in the matched grid. */
    public int size() {
        return new GridTester<Grid<V>, V>((Grid<V>) component()).size();
    }

    /** Returns the item at the given (0-based) row index. */
    public V getRow(int row) {
        return new GridTester<Grid<V>, V>((Grid<V>) component()).getRow(row);
    }

    /** Clicks the given (0-based) row with the primary mouse button. */
    public void clickRow(int row) {
        new GridTester<Grid<V>, V>((Grid<V>) component()).clickRow(row);
    }

    /** Selects the given (0-based) row. */
    public void select(int row) {
        new GridTester<Grid<V>, V>((Grid<V>) component()).select(row);
    }
}
