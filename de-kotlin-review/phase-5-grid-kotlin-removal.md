# Reviewer notes — Phase 5: Grid port + kotlin-stdlib drop

**Branch:** `feat/no-kotlin-grid` (chained on Phase 4)
**Commit:** `fdb4c1f refactor: port Grid.kt to Java, drop kotlin-stdlib from compile scope`

Phase 5 closes the de-Kotlin loop on shared's main sources. The heaviest single Kotlin file (`component/Grid.kt`, 837 LOC) is ported to Java; the Kotlin DSL shim that survived Phase 4 moves to test scope; `kotlin-stdlib` drops out of shared's compile classpath.

After this commit, downstream consumers of the shared module no longer pull the Kotlin runtime transitively at all.

## Scope

- `shared/src/main/kotlin/component/Grid.kt` (837 LOC) → `shared/src/main/java/component/Grid.java` (1120 LOC)
- `shared/src/main/kotlin/internal/LocatorDsl.kt` (138 LOC) → `junit6/src/test/kotlin/internal/LocatorDsl.kt` (123 LOC, after stripping `IntRange.size` + unused `filterNotBlank`)
- new `shared/src/main/java/internal/CountRange.java` (95 LOC) — pure-Java replacement for `kotlin.ranges.IntRange` in `SearchSpec.count` and `ComponentQuery.LocatorSpec.count`
- `shared/pom.xml` — Kotlin compile-side cleanup
- 4 Java files (AccordionTester, CheckboxGroupTester, RadioButtonGroupTester, plus one source) — dropped `org.jetbrains.annotations.@NotNull/@Nullable` imports that came transitively via kotlin-stdlib

`shared/src/main/kotlin/` is deleted entirely.

## LOC delta

| | Files | LOC |
|---|---|---|
| Kotlin removed (main) | 2 (Grid.kt, LocatorDsl.kt) | ~975 |
| Kotlin moved to test | 1 (LocatorDsl.kt, with strip) | 123 |
| Java added | 2 (Grid.java, CountRange.java) | ~1,215 |
| pom slimmed | 1 | ~30 lines net |

Expansion on the Java side ≈ +34% — lower than prior phases because Grid was reflection-heavy where the original Kotlin already had Java-like try/catch patterns.

## Highlight: kotlin-stdlib drops from compile classpath

`mvn dependency:tree -pl shared -Dscope=compile` confirms **zero** `org.jetbrains.kotlin:*` entries.
`mvn dependency:tree -pl shared -Dscope=test` still shows `kotlin-stdlib-jdk8` (+jdk7/+jdk8 transitive) because shared's test sources include 20 tests in `src/test/kotlin/`.

Per `de-kotlin-plan.md` open question #1, leaving test-side Kotlin intact is the explicit chosen path. Test-scoped dependencies don't reach downstream consumers, so the goal of "downstream is Kotlin-free" is met.

## Notable design decisions

### `CountRange` replaces `IntRange`

Phase 4 left `kotlin.ranges.IntRange` in two public fields:
- `SearchSpec.count: IntRange?`
- `ComponentQuery.LocatorSpec.count`

Moving `kotlin-stdlib` to test scope would have broken the compile if those types stayed. Solution: new `CountRange` helper with `start`, `endInclusive`, `contains(int)`, plus a `CountRange.ANY` constant.

