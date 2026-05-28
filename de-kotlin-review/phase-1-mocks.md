# Reviewer notes — Phase 1: mocks package port

**Branch:** `feat/no-kotlin-mocks`
**Commit:** `162a1ad refactor: port browserless-test mocks/ package from Kotlin to Java`

## Scope

12 Kotlin files in `shared/src/main/kotlin/com/vaadin/browserless/mocks/` → 14 Java files in `shared/src/main/java/com/vaadin/browserless/mocks/`. Mechanical translation, no behavior change.

## LOC delta

| | Files | LOC |
|---|---|---|
| Kotlin removed | 12 | 1,529 |
| Java added | 14 | 2,354 |
| Net | +2 files | +54% LOC |

The expansion is mostly Java boilerplate: explicit getters/setters around what were Kotlin `val`s, explicit `Instantiator` delegation, explicit `try/catch (ReflectiveOperationException)` where Kotlin silently propagated. Expected for this kind of port — same ratio holds in Phase 2.

## Structural decisions worth knowing

- **`MockHttpEnvironment.kt` held three things**, split into:
  - `MockHttpEnvironment.java` — the singleton with mutable static fields (`localPort`, `serverPort`, etc.)
  - `MockServletConfig.java` — the `ServletConfig` class
  - `MockUtils.java` — houses the `putOrRemove` helper that the three remaining mock callers need

- **`MockVaadinServlet`'s top-level extension/factory functions** (`serviceSafe`, `createVaadinServletRequest/Response`, `_createVaadinSession`, `WebBrowser`) become public static methods on the `MockVaadinServlet` class. The `WebBrowser(request)` Kotlin factory is renamed to `createWebBrowser(request)` to avoid clashing with `com.vaadin.flow.server.WebBrowser` in Java syntax.

- **`SessionAttributeMap`'s `HttpSession.attributes` extension property is gone.** Callers (only one production, one test) now use `new SessionAttributeMap(session)` directly.

- **`MockInstantiator`'s `Instantiator by delegate` is unrolled.** Java explicitly forwards all 10 `Instantiator` methods to a held delegate. Dead `ByteBuddyUtils` (only referenced from commented-out code) was dropped during the port — removes the only direct `net.bytebuddy.*` reference in shared/.

- **`MockRequest` keeps an explicit `setUserInRole(BiPredicate)` setter** so the spring module's `MockSpringServlet` keeps its current call sites. Kotlin's `var isUserInRole` had previously generated this setter on the bytecode via the `is`-prefix property convention.

## Other adjustments to call out

- `MockService.createVaadinSession` and `isAtmosphereAvailable` use `protected` (not `public`) — they're protected on the `VaadinService` parent and the original Kotlin overrides were `open` (Kotlin doesn't show this distinction the same way).
- `MockedUI.simulateClosedEvent` import points to `ComponentUtilsKt`, not the `ElementUtilsKt` my initial brief assumed — corrected by the implementing agent.
- `BrowserlessLookupInitializerTest.kt` needed updating because `additionalServices`'s type changed from `Map<KClass<*>, KClass<*>>` to `Map<Class<?>, Class<?>>` (per the brief, since `KClass` would have re-introduced kotlin-reflect type-only references).
- Vaadin Commercial License header preserved from the Kotlin originals (translated to Java block-comment form).

## Cross-module callers updated

- `shared/internal/MockVaadin.kt` — switched to `MockVaadinServlet.*` statics for the 5 helpers that used to be Kotlin top-level fns.
- `junit6/test/mocks/SessionAttributeMapTest.kt` — instantiates `SessionAttributeMap` directly.
- `junit6/test/mocks/MockRequestTest.kt` — wraps role-checker lambda in `BiPredicate { ... }` since the field type is no longer a Kotlin functional type doing SAM conversion at field-assignment.

## Validation

Clean build + four test suites:

| Suite | Tests |
|---|---|
| shared | 20 |
| junit6 | 1007 |
| spring | 29 |
| quarkus | 27 |

All green.

## Runtime classpath — unchanged

Phase 1 alone does not drop any Kotlin jars. `kotlin-stdlib` and `kotlin-reflect` are still on the classpath after this commit because `internal/` (Phase 2), `Locator.kt` / `MockVaadin.kt` / `PrettyPrintTree.kt` / `Routes.kt` (Phases 3–4), `Grid.kt` (Phase 5), and the test-side Kotlin still use them.

## Branch state

Off **local `main`** (`a333ccc`). Local main was 3 commits behind `origin/main` at the time of branch creation — couldn't fetch in-session due to SSH unavailability. **Rebase onto `origin/main` before opening a PR.**
