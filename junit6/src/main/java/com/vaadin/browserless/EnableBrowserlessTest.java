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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Enables browserless multi-user testing with Spring Boot. Adds a
 * {@link VaadinTestApplicationContext} bean that can be injected with
 * {@code @Autowired}.
 *
 * <pre>
 * {@code
 * @SpringBootTest
 * @EnableBrowserlessTest
 * class MyTest {
 *     @Autowired
 *     VaadinTestApplicationContext app;
 *
 *     @Test
 *     void test() {
 *         VaadinTestUiContext ui = app.newUser().newWindow();
 *         ui.navigate(MyView.class);
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(BrowserlessTestConfiguration.class)
@TestExecutionListeners(listeners = BrowserlessTestConfiguration.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface EnableBrowserlessTest {
}
