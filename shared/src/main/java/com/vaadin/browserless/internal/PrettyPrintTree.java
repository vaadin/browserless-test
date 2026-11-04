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

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;

/**
 * Utility class to create a pretty-printed ASCII tree of arbitrary nodes that
 * can be printed to the console. You can build the tree out of any tree
 * structure, just fill in this node {@link #getName() name} and its
 * {@link #getChildren() children}.
 * <p>
 * To create a pretty tree dump of a Vaadin component, just use
 * {@link Companion#ofVaadin(Component)}.
 */
public final class PrettyPrintTree {

    /**
     * Provides {@link Companion#ofVaadin(Component)}.
     */
    public static final Companion Companion = new Companion();

    private final String name;
    private final List<PrettyPrintTree> children;
    private final char pipe;
    private final String branchTail;
    private final String branch;

    /**
     * Creates a tree node.
     *
     * @param name
     *            the node name.
     * @param children
     *            the node children, mutable.
     */
    public PrettyPrintTree(String name, List<PrettyPrintTree> children) {
        this.name = name;
        this.children = children;
        boolean ascii = PrettyPrintTreeKt.getPrettyPrintUseAscii();
        this.pipe = !ascii ? '│' : '|';
        this.branchTail = !ascii ? "└── " : "\\-- ";
        this.branch = !ascii ? "├── " : "|-- ";
    }

    /**
     * Returns the node name.
     *
     * @return the node name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the node children.
     *
     * @return the mutable list of children.
     */
    public List<PrettyPrintTree> getChildren() {
        return children;
    }

    /**
     * Prints this tree.
     *
     * @return the pretty-printed tree.
     */
    public String print() {
        StringBuilder sb = new StringBuilder();
        print(sb, "", true);
        return sb.toString();
    }

    private void print(StringBuilder sb, String prefix, boolean isTail) {
        sb.append(prefix + (isTail ? branchTail : branch) + name + "\n");
        for (int i = 0; i < children.size() - 1; i++) {
            children.get(i).print(sb, prefix + (isTail ? "    " : pipe + "   "),
                    false);
        }
        if (children.size() > 0) {
            children.get(children.size() - 1).print(sb,
                    prefix + (isTail ? "    " : pipe + "   "), true);
        }
    }

    /**
     * Factory methods for {@link PrettyPrintTree}.
     */
    public static final class Companion {

        private Companion() {
        }

        /**
         * Creates a tree out of the given Vaadin component and all of its
         * descendants, as provided by
         * {@link TestingLifecycleHook#getAllChildren(Component)}.
         *
         * @param root
         *            the root component.
         * @return the tree.
         */
        public PrettyPrintTree ofVaadin(Component root) {
            PrettyPrintTree result = new PrettyPrintTree(
                    PrettyPrintTreeKt.toPrettyString(root), new ArrayList<>());
            for (Component child : TestingLifecycleHookKt
                    .getTestingLifecycleHook().getAllChildren(root)) {
                result.children.add(ofVaadin(child));
            }
            return result;
        }
    }
}
