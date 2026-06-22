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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dialog.DialogTester;

@ComponentTesterPackages("com.vaadin.browserless")
@ViewPackages(packages = "com.vaadin.browserless")
public class TesterResolutionTest extends BrowserlessTest {

    @Test
    public void wrapTest_returnsTestWrap() {
        TestComponent tc = new TestComponent();

        Assertions.assertTrue(test(tc) instanceof TestTester);
    }

    @Test
    public void wrapComponentExtendingTest_returnsTestWrap() {
        MyTest mt = new MyTest();

        Assertions.assertTrue(test(mt) instanceof TestTester);
    }

    @Test
    public void wrapOtherComponent_returnsGenericComponentWrap() {
        SpecialComponent sc = new SpecialComponent();
        Assertions
                .assertTrue(test(sc).getClass().equals(ComponentTester.class));
    }

    @Test
    public void wrapTestComponentForConcreteWrapper_returnsNonGenericTestWrap() {
        TestComponentForConcreteTester component = new TestComponentForConcreteTester();
        Assertions.assertEquals(test(component).getClass(),
                NonGenericTestTester.class);
    }

    @Test
    public void wrapDialogSubclass_typedOverload_returnsCustomTester() {
        // Reproduces issue #100: a custom tester extending the concrete
        // DialogTester and wrapping a Dialog subclass. The typed test(Dialog)
        // overload must return the registered custom tester, which is still a
        // DialogTester so the historical cast keeps working.
        CustomDialog dialog = new CustomDialog();

        DialogTester tester = test(dialog);
        Assertions.assertInstanceOf(CustomDialogTester.class, tester);
    }

    @Test
    public void wrapDialogSubclass_explicitTesterClass_returnsCustomTester() {
        // Reproduces issue #100: instantiating a custom tester explicitly must
        // not fail with NoSuchMethodException even though the tester's
        // constructor declares the Dialog subclass rather than Dialog.
        CustomDialog dialog = new CustomDialog();

        CustomDialogTester tester = test(CustomDialogTester.class, dialog);
        Assertions.assertNotNull(tester);
    }

    @Test
    public void wrapDialogSubclass_asComponent_returnsCustomTester() {
        // The registry-based resolution must also instantiate the custom tester
        // when the component is wrapped through the generic test(Component)
        // path.
        Component dialog = new CustomDialog();

        Assertions.assertInstanceOf(CustomDialogTester.class, test(dialog));
    }

    @Test
    void detectComponentType_resolvesComponentTypeThroughHierarchy() {
        Assertions.assertEquals(Component.class,
                TesterRegistry.detectComponentType(ComponentTester.class));
        Assertions.assertEquals(TestComponent.class,
                TesterRegistry.detectComponentType(MyTester.class));
        Assertions.assertEquals(MyTest.class,
                TesterRegistry.detectComponentType(MyExtendedTester.class));
        Assertions.assertEquals(TestComponentForConcreteTester.class,
                TesterRegistry.detectComponentType(NonGenericTestTester.class));
    }

    public static class MyTest extends TestComponent {
    }

    @Tag("div")
    public static class SpecialComponent extends Component {
    }

    static class MyTester<Y, Z extends TestComponent>
            extends ComponentTester<Z> {
        public MyTester(Z component) {
            super(component);
        }
    }

    static class MyExtendedTester<Y> extends MyTester<Y, MyTest> {
        public MyExtendedTester(MyTest component) {
            super(component);
        }
    }

}
