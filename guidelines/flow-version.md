# Flow Version

The single most important difference between this repository and most Vaadin
repositories: **one Browserless Test version targets exactly one Vaadin
version** — one Flow version and the components that ship with it. There is no
need — and no wish — to keep working against older releases of either.

## The mapping

| Branch | Version        | Vaadin / Flow |
| ------ | -------------- | ------------- |
| `1.0`  | `1.0.x`        | 25.1          |
| `1.1`  | `1.1.x`        | 25.2          |
| `main` | `25.3-SNAPSHOT`| 25.3          |

From 25.3 on the version *is* the Vaadin version it targets: the separate
`1.x` numbering is gone, so the branch developing against Vaadin `x.y` carries
`x.y-SNAPSHOT` and releases `x.y.z`. The older `1.0` and `1.1` branches keep
their own numbers.

`flow.version` and `vaadin.version` in the root `pom.xml` hold the target, and
Flow is a `provided` dependency resolved from
`maven.vaadin.com/vaadin-prereleases`. A branch tracks the matching Vaadin
snapshot for its whole development cycle, so `main` always builds against the
in-development Flow, not against a released one.

Bumping those properties is a release activity (`chore: bump to 25.4-SNAPSHOT
and Vaadin to 25.4-SNAPSHOT`), not something an individual change does.

## What follows from it

### Fix it upstream first

When a tester needs state or behavior that is not exposed, the preferred fix is
to **add it upstream and use it directly here**. Everything ships in the same
release train, so there is never a window where the hook is missing.

Upstream is not always Flow. Pick the repository that owns the gap:

- `vaadin/flow` — the core server-side API a tester builds on: `Element`,
  `ComponentUtil`, `AbstractField` / `AbstractFieldSupport`,
  `ElementPropertyMap`, the router, `Instantiator` and `Lookup`.
- `vaadin/flow-components` — the Java component the tester wraps. A missing
  getter on `Grid`, a validator a picker does not expose, state a component
  keeps private: that belongs there, not in a reflective workaround here. It is
  frequently the right place, because most testers are blocked by their own
  component rather than by core Flow.

Either is nearly always better than the alternatives:

- reflection into a private field, which breaks silently on the next upstream
  refactor and hides the fact that the API gap exists;
- copying component logic into a tester, which drifts from the component the
  moment the component changes;
- asking the user to reach around the tester.

The practical shape of the work is: open the upstream pull request, build that
branch locally so its snapshot lands in the local repository (`mvn clean
install -DskipTests` in the module you changed, plus `-am`), write the
Browserless Test change against it, and land the upstream side first. Mention
the upstream pull request in the Browserless Test one so a reviewer can see the
pair.

### No compatibility branches

Do not write code whose purpose is to work against more than one Vaadin
version:

- no `Class.forName` / `try { getMethod(…) }` probes to detect whether an API
  exists;
- no `if` on a version number or on the presence of a class;
- no reflective fallback "for older Vaadin";
- no deprecation cycle kept alive because an older Flow or an older component
  needed it.

When you touch code that still has such a branch, delete the branch instead of
extending it. Pull request #211 is the model: adding `isValid()` to the picker
testers removed the reflection-based Vaadin 24.4 / 24.5 branch they carried,
and the testers now call `getDefaultValidator()` directly.

### Reflection is a last resort, and it is documented

`ComponentTester` has `getField(…)` / `getMethod(…)` helpers, and a handful of
testers still use them (`ComboBoxTester`, `UploadTester`, `TreeGridTester`,
`VirtualListTester` and a few others). Before adding another one, check whether
the component — or Flow — can expose the state properly. If reflection really
is the only way for now, say in a comment which upstream API is missing and in
which repository, so the next person knows what to ask for.

### Deprecated upstream API

Because the version is pinned, an upstream deprecation is a signal to act, not
to wait. When Flow or a component deprecates something a tester uses, move to
the replacement on the branch that first sees the deprecation, rather than
carrying the old call until it is removed. The same applies to this
repository's own deprecations — `MockInstantiator`, for instance, is scheduled
for removal and nothing new should build on it.

## What this does *not* license

The version coupling is about Flow and the components, not about this
framework's own users.
Applications compile against `ComponentTester`, the testers, the locators and
the `BrowserlessTest` base classes, so those remain real public API — see
[Design](design.md) and the Public API section of
[`CONVENTIONS.md`](../CONVENTIONS.md).
