# Migrating the Kotlin sources to plain Java

Working plan for removing Kotlin from `browserless-test`. The goal is that
`shared/src/main` is 100% Java and that downstream consumers of
`browserless-test-shared` pull no Kotlin runtime transitively.

## Current state

| Area | Files | LOC | Notes |
| --- | --- | --- | --- |
| `shared/src/main/kotlin/…/mocks` | 12 | 1,721 | servlet API mocks, `MockService`, `MockedUI` |
| `shared/src/main/kotlin/…/internal` | 12 | 3,043 | `MockVaadin`, `Locator`, `Routes`, `PrettyPrintTree`, utilities |
| `shared/src/main/kotlin/…/component/Grid.kt` | 1 | 901 | the heaviest single file |
| `junit6/src/test/kotlin` | 23 | ~3,050 | DynaTest + Karibu DSL |
| `shared/src/test/kotlin` | 2 | 144 | DynaTest |

All of it is ported as of Phase 6; the table is the starting point the phases
below work through.

`spring`, `quarkus`, `junit6/src/main` and `locator-processor` are already pure
Java, but about 48 Java files reference the Kotlin packages and 24 call sites go
through Kotlin file facades (`LocatorKt`, `PrettyPrintTreeKt`, `GridKt`,
`UtilsKt`, `BasicUtilsKt`, `ShortcutsKt`).

## Prior art

`origin/feat/no-kotlin-grid` carries a complete five-commit port of all 25
main-source Kotlin files, verified green on all four suites
(20 / 1007 / 29 / 27 tests). It branched from `a333ccc` and is 82 commits behind
`main`, so it is a source to rebase from, not a branch to merge.

Reviewer notes from that effort live on the branch in `de-kotlin-review/`.

Drift to reconcile while rebasing — most files differ only by the
commercial → Apache license header swap (`08abcfa`); the real drift is:

| File | Drift | Why |
| --- | --- | --- |
| `internal/MockVaadin.kt` | +222 | reload / window-name plumbing, `liveUI`, `fireSessionDestroyAndDrain`, multi-user session objects |
| `mocks/MockInstantiator.kt` | +118 | deprecated, forwarders hand-unrolled |
| `internal/PrettyPrintTree.kt` | +93 | `hrefValue()` already rewritten in plain Java reflection — expect a conflict with Phase 3 |
| `component/Grid.kt` | +86 | selection and click additions |
| `mocks/MockedUI.kt` | +82 | `navigate()` override and `toLocation()` |
| `internal/TestingLifecycleHook.kt` | +60 | slot / children rules |
| `mocks/MockRequest.kt` | +60 | role checker, principal provider |
| `mocks/MockHttpSession.kt` | +39 | `changeSessionId()` |

`df099d0` already dropped `kotlin-reflect` on `main`, so that dependency win is
banked independently of this work.

## Phases

Each phase is independently mergeable and leaves the build green.

### Phase 0 — prerequisites

Decisions that constrain later phases:

- **Nullability annotations.** 11 Java files in `shared`, `spring` and
  `quarkus` import `org.jetbrains.annotations.@NotNull` / `@Nullable`, which
  arrives transitively via `kotlin-stdlib`. Move them to JSpecify (already used
  in 5 files) or add a direct `org.jetbrains:annotations` dependency. Without
  this the build breaks the moment `kotlin-stdlib` leaves compile scope.
- **`SearchSpec.count`.** Pick the replacement for `kotlin.ranges.IntRange`
  (the prior effort introduced a small `CountRange` helper). This constrains
  Phases 4 and 5.
- **Test-side Kotlin.** In or out of scope — see below.

### Phase 1 — `mocks/` — done

12 Kotlin files → 14 Java files, on `refactor/no-kotlin-mocks`. All five suites
match the `main` baseline exactly: shared 42, junit6 1406, junit6-cdi-tests 4,
spring 41, quarkus 31.

- `MockHttpEnvironment.kt` splits into `MockHttpEnvironment`,
  `MockServletConfig` and a `MockUtils.putOrRemove` helper.
