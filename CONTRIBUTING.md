# Contributing

Thanks for contributing to Vaadin Browserless Test! A few conventions to keep in
mind when opening a pull request.

## Javadoc `@since` tags

Individual pull requests should **not** add `@since` tags to new public API.

`@since` values are added in a single, separate pass just before a release, once
the target version is known. Adding them per-PR is error-prone: the version a
change actually ships in may differ from what is current on the branch (releases
slip, changes get backported, etc.), so tags added during development are often
wrong by the time the API is published. Deferring them to a pre-release pass
keeps the tags accurate.

## Test contracts for value testers

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
- **`RefusesEmptyValueContract`** — extends `ClearContract`; implement it
  *instead* when the tester's `setValue` rejects the empty value on a required
  field, as the number field and picker testers do. Adds the assertion that
  `setValue(emptyValue)` throws and leaves the value alone while `clear()`
  still empties the field — the validity-check bypass that `clear()` exists
  for. Extra hook: `setEmptyValue()`.

`fieldUnderTest()` must return the component already attached and holding a
non-empty value; it is called once per test, and the contract marks the field
required itself.

Testers whose component has no clear button (`DateTimePicker`, the html
`Input`) implement only the `clear()` side; the combo box testers model
unconditional emptying as `selectItem(null)` rather than `clear()`, so they
implement only `ClearButtonContract`.
