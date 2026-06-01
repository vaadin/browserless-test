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
- **Typed locators** — fluent, compile-time-safe `findButton()` /
  `findTextField()` entry points that combine query filters with tester
  actions
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
        Span cartCount = find(Span.class).withId("cart-count").single();
        assertEquals("1", cartCount.getText());
    }

    @Test
    void queryComponents() {
        navigate(CartView.class);

        // find components by type
        Button btn = find(Button.class).first();
        assertNotNull(btn);
    }
}
```

## Locators API

The Locators API is a typed, fluent layer over `find(Class)` /
`ComponentQuery`. Every built-in component tester has a matching `findXxx()`
entry point that returns a *locator* — an object that exposes both the query
filters *and* that component's tester actions, so you can find and act on a
component in a single chain. Resolution is deferred to the first action and
cached, so a locator can be reused (for example across a `roundTrip()`).

```java
window.findTextField().withId("name").setValue("World");
window.findButton().withText("Save").click();
assertEquals("Saved: World", window.findSpan().withId("echo").getText());
```

### Availability

The typed `findXxx()` / `use(...)` entry points are available out of the box on
every `BrowserlessUIContext` window (`app.newUser().newWindow()`), as used in
the examples here.

The single-user `BrowserlessTest` base class exposes the lower-level
`find(Class)` query API. To get the same typed locators in a plain
`BrowserlessTest`, have your test (or a shared base class) implement
`com.vaadin.browserless.locator.Locators`:

```java
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.locator.Locators;

class CartViewTest extends BrowserlessTest implements Locators {

    @Test
    void addItemToCart() {
        navigate(CartView.class);
        findButton().withText("Add to cart").click();
        assertEquals("1", findSpan().withId("cart-count").getText());
    }
}
```

### Filters

Locators carry the common filters directly: `withId`, `withTestId`,
`withClassName` / `withoutClassName`, `withAttribute` (with or without an
expected value), `withoutAttribute`, and `withCondition` for an arbitrary
typed predicate.

Filters that depend on a component capability are mixed in only where the
component actually supports them, so misuse is a compile error rather than a
runtime surprise:

| Filter                                | Available when the component is |
|---------------------------------------|---------------------------------|
| `withText` / `withTextContaining`     | `HasText`                       |
| `withLabel` / `withLabelContaining`   | `HasLabel`                      |
| `withAriaLabel` / `withAriaLabelContaining` | `HasAriaLabel`            |
| `withValue`                           | `HasValue` (typed to its value) |
| `withTheme` / `withoutTheme`          | `HasTheme`                      |

```java
// Button is HasText — compiles
window.findButton().withText("Save").click();

// TextField is HasLabel + HasValue, but not HasText
window.findTextField().withLabel("Name").setValue("Ada");
window.findTextField().withValue("Ada");   // value type is checked: String here

// window.findTextField().withText("Name");  // does NOT compile
```

For filters not surfaced on the locator (for example `withPropertyValue` or
`withResultsSize`), use the `with(q -> ...)` escape hatch to reach the
underlying `ComponentQuery`:

```java
window.findButton().with(q -> q.withPropertyValue(Button::getText, "Save"))
        .click();
```

### Selecting and scoping

When a filter chain matches more than one component, pick one with `atIndex(n)`
(1-based). Scope the search to a subtree with `inside(component)` or
`inside(otherLocator)` — the latter resolves its parent lazily, at the moment
the child is resolved:

```java
// pick the second button in the view
window.findButton().atIndex(2).click();

// only look inside a resolved parent
window.findButton().inside(window.findButton().withId("toolbar")).click();
```

Beyond the action methods, locators expose `component()` (the single match,
cached), `components()` (all matches), `exists()` (true if anything matches),
and `invalidate()` (drop the cached resolution and the `atIndex` pick so the
next action re-resolves — useful after a UI change replaces the component).

### Seeding with `use(...)`

When the test already holds a component reference, `use(component)` seeds a
locator with it directly instead of running a query:

```java
window.use(form.nameField).setValue("Ada");
window.use(form.submit).click();
```

### Custom locators

For composite components, subclass `Locator<C, SELF>` and compose the built-in
locators, scoping them to the composite's subtree with `inside(this)`:

```java
import com.vaadin.browserless.locator.Locator;
import com.vaadin.flow.component.button.ButtonLocator;
import com.vaadin.flow.component.textfield.TextFieldLocator;