- `MockVaadinServlet.kt`'s top-level factories (`serviceSafe`,
  `createVaadinServletRequest` / `…Response`, `_createVaadinSession`,
  `WebBrowser(request)`) become statics on `MockVaadinServlet`. The last one is
  renamed `createWebBrowser` to avoid clashing with
  `com.vaadin.flow.server.WebBrowser`.
- `SessionAttributeMap` becomes a public class; the `HttpSession.attributes`
  extension property disappears and callers construct it directly.
- `MockInstantiator`'s `Instantiator by delegate` is unrolled by hand — see the
  silent-risk list below.
- `MockRequest` keeps an explicit `setUserInRole(BiPredicate)` setter, matching
  what Kotlin's `is`-prefix property convention emitted.
- `MockVaadinServlet.createServletService` now declares
  `throws ServiceException`, as its `VaadinServlet` superclass does — see the
  breaking changes below.
- KDoc is rewritten as Javadoc rather than carried over verbatim: `[Foo]`
  becomes `{@link Foo}`, `*` bullets become `<ul><li>`, and every public member
  gets `@param` / `@return`. A mock's class Javadoc states which container
  behavior it reproduces and where it stops, per
  [`guidelines/documenting.md`](guidelines/documenting.md). Once Dokka is gone
  (Phase 5) this is what `maven-javadoc-plugin` publishes.

### Phase 2 — `internal/` utilities — done

`BasicUtils`, `ComponentUtils`, `ElementUtils`, `DepthFirstTreeIterator`,
`Renderers`, `Shortcuts`, `TestingLifecycleHook`, `Utils`, on
`refactor/no-kotlin-internal-utils`. All five suites match the `main` baseline:
shared 42, junit6 1406, junit6-cdi-tests 4, spring 41, quarkus 31.

One Java utility class per Kotlin file: `public final`, private constructor,
top-level and extension functions become `public static` methods with the
receiver as the first parameter. The leading-underscore convention
(`_fireEvent`, `_isVisible`, `_saneFetchLimit`) is preserved.

`TestingLifecycleHook` splits into the interface plus a `TestingLifecycleHooks`
holder, because a Java interface cannot hold the mutable global that the Kotlin
top-level `var testingLifecycleHook` provided. The global is a
`getCurrent()` / `setCurrent(…)` pair, not a public field — see the
`MockHttpEnvironment` lesson from Phase 1.

A 20-line `Matches.kt` shim stays behind, holding `Component.matches(…)` and
`IntRange.size`. Both take Kotlin-only types and go away with `SearchSpec`
(Phase 4) and `Grid` (Phase 5).

Two things the drift check caught, both in `TestingLifecycleHook`: its
`getAllChildren` had a `Grid` branch that was commented out when the prior port
was written and is live on `main`, and its fallback moved from
`_getVirtualChildren` to `ComponentUtil.getAllChildren`. Porting the old Java as
written would have silently reverted both.

### Phase 3 — `PrettyPrintTree` + `Routes` — done

On `refactor/no-kotlin-pretty-routes`. All five suites match the `main`
baseline: shared 42, junit6 1406, junit6-cdi-tests 4, spring 41, quarkus 31.

`MockRouteNotFoundError` and `MockInternalSeverError` move to their own files
(Java allows one public class per file). `main`'s `hrefValue()` had already been
rewritten in plain Java reflection, and it is considerably more thorough than
what the prior port carried — it walks the class hierarchy reading declared
methods *and* fields at any visibility, because `Anchor` keeps its `href` in a
private field. That version is the one to port; the prior port's getter-only
lookup would have quietly changed what a tree dump shows for an `Anchor`.

`TreeOnFailureExtension` (`PrettyPrintTree.Companion.ofVaadin`) is fixed here.
Nothing else referenced it, and the line only runs on test failure, so a green
suite does not catch it.

`kotlin-reflect` was already dropped on `main` by `df099d0`, so this phase has
no dependency change.

### Phase 4 — `Locator` + `MockVaadin` — done

On `refactor/no-kotlin-locator-mockvaadin`. All five suites match the `main`
baseline: shared 42, junit6 1406, junit6-cdi-tests 4, spring 41, quarkus 31.
After this phase no Java source in any module references a Kotlin type.

