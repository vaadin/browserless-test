# Conventions

The canonical list of checkable conventions for this repository. Read it in
full when authoring or reviewing code. The design-level reasoning behind
several of these rules lives in `guidelines/` — see
[`guidelines/overview.md`](guidelines/overview.md).

## Vaadin Version Coupling

One Browserless Test version targets exactly one Vaadin version — the branch's
`flow.version` / `vaadin.version` in the root `pom.xml`, mapped per branch in
[`guidelines/flow-version.md`](guidelines/flow-version.md). Do not write code
that keeps working against an older release. No reflective fallbacks, no
`Class.forName` probes, no "if this method exists" branches, no deprecation
cycles for the sake of an older Flow or an older component.

When what a tester needs is not exposed, add it upstream first and use it
directly here. That is the preferred fix, not a workaround built on reflection.
Upstream is `vaadin/flow` for core server-side API and `vaadin/flow-components`
for the component being wrapped — the component is frequently the right place.
Everything lands in the same release train, so there is no window in which the
hook is missing.

When you touch code that still branches on a version, delete the branch instead
of extending it.

Reflection into Flow or component internals is a last resort, and it needs a
comment saying which upstream API is missing and where it belongs.
`ComponentTester` has `getField(...)` / `getMethod(...)` helpers for the cases
that are already there; do not add new ones without first checking whether the
state can be exposed properly instead.

See [`guidelines/flow-version.md`](guidelines/flow-version.md).

## Simulating the Browser

A tester interaction has to be indistinguishable from the real one: every event
an application observes must report `isFromClient() == true`. Never drive a
component through its plain server-side setter from a tester — use the shared
`ComponentTester` helpers (`setValueAsUser`, `setPropertyAsUser`,
`clearAsUser`, `clickClearButtonAsUser`).

Call `ensureComponentIsUsable()` first in every tester method that changes
state. A value change that claims to come from the client is silently dropped
on a read-only field, so without the check the call quietly does nothing
instead of throwing.

Commit whatever the browser would commit. A value outside `min` / `max`, off
the `step` scale, or the empty value on a required field is set and leaves the
field invalid — that is the state a validation test needs to reach. Assert it
with `isValid()` rather than refusing the value at set time.

Refuse a value only when the real control physically cannot produce it: a
slider clamps to its range and snaps to its step, so `RangeInputTester` and
`NumberSliderTester` do reject out-of-range and off-step values, and a text
input truncates at `maxLength` and filters the keystrokes `allowedCharPattern`
does not match, so `TextFieldTester` and `TextAreaTester` reject a value that
breaks either one. `minLength`, `pattern` and required are validation-only and
keep committing an invalid value. Structural refusals stay too, such as `null`
on a field whose empty value is not `null`.

Read-only state counts towards usability. `isUsable()` is enabled + attached +
effectively visible + not inert + not read-only, and effective visibility walks
the parent chain — a component inside a hidden parent is not usable.

See [`guidelines/testers.md`](guidelines/testers.md).

## Tester API

One tester per component, named `<Component>Tester`, in the same package as the
component it wraps (`com.vaadin.flow.component.<component>`), annotated with
`@Tests(<Component>.class)` — or `@Tests(fqn = "…")` for a generic or
multi-target tester, where a class literal would be raw or would need more than
one entry.

Name a tester method after the user action, not after the component's setter:
`click()`, `selectItem(...)`, `clickClearButton()`, `expand(...)`. A method
that only reads state keeps the component's own name (`getText()`,
`isValid()`).

Keep helper methods for tester authors `protected`, not `public`. Every public
method of a tester is delegated onto the generated locator, so a public helper
becomes public API of the locator API too.

A tester whose component implements `HasClearButton` must declare a public
`clickClearButton()`. `LocatorProcessor` fails the build otherwise.

Resolution of `test(component)` happens by classpath scan of `@Tests`, but the
*typed* overload does not: add a `test(<Component>)` method to `TesterWrappers`
— or to `CommercialTesterWrappers` when the component lives in the Charts,
Dashboard or GridPro package — so a test gets the specific tester type without
a cast.

Do not hand-write a `*Locator` class for a built-in tester — the annotation
processor generates it. Hand-written locators are for application-level
composites (`Locator<C, SELF>` subclasses).

See [`guidelines/design.md`](guidelines/design.md) and
[`guidelines/locators.md`](guidelines/locators.md).

## Mocked Environment

Keep the mocks behaving like the real thing they stand in for, including the
failure modes. `MockRequest.changeSessionId()` throws `IllegalStateException`
without a valid session because a servlet container does; do not simplify a
mock into always succeeding.

