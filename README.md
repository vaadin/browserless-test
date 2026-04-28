# Vaadin Browserless Test

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A fast, browser-free UI testing framework for [Vaadin](https://vaadin.com/) 25+ applications.

Browserless Test lets you write unit-style tests for your Vaadin views and
components without launching a browser or a servlet container. Tests run entirely
in-process against a mocked Vaadin environment, giving you millisecond-level
execution times while still exercising real server-side component logic.

It complements [Vaadin TestBench](https://vaadin.com/testbench) (browser-based
end-to-end testing) by covering the fast-feedback layer of the testing pyramid.

## Features

- **65+ built-in component testers** — ready-made wrappers for Grid, Button,
  TextField, ComboBox, Dialog, DatePicker, Upload, Charts, and many more
- **View navigation** — navigate to `@Route`-annotated views with path, query,
  and template parameters
- **Component queries** — find components by type from the current view or any
  parent layout
- **Keyboard shortcut simulation** — fire shortcuts with modifier keys
- **Signals / reactive state** — process pending signal tasks in tests
- **Round-trip simulation** — flush pending server-side changes
- **Component tree debugging** — print the UI tree on test failure with
  `TreeOnFailureExtension`
- **Spring Boot integration** — `SpringBrowserlessTest` base class with full
  Spring context support, including `@WithMockUser` security testing
- **Quarkus integration** — `QuarkusBrowserlessTest` base class with CDI
  injection and `@TestSecurity` support
- **Multi-user / multi-window testing** — drive multiple users and multiple
  browser windows per user against a shared application within a single test;
  Vaadin thread-locals and per-user security context are switched
  automatically as you interact with each window
- **External navigation capture** — assert URLs triggered by
  `Page.setLocation()` and `Page.open()` (including `_blank`, named, and
  `_self` / `_parent` / `_top` targets) without leaving the test
- **Custom testers** — create your own `ComponentTester` implementations and
  register them with `@Tests`

## Modules

| Module      | Artifact ID                | Description                                                                       |
|-------------|----------------------------|-----------------------------------------------------------------------------------|
| **shared**  | `browserless-test-shared`  | Core framework: mocked Vaadin environment, component testers, navigation, queries |
| **junit6**  | `browserless-test-junit6`  | JUnit 6 integration: base classes and extensions                                  |
| **spring**  | `browserless-test-spring`  | Spring / Spring Boot integration                                                  |
| **quarkus** | `browserless-test-quarkus` | Quarkus integration                                                               |
| **bom**     | `browserless-test-bom`     | Bill of Materials for dependency management                                       |

## Requirements

- Java 21+
- Vaadin 25.1+
- Maven (the framework is distributed as Maven artifacts)

## Getting Started

### 1. Import the BOM

Add the BOM to your `<dependencyManagement>` section:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>browserless-test-bom</artifactId>
            <version>${browserless-test.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Add the test dependency

**Spring Boot:**

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-spring</artifactId>
    <scope>test</scope>
</dependency>
```

**Quarkus:**

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-quarkus</artifactId>
    <scope>test</scope>
</dependency>
```

**Plain JUnit 6:**

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-junit6</artifactId>
    <scope>test</scope>
</dependency>
```

## Quick Start

### Spring Boot

```java
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.junit.jupiter.api.Test;

@ContextConfiguration(classes = TestConfig.class)
@ViewPackages(classes = AdminView.class)
class AdminViewTest extends SpringBrowserlessTest {

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessView() {
        AdminView view = navigate(AdminView.class);
        assertNotNull(view);
    }
}
```

### Quarkus

```java
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
@ViewPackages(classes = MainView.class)
class MainViewTest extends QuarkusBrowserlessTest {

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void accessProtectedView() {
        MainView view = navigate(MainView.class);
        assertNotNull(view);
    }
}
```

### Plain JUnit 6

```java
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@ViewPackages(classes = CartView.class)
class CartViewTest extends BrowserlessTest {

    @Test
    void addItemToCart() {
        CartView view = navigate(CartView.class);

        // interact with components through testers
        test(view.getAddButton()).click();

        // find components and verify state
        Span cartCount = $(Span.class).withId("cart-count").single();
        assertEquals("1", cartCount.getText());
    }

    @Test
    void queryComponents() {
        navigate(CartView.class);

        // find components by type
        Button btn = $(Button.class).first();
        assertNotNull(btn);
    }
}
```

## Multi-user and multi-window testing

For tests that need to drive multiple users — or multiple browser windows for
the same user — against a single application, Browserless Test exposes a
layered context API that mirrors the Vaadin hierarchy:

| Context                            | Maps to                          | Created via                                                                          |
|------------------------------------|----------------------------------|--------------------------------------------------------------------------------------|
| `BrowserlessApplicationContext<C>` | shared `VaadinServletService`    | `BrowserlessApplicationContext.create(routes)` (or a framework factory, see below)   |
| `BrowserlessUserContext`           | one `VaadinSession` (one user)   | `app.newUser()` / `app.newUser(credentials)` / `app.newUser(username, roles...)`     |
| `BrowserlessUIContext`             | one `UI` (one browser window)    | `user.newWindow()`                                                                   |

`BrowserlessUIContext` exposes the same DSL as `BrowserlessTest` (`navigate`,
`$`, `$view`, `test`, `roundTrip`). Every DSL call automatically activates the
context: Vaadin thread-locals (`VaadinService`, `VaadinSession`, `UI`,
`VaadinRequest`, `VaadinResponse`) are switched to the target window, and on a
user-switch the outgoing user's security context is saved and the incoming
user's snapshot is restored. You can interleave operations on different
windows freely without manual context switching.

The application context is `AutoCloseable`: closing it (typically via
`try-with-resources`) closes every user and every window in the right order,
fires destroy listeners, and clears Vaadin and security thread-locals.

### Plain Java

```java
import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedCounterTest {

    @Test
    void twoUsersShareApplicationState() {
        Routes routes = new Routes()
                .autoDiscoverViews(SharedCounterView.class.getPackageName());
        try (var app = BrowserlessApplicationContext.create(routes)) {
            var w1 = app.newUser().newWindow();
            var w2 = app.newUser().newWindow();

            w1.navigate(SharedCounterView.class);
            w2.navigate(SharedCounterView.class);

            // user 1 increments — only their UI reflects it locally
            w1.test(w1.$(Button.class).withText("Increment").single()).click();
            assertEquals("Count: 1", w1.$(Paragraph.class).single().getText());
            assertEquals("Count: 0", w2.$(Paragraph.class).single().getText());

            // user 2 refreshes to observe the shared application state
            w2.test(w2.$(Button.class).withText("Refresh").single()).click();
            assertEquals("Count: 1", w2.$(Paragraph.class).single().getText());
        }
    }
}
```

### Spring

`SpringBrowserlessApplicationContext.create(routes, springCtx)` wires the
application context to the Spring `ApplicationContext` and (when Spring
Security is on the classpath) installs a `SecurityContextHandler` so per-user
authentication is automatically isolated across windows. The
`newUser(username, roles...)` shorthand mirrors `@WithMockUser`.

```java
import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.internal.Routes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityTestConfig.class)
class MultiUserSecurityTest {

    @Autowired
    private ApplicationContext springCtx;

    @Test
    void securityContextIsIsolatedPerUser() {
        Routes routes = new Routes().autoDiscoverViews("com.testapp.security");
        try (BrowserlessApplicationContext<Authentication> app =
                SpringBrowserlessApplicationContext.create(routes, springCtx)) {

            var adminWindow = app.newUser("john", "USER").newWindow();
            var anonWindow = app.newUser().newWindow();

            adminWindow.navigate(ProtectedView.class);
            assertInstanceOf(ProtectedView.class, adminWindow.getCurrentView());

            // Anonymous user is redirected to the login view
            assertThrows(IllegalArgumentException.class,
                    () -> anonWindow.navigate(ProtectedView.class));
            assertInstanceOf(LoginView.class, anonWindow.getCurrentView());

            // Switch back — admin's SecurityContext is restored automatically
            adminWindow.navigate(ProtectedView.class);
            assertInstanceOf(ProtectedView.class, adminWindow.getCurrentView());
        }
    }
}
```

For full control over the principal, `app.newUser(authentication)` accepts a
hand-built `Authentication` token.

### Quarkus

`QuarkusBrowserlessApplicationContext.create(routes)` resolves Quarkus beans
through CDI and installs a `SecurityContextHandler` backed by
`CurrentIdentityAssociation`. The `newUser(username, roles...)` shorthand
builds a matching `QuarkusSecurityIdentity`.

```java
import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessApplicationContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MultiUserSecurityTest {

    @Test
    void securityContextIsIsolatedPerUser() {
        Routes routes = new Routes().autoDiscoverViews("com.testapp.security");
        try (BrowserlessApplicationContext<SecurityIdentity> app =
                QuarkusBrowserlessApplicationContext.create(routes)) {

            var adminWindow = app.newUser("john", "USER").newWindow();
            var anonWindow = app.newUser().newWindow();

            adminWindow.navigate(ProtectedView.class);
            assertInstanceOf(ProtectedView.class, adminWindow.getCurrentView());

            assertThrows(IllegalArgumentException.class,
                    () -> anonWindow.navigate(ProtectedView.class));
            assertInstanceOf(LoginView.class, anonWindow.getCurrentView());

            adminWindow.navigate(ProtectedView.class);
            assertInstanceOf(ProtectedView.class, adminWindow.getCurrentView());
        }
    }
}
```

For a hand-built identity, pass it directly:
`app.newUser(QuarkusSecurityIdentity.builder()...build())`.

### Capturing external navigation

When a view triggers `Page.setLocation()` or `Page.open()`, the URL is
captured on the window's mock `Page` and can be asserted directly:

```java
var w = app.newUser().newWindow();
w.navigate(CheckoutView.class);

// Page.setLocation("https://vaadin.com/") — _self navigation
w.test(w.$(Button.class).withText("Go to Vaadin").single()).click();
assertEquals("https://vaadin.com/", w.getExternalNavigationURL());

// Page.open("https://payment.example.com/checkout?id=123") — _blank
w.test(w.$(Button.class).withText("Pay").single()).click();
assertEquals("https://payment.example.com/checkout?id=123",
        w.getExternalNavigationURL("_blank"));

// All windows opened by name (excluding _self / _parent / _top navigations)
Map<String, List<String>> opened = w.getOpenedWindows();
```

`getExternalNavigationURL()` (no argument) covers same-window navigations
(`_self`, `_parent`, `_top`, empty, or `null`);
`getExternalNavigationURL(name)` and `getOpenedWindows()` cover named windows
and `_blank`.

### Notes

- `app.newUser(username, roles...)` requires the application context to be
  configured with a `SecurityContextHandler`; the Spring and Quarkus
  factories install one by default.
- The per-user security snapshot is captured at user-switch time (not on
  every activate), so mutations made while the user is active persist on the
  thread until you switch to a different user — at which point the live
  state is captured into that user's snapshot and restored on every
  subsequent activation.
- Same-user window switches don't touch the snapshot, so per-window UI state
  is preserved across interleaved operations within one user.

## License

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