- `Locator.kt` → `Locator` + `SearchSpec`.
- `MockVaadin.kt` → `MockVaadin` + `SessionObjects` + `UIFactory` +
  `MockRequestCustomizer` + `MockPage`.
- `runUIQueue` needs the `sneakyThrow` idiom, or `AsyncTest`'s
  `expectThrows(ExecutionException)` fails — Kotlin rethrows arbitrary
  throwables where Java cannot.
- Keep `UIFactory`'s SAM method named `invoke()` so `MockedUI::new` call sites
  and the Spring / Quarkus constructors keep binding.
- `CountRange` replaces `kotlin.ranges.IntRange` here rather than in Phase 5, so
  `SearchSpec` and `ComponentQuery.LocatorSpec` are edited once instead of
  twice.
- The Kotlin DSL shim goes straight to `junit6/src/test/kotlin` as
  `LocatorDsl.kt`, skipping the prior effort's intermediate stop in
  `shared/src/main/kotlin`. Nothing in `shared/src/test` uses the locator DSL.
- The three `@Deprecated(forRemoval = true)` Spring constructors taking
  `Function0<UI>` are removed, as agreed: with a plain Java `UIFactory` they are
  ambiguous against the `UIFactory` overload at any `MockedUI::new` call site.
- `MockVaadin` fires session-init, service-destroy and UI-init through
  `VaadinService.getEventBus()` with a rethrowing failure handler, as `main`
  does. The prior port reflected into `VaadinService`'s private listener
  collections and swallowed the rethrow, which both
  [`CONVENTIONS.md`](CONVENTIONS.md) and
  [`guidelines/architecture.md`](guidelines/architecture.md) argue against.

Two things the drift check caught. `SearchSpec` had no `testId` at all, because
that field postdates the prior port — it would have broken the public
`ComponentQuery.withTestId(…)`. And `MockPage.reload()` was missing the
logout-idiom early return, the wrong-UI guard and the
`recordReloadReplacement(…)` call, which six reload tests caught.

### Phase 5 — `Grid.kt` and the kotlin-stdlib drop — done

On `refactor/no-kotlin-grid`. All five suites match the `main` baseline:
shared 42, junit6 1406, junit6-cdi-tests 4, spring 41, quarkus 31.
`shared/src/main` is now 100% Java and `kotlin-stdlib` is off its compile
classpath, so downstream consumers pull no Kotlin runtime.

- `Sequence<T>` returns (`_rowSequence`) become `Stream<T>` built over
  `DepthFirstTreeIterator` + `Spliterator`. Laziness matters: `TreeGrid._size()`
  is `_rowSequence().count()`.
- The `KProperty1` overloads of `HeaderRow.getCell` / `FooterRow.getCell` have
  no callers — drop them; the `getCell(String)` overloads stay.
- Delete `shared/src/main/kotlin`, move `kotlin-stdlib` to test scope, replace
  Dokka with `maven-javadoc-plugin`.
- `_dump(Grid, IntRange)` becomes `_dump(Grid, int from, int to)`. A row range
  is not a count, so it needs no `CountRange`.
- The `org.jetbrains.annotations` imports move to JSpecify, which Vaadin already
  puts on the classpath at `provided` scope — no new dependency.