public class PersonFormLocator extends Locator<PersonForm, PersonFormLocator> {

    public PersonFormLocator() {
        super(PersonForm.class);
    }

    public PersonFormLocator fillIn(String name, String email) {
        new TextFieldLocator().withId("pf-name").inside(this).setValue(name);
        new TextFieldLocator().withId("pf-email").inside(this).setValue(email);
        return this;
    }

    public void submit() {
        new ButtonLocator().withId("pf-submit").inside(this).click();
    }
}
```

Invoke a custom locator through the generic `find(Supplier)` entry point:

```java
window.find(PersonFormLocator::new).fillIn("Ada", "ada@example.com").submit();
```

### Relationship to `ComponentQuery`

Locators are the typed convenience layer; `find(Class)` and `ComponentQuery`
remain available for ad-hoc, lower-level queries and for filters not surfaced
on locators. Use whichever fits — they search the same component tree.

## Multi-user and multi-window testing

For tests that need to drive multiple users — or multiple browser windows for
the same user — against a single application, Browserless Test exposes a
layered context API that mirrors the Vaadin hierarchy:

| Context                            | Maps to                          | Created via                                                                                |
|------------------------------------|----------------------------------|--------------------------------------------------------------------------------------------|
| `BrowserlessApplicationContext<C>` | shared `VaadinServletService`    | `BrowserlessApplicationContext.create(viewPackagesOrClasses)` (or a framework factory)     |
| `BrowserlessUserContext`           | one `VaadinSession` (one user)   | `app.newUser()` / `app.newUser(credentials)` / `app.newUser(username, roles...)`           |
| `BrowserlessUIContext`             | one `UI` (one browser window)    | `user.newWindow()`                                                                         |

`BrowserlessUIContext` exposes the same DSL as `BrowserlessTest` (`navigate`,
`find`, `findInView`, `test`, `roundTrip`). Every DSL call automatically activates the
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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedCounterTest {

    @Test
    void twoUsersShareApplicationState() {
        try (var app = BrowserlessApplicationContext
                .create(SharedCounterView.class)) {
            var w1 = app.newUser().newWindow();
            var w2 = app.newUser().newWindow();

            w1.navigate(SharedCounterView.class);
            w2.navigate(SharedCounterView.class);

            // user 1 increments — only their UI reflects it locally
            w1.test(w1.find(Button.class).withText("Increment").single()).click();
            assertEquals("Count: 1", w1.find(Paragraph.class).single().getText());
            assertEquals("Count: 0", w2.find(Paragraph.class).single().getText());

            // user 2 refreshes to observe the shared application state
            w2.test(w2.find(Button.class).withText("Refresh").single()).click();
            assertEquals("Count: 1", w2.find(Paragraph.class).single().getText());
        }
    }
}
```

### Spring

`SpringBrowserlessApplicationContext.create(springCtx, viewPackagesOrClasses)`
wires the application context to the Spring `ApplicationContext` and (when
Spring Security is on the classpath) installs a `SecurityContextHandler` so
per-user authentication is automatically isolated across windows. The
`newUser(username, roles...)` shorthand mirrors `@WithMockUser`.

```java
import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
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
        try (SecuredBrowserlessApplicationContext<Authentication> app =
                SpringBrowserlessApplicationContext.createSecured(
                        springCtx, "com.testapp.security")) {

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

`QuarkusBrowserlessApplicationContext.create(viewPackagesOrClasses)` resolves
Quarkus beans through CDI and installs a `SecurityContextHandler` backed by
`CurrentIdentityAssociation`. The `newUser(username, roles...)` shorthand
builds a matching `QuarkusSecurityIdentity`.

```java
import com.testapp.security.LoginView;
import com.testapp.security.ProtectedView;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessApplicationContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MultiUserSecurityTest {

    @Test
    void securityContextIsIsolatedPerUser() {
        try (SecuredBrowserlessApplicationContext<SecurityIdentity> app =
                QuarkusBrowserlessApplicationContext
                        .createSecured("com.testapp.security")) {

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
w.test(w.find(Button.class).withText("Go to Vaadin").single()).click();
assertEquals("https://vaadin.com/", w.getExternalNavigationURL());

// Page.open("https://payment.example.com/checkout?id=123") — _blank
w.test(w.find(Button.class).withText("Pay").single()).click();
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
