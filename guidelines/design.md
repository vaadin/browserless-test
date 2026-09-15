# Design

Guidance for the shape of new public API in Browserless Test. Consult this
before starting anything non-trivial: a new tester family, a new entry point on
the test base classes, a new context capability.

`shared`, `junit6`, `spring` and `quarkus` are published artifacts. Their
public and protected members are what applications compile against, and a
tester's public methods are additionally the API of the generated locator — see
[Locators](locators.md).

## Before you start

- **Find the precedent.** There is already a pattern for value testers, for
  selection testers, for the context hierarchy, for DSL mixin interfaces
  (`TesterWrappers`, `Locators`) and for framework integration
  (`Spring…`/`Quarkus…` pairs). Match the existing shape rather than inventing
  a new one.
- **Check whether Flow should own it.** The Flow version is pinned, so the
  cheapest fix for a missing hook is often a Flow pull request rather than a
  clever workaround here. See [Flow Version](flow-version.md).
- **Understand the blast radius.** `ComponentTester`, `LocatorProcessor` and
  the mocks are each used by everything — see the Blast radius section of
  [Repository](repository.md).

## Naming

**A tester is named after the component**: `<Component>Tester`, in the
component's own package. No exceptions, because both the registry lookup and
the generated `find<Component>()` name are derived from the target.

**A tester method is named after the user action.** `click()`,
`selectItem(…)`, `clickClearButton()`, `expand(…)`, `stepUp()`. Not
`setSelectedItem` — that is the component's setter, and a test reading
`test(grid).setSelectedItem(x)` no longer says "a user selected a row".

**A read-only accessor keeps the component's vocabulary**: `getText()`,
`isValid()`, `getSelected()`, `size()`. A test reads better when the assertion
side looks like the component and the action side looks like the user.

**Ask-then-do pairs stay symmetric.** When an action can be refused, the
question that predicts it uses the same word as the action — `isUsable()`
before an interaction, not a second vocabulary for the same condition.

## Visibility

- `public` on a tester = published API of the tester *and* of the locator.
  Deliberate only.
- `protected` for helpers meant for tester subclasses — the machinery a tester
  reuses rather than something a test calls (`setValueAsUser`,
  `setPropertyAsUser`, `roundTrip`, `getField`).
- package-private or `private` for anything the framework alone needs.
- `final` on a method whose contract must not be weakened.
  `ensureComponentIsUsable()` is `public final`: public because a test may
  assert on it, final so a subclass cannot make an interaction silently pass.

## Method shape

**Overload rather than add a flag.** `test(component)` and
`test(TesterType.class, component)`; `newUser()`, `newUser(credentials)` and
`newUser(username, roles…)`. A boolean parameter at a call site says nothing
about what it does.

**Keep the argument the user's mental model.** `selectItem(item)` takes the
item, not its index, when the component is item-based;
`selectItem(null)` is how the combo box testers model unconditional emptying,
because that is what a user clearing a selection does.

**Return `void` from an action, a value from a query.** Locators return
`SELF` from their filter methods so a chain reads as one sentence; tester
actions do not pretend to be chainable.

**Throw the exception the situation is.** `IllegalStateException` when the
component cannot be interacted with (`ensureComponentIsUsable()`),
`IllegalArgumentException` when the *argument* cannot exist (an index out of
range, `null` where the empty value is not `null`). Do not use
`IllegalArgumentException` for a value a browser would have accepted — see
[Testers](testers.md).

## Extension points

When the framework needs a hook a user may want to replace, make it an
interface with a single responsibility and let the integration modules supply
an implementation, the way `SecurityContextHandler` and
`RequestContextHandler` do for Spring and Quarkus. Do not add a boolean to a
core class to select between two behaviors that belong to two environments.

When the framework needs to *know* something about a component that varies per
component, put it on the tester, not in a `switch` in a core class. The
registry exists so the specific knowledge lives in the specific tester.

## Breaking changes

Breaking this framework's own API is allowed between minors, and this
repository does it — but never silently:

- mark the commit and pull request title with `!`;
- say which calls stop compiling and what to write instead, in the first
  bullets of `## What changed`;
- when it costs little, add the new name and deprecate the old one with a
  `@deprecated` pointer to the replacement instead of removing it outright.

A test-source-only change (a renamed test contract interface, for instance) is
not a breaking change for users; say so explicitly rather than leaving a
reviewer to guess.