**Replacing Dokka turns Javadoc validation on for the first time.** Dokka
validated nothing, so `maven-javadoc-plugin` fails `shared` the moment it
replaces it. `shared` carried `<doclint>none</doclint>` through Phase 5 so the
port would not turn into a module-wide Javadoc cleanup; see
[Phase 7](#phase-7--the-doclint-cleanup--done) for that cleanup.

### Phase 6 — test-side Kotlin — done

The last 25 Kotlin files, all test-scoped, are now JUnit 6 Java, and the word
Kotlin is gone from the build: no `kotlin-maven-plugin`, no `dynatest`, no
`karibu-dsl`, no `kotlin-stdlib`, no Dokka, no `kotlin` block in Spotless. All
five suites match the `main` baseline: shared 42, junit6 1406,
junit6-cdi-tests 4, spring 41, quarkus 31.

- `group { … }` / `test { … }` map onto `@Nested` / `@Test`; `beforeEach` and
  `afterEach` onto `@BeforeEach` / `@AfterEach`, and a `beforeGroup` onto
  `@BeforeAll`. `AllTests.kt` was a pure aggregator: its two standalone tests
  became `TestClasspathTest` and the rest of it disappeared.
- `locatorTest()` and `locatorTest2()` were two DynaTest fragments the
  aggregator wired under different lifecycle hooks, so they became two classes:
  `LocatorTest` (with `MyLifecycleHook` installed) and
  `LocatorWithoutLifecycleHookTest`.
- `LocatorDsl.kt` existed only to keep `_get<Button> { caption = "…" }` working
  for Kotlin callers. With the tests in Java it is deleted, and they call
  `Locator._get(clazz, spec -> …)` directly — which is also what a user writes.
- DynaTest's helpers needed Java equivalents: `cloneBySerialization()` became
  `TestSerialization`, `expectThrows(clazz, message) { … }` became
  `TestAssertions.expectThrows(Class, regex, Executable)`, and `expectList(…)`
  became a plain `assertEquals(List.of(…), …)`.
- Karibu DSL builders (`verticalLayout { textField(…) }`) become plain Vaadin
  constructor and `add()` calls.
- Watch the assertions that Kotlin's loose typing hid. A `var flag = false`
  asserted with `expect(true)` says nothing about how many times a listener
  fired; ported to a counter asserted `== 1` it turned red, because
  `VaadinSession.close()` detaches the UI twice.
- Test names: DynaTest's free-text names (`"attributes"`, `"spec"`,
  `"FailsOnNoComponents UI"`) become method names in the repository's
  `subject_scenario_expectation` shape, which is what surefire reports.

**Verification.** A green build is not enough here — a dropped `test { … }`
block leaves no trace. Every step was checked by comparing the *multiset of
`<testcase name="…">` values* in the surefire XML against a baseline worktree,
not the `tests="N"` attribute, which DynaTest under-reports. 1,448 before,
1,448 after, with all 225 renamed names pairing one-for-one.

### Phase 7 — the doclint cleanup — done

`shared` builds clean under doclint, errors and warnings both, and
`<doclint>none</doclint>` is gone from its pom.

The "roughly 200 issues" the Phase 5 note quoted was **javadoc's output cap**,
not the count: the tool stops after 100 errors and 100 warnings. The real
number was 935. The root pom now passes `-Xmaxerrs`/`-Xmaxwarns` so the next
person reads a count rather than a ceiling.

- Three groups were genuine defects: unresolvable `@link` and `@throws`
  targets, `<p/>` and empty `<p>`, `<` and `>` written literally, and a `<pre>`
  block whose `ContextMenuTester<ContextMenu>` parsed as an HTML tag. 76 of
  those.
- 291 were in the *generated* `*Locator` files, so they were fixed in
  `locator-processor`: the class Javadoc now carries `@param` for the locator's
  type parameters, both generated constructors carry Javadoc, and a copied
  `{@link #member}` is rewritten to point back at the tester it came from.
- A tester's Javadoc is copied verbatim into a generated locator that has no
  imports, so a cross-package `{@link Span}` resolves in the tester and fails in
  the copy. The rule — spell such targets out in full — is now in
  [`guidelines/documenting.md`](guidelines/documenting.md).
- The rest was the `missing` group: 316 `@param`, 236 undocumented members, 116
  `@return`. Almost all of it in the files this port rewrote, because KDoc
  carried none of those tags.
- The port had also left 65 Javadoc lines in KDoc markup — `[TreeGrid]`,
  `[_rowSequence]`, `*skip*` — which render as literal brackets and asterisks.
  Those are now `{@link}`, `{@code}` and `<em>`.

Still open, and untouched here because the ask was `shared`: `junit6`, `spring`
and `quarkus` report 36 doclint warnings between them. They never failed a
build, because doclint warnings do not.

## Breaking changes

Four genuinely breaking changes, all in `com.vaadin.browserless.internal`,
`.mocks` or `.component`, or already deprecated.

### Checked exceptions reappear on overridable methods

Kotlin has no checked exceptions, so a Kotlin `override` declared none even when
the Java superclass did. In Java the `throws` clause comes back:
`MockVaadinServlet.createServletService` declares `throws ServiceException` and
`createDeploymentConfiguration` declares `throws ServletException`.