Do not add an `UnsupportedOperationException` stub to a mock and leave it. If a
test needs the method, implement it the way the container behaves.

Go through the Vaadin extension points rather than around them — fire service
and session lifecycle events through the `VaadinService` event bus, and forward
`Instantiator` calls to the real instantiator instead of reimplementing them.

New code in `shared` is written in Java. The Kotlin sources under
`shared/src/main/kotlin` are the older mock and internal layer and are being
ported to Java; do not add new Kotlin files there.

See [`guidelines/architecture.md`](guidelines/architecture.md).

## Public API

`shared`, `junit6`, `spring` and `quarkus` are published artifacts, so their
public and protected members are API that applications compile against. The
`locator-processor` options are an internal contract and can be broken freely.

Prefer an existing precedent over a new shape. There is already a pattern for
value testers, for selection testers, for the context hierarchy
(`BrowserlessApplicationContext` → `BrowserlessUserContext` →
`BrowserlessUIContext`) and for the DSL mixin interfaces (`TesterWrappers`,
`Locators`) — match it rather than inventing a new one.

Breaking the framework's own API is allowed between minors, but it is never
silent: mark the commit and PR title with `!`, and say in the description which
calls stop compiling and what to write instead. Prefer adding the new name and
deprecating the old one with a `@deprecated` pointer to the replacement when
that costs little.

This is about the API *this* framework publishes. It does not soften the Flow
rule above — against Flow there is nothing to keep compatible.

## Javadoc

Do not add `@since` tags. They are reconciled against the release history in a
separate pass before a release — see the `Update @since tags` workflow.

Javadoc on a tester method explains what the *user action* is, what it leaves
the component in, and when it throws. `@throws IllegalStateException` when the
method calls `ensureComponentIsUsable()` is part of the contract, not a
detail.

Javadoc describes the code today, not what changed. Change history belongs in
commit messages.

See [`guidelines/documenting.md`](guidelines/documenting.md).

## Testing

Write the tests that should pass first. If they expose problems in the
implementation, fix the implementation — do not rewrite the tests to match a
broken implementation.

Analyze why a test fails, code does not compile, or a build breaks, before
changing anything. Do not start rewriting code.

A new or changed value tester implements the shared contracts that apply —
`ClearContract`, `ClearButtonContract`, `CommitsEmptyValueContract`. The
compiler asks for the hooks.

Tests for code in `shared` go in `junit6`, next to the component package they
cover (`junit6/src/test/java/com/vaadin/flow/component/<component>/`), because
`BrowserlessTest` and the extensions live in `junit6`. Views and fixtures the
tests navigate to go under `com/example/`.

Keep the test count minimal — add only the essential cases. Assert concrete
outputs, not just "not null".

Assert that an interaction reached the application as a user action
(`isFromClient() == true`) and that it is refused on a component that is not
usable. Those two are the cases a tester regresses on silently.

Add a test to the existing test class for the component under change rather
than creating a near-duplicate class.

See [`guidelines/testing.md`](guidelines/testing.md).

## Code Style

Run `mvn spotless:apply` before every commit. The `Format Check` CI job fails
on unformatted code; commenting `/format` on a pull request applies it for you.

No wildcard imports — Spotless rejects them.

Names and comments describe how the code works and why, not what changed from
a previous version.

Use Java text blocks for multi-line strings instead of string concatenation.

## Commit & PR Hygiene

Commit subjects follow Conventional Commits: `<type>: <summary>` or
`<type>(<scope>): <summary>`, under 72 characters, imperative verb, with `!`
before the colon for a breaking change. Types in use here: `feat`, `fix`,
`refactor`, `docs`, `test`, `chore`, `ci`.

Branch names follow the type too — `feat/…`, `fix/…`, `docs/…`, `chore/…`.

Do not sign commits for the tool that wrote them: no `Co-Authored-By` trailer
and no generated-with footer. This repository squash-merges, so those trailers
would end up on `main`.

A pull request description uses the sections this repository already uses, in
this order, omitting the ones that do not apply:

- `## Summary` — what was wrong or missing and what now happens, in plain
  words, no class names
- `## What changed` — one bullet per behavior, identifiers in backticks;
  breaking changes first and spelled out
- `## Use case` — a short realistic snippet, when the change adds API
- `## API Changes` — the public/protected delta, for a change that has one
- `## Test summary` — what each test pins down and why it matters

Wrap lines at about 75 characters, and keep the description readable as a
commit message — that is what it becomes on `main`.
