# Reviewer notes — Phase 2: internal/ utilities port

**Branch:** `feat/no-kotlin-internal-utils` (chained on Phase 1)
**Commit:** `3aab059 refactor: port browserless-test internal/ utilities from Kotlin to Java`

## Scope

8 Kotlin utility files in `shared/src/main/kotlin/com/vaadin/browserless/internal/` → 9 Java files in `shared/src/main/java/com/vaadin/browserless/internal/`. Mechanical translation, no behavior change.

Ported: `BasicUtils`, `ComponentUtils`, `ElementUtils`, `DepthFirstTreeIterator`, `Renderers`, `Shortcuts`, `TestingLifecycleHook`, `Utils`.

Not ported (Phase 3 / 4 territory): `Locator.kt`, `MockVaadin.kt`, `PrettyPrintTree.kt`, `Routes.kt`. Their call sites into Phase-2 files were rewritten to use the new Java statics.

## LOC delta

| | Files | LOC |
|---|---|---|
| Kotlin removed | 8 | 1,179 |
| Java added | 9 | 1,729 |
| Kotlin kept (`Matches.kt`) | 1 | 24 |

Expansion ≈ +47%. Same shape as Phase 1.

## Naming convention applied

- Every Kotlin file becomes one Java class of the same name with `public final` modifier and a `private` constructor (pure utility-class shape).
- Top-level functions + extension functions + extension properties all become `public static` methods on that class. Extension receiver becomes the first parameter.
- Extension properties with `set()` blocks emit getter+setter overload pairs (`id_(c)` getter, `id_(c, v)` setter).
- Karibu leading-underscore convention preserved (`_fireEvent`, `_text`, `_isVisible`, `_saneFetchLimit`, `_fireShortcut`, etc.) — deliberate signal for "test-time helper, not real Vaadin API".

## Structural decisions worth knowing

### `TestingLifecycleHook` — two classes

- `TestingLifecycleHook.java` — interface (matches the Kotlin `interface`). Has `static TestingLifecycleHook DEFAULT` for the no-op implementation.
- `TestingLifecycleHooks.java` (plural) — holder class for the mutable global `current` field and the `cleanupDialogs()` static method. Java interfaces can't hold mutable static fields the way a Kotlin top-level `var` can, so this split is necessary.

Callers that used to read/write `testingLifecycleHook` (the global) now use `TestingLifecycleHooks.current`. 6 cross-Kotlin call sites updated.

### `Matches.kt` — single deferred function

`BasicUtils.kt`'s `fun Component.matches(spec: SearchSpec<Component>.() -> Unit): Boolean` uses both `SearchSpec` (Phase 4 territory) and the Kotlin DSL block-parameter idiom. Moving it to Phase 4 alongside the locator engine is cleaner than porting it standalone now. Lives in `internal/Matches.kt` (24 LOC) until then.

Same file also keeps `IntRange.size` extension — 1 line, used by `component/Grid.kt._dump`. My pre-flight grep missed it; the agent caught it during build.

### Dead code dropped

`Utils.kt` had `serializeToBytes`, `deserialize` (inline reified), `serializeDeserialize` — all three with zero callers anywhere in the repo. Not carried across.

## Notable adjustments beyond a strict translation

- **`ComponentUtils.dataProvider` dropped its `HasDataProvider<*>` branch.** The Kotlin `is HasDataProvider<*> -> this.dataProvider` relied on a recursive extension-property lookup that doesn't apply to current Vaadin — `HasDataProvider` only declares `setDataProvider` (no getter). The remaining `instanceof` branches (Grid, Select, ListBoxBase, RadioButtonGroup, CheckboxGroup, ComboBox) cover the actual hierarchy and tests pass.

- **`Element.insertBefore` parent comparison uses `.equals()`**, not `==`. Vaadin's `Element` overrides `equals`, so identity comparison was a latent bug — caught when an `ElementUtilsTest` failed.

- **`checkEditableByUser` calls `component.isAttached()` (instance method)**, not a static helper, so test-subclass overrides like `AttachedTextField.isAttached()` are honored.

- **`Button.caption` extension folded into the generic `caption(Component)` method**. Kotlin's static-dispatch for "more specific extension" doesn't translate to Java; an `if (c instanceof Button)` short-circuit at the top of the generic method produces the same runtime behavior.

## Call-site update count

| Surface | Files | Count |
|---|---|---|
| Java | shared + spring + quarkus | 12 sites across ~10 files |
| Kotlin (production) | Locator, MockVaadin, PrettyPrintTree, Routes, Grid | ~25 sites |
| Kotlin (test) | 11 test files | ~200+ sites |

Test-side rewrites are bulk: each `component._foo()` extension call becomes `BasicUtils._foo(component)`. Eyeballed during the port to avoid Java-static-dispatch surprises (e.g., the `Button.caption` vs `Component.caption` case above).

## Validation

| Suite | Tests |
|---|---|
| shared | 20 |
| junit6 | 1007 |
| spring | 29 |
| quarkus | 27 |

All green after a clean rebuild. (One mid-flight false alarm: a stale `~/.m2` artifact from a branch-hop made quarkus security tests appear to fail; clean rebuild from this branch's source passes all 27.)

## Runtime classpath — still unchanged

Phase 2 doesn't drop any Kotlin jars either. The remaining Kotlin files (`Locator.kt`, `MockVaadin.kt`, `PrettyPrintTree.kt`, `Routes.kt`, `Matches.kt`, `component/Grid.kt`) keep both kotlin-stdlib and kotlin-reflect on the classpath. `kotlin-reflect` actually drops earlier on a different branch — see Phase 3 notes.

## Branch state

Chained on `feat/no-kotlin-mocks` (`162a1ad`). When the Phase 1 PR lands on `origin/main`, this branch should rebase cleanly. The two phases can be reviewed and merged in order or as a stacked pair.