Overriding either still compiles — an override may throw fewer exceptions — but
an override that calls `super` has to declare the exception, which it did not
have to before. `BuilderVaadinConfigurationTest` in this repository was the only
affected caller. The alternative, omitting the `throws` clause and rethrowing
through `sneakyThrow`, keeps every caller compiling but propagates a checked
exception from a method that does not declare it; the honest signature won.

Watch for the same pattern in later phases, wherever a Kotlin override sits on
top of a Java method that declares checked exceptions.

### Kotlin types in signatures

| Symbol | Where | Impact |
| --- | --- | --- |
| `kotlin.ranges.IntRange` | `SearchSpec.count`, `Grid._dump(IntRange)`, `Utils.IntRange.size`, `ComponentQuery.LocatorSpec.count` | **Breaking.** `ComponentQuery.withResultsSize(int)`, the documented entry point, is unaffected; only direct `SearchSpec` users break |
| `kotlin.Unit` | return type of the `SearchSpec<T>.() -> Unit` block in `_get` / `_find` / `_expect*`; `ComponentQuery.java` imports it today | becomes `Consumer<SearchSpec<T>>` |
| `kotlin.jvm.functions.Function0<UI>` | `UIFactory`, plus three `@Deprecated(forRemoval = true)` constructors in `MockSpringServlet`, `MockSpringServletService`, `MockSpringVaadinSession` | **Breaking:** those three constructors have to go — a Java `UIFactory` no longer extends `Function0`, so `MockedUI::new` becomes ambiguous against them. No in-repo callers |
| `kotlin.jvm.functions.Function1` / `Function2` | `MockVaadin.mockRequestFactory`, `MockRequest.isUserInRole`, `MockRequest.userPrincipalProvider`, `PrettyPrintTree.prettyStringHook`, `DepthFirstTreeIterator(root, children)`, `findAncestor(predicate)`, `SearchSpec.toPredicate()` | swap to `Function` / `Predicate` / `BiPredicate` / `Supplier` / `BiConsumer`. `toPredicate()` changes from `predicate(c)` to `predicate.test(c)` |
| `kotlin.reflect.KClass` | `MockVaadinHelper.BrowserlessLookupInitializer.additionalServices` (`protected open`, subclassed in `BrowserlessLookupInitializerTest`) | becomes `Map<Class<?>, Class<?>>` |
| `kotlin.reflect.KProperty1` | `HeaderRow.getCell` / `FooterRow.getCell` in `Grid.kt` | no callers — drop |

### Generated shapes that vanish

- **Every `*Kt` facade class.** 24 in-repo Java call sites get rewritten; any
  external caller breaks.

  Because the class name changes anyway (`UtilsKt` → `Utils`), the accessor
  names generated for Kotlin properties are renamed at the same time and at no
  extra cost: `getCurrentUI()` → `currentUI()`, `get_saneFetchLimit()` →
  `_saneFetchLimit()`, `getId_()` / `setId_()` → `id_()` / `id_(…)`, and so on
  throughout Phase 2.
- **Kotlin `internal` helpers stop being callable from outside the package.**
  `internal` is public in bytecode, so `splitByWhitespaces`, `ellipsize`,
  `hasCustomToString`, `isRouteNotFound`, `getErrorParameterType`,
  `isEffectivelyVisible`, `isPolymerTemplate` and friends were reachable by
  accident. They are package-private in Java, which is what `internal` meant.
- **`Button.caption` folds into `caption(Component)`.** Kotlin dispatches
  extensions on the static type, so a more specific `Button.caption` shadowed
  the generic one; Java has no equivalent, so the generic method takes an
  `instanceof Button` short-circuit and the two-overload API becomes one.
- **`.Companion` accessors** — `PrettyPrintTree.Companion.ofVaadin` (live in
  `TreeOnFailureExtension`), `TestingLifecycleHook.Companion.getDefault`,
  `MockHttpSession.Companion.create`.
