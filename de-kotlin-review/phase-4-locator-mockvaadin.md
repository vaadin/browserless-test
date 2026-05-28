# Reviewer notes — Phase 4: Locator + MockVaadin port

**Branch:** `feat/no-kotlin-locator-mockvaadin` (chained on amended Phase 3)
**Commit:** `a448aa0 refactor: port Locator + MockVaadin to Java, keep tiny Kotlin DSL shim`

Phase 4 is the **medium-risk** phase per `de-kotlin-plan.md`. The locator engine is the most behaviorally sensitive surface in the library, and `MockVaadin` is the central setup/teardown utility every test calls. The port is straight translation — no behavior change.

## Scope

Two Kotlin files in `shared/src/main/kotlin/com/vaadin/browserless/internal/` → Java in `shared/src/main/java/com/vaadin/browserless/internal/`:

- `Locator.kt` (418 LOC) → split into `Locator.java` + `SearchSpec.java`
- `MockVaadin.kt` (581 LOC) → split into `MockVaadin.java` + `SessionObjects.java` + `UIFactory.java` + `MockRequestCustomizer.java`

The `Matches.kt` shim from Phase 2 is renamed `LocatorDsl.kt` (138 LOC) and grows to hold the Kotlin DSL conveniences over the Java engine, preserving test-side syntax.

## LOC delta

| | Files | LOC |
|---|---|---|
| Kotlin removed | 3 (Locator, MockVaadin, Matches) | ~1,024 |
| Java added | 6 (Locator, SearchSpec, MockVaadin, SessionObjects, UIFactory, MockRequestCustomizer) | ~1,561 |
| Kotlin shim added | 1 (LocatorDsl) | 138 |

Expansion ≈ +66%. Highest of any phase so far — primarily due to:
- SearchSpec's explicit field accessors + setters (~150 LOC of straightforward getters/setters).
- MockVaadin's explicit try/catch around what Kotlin let propagate silently.
- Java requiring explicit `equals/hashCode/toString/copy` for the data-class equivalent on SessionObjects.

## Why a Kotlin shim survives

Phase 4 keeps `LocatorDsl.kt` (~138 LOC) in `shared/src/main/kotlin/` deliberately:

- Java has no equivalent of Kotlin's `inline fun <reified T>` or `SearchSpec<T>.() -> Unit` DSL receiver syntax.
- Test code calls like `Button()._get(Button::class.java) { caption = "bar" }` would each need a 3-line Java-style rewrite if the shim disappeared. ~20 sites in `LocatorTest.kt` alone.
- `kotlin-stdlib` is **still** required on shared's classpath after this phase regardless (Grid.kt is still Kotlin), so adding back a thin Kotlin shim costs nothing dependency-wise.

`LocatorDsl.kt` will be inlined/test-moved/deleted in Phase 5 when Grid is ported and `kotlin-stdlib` can be dropped from shared.

## Notable design decisions

### `UIFactory` SAM is `invoke()`, not `create()`

The Kotlin original was `fun interface UIFactory : () -> UI, Serializable`. The bytecode interface extends `kotlin.jvm.functions.Function0<UI>` with `invoke()` as the SAM method. Many Java callers pass `MockedUI::new` lambdas that are compatible because the SAM signature is `() -> UI`.

The Java port keeps `invoke()` as the method name. Two reasons:
1. Existing `MockedUI::new` method references continue to bind without changes.
2. Three Spring constructors that were `@Deprecated(forRemoval=true)` and accepted `Function0<UI>` previously could still bridge via `uiFactory::invoke`.

If a future cleanup wants to rename to something more Java-idiomatic (`create()` / `newUI()`), it's a single rename + the call sites.

### Three Spring deprecated constructors removed

`MockSpringServlet`, `MockSpringServletService`, `MockSpringVaadinSession` each had a `@Deprecated(forRemoval=true)` constructor accepting `kotlin.jvm.functions.Function0<UI>`. With the new Java `UIFactory` no longer extending Function0, the old constructors became ambiguous at call sites that pass `MockedUI::new` (the lambda matches both the new `UIFactory` overload and the deprecated `Function0` overload).

These constructors:
- Had `@Deprecated(forRemoval=true)` annotations (signalling planned removal).
- Had zero internal callers within this repo.
- Were the only references pulling `kotlin.jvm.functions.Function0` into the Spring module's API surface.

