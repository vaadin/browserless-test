# Repository

## Technology stack

- **Java 21+**, **Maven** (multi-module, parent `com.vaadin:vaadin-parent`).
- **Vaadin 25 / Flow** as a `provided` dependency, one fixed version per
  branch — see [Flow Version](flow-version.md).
- **Kotlin** only in the test sources (`shared/src/test/kotlin`,
  `junit6/src/test/kotlin`). The main sources are pure Java — see
  [`de-kotlin-plan.md`](../de-kotlin-plan.md).
- **JUnit 6 (Jupiter)** for the published test API and for this repository's
  own tests. A few legacy Kotlin tests still use DynaTest and Karibu DSL.
- **ClassGraph** for classpath scanning (routes, testers).
- **Spotless** with the Eclipse formatter for style; no checkstyle.
- **maven-javadoc-plugin** for Javadoc in every module.

## Module structure

| Module              | Artifact                               | Contents                                                                                     |
| ------------------- | -------------------------------------- | -------------------------------------------------------------------------------------------- |
| `shared`            | `browserless-test-shared`              | The framework: mocked Vaadin environment, 80 component testers, `ComponentQuery`, locators, the multi-user/multi-window contexts. |
| `junit6`            | `browserless-test-junit6`              | JUnit 6 integration (`BrowserlessTest`, `BrowserlessExtension`, `BrowserlessClassExtension`, `TreeOnFailureExtension`) — and most of this repository's tests. |
| `spring`            | `browserless-test-spring`              | Spring / Spring Boot integration, `SpringBrowserlessTest`, Spring Security support.           |
| `quarkus`           | `browserless-test-quarkus`             | Quarkus integration, `QuarkusBrowserlessTest`, CDI and Quarkus Security support.               |
| `junit6-cdi-tests`  | —                                      | CDI integration tests only, kept separate so Weld stays off the other modules' classpath.      |
| `locator-processor` | `browserless-test-locator-processor`   | Internal annotation processor that generates the `*Locator` classes and the `findXxx()` entry points. Not published for end users. |
| `bom`               | `browserless-test-bom`                 | Bill of materials.                                                                             |

Dependency direction is `spring`/`quarkus` → `junit6` → `shared`, with
`locator-processor` wired in as an `annotationProcessorPaths` entry of the
modules that declare testers.

## Where things live

- **A tester** lives in the package of the component it wraps:
  `shared/src/main/java/com/vaadin/flow/component/<component>/<Component>Tester.java`.
  That is deliberate — the tester reads as part of the component's own testing
  surface, and the generated locator is emitted as its sibling.
- **Framework classes** live in `com.vaadin.browserless` in `shared`:
  `ComponentTester`, `ComponentQuery`, `TesterRegistry`, `TesterWrappers`,
  `BrowserlessApplicationContext` / `…UserContext` / `…UIContext`,
  `TestSignalEnvironment`.
- **Locator plumbing** lives in `com.vaadin.browserless.locator`:
  `Locator`, `Locators`, `CommercialLocators` and the `Has*Filter` mixins. The
  `*Locator` classes themselves are generated into
  `target/generated-sources/annotations`.
- **The mocks** live in `com.vaadin.browserless.mocks` (Java):
  `MockService`, `MockVaadinServlet`, `MockRequest`, `MockResponse`,
  `MockHttpSession`, `MockedUI`.
- **Internal helpers** live in `com.vaadin.browserless.internal` (Java):
  `MockVaadin`, `Routes`, `Locator`, `PrettyPrintTree`, `Shortcuts`.
- **Tests** live in `junit6/src/test/java`, mirroring the package of the code
  they cover. See [Testing](testing.md).

## Build

The commands live in [`CLAUDE.md`](../CLAUDE.md); what is worth knowing beyond
them is how the modules interact.

The `locator-processor` module has to be installed before `shared` compiles,
which `mvn install` at the root handles. When you build `shared` alone, include
`-am` so the processor is built too.

CI (`.github/workflows/validation.yml`) runs `spotless:check`, then
`mvn clean install -DskipTests -Djavadocs`, then `mvn test`, and publishes the
surefire reports. Commenting `/format` on a pull request runs `spotless:apply`
and pushes the result.

## Code style

Formatting is applied by `mvn spotless:apply` and validated by
`mvn spotless:check`: the Eclipse formatter profile in
`eclipse/VaadinJavaConventions.xml`, the import order in
`eclipse/flow.importorder`, the Apache 2 license header in
`eclipse/apache2-license-header.txt`, no wildcard imports, and a trailing
newline. Kotlin files get the license header check only.

## Blast radius

The interesting changes in this repository are rarely local.

- `ComponentTester` is the base class of all 80 testers and the source of every
  delegated method on the generated locators. A change to a `protected` helper
  there can change behavior in every tester at once — which is usually the
  point, and always worth a full `mvn test`.
- `LocatorProcessor` output is compiled as part of the build, so a processor
  change can break compilation of modules that have no processor code in them.
  `GeneratedAggregatorsTest` and `LocatorApiTest` are the first places to look.
- The mocked environment (`MockVaadin`, `MockService`, `MockRequest`) stands in
  for the servlet container for every test in every module, including the
  Spring and Quarkus ones. Changes there need the whole test suite, not just
  `junit6`.
- The Spring and Quarkus modules subclass the mocks
  (`MockSpringServlet` and friends), so a constructor or protected-method
  change in `shared` can break them without touching their sources.