- **`INSTANCE` on a Kotlin `object`.** A `var` on an `object` is not a public
  static field: it compiles to a private static backing field plus accessors on
  the singleton, reached from Java as
  `MockHttpEnvironment.INSTANCE.setLocalPort(…)`. The Java port gives the same
  accessor names as statics, so `INSTANCE`-qualified calls break. Same for
  `MockVaadinHelper`.
- **A `Boolean` property is `getX()`, not `isX()`.** Kotlin only uses the `is`
  prefix when the property name already starts with `is`, so `var skipPwaInit`
  compiles to `getSkipPwaInit()` and `var prettyPrintUseAscii` to
  `getPrettyPrintUseAscii()`. Naming the Java getter the bean-conventional
  `isSkipPwaInit()` breaks every Kotlin caller using the property syntax, and
  every Java caller too. Keep Kotlin's name.
- **`Companion` and other Kotlin synthetics** — `MockContext.Companion`,
  `MockHttpSession.Companion` (whose `create` becomes a real static),
  `MockInstantiator.Companion`, the `$default` bridges behind default arguments,
  and the `DefaultConstructorMarker` constructors. All replaced by real Java
  statics and overloads.
- **`data class` members** — `Routes.copy()` / `componentN()` / `equals` /
  `hashCode`, and the same on `SessionObjects`. `RoutesTest` uses the two-arg
  `Routes(…)` form, so the constructor set `@JvmOverloads` produced has to be
  written out by hand.
- **`is`-prefix property setters.** Kotlin's `var isUserInRole` emits
  `setUserInRole(…)`, not `setIsUserInRole`. `MockSpringServlet` calls it
  cross-module — preserve the exact name.
- **Underscore-prefixed property accessors** — `BasicUtilsKt.get_saneFetchLimit()`,
  `UtilsKt.getContext(…)`, `UtilsKt.getMock(…)`. Java-idiomatic renames here are
  silent source breaks outside the repo, so keep the name unless the value is
  reachable another way.

  Phase 1 renamed five of them: `MockResponse`'s `_bufferSize`,
  `_characterEncoding`, `_contentType`, `_locale` and `_status` were public
  Kotlin `var`s and are now private fields. Every one is reachable through the
  `HttpServletResponse` getter/setter pair it backs (`getStatus` /
  `setStatus`, …), and nothing in the repo read them, so only the synthetic
  `get_status()`-style accessors are gone.
- **`internal` visibility is public in bytecode**, and Java already relies on
  it: `UtilsKt.findClass` / `findClassOrThrow` from `spring`, and `MockPage`
  from `BrowserlessUIContext`. These must stay `public` in Java.

### Silent-behavior risks

No compile error catches these.

1. **`MockInstantiator : Instantiator by delegate`.** Kotlin generates
   forwarders only for *abstract* members, which is why the methods Flow
   declares as `default` already had to be hand-written (the
   `PageTitleGenerator` bug). A Java port must forward every method explicitly,
   or calls silently fall through to Flow's defaults. Highest-risk item, and it
   bit the prior effort: its port forwarded 10 of `Instantiator`'s 11 instance
   methods, dropping `getPageTitleGenerator()`. Check the port against the
   interface's own member list, not against the Kotlin source.
2. **`Element.equals()` is overridden in Vaadin.** Kotlin `==` is `.equals()`;
   translating it to Java `==` becomes identity comparison. This already broke
   `ElementUtilsTest` during the prior effort.
3. **Extension resolution is static in Kotlin.** `Button.caption` shadowing
   `Component.caption` has to become an `instanceof` short-circuit inside one
   generic method.
4. **`check()` / `require()` message text.** Tests assert on exact messages from
   `checkEditableByUser`, `MockPage.reload` and `setupServlet`. Preserve them
   verbatim.
5. **Construction-time flag capture.** `PrettyPrintTree` caches
   `prettyPrintUseAscii` in its constructor; a Java port reading the static per
   call behaves differently when the flag is toggled mid-test.
6. **`ComponentUtils.dataProvider`'s `HasDataProvider<*>` branch** relies on a
   recursive extension-property lookup with no Java equivalent. The prior port
   dropped it and stayed green — verify against today's `Grid` / `ComboBox`
   testers.

### What Kotlin consumers lose

