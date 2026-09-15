# Locators

The locator API is the typed, fluent layer over `find(Class)` /
`ComponentQuery`: `window.findButton().withText("Save").click()` finds the
component and acts on it in one chain. Almost all of it is generated, which
changes what "designing a locator" means.

## What is generated

`LocatorProcessor` (module `locator-processor`) runs as an annotation processor
over the `@Tests`-annotated `ComponentTester` subclasses in a compilation unit
and emits, into `target/generated-sources/annotations`:

- a `<Component>Locator` class next to each tester, delegating every public
  tester method and carrying the filter chain;
- `GeneratedLocators` and `GeneratedCommercialLocators`, the interfaces that
  hold one `find<Component>()` and one `use(<Component>)` method per locator.

`Locators` and `CommercialLocators` (hand-written, in
`com.vaadin.browserless.locator`) extend the generated interfaces and are what
tests mix in. `BrowserlessUIContext` and the JUnit extensions implement
`Locators`, which is how `window.findButton()` exists at all.

So: **do not hand-write a `*Locator` for a built-in tester.** Add the tester
method and the locator method appears. Hand-written `Locator<C, SELF>`
subclasses are for application-level composites — see the custom-locator
section of the README.

## What the processor enforces

These are build errors, not review comments:

- A tester whose component implements `HasClearButton` must declare a public
  `clickClearButton()`. Partial coverage is the kind of gap you would otherwise
  only find by compiling against the generated locator.
- Two testers may not produce the same `find<X>()` entry method. That happens
  when two `@Tests` targets share a simple name across packages, or when two
  testers cover the same component. One of them has to give.
- A tester may not live in the default package.

And a warning worth heeding: if `GeneratedLocators` is already on the
classpath, generation is skipped. A downstream project that wants its own entry
point sets `-Alocator.entrypoint.fqn=<your.package.YourLocators>`.

## Consequences for tester authors

**Public means public API twice over.** Every public method of a tester is
delegated onto the generated locator, so it is API of the tester *and* of the
locator API. A helper meant for tester subclasses is `protected`. This is what
`refactor!: keep internal tester helpers off the generated locator API` was
about.

**`ComponentTester` base machinery is not delegated.** `getComponent`,
`isUsable`, `setModal`, `find` and `ensureComponentIsUsable` are on the skip
list, because the locator provides its own resolution and usability surface.
`click`, `middleClick` and `rightClick` are *not* skipped: a tester override is
delegated like any other method, and when no tester in the chain declares them
the locator picks them up from its own `implements Clickable<C>`.

**Filters follow the component's capabilities.** The processor walks the target
component's supertypes and mixes in a filter interface per match:

| Component interface | Locator filter mixin                        |
| ------------------- | ------------------------------------------- |
| `HasText`           | `withText` / `withTextContaining`           |
| `HasLabel`          | `withLabel` / `withLabelContaining`         |
| `HasAriaLabel`      | `withAriaLabel` / `withAriaLabelContaining` |
| `HasPlaceholder`    | `withPlaceholder`                           |
| `HasValue<E, V>`    | `withValue`, typed to `V`                   |
| `HasTheme`          | `withTheme` / `withoutTheme`                |

That is why `findButton().withLabel("Save")` is a compile error rather than a
silent no-op. Adding a new filter mixin means adding it to `FILTER_MIXINS` in
the processor, with a `Simple` descriptor for a parameterless mixin or a
`Typed` one when a type argument has to be threaded through (as `HasValue`'s
`V` is).

**Commercial components go to a separate entry point.** Targets in
`com.vaadin.flow.component.charts`, `…dashboard` and `…gridpro` land in
`GeneratedCommercialLocators`, so a test that does not depend on the commercial
artifacts still compiles. A new commercial component's tester needs its package
in that list (default in `LocatorProcessor`, overridable with
`-Alocator.commercial.packages`).

## The processor itself is internal

`browserless-test-locator-processor` is not published for end users, and its
`-A` options are an internal contract — break them freely if a refactor
benefits, no deprecation cycle is owed. What *is* public is the generated
surface: the `findXxx()` methods, the filters and the locator methods.

Generated source deliberately uses fully-qualified names everywhere and keeps a
deterministic iteration order, so output is stable across builds and there is
no import management to get wrong. Keep it that way — a stable generated file
is what makes a processor change reviewable.

## Resolution semantics to preserve

A locator resolves lazily on the first action and caches the result, so it can
be reused across a `roundTrip()`. `invalidate()` drops the cached resolution
and the `atIndex` pick. `inside(otherLocator)` resolves its parent lazily, at
the moment the child is resolved. A change that makes resolution eager, or that
caches across an `invalidate()`, breaks tests that hold a locator in a field.
