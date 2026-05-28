# Reviewer notes — de-Kotlin overview

Five stacked branches landing the complete de-Kotlin effort on `shared/src/main/` planned in [`../de-kotlin-plan.md`](../de-kotlin-plan.md). With Phase 5 merged, the shared module's main sources are 100% Java and downstream consumers no longer transitively pull any Kotlin runtime.

## Branches in flight

| Phase | Branch | Commit | Files changed | Net LOC | Status |
|---|---|---|---|---|---|
| 1 (mocks) | `feat/no-kotlin-mocks` | `162a1ad` | 29 | +821 | green |
| 2 (internal utils) | `feat/no-kotlin-internal-utils` | `3aab059` | 45 | +452 | green, chained on Phase 1 |
| 3 (PrettyPrintTree + Routes + reflect drop) | `feat/no-kotlin-pretty-routes` | `c983213` | 18 | +192 | green, chained on Phase 2 |
| 4 (Locator + MockVaadin) | `feat/no-kotlin-locator-mockvaadin` | `a448aa0` | 17 | +631 | green, chained on Phase 3 |
| 5 (Grid + kotlin-stdlib drop) | `feat/no-kotlin-grid` | `fdb4c1f` | 12 | +292 | green, chained on Phase 4 |

The earlier `feat/no-kotlin-lib` branch (a minimal kotlin-reflect-only fix at `84c33c1`) is **superseded by Phase 3** and should be dropped once Phase 3 merges.

Phase 3 was amended (was `2ab03ca`, now `c983213`) to fold in a one-line fix to `junit6/src/main/java/com/vaadin/browserless/TreeOnFailureExtension.java` that the Phase 4 agent caught — see Phase 3 / Phase 4 notes.

Per-phase details:

- [`phase-1-mocks.md`](phase-1-mocks.md)
- [`phase-2-internal-utils.md`](phase-2-internal-utils.md)
- [`phase-3-kotlin-reflect-drop.md`](phase-3-kotlin-reflect-drop.md)
- [`phase-4-locator-mockvaadin.md`](phase-4-locator-mockvaadin.md)
- [`phase-5-grid-kotlin-removal.md`](phase-5-grid-kotlin-removal.md)
- [`self-review.md`](self-review.md) — compact API-impact / Kotlin-user / test-coverage assessment

## Merge order

The five branches form a stack (Phase 1 → 2 → 3 → 4 → 5). Recommended order: merge in sequence, rebasing the next branch onto `origin/main` after each merge.

If a reviewer wants to land just the runtime-classpath wins:
- After Phase 3 merges, `kotlin-reflect` (~3.3 MB) is gone.
- After Phase 5 merges, `kotlin-stdlib` is gone from compile scope entirely.

Phases 1, 2, and 4 don't drop dependencies on their own — they prepare the ground.

## What's Kotlin after Phases 1–5 land

In `shared/src/main/`: **nothing**. Pure Java.

Test-side Kotlin still exists, per the de-kotlin-plan's open question #1 explicit decision:

| Location | LOC | Notes |
|---|---|---|
| `shared/src/test/kotlin/` | ~600 | shared module's own Kotlin tests |
| `junit6/src/main/` | small | only Java |
| `junit6/src/test/kotlin/` | ~3,000 | junit6 module's Kotlin tests (the bulk) |

Test-scoped dependencies don't reach downstream consumers, so this Kotlin doesn't show up on their classpath.

## Runtime jars on shared's classpath after each branch

| After branch | `kotlin-stdlib` (compile) | `kotlin-reflect` (compile) | `jetbrains:annotations` (compile) | `kotlin-stdlib` (test) |
|---|---|---|---|---|
| `main` (origin) | ✓ | ✓ | ✓ | ✓ |
| `feat/no-kotlin-mocks` | ✓ | ✓ | ✓ | ✓ |
| `feat/no-kotlin-internal-utils` | ✓ | ✓ | ✓ | ✓ |
| `feat/no-kotlin-pretty-routes` | ✓ | **gone** | ✓ | ✓ |
| `feat/no-kotlin-locator-mockvaadin` | ✓ | gone | ✓ | ✓ |
| `feat/no-kotlin-grid` | **gone** | gone | **gone** | ✓ |

After Phase 5, downstream consumers see zero Kotlin transitively. The shared module's build still uses `kotlin-maven-plugin` (for test-side compilation only) but it's not exposed as a dependency.

## API-shape changes worth flagging to reviewers

