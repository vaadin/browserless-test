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
- **Custom testers** — create your own `ComponentTester` implementations and
  register them with `@Tests`

## Modules

| Module | Artifact ID | Description                                                                       |
|--------|-------------|-----------------------------------------------------------------------------------|
| **shared** | `browserless-test-shared` | Core framework: mocked Vaadin environment, component testers, navigation, queries |
| **junit6** | `browserless-test-junit6` | JUnit 6 integration: base classes and extensions                                  |
| **quarkus** | `browserless-test-quarkus` | Quarkus integration                                                               |
| **bom** | `browserless-test-bom` | Bill of Materials for dependency management                                       |

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

**Plain JUnit 6:**

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-junit6</artifactId>
    <scope>test</scope>
</dependency>
```

**Spring Boot:**

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-junit6</artifactId>
    <scope>test</scope>
</dependency>
```

The Spring integration is built into the junit6 module. Use `SpringBrowserlessTest`
as your base class (see [Quick Start](#quick-start) below).

**Quarkus:**

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-quarkus</artifactId>
    <scope>test</scope>
</dependency>
```

## Quick Start

### Plain JUnit 5

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

## License

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
