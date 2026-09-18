# Architecture

How the pieces of the mocked environment fit together, and what to keep in mind
when touching them.

## The mocked Vaadin environment

There is no servlet container and no browser. `MockVaadin.setup(routes,
uiFactory, lookupServices)` builds the objects Flow expects and installs them
as thread-locals:

- `MockVaadinServlet` / `MockService` — a `VaadinServletService` that resolves
  routes from a `Routes` instance instead of from a real deployment. Object
  creation is *not* mocked: the service uses whatever `Instantiator` the
  environment's `Lookup` provides, seeded by `BrowserlessLookupInitializer` and
  by the Spring and Quarkus lookup initializers, which is how views and beans
  still come from the real container.
  (`MockInstantiator` is deprecated and scheduled for removal — it forwards
  every call to that same instantiator and mocks nothing. Do not build on it.)
- `MockHttpSession` + `MockRequest` / `MockResponse` — the servlet API surface
  a `VaadinSession` needs. IDs come from a sequential counter, so test output
  stays readable.
- `MockedUI` — the `UI` instance. The UI factory *must* return a fresh
  instance per test, or a test inherits the previous test's state.
- Strong references to the session, UI, request and response are held in
  thread-locals, because Flow itself only soft-references them and they would
  otherwise be collected mid-test.

`Routes` is populated by `RouteDiscovery`, which classpath-scans the packages
named by `@ViewPackages` (a full scan when the annotation is absent — slow,
so tests should always narrow it).

The Spring and Quarkus modules do not reimplement any of this: they subclass
the mocks (`MockSpringServlet` / `MockSpringServletService`,
`MockQuarkusServlet` / `MockQuarkusServletService`) so views and beans come
from the real container.

**Keep the mocks faithful, including the failure modes.** A mock that always
succeeds is worse than no mock, because the application code it exercises is
the code that handles the failure. `MockRequest.changeSessionId()` throws
`IllegalStateException` without a valid session because a servlet container
does. Go through the Vaadin extension points rather than around them: fire
service and session lifecycle events through the `VaadinService` event bus, and
forward `Instantiator` calls to the real instantiator instead of
reimplementing them.

## Round trips

The browser is what normally makes Flow flush the state tree, run
`UI.access()` tasks and deliver `beforeClientResponse` callbacks. Browserless
Test does it explicitly:

- `MockVaadin.clientRoundtrip()` runs the pending UI queue and the
  `beforeClientResponse` callbacks, i.e. everything a real client response
  would trigger.
- `MockVaadin.runUIQueue()` runs just the queued `UI.access()` tasks, and can
  propagate an exception to the `ErrorHandler` when a test wants to assert on
  it.
- `ComponentTester#roundTrip()` (protected) is the tester-level entry point;
  `roundTrip()` on the test base class and `window.roundTrip()` are the
  test-level ones.
- `TestingLifecycleHook.awaitBeforeLookup` calls the round trip before a
  component lookup by default, so a query already sees the state a real client
  response would have produced.

A tester that pushes a change through the client path (`setPropertyAsUser`)
must round-trip for the derived event to fire — see [Testers](testers.md).

## Component tree queries

`ComponentQuery<T>` walks the tree from a root (the current view, a parent
layout, or any component) and filters by type, id, test id, class name,
attribute, text, label, value, theme or an arbitrary predicate. Traversal goes
through `ComponentUtil.getAllChildren`, so slotted and virtual children are
found too — a Dialog's content, a Grid's header, footer and editor components,
a Card's slots.

`ElementConditions` holds the element-level predicates the query builds on.
`Locator` (the internal Kotlin one, not the public locator API) and
`DepthFirstTreeIterator` do the walking; `PrettyPrintTree` renders the tree for
`TreeOnFailureExtension`.

## Tester resolution

`TesterRegistry` maps component class → tester class. It is seeded at class
load with the built-in testers and extended by `registerPackages(…)` from
`@ComponentTesterPackages`, using ClassGraph to find `@Tests`-annotated
`ComponentTester` subclasses. Each package is scanned at most once per JVM.

Resolution walks the component's superclass chain and returns the most specific
tester registered, so a custom tester for a component subclass wins over the
built-in one. `test(component)` goes through this; the typed `test(…)`
overloads in `TesterWrappers` only pin the *return* type, and still prefer a
more specific registered tester at runtime.

## The context hierarchy

For multi-user and multi-window tests the environment is layered to mirror
Vaadin's own:

| Context                         | Maps to                       |
| ------------------------------- | ----------------------------- |
| `BrowserlessApplicationContext` | shared `VaadinServletService` |
| `BrowserlessUserContext`        | one `VaadinSession`           |
| `BrowserlessUIContext`          | one `UI`                      |

Every DSL call on a `BrowserlessUIContext` calls `activate()` first, which
switches the Vaadin thread-locals to that window and, on a user switch, saves
the outgoing user's security context and restores the incoming user's
snapshot. That is why tests can interleave `w1.…` and `w2.…` freely.

Two properties to preserve when touching this code: the contexts are
**thread-affine** (created, used and closed on one thread, with the active
context in a `ThreadLocal`), and closing the application context closes users
and windows in order, fires destroy listeners and clears both Vaadin and
security thread-locals.

The single-user `BrowserlessTest` base class and the JUnit extensions are a
thinner path over the same mocks — a change to session or UI setup has to hold
for both.

## Signals

Signal effects and shared-signal confirmations are not dispatched to a
background pool in a browserless test. `TestSignalEnvironment` replaces the
`SignalEnvironment` effect dispatcher with one that queues tasks, and
`runPendingSignalsTasks()` drains the queue on the test thread — releasing the
`VaadinSession` lock while it does, so a background thread can take it.

The consequence worth knowing when writing or reviewing signal code: a write to
a shared signal is applied optimistically and visible through `peek()` at once,
but the `SignalOperation` it returns completes only when the queued
confirmation runs. Blocking on the operation without draining the queue always
times out, because the confirmation task needs the thread that is blocked.

## Client-side behavior that has no client

Some things the browser does have to be modeled explicitly, and the model is
the framework's contract with its users:

- **`isFromClient()`** — the whole point of the tester layer; see
  [Testers](testers.md).
- **External navigation** — `Page.setLocation()` and `Page.open()` are captured
  on the window's mock `Page` and asserted with `getExternalNavigationURL()` /
  `getOpenedWindows()` instead of leaving the test.
- **Shortcuts** — `Shortcuts` fires the key events a shortcut listens for,
  including modifiers, and then round-trips.
- **Modality and inertness** — `setModal(…)` generates the client-side change,
  and an inert node is not usable, so a test can assert that a dialog blocks
  the view behind it.