The de-Kotlin effort tries hard to preserve the public surface that downstream consumers use. Two changes are worth specifically calling out:

- **Phase 4 removes three `@Deprecated(forRemoval=true)` constructors** from `spring/.../MockSpringServlet.java`, `MockSpringServletService.java`, `MockSpringVaadinSession.java`. They accepted `kotlin.jvm.functions.Function0<UI>` and were the only references in Spring's API surface pulling in `kotlin.jvm.functions`. No internal callers; the constructors were already API-marked for removal.

- **Phase 5 replaces `kotlin.ranges.IntRange` with new `CountRange`** in two public fields:
  - `SearchSpec.count: CountRange` (was `IntRange?`)
  - `ComponentQuery.LocatorSpec.count: CountRange` (was `IntRange?`)
  No existing test reads/writes these fields, so the change is invisible to current call sites. Downstream consumers constructing a SearchSpec by setting count would need to update.

- **Phase 5 drops `org.jetbrains.annotations.@NotNull/@Nullable` from 4 Java files** (documentation-only annotations that came transitively via kotlin-stdlib). `org.jspecify.annotations.@Nullable` usage in geolocation files is unaffected (separate, standard nullability annotation library).

Everything else (`MockVaadin.setup`, `MockVaadin.tearDown`, `Routes(...)`, `_get`/`_find`/`_expect*`, `BasicUtils.*`, `GridTester.*`, etc.) keeps its call-site syntax through one of these mechanisms:
1. Kotlin `@JvmStatic` already exposed identical Java syntax — port is invisible to Java callers.
2. New Java method overloads added to preserve binary compatibility (e.g., `MockRequest.setUserInRole` setter).
3. Kotlin shim file (`LocatorDsl.kt` in test scope) re-exposes Kotlin DSL idioms over the Java engine for Kotlin test callers.

## Pre-merge checklist for any branch

