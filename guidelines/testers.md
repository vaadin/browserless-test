# Testers

A tester is the wrapper through which a test interacts with one component. This
chapter is the reference for writing and changing one.

## The rule everything else follows from

**A tester interaction has to be indistinguishable from the real one.** The
events an application observes must report `isFromClient() == true`, because
application code routinely branches on it to tell "the user did this" from "we
set it programmatically".

So never drive a component through its plain server-side setter from a tester —
use the shared `ComponentTester` helpers.

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
  `setValueAsUser` and empty the field unconditionally, because emptying it
  stays legal even when that leaves the field invalid. See the contracts in
  [Testing](testing.md).
- **`fireDomEvent(…)`** — for an interaction that is a DOM event rather than a
  value or property change (a click on a non-`Clickable` element, a keyboard
  event). It produces an event with `isFromClient() == true` as well.

One consequence to keep in mind: a value change that claims to come from the
client is silently dropped on a read-only field. Any tester method built on
these helpers must therefore call `ensureComponentIsUsable()` first, or the
call quietly does nothing instead of throwing.

## Usability

`ensureComponentIsUsable()` throws `IllegalStateException` when the component
is not usable, and `isUsable()` reports the same thing without throwing.
Usable means enabled, attached, effectively visible (the whole parent chain is
visible), not inert, and — for a `HasValue` — not read-only.

Every tester method that *changes* something starts with
`ensureComponentIsUsable()` — no exceptions, because without it the change is
dropped silently on a read-only field.

Read accessors are a judgement call and the existing testers go both ways:
`HtmlComponentTester.getText()` and `NumberFieldTester.isValid()` read a
component in any state, while `GridTester.getSelected()` and
`DetailsTester.isOpen()` check usability first, because reading that state only
means something for a component the user can see. Follow the neighbouring
methods in the same tester, and declare the `@throws IllegalStateException`
when you do check.

When a tester subclass has its own usability rules, override both
`isUsable()`/`isComponentReadOnly()` and `notUsableReasons(Consumer<String>)`,
so the exception message explains what was wrong instead of just failing.

## What a `setValue` may and may not refuse

The same "be the browser" principle decides this. A tester commits whatever the
browser would commit: a value outside `min` / `max`, off the `step` scale, or
the empty value on a required field is set and simply leaves the field invalid,
which is exactly the state a test about validation wants to reach. Validity is
therefore *asserted* — the number field and picker testers expose `isValid()`
for it — not enforced at set time.

The exception is a control that physically cannot produce the value: a slider
clamps to its range and snaps to its step, so `RangeInputTester` and
`NumberSliderTester` do refuse out-of-range and off-step values. A text input
truncates what is over `maxLength` and filters out the keystrokes
`allowedCharPattern` does not match, so `TextFieldTester` and `TextAreaTester`
refuse a value that breaks either one — while `minLength`, `pattern` and
required stay validation-only and keep committing an invalid value. Structural
refusals stay too, such as `null` on a field whose empty value is not `null`.

Where the line falls is a question about the control, not about the constraint:
ask whether a user sitting in front of the component could hand the field that
value at all. When they could not, refuse it with an `IllegalArgumentException`
whose message says what the browser does instead, rather than silently
correcting the value — a test that asks for the impossible has a bug in it, and
truncating or clamping behind its back would hide it.

`isValid()` means "not marked invalid, and the current value passes the
component's own default validator" — it delegates to `getDefaultValidator()`
rather than re-checking required / `min` / `max` / `step` by hand. Re-checking
by hand drifts from the component and misses constraints (the `step` scale was
the one the old hand-rolled check never covered).

## Adding a tester

1. Put it in the component's own package,
   `shared/src/main/java/com/vaadin/flow/component/<component>/<Component>Tester.java`,
   extending `ComponentTester<T>` (or `HtmlComponentTester` /
   `HtmlContainerTester` for an HTML element component), with
   `@Tests(<Component>.class)` — or `@Tests(fqn = "…")`, which is what the
   generic and multi-target testers use (`GridTester`, `ComboBoxTester`,
   `NumberFieldTester` for both `NumberField` and `IntegerField`).
2. Keep the type parameter (`<T extends Button>`), so a tester for a component
   subclass can extend it.
3. Model **user actions**, not setters. `click()`, `selectItem(…)`,
   `expand(…)`, `clickClearButton()`. A read-only accessor keeps the
   component's name: `getText()`, `isValid()`, `isOpened()`.
4. Add a typed `test(<Component>)` overload to `TesterWrappers` — or to
   `CommercialTesterWrappers` when the component is in the Charts, Dashboard or
   GridPro package — so tests get the specific tester type without a cast.
5. Keep helpers `protected`. Every **public** method is delegated onto the
   generated locator, so a public helper silently becomes part of the locator
   API. See [Locators](locators.md).
6. If the component implements `HasClearButton`, declare a public
   `clickClearButton()` delegating to `clickClearButtonAsUser()`. The
   annotation processor fails the build otherwise.
7. Add a test class next to the component's package in `junit6`, and implement
   the value-tester contracts that apply. See [Testing](testing.md).

## Reaching component state

Prefer the component's public API. When that is not enough:

- Ask whether the gap belongs upstream, and fix it there — the version is
  pinned, so there is no waiting period. Often that is the component itself in
  `vaadin/flow-components` (a missing getter, a validator it does not expose)
  rather than core Flow. This is the preferred route, see
  [Flow Version](flow-version.md).
- `ComponentTester` has `getField(…)` / `getMethod(…)` reflection helpers for
  the cases that already exist. A new use needs a comment naming the missing
  upstream API and where it belongs.
- For renderer-produced content, use `LitRendererTestUtil` and the renderer
  helpers rather than re-implementing rendering.
- Tree traversal goes through `ComponentUtil.getAllChildren` so slotted and
  virtual children are included — do not hand-roll `getChildren()` walks, or
  the tester will miss a Dialog's content or a Grid's header components.
