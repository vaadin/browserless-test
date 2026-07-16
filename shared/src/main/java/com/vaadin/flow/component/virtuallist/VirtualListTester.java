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
package com.vaadin.flow.component.virtuallist;

import tools.jackson.databind.node.ArrayNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.LitRendererTestUtil;
import com.vaadin.browserless.Tests;
import com.vaadin.flow.automation.Readable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Tester for VirtualList components.
 *
 * @param <T>
 *            component type
 * @param <Y>
 *            value type
 */
@Tests(VirtualList.class)
public class VirtualListTester<T extends VirtualList<Y>, Y>
        extends ComponentTester<T> {

    /**
     * Wrap given component for testing.
     *
     * @param component
     *            target component
     */
    public VirtualListTester(T component) {
        super(component);
    }

    /**
     * Get the amount of items in the virtual list.
     *
     * @return number of items in the virtual list
     */
    public int size() {
        ensureVisible();
        return automation().of(getComponent()).as(Readable.class).count();
    }

    /**
     * Get the item at the given index.
     *
     * @param index
     *            the zero-based index of the item to get
     * @return virtual list item at index
     */
    public Y getItem(int index) {
        ensureVisible();

        return getComponent().getDataCommunicator().getItem(index);
    }

    /**
     * Get the displayed text for the item at index.
     * <p/>
     * The index is zero-based.
     * <p/>
     * The text is the item's displayed text as resolved by the {@link Readable}
     * capability: the item accessible-name generator's value when one is set,
     * otherwise the text the renderer paints (a {@link ComponentRenderer}'s
     * rendered component text, or the single- {@code label} {@link LitRenderer}
     * that {@code setRenderer(ValueProvider)} installs). This is the same
     * display string the AI/inspect surface reads, so the two never diverge.
     * <p/>
     * Unlike the best-effort capability read, this tester method keeps a
     * fail-fast contract: it throws {@link IndexOutOfBoundsException} for an
     * index outside {@code [0, size())}, and
     * {@link UnsupportedOperationException} for a multi-property/function
     * {@link LitRenderer} or any other renderer it does not understand (rather
     * than returning a {@code String.valueOf} guess).
     *
     * @param index
     *            the zero-based index of the item
     * @return the item's displayed text
     * @throws IndexOutOfBoundsException
     *             when the index is negative or not less than {@link #size()}
     * @throws UnsupportedOperationException
     *             when the VirtualList uses a renderer whose text this tester
     *             cannot resolve
     */
    public String getItemText(int index) {
        ensureVisible();

        var readable = automation().of(getComponent()).as(Readable.class);

        // Readable.items() is best-effort: it clamps out-of-range indices and
        // never throws. Restore the tester's fail-fast bounds contract before
        // delegating.
        var size = readable.count();
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "VirtualList item index out of bounds: " + index + " (size "
                            + size + ").");
        }

        // The tester only reports item text for renderers it understands;
        // reject the rest rather than accepting the capability's
        // String.valueOf fallback. A ComponentRenderer and the single-"label"
        // LitRenderer that setRenderer(ValueProvider) installs are exactly what
        // Readable resolves to rendered text.
        var itemRenderer = getItemRenderer();
        if (!(itemRenderer instanceof ComponentRenderer)) {
            if (itemRenderer instanceof LitRenderer<Y> litRenderer) {
                if (!isSingleLabelLitRenderer(litRenderer)) {
                    throw new UnsupportedOperationException(
                            "VirtualListTester is unable to get item text when VirtualList uses a LitRenderer.");
                }
            } else {
                throw new UnsupportedOperationException(
                        "VirtualListTester is unable to get item text for this VirtualList's renderer.");
            }
        }

        // Delegate text resolution to the Readable capability so the tester and
        // the AI/inspect surface share one source of truth for display text.
        return readable.items(null, index, 1).get(0);
    }

    /**
     * Whether the given LitRenderer is the single-{@code label}, no-function
     * shape that {@code VirtualList.setRenderer(ValueProvider)} installs, i.e.
     * the LitRenderer whose displayed text {@link Readable} can resolve.
     */
    private boolean isSingleLabelLitRenderer(LitRenderer<Y> litRenderer) {
        return LitRendererTestUtil.getProperties(litRenderer, this::getField)
                .stream().allMatch(propertyName -> propertyName.equals("label"))
                && LitRendererTestUtil
                        .getFunctionNames(litRenderer, this::getField)
                        .isEmpty();
    }

    /**
     * Get an initialized copy of the component for the item.
     * <p>
     * Note, this is not the actual component.
     *
     * @param index
     *            the zero-based index of the item
     * @return initialized component for the target item
     * @throws IllegalArgumentException
     *             when the VirtualList is not using a ComponentRenderer
     */
    public Component getItemComponent(int index) {
        ensureVisible();

        if (getItemRenderer() instanceof ComponentRenderer<?, Y> componentRenderer) {
            var item = getItem(index);
            return componentRenderer.createComponent(item);
        }
        throw new IllegalArgumentException(
                "VirtualList doesn't use a ComponentRenderer.");
    }

    @SuppressWarnings("unchecked")
    private Renderer<Y> getItemRenderer() {
        var rendererField = getField("renderer");
        try {
            return (Renderer<Y>) rendererField.get(getComponent());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get property value for item's LitRenderer.
     *
     * @param index
     *            the zero-based index of the item
     * @param propertyName
     *            the name of the LitRenderer property
     * @param propertyClass
     *            the class of the value of the LitRenderer property
     * @param <V>
     *            the type of the LitRenderer property
     * @return value of the LitRenderer property
     * @throws IllegalArgumentException
     *             when the VirtualList is not using a LitRenderer or when the
     *             given type of the property does not match the actual property
     *             type
     */
    public <V> V getLitRendererPropertyValue(int index, String propertyName,
            Class<V> propertyClass) {
        ensureVisible();

        if (getItemRenderer() instanceof LitRenderer<Y> litRenderer) {
            return LitRendererTestUtil.getPropertyValue(litRenderer,
                    this::getField, this::getItem, index, propertyName,
                    propertyClass);
        } else {
            throw new IllegalArgumentException(
                    "This VirtualList doesn't use a LitRenderer.");
        }
    }

    /**
     * Invoke named function for item's LitRenderer using the supplied JSON
     * arguments.
     *
     * @param index
     *            the zero-based index of the item
     * @param functionName
     *            the name of the LitRenderer function to invoke
     * @param jsonArray
     *            the arguments to pass to the function
     *
     * @see #invokeLitRendererFunction(int, String)
     * @throws IllegalArgumentException
     *             when the VirtualList is not using a LitRenderer
     */
    public void invokeLitRendererFunction(int index, String functionName,
            ArrayNode jsonArray) {
        ensureVisible();

        if (getItemRenderer() instanceof LitRenderer<Y> litRenderer) {
            LitRendererTestUtil.invokeFunction(litRenderer, this::getField,
                    this::getItem, index, functionName, jsonArray);
        } else {
            throw new IllegalArgumentException(
                    "This VirtualList doesn't use a LitRenderer.");
        }
    }

    /**
     * Invoke named function for item's LitRenderer.
     *
     * @param index
     *            the zero-based index of the item
     * @param functionName
     *            the name of the LitRenderer function to invoke
     *
     * @see #invokeLitRendererFunction(int, String, ArrayNode)
     * @throws IllegalArgumentException
     *             when the VirtualList is not using a LitRenderer
     */
    public void invokeLitRendererFunction(int index, String functionName) {
        invokeLitRendererFunction(index, functionName,
                JacksonUtils.createArrayNode());
    }

}
