# Testing

## Where tests live

**Most tests live in `junit6`**, including the tests for code in `shared`.
`BrowserlessTest` and the JUnit extensions are declared in `junit6`, which
depends on `shared` and not the other way round, so any test that drives a
mocked UI through them has to be there. `shared/src/test` keeps only
the few tests that exercise a class in isolation, such as the application
context builder ones.

Layout inside `junit6/src/test`:

- `java/com/vaadin/flow/component/<component>/` — the tester tests, mirroring
  the tester's own package. `<Component>TesterTest`, plus the views and beans
  the test needs (`BasicGridView`, `Person`, …).
- `java/com/vaadin/browserless/` — tests of the framework itself
  (`ComponentQueryTest`, `TesterResolutionTest`, `MultiUserTest`,
  `LocatorApiTest`, `SignalsTest`) and the shared test contracts.
- `java/com/example/…` — views and fixtures that tests navigate to, kept out of
  the framework packages so route scanning stays predictable.
- `kotlin/` — the remaining DynaTest/Karibu-style tests of the Kotlin internals.
  Do not add new ones; write new tests as JUnit 6 in Java.

The Spring, Quarkus and CDI integrations have their own test sources, because
each needs a different container on the classpath. `junit6-cdi-tests` exists
only so Weld does not leak into the other modules.

## The shared value-tester contracts

`clear()` and `clickClearButton()` behave the same on every value tester,
because both delegate to shared `ComponentTester` helpers
(`clearAsUser()` / `clickClearButtonAsUser()`). Their expectations are
therefore asserted once, as `default` test methods on three interfaces in
`junit6/src/test/java/com/vaadin/browserless/`, which each tester's own test
class implements. If you add or change a value tester, implement the ones that
apply — the compiler will ask you for the hooks.

- **`ClearContract`** — implement when the tester declares `clear()`. Asserts
  that `clear()` empties the field with no clear button present, and that it
  refuses to run on a component that is not usable. Hooks: `fieldUnderTest()`
  and `clear()`.
- **`ClearButtonContract`** — implement when the tested component implements
  `HasClearButton`, which is exactly when the tester must declare
  `clickClearButton()` (`LocatorProcessor` fails the build otherwise). Asserts
  that it empties the field when the clear button is visible, and throws when
  the button is hidden or the component is not usable. Hooks:
  `fieldUnderTest()` and `clickClearButton()`.
- **`CommitsEmptyValueContract`** — extends `ClearContract`; implement it
  *instead* when the tester exposes `isValid()`, as the number field and
  picker testers do. Adds the assertion that `setValue(emptyValue)` commits the
  empty value on a required field and leaves the field invalid, rather than
  refusing it. Extra hooks: `setEmptyValue()` and `isValid()`.

`fieldUnderTest()` must return the component already attached and holding a
non-empty value; it is called once per test, and the contract marks the field
required itself.

Testers whose component has no clear button (`DateTimePicker`, the html
`Input`) implement only the `clear()` side; the combo box testers model
unconditional emptying as `selectItem(null)` rather than `clear()`, so they
implement only `ClearButtonContract`.

## Writing tests

- **Write the tests that should pass first.** If they expose problems in the
  implementation, fix the implementation afterwards — do not rewrite the tests
  to match a broken implementation.
- Keep the test count minimal — only the essential cases. More tests are not
  better; focused tests are.
- Assert concrete outputs, not just "not null". Assert the text a cell renders,
  the selection a click produced, the URL that was captured.
- **Assert the two things a tester regresses on silently:** that the
  interaction arrived as a user action (`isFromClient() == true` on the event
  the application observes), and that the method refuses to run on a component
  that is not usable (disabled, hidden, read-only, inert). A tester that quietly
  does nothing passes every other assertion.
- Add to the existing test class for the component under change rather than
  creating a near-duplicate class. New view fixtures go next to the test that
  needs them.
- Do not assert on incidental ordering — theme names in a tree, scan order of
  routes. Those change with unrelated Flow work.
- Cover the error branch, not only the happy path. For a tester that throws,
  assert the *message* condition too when the tester adds its own reason
  (`notUsableReasons`), because that is what a user debugging a failing test
  reads.

## Signals in tests

Signal effects and shared-signal confirmations run on the test thread when the
queue is drained, not on a pool. A test that triggers one calls
`runPendingSignalsTasks()` before asserting. Blocking on a `SignalOperation`
without draining first always times out — if a test hangs on
`operation.result().get(…)`, the queue has not been drained, the write was not
lost.

## Debugging failures

- Analyze *why* a test fails, code does not compile, or a build breaks, before
  changing anything. Do not start rewriting code.
- Print the component tree. `TreeOnFailureExtension` does it on failure, and
  `PrettyPrintTree` is available directly — most "the query found nothing"
  failures are visible immediately in the tree, usually because the component
  is in a slot or a virtual child.
- A component that is present but "not usable" is the second most common cause.
  `notUsableReasons(…)` spells out which condition failed.
- Run the whole module, not just the one test, after touching
  `ComponentTester`, `LocatorProcessor` or the mocks. Those three reach
  everything — see the Blast radius section of [Repository](repository.md).
