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

## Simulating the browser in testers

A tester interaction has to be indistinguishable from the real one: the events
an application observes must report `isFromClient() == true`, because
application code routinely branches on it to tell "the user did this" from "we
set it programmatically". So never drive a component through its plain
server-side setter from a tester — use the shared `ComponentTester` helpers.

- **`setValueAsUser(value)`** — sets the wrapped component's value as if the
  browser had sent it, so the `ValueChangeEvent` reports
  `isFromClient() == true`. This is the default for any tester action that
  changes a field value. It goes straight to `AbstractFieldSupport` and
  therefore bypasses the component's own `setValue`; when that setter does
  more than store the value — `CheckboxGroup.setValue` also refreshes the
  child check boxes, for instance — the tester has to do the rest itself.
- **`setValueAsUser(field, value)`** — the same, for a field other than the
  wrapped component, such as an editor field owned by it (as `GridProTester`
  does for `custom()` editors). Guard it with
  **`canSetValueAsUser(field)`**: only `AbstractField` and
  `AbstractCompositeField` based fields have a client value path, so a
  foreign `HasValue` implementation has to fall back to a plain
  `setValue`.
- **`setPropertyAsUser(property, value)`** — for state a component exposes as
  a synchronized element property rather than as a field value, such as the
  `opened` property of `Details` and `Accordion`. It pushes the update
  through `ElementPropertyMap.deferredUpdateFromClient` and then round-trips,
  so the derived event (`OpenedChangeEvent`) reports
  `isFromClient() == true`. The property has to be `@Synchronize`d, or the
  call throws.
- **`clearAsUser()` / `clickClearButtonAsUser()`** — build on
  `setValueAsUser` and additionally bypass the set-time validity check,
  because emptying a field stays legal even when it leaves the field invalid.
  See the contracts below.

One consequence to keep in mind: a value change that claims to come from the
client is silently dropped on a read-only field. Any tester method built on
these helpers must therefore call `ensureComponentIsUsable()` first, or the
call quietly does nothing instead of throwing.

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
