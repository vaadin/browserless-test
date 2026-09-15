# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

Vaadin Browserless Test is a browser-free UI testing framework for Vaadin
applications. Tests run in-process against a mocked Vaadin environment, so a
test can navigate to a `@Route` view, query the component tree and interact
with components without a browser or a servlet container.

The framework is a thin layer on top of Flow's own server-side classes: it
mocks `VaadinService`, `VaadinSession`, `UI` and the servlet API, and it wraps
each Vaadin component in a *tester* that drives it the way a browser would.

### Technologies

- Java 21+, Maven (multi-module)
- Vaadin 25 / Flow — a `provided` dependency, one fixed version per branch
- Kotlin for the older mock and internal layer in `shared/src/main/kotlin`
- JUnit 6 (Jupiter) for the test API and for this repository's own tests
- An annotation processor (`locator-processor`) that generates the typed
  locator API at build time

### Key Modules

- `shared`: the framework itself — mocked Vaadin environment, the 80 component
  testers, `ComponentQuery`, the multi-user/multi-window contexts
- `junit6`: JUnit 6 integration (`BrowserlessTest`, `BrowserlessExtension`) and
  the bulk of this repository's tests
- `spring`: Spring / Spring Boot integration, including Spring Security
- `quarkus`: Quarkus integration, including CDI and Quarkus Security
- `junit6-cdi-tests`: CDI integration tests, separated to keep Weld off the
  other modules' classpath
- `locator-processor`: internal annotation processor that generates
  `*Locator` classes and the `findXxx()` entry points
- `bom`: bill of materials

See `guidelines/repository.md` for the full module map and
`guidelines/architecture.md` for how the mocked environment fits together.

## Guidelines & Conventions

Always read `CONVENTIONS.md` in full when **authoring** or **reviewing** code,
and before **committing** or **opening a pull request** — it is the canonical
list of checkable conventions.

Design and implementation guidelines live in `guidelines/`. Read the chapters
mapped in `guidelines/overview.md` selectively for the topics your work
touches. Two of them apply to almost every change:

- `guidelines/flow-version.md` — one Browserless Test version targets exactly
  one Flow version (the branch's `flow.version`). **There is no backwards
  compatibility with older Flow releases**, so a missing hook can be added to
  Flow first and used directly here, and version-branching code should be
  deleted rather than kept.
- `guidelines/testers.md` — a tester interaction must be indistinguishable from
  a real user interaction.

## Development Commands

### Building and Testing

```bash
# Build the whole project
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build a single module and what it needs
mvn clean install -pl shared -am

# Run the tests of one module
mvn test -pl junit6

# Run a single test class
mvn test -pl junit6 -Dtest=BasicGridTesterTest

# Run a single test method
mvn test -pl junit6 -Dtest=BasicGridTesterTest#basicGrid_selectionOnClick

# Run the tests matching a pattern
mvn test -pl junit6 -Dtest="*ComboBox*Test"

# Generate the Javadoc/Dokka artifacts the way CI does
mvn clean install -DskipTests -Djavadocs
```

Most tests live in `junit6`, not next to the code they cover in `shared` — see
`guidelines/testing.md` for why and for where to add a new test.

### Code Quality

```bash
# Format code, must be run before every commit
mvn spotless:apply

# Check formatting the way CI does
mvn spotless:check
```

There is no checkstyle in this repository; Spotless (Eclipse formatter, import
order, license headers, no wildcard imports) is the whole of the automated
style check, and the `Format Check` CI job fails on unformatted code.

### Depending on a Flow change

Flow is consumed as a snapshot from `maven.vaadin.com/vaadin-prereleases`. To
use an unreleased Flow change, build it locally and point this repository at
it:

```bash
# in a vaadin/flow checkout, on the branch that matches flow.version here
mvn clean install -DskipTests -pl flow-server -am

# back here — the local snapshot is picked up automatically
mvn clean install
```

Bumping `flow.version` / `vaadin.version` in the root `pom.xml` is a release
activity, not something an individual change does. See
`guidelines/flow-version.md`.