**This is a public-API breaking field-type rename.** No existing test reads/writes those fields directly, so the change is invisible to current call sites — but downstream consumers constructing a SearchSpec by setting count would need to update. The second small breaking change in the four-phase chain (the first was Phase 4's removal of three `@Deprecated(forRemoval=true)` Spring constructors).

### Java `Grid` class kept (not renamed)

Despite the simple-name clash with Vaadin's `com.vaadin.flow.component.grid.Grid`, the Java port keeps the class name `Grid` to match the original Kotlin file name and the established pattern (`Routes`, `Locator`, `PrettyPrintTree`). `GridTester.java` (which lives in `com.vaadin.flow.component.grid` itself) uses the fully-qualified name `com.vaadin.browserless.component.Grid.method(...)` for its 6 call sites instead of importing.

### `KProperty1<*, *>` overloads dropped

`HeaderRow.getCell(KProperty1<*, *>)` and `FooterRow.getCell(KProperty1<*, *>)` had zero callers. Dropped. The `getCell(String key)` overloads (which the KProperty1 versions delegated to) remain.

### Kotlin `Sequence<T>` → Java `Stream<T>`

`TreeGrid._rowSequence(...)` and `HierarchicalDataProvider._rowSequence(...)` returned `Sequence<T>` in Kotlin. Java port returns `Stream<T>`, built via `DepthFirstTreeIterator` + `Spliterator` so that callers consuming with `.count()` / `.skip().limit()` keep their laziness semantics.

### Dokka plugin removed entirely

With no main Kotlin sources, dokka is dead weight. The dokka-javadocs profile (active with `-Djavadocs`) is removed; the release profile now uses `maven-javadoc-plugin` for the javadoc artifact.

### `org.jetbrains.annotations.@NotNull/@Nullable` dropped from 4 Java files

These were used in `AccordionTester`, `CheckboxGroupTester`, `RadioButtonGroupTester`, and one other source. Documentation-only annotations; no behavior impact. They came transitively via kotlin-stdlib. Removed alongside the kotlin-stdlib scope change.

Note: `org.jspecify.annotations.@Nullable` is used in geolocation files — that's a **separate** dependency (JSpecify, the JVM-standard nullability annotation library) unaffected by the kotlin-stdlib drop.

## Cross-call-site update count

| Surface | Files | Count |
|---|---|---|
| Java | GridTester (6 sites + 1 import comment swap), 4 annotation drops | small |
| Kotlin (production) | none | zero |
| Kotlin (test) | none — LocatorDsl.kt move handles all 98 LocatorTest sites in-place via same-package resolution | zero |

The Java port of Grid keeps every method name + signature shape that GridTester needs, so the GridTester delta is minimal.

## Validation

| Suite | Tests |
|---|---|
| shared | 20 |
| junit6 | 1007 |
| spring | 29 |
| quarkus | 27 |

All green after a clean rebuild.

```
$ mvn -pl shared dependency:tree -Dscope=compile | grep -i kotlin
(no output)

$ mvn -pl shared dependency:tree -Dscope=test | grep -i kotlin
+- org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:2.2.20:test
|  +- org.jetbrains.kotlin:kotlin-stdlib:jar:2.3.20:test
|  \- org.jetbrains.kotlin:kotlin-stdlib-jdk7:jar:2.3.20:test
|     |  \- org.jetbrains.kotlin:kotlin-test:jar:2.3.20:test
```

## Branch state

Chained on Phase 4 (`a448aa0`). Full stack:

- Phase 1 → `162a1ad`
- Phase 2 → `3aab059`
- Phase 3 → `c983213`
- Phase 4 → `a448aa0`
- Phase 5 → `fdb4c1f`

## What's left

`shared/src/main/` is now 100% Java. `shared/src/test/` still has 20 Kotlin tests; that's left intact per the de-kotlin-plan's explicit scope decision. Junit6 module still has Kotlin tests + the moved DSL shim — also unchanged in production code.

## What this completes

The 5-phase de-Kotlin effort:

- 25 Kotlin files removed from `shared/src/main/`.
- ~5,100 LOC of Kotlin translated to ~7,800 LOC of Java.
- 1 runtime dependency (`kotlin-reflect`, ~3.3 MB) eliminated entirely.
- `kotlin-stdlib` (+jdk7/jdk8) drops from downstream consumers' transitive classpath.
- Dokka build plugin removed; replaced with maven-javadoc-plugin.
- Build system retains kotlin-maven-plugin for test-only Kotlin compilation.

The "alternative usage" of `browserless-test-shared` that motivated this effort (a Kotlin-stdlib-free downstream consumer) is now viable.