- [ ] Rebase onto `origin/main` (each branch was cut from local `main` which was 3 commits behind during the porting session).
- [ ] Re-run all four test suites after rebase: `mvn test` from repo root. Use `rm -rf */target` first to avoid stale-m2 issues across modules.
- [ ] `mvn dependency:tree -pl shared -Dscope=compile` to confirm runtime-jar expectations match the table above.
- [ ] After Phase 5: `mvn dependency:tree -pl shared -Dscope=test` should still show `kotlin-stdlib-jdk8` — test-side Kotlin tests need it.
- [ ] Spot-check at least one downstream consumer (e.g. a sample Vaadin app's test suite) if available — open question #4 in `de-kotlin-plan.md`.

## Translation patterns used across Phases 1–5

For anyone porting downstream code with similar shape: these Kotlin-to-Java mappings were applied consistently and produced no surprises.

| Kotlin | Java |
|---|---|
| Top-level `object Foo { ... }` | `public final class Foo` + private constructor + static members |
| `val foo: T` top-level | `public static T foo()` getter |
| `var foo: T` top-level | `public static T foo` mutable field, or getter+setter pair |
| `fun X.foo(): R` extension | `public static R foo(X x)` static helper |
| `val X.foo: R` extension property | `public static R foo(X x)` getter |
| `var X.foo: R` extension property | getter `foo(X)` + setter `foo(X, R)` pair |
| `interface I by delegate` | explicit forwarding of every method |
| `data class D(...)` | POJO + explicit equals/hashCode/toString/copy |
| `fun interface F : () -> R` | `@FunctionalInterface interface F` with `R invoke()` (preserves SAM compat) |
| `lambda: (T) -> R` | `Function<T, R>` (or `Consumer` / `Predicate` / `BiPredicate` / `Supplier`) |
| `SearchSpec<T>.() -> Unit` DSL | `Consumer<SearchSpec<T>>` (kept as Kotlin shim for test ergonomics) |
| Kotlin `Sequence<T>` | `Stream<T>` via Spliterator for laziness |
| `MutableMap<K, V>` | `Map<K, V>` (`HashMap` impl) |
| `Array<T>` | `T[]` |
| `Class<*>` | `Class<?>` |
| `inline fun <reified T> foo()` | `<T> T foo(Class<T> type)` — pass class explicitly |
| `?.let { ... }` | `if (x != null) { ... }` |
| `when (x) { is A -> ... ; is B -> ... }` | `instanceof` chain |
| `throw checkedException` in Kotlin | Java `sneakyThrow` trick (or wrap, depending on caller expectations) |
| `kotlin.ranges.IntRange` in public field | Custom Java range helper (we used `CountRange`) |
| `buildString { ... }` | `StringBuilder` + `.toString()` |
| `coerceAtLeast(0)` | `Math.max(0, x)` |
| `kotlin.streams.toList()` on Stream | `.collect(Collectors.toList())` or Java 16+ `.toList()` |
| KDoc `[Foo]` cross-reference | preserved verbatim in Java `/** */` |

## Translation footguns encountered (cumulative)

- **Kotlin extensions resolve by static type at compile time.** When a more-specific extension overrides a less-specific one (e.g. `Button.caption` vs `Component.caption`), Java needs the dispatch handled explicitly — fold it into the general method with an `instanceof` short-circuit at the top.
- **Kotlin `var x: T?` defaults are non-null in Java.** Don't add defensive `Objects.requireNonNull` — if the Kotlin original used `var x: T? = null`, the Java port uses `T x` with no default and that's correct.
- **`Element.equals()` is overridden in Vaadin.** Identity comparison via `==` works in Kotlin (which uses `.equals` for `==`) but breaks the same code in Java if you mechanically translate `==` to Java `==`. Use `.equals(...)` explicitly.
- **Kotlin's `is`-prefix property convention emits a `set` setter on the JVM**: `var isUserInRole` produces `setUserInRole(...)` as a bytecode setter, *not* `setIsUserInRole`. If you Java-port the field and don't preserve the setter name, binary-compatibility breaks for cross-module callers.
- **Kotlin allows multiple top-level `open class` declarations in one file.** Java doesn't — split into one file per public class.
- **Caching boolean toggles at construction vs. read-on-use.** Phase 3 noticed Kotlin's `class { private val pipe = if (!globalFlag) ... }` caches the flag at construction. The Java port that reads the static field on every method call has subtly more-correct behavior when the flag is mutated after construction. Document such changes rather than fight them.
- **Kotlin can call Java methods with named-arg syntax only if compiled with `-parameters`.** Test-side Kotlin calling Java `MockVaadin.setup(uiFactory = ...)` won't resolve; switch to positional or explicit `apply { }`.
- **Kotlin "checked exceptions don't exist" → Java sneakyThrow.** When a Kotlin block `throw t` propagates an arbitrary Throwable that the Java port can't `throw checkedException` directly, use the standard sneakyThrow pattern.
- **`fun interface F : () -> R` compiles to bytecode extending Function0.** Java callers passing method refs / lambdas to such interfaces are tied to the SAM signature. Keep the Java port's method name as `invoke()` if you want zero-churn migration.
- **Stale `~/.m2` after branch-hops** is a real time-waster. If you check out a sibling branch mid-port to baseline-compare, delete `*/target` (covering all modules, not just shared) and re-install before running cross-module tests, or trust nothing.
- **JUnit extensions / fallback code paths**: tests that pass don't necessarily exercise every line. A `@AfterTestExecutionCallback` that only runs on failure can quietly carry a compile error past a green test suite. Always do a clean rebuild before reporting "done".
- **Kotlin `IntRange` in public field types** keeps the kotlin-stdlib transitive even after the rest of the file is Java. Catch it during the final Kotlin-removal phase or earlier; introduce a small Java range type (we used `CountRange`).
- **`org.jetbrains.annotations` is a transitive of kotlin-stdlib.** If Java files use `@NotNull` / `@Nullable` from that package and you drop kotlin-stdlib, the annotations vanish too. Either keep them on a direct `jetbrains:annotations` dependency, switch to JSpecify, or remove them (documentation-only).

## Total stats — Phases 1–5

- **25 Kotlin files removed** from `shared/src/main/`.
- ~5,100 LOC of Kotlin → ~7,800 LOC of Java (+53% expansion ratio overall).
- 123 LOC of Kotlin shim survives in junit6 test scope (`LocatorDsl.kt`) for Kotlin-DSL test readability.
- `kotlin-reflect` (~3.3 MB) and `kotlin-stdlib` (+jdk7/jdk8) both gone from shared's compile classpath.
- Build still uses `kotlin-maven-plugin` in shared (test-only) and junit6 (main+test) for Kotlin test compilation.
- `dokka` plugin removed entirely; `maven-javadoc-plugin` used for the javadoc artifact.
- 2 small public-API breaking changes (Spring deprecated ctor removal in Phase 4, `IntRange` → `CountRange` field type in Phase 5).

The "alternative usage" of `browserless-test-shared` that motivated this effort — a Kotlin-runtime-free downstream consumer — is now viable.