Tests written in Kotlin against the internal API lose the reified / receiver
DSL: `_get<Button> { id = "foo" }` becomes
`Locator._get(Button::class.java) { it.id = "foo" }`. The public surface
(`BaseBrowserlessTest`, `ComponentQuery`, `ui.findButton()`, the testers,
`MockVaadin.setup` / `tearDown`) is unaffected — `@JvmStatic` on the Kotlin
`object`s already exposed exactly the shapes a Java class produces.

## Build changes

- `shared/pom.xml` — drop the `kotlin-maven-plugin` main execution, drop Dokka
  in favour of `maven-javadoc-plugin`, drop the `build-helper`
  `add-kotlin-sources-for-source-jar` execution, move `kotlin-stdlib` to `test`
  scope.
- `junit6/pom.xml` — the `kotlin-maven-plugin` `compile` execution points at a
  `src/main/kotlin` that does not exist. Dead config, remove it.
- Root `pom.xml` — drop `dokka.version`; keep `kotlin.version` only while
  test-side Kotlin lives. The Spotless `<kotlin>` block goes with the last
  `.kt` file.
- No `japicmp` / `revapi` gate exists in CI, so nothing enforces compatibility
  automatically. The breaking-change list above has to be checked by hand.

## Translation reference

| Kotlin | Java |
| --- | --- |
| top-level `object Foo` | `public final class Foo`, private constructor, static members |
| top-level `val foo: T` | `public static T foo()` |
| top-level `var foo: T` | `public static` getter + setter over a private field, **not** a public field |
| `var foo` on an `object` | `public static` getter + setter, keeping Kotlin's accessor names |
| `fun X.foo(): R` | `public static R foo(X x)` |
| `val X.foo: R` | `public static R foo(X x)` |
| `var X.foo: R` | `foo(X)` + `foo(X, R)` |
| `interface I by delegate` | explicit forwarding of every method |
| `data class D(…)` | POJO plus explicit `equals` / `hashCode` / `toString` / `copy` |
| `fun interface F : () -> R` | `@FunctionalInterface interface F { R invoke(); }` |
| `(T) -> R` | `Function<T, R>` (or `Consumer` / `Predicate` / `Supplier`) |
| `SearchSpec<T>.() -> Unit` | `Consumer<SearchSpec<T>>` |
| `Sequence<T>` | `Stream<T>` via `Spliterator`, to keep laziness |
| `inline fun <reified T> foo()` | `<T> T foo(Class<T> type)` |
| `when (x) { is A -> … }` | `instanceof` chain |
| `buildString { … }` | `StringBuilder` |
| `coerceAtLeast(0)` | `Math.max(0, x)` |
| KDoc `[Foo]` | `{@link Foo}` |

## Verifying a port against the Kotlin it replaces

Reading the Kotlin source is not enough to know what the published API is: a
`val`/`var` looks like a field and compiles to accessors over a private one, and
a `object` adds an `INSTANCE`. Phase 1's first pass flattened properties into
public fields in ten classes before this check caught it.

Build `main` in a throwaway worktree and diff the compiled surface:

```bash
git worktree add /tmp/kt-baseline main
(cd /tmp/kt-baseline && mvn -o -q clean compile -DskipTests -pl shared -am)

for c in MockContext MockRequest …; do
  for cp in /tmp/kt-baseline/shared/target/classes shared/target/classes; do
    javap -p -cp "$cp" com.vaadin.browserless.mocks.$c \
      | grep -E '^  (public|protected)' | sed 's/^  //; s/\bfinal //' | sort
  done | …   # diff the two listings
done
```

Every surviving difference has to be one you can name and justify. After
Phase 1 the list was: checked exceptions reappearing on overrides, Kotlin
synthetics (`Companion`, `INSTANCE`, `$default`, `DefaultConstructorMarker`)
replaced by real statics and overloads, and `Function0`/`Function2` becoming
`Supplier`/`BiPredicate` — which is the point of the exercise.

## Verification per phase

```bash
rm -rf */target          # stale artifacts across modules caused two false alarms before
mvn clean install
mvn test                 # baseline: shared 20, junit6 1007, spring 29, quarkus 27
mvn spotless:check
mvn dependency:tree -pl shared -Dscope=compile | grep -i kotlin   # empty after Phase 5
```
