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

import java.util.Collections;
import java.util.List;

import com.example.SingleParam;
import com.example.TemplatedParam;
import com.example.base.HelloWorldView;
import com.example.base.WelcomeView;
import com.example.failing.FailingAssertionView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.internal.MockRouteNotFoundError;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;

@ViewPackages(packages = "com.example")
public class BrowserlessNavigationTest extends BrowserlessTest {

    @Test
    public void getCurrentView_returnsExpectedView() {
        Assertions.assertTrue(getCurrentView() instanceof WelcomeView,
                "WelcomeView has the empty RouteAlias so it should be served as default view");

        HelloWorldView helloWorldView = navigate(HelloWorldView.class);

        Assertions.assertTrue(getCurrentView().equals(helloWorldView),
                "getCurrentView should return the same instance as gotten on navigation");
    }

    @Test
    public void navigationWithLocation_checksGeneratedViewType() {
        navigate("helloworld", HelloWorldView.class);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> navigate("welcome", HelloWorldView.class),
                "Navigation to path not returning given class should throw");

    }

    @Test
    public void navigationToParameterView_noParameterGiven_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> navigate(SingleParam.class),
                "Illegal argument should be thrown for missing parameter");
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> navigate("param", SingleParam.class),
                "No matching route should be found for string without parameter");
        Assertions.assertTrue(exception.getMessage()
                .contains("Navigation resulted in unexpected class"));
        Assertions.assertTrue(exception.getMessage()
                .contains(MockRouteNotFoundError.class.getName()));
    }

    @Test
    public void navigationWithQueryString_queryParametersReachView() {
        final TemplatedParam view = navigate(
                "template/ORD-1?tab=history&page=2#details",
                TemplatedParam.class);

        Assertions.assertEquals("ORD-1", view.parameter,
                "Route parameter should be resolved from the path, without the query string or fragment");
        Assertions.assertEquals(List.of("history"),
                view.queryParameters.getParameters().get("tab"),
                "Query parameter of the location should be available to the view");
        Assertions.assertEquals(List.of("2"),
                view.queryParameters.getParameters().get("page"),
                "Query parameter of the location should be available to the view");
    }

    @Test
    public void uiNavigationWithQueryStringAndQueryParameters_throwsWithExplanation() {
        // Given both, the query string in the location would be lost
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> UI.getCurrent().navigate("template/ORD-1?tab=history",
                        QueryParameters.of("page", "2")),
                "Giving the query both in the location and separately should be rejected");
        Assertions.assertTrue(
                exception.getMessage().contains("template/ORD-1?tab=history"),
                "Exception should name the offending location, but was: "
                        + exception.getMessage());

        UI.getCurrent().navigate("template/ORD-1",
                QueryParameters.of("tab", "history"));
        Assertions.assertEquals(List.of("history"),
                ((TemplatedParam) getCurrentView()).queryParameters
                        .getParameters().get("tab"),
                "Query parameters given to UI.navigate should reach the view");
    }

    @Test
    public void navigationToFragmentOnlyLocation_keepsCurrentView() {
        final TemplatedParam view = navigate("template/ORD-1",
                TemplatedParam.class);

        // A fragment identifies a place within the page, not a route: it does
        // not replace the current view with the one of the "" route
        UI.getCurrent().navigate("#details");

        Assertions.assertSame(view, getCurrentView(),
                "A fragment-only location should not navigate away from the current view");
    }

    @Test
    public void navigationToViewFailingAnAssertion_reportsTheAssertion() {
        AssertionError error = Assertions.assertThrows(AssertionError.class,
                () -> navigate("failing-assertion", FailingAssertionView.class),
                "The error thrown while entering the view should reach the caller");
        Assertions.assertEquals(FailingAssertionView.MESSAGE,
                error.getMessage(),
                "The caller should see what was thrown, not a wrapper hiding it");
    }

    @Test
    public void navigationToParametrisedView_returnsInstantiatedView() {
        final String PARAMETER = "single";

        final SingleParam single = navigate(SingleParam.class, PARAMETER);
        Assertions.assertEquals(PARAMETER, single.parameter,
                "View should contain given parameter");

        final TemplatedParam param = navigate(TemplatedParam.class,
                Collections.singletonMap("param", PARAMETER));
        Assertions.assertEquals(PARAMETER, param.parameter,
                "Template parameter should be available.");
    }
}