**This is the only externally-visible breaking-API change in Phases 1–4.** Worth a sign-off from the Spring module owner. If a known external caller depends on these constructors, an alternative is to keep them but rename UIFactory's SAM to avoid the ambiguity.

### `SearchSpec.toPredicate()` returns Java `Predicate<T>`

The Kotlin original returned `(T) -> Boolean` which is invoked as `predicate(component)`. The Java port returns `Predicate<T>` invoked as `predicate.test(component)`. Six call sites in `SearchSpecTest.kt` updated.

### `SearchSpec.count` keeps `kotlin.ranges.IntRange`

This is a deliberate scoping choice for Phase 4. Replacing `IntRange` with a Java range type would have widened the diff into `ComponentQuery.java` (the main Java caller) and into every test that constructs a SearchSpec. Defer to Phase 5 alongside the IntRange.size helper.

### `MockVaadin.runUIQueue` uses `sneakyThrow`

The Kotlin original re-throws captured exceptions with bare `throw errors[0]` (Kotlin treats checked exceptions as unchecked). The Java equivalent is the standard `@SuppressWarnings("unchecked")` cast trick. Without it, `AsyncTest.clientRoundtrip() propagates failures` would wrap the caught throwable in a `RuntimeException` and the test's `expectThrows(ExecutionException::class)` would fail.

### Added one extra `setup(UIFactory)` overload

Not strictly in the brief — needed because Kotlin tests calling `MockVaadin.setup(UIFactory { ui })` need unambiguous resolution against the existing `setup(Routes)` and `setup(VaadinServlet)` overloads. One extra Java method, ~10 LOC.

## Cross-call-site update count

| Surface | Files | Count |
|---|---|---|
| Java production | ComponentQuery (4), BrowserlessUIContext (1 import), Spring×3 (deprecated ctor removals) | small |
| Kotlin tests | MockVaadinTest (4 named-arg → positional), SearchSpecTest (6 `.invoke` → `.test` + apply rewrites; 110-line diff) | ~14 |
| Kotlin tests via shim | LocatorTest (20+ sites), AsyncTest, others | **zero** — shim preserves syntax |

Java callers of MockVaadin's static methods (`MockVaadin.setup`, `MockVaadin.tearDown`, `MockVaadin.createUI`, etc.) didn't change at all — Kotlin `@JvmStatic` on a Kotlin `object` already exposed the methods as Java statics, so renaming the underlying class from a Kotlin object to a Java class preserves the call-site syntax exactly.

## Validation

| Suite | Tests |
|---|---|
| shared | 20 |
| junit6 | 1007 |
| spring | 29 |
| quarkus | 27 |

All green after a clean rebuild. `mvn dependency:tree -pl shared` confirms `kotlin-reflect` still absent (from Phase 3); `kotlin-stdlib` (+jdk7/jdk8) still present (needed by `Grid.kt` + `LocatorDsl.kt`).

## Phase 3 fix folded in

While porting Phase 4, the agent caught a leftover Kotlin syntax in `junit6/src/main/java/com/vaadin/browserless/TreeOnFailureExtension.java`:

```java
PrettyPrintTree.Companion.ofVaadin(UI.getCurrent())
```

This stopped compiling when Phase 3 ported `PrettyPrintTree` to Java (no more `.Companion`). My Phase 3 verification missed it because the file is a JUnit extension that only activates on test failure — no test exercised the code path. The fix (one line) was amended into the Phase 3 commit (`c983213` supersedes the original `2ab03ca`) and Phase 4 was rebased on top.

Lesson: `rm -rf target/` should cover sibling modules, not just `shared/`. Added to the footguns list in `overview.md`.

## Branch state

Chained on `feat/no-kotlin-pretty-routes` (Phase 3, now at amended commit `c983213`). The cumulative stack:

- Phase 1 → `162a1ad`
- Phase 2 → `3aab059`
- Phase 3 → `c983213` (amended to include the TreeOnFailureExtension fix)
- Phase 4 → `a448aa0`

## What's left

Only one Kotlin file in `shared/src/main/kotlin/`:

- `component/Grid.kt` (837 LOC) — Phase 5
- Plus the `LocatorDsl.kt` shim (138 LOC) which goes away in Phase 5 either by inlining test sites or moving to test scope.

After Phase 5, `kotlin-stdlib` drops from shared's classpath, the `kotlin-maven-plugin` is removed from the build, and the de-Kotlin effort on main sources is complete.
