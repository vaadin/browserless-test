# Flow Version

The single most important difference between this repository and most Vaadin
repositories: **one Browserless Test version targets exactly one Flow
version.** There is no need — and no wish — to keep working against older Flow
releases.

## The mapping

| Branch | Version       | Vaadin / Flow |
| ------ | ------------- | ------------- |
| `1.0`  | `1.0.x`       | 25.1          |
| `1.1`  | `1.1.x`       | 25.2          |
| `main` | `1.2-SNAPSHOT`| 25.3          |

`flow.version` and `vaadin.version` in the root `pom.xml` hold the target, and
Flow is a `provided` dependency resolved from
`maven.vaadin.com/vaadin-prereleases`. A branch tracks the matching Vaadin
snapshot for its whole development cycle, so `main` always builds against the
in-development Flow, not against a released one.

Bumping those properties is a release activity (`chore: bump to 1.2-SNAPSHOT
and Vaadin to 25.3-SNAPSHOT`), not something an individual change does.

## What follows from it

### Fix it in Flow first

When a tester needs state or behavior that Flow does not expose, the preferred
fix is to **add it to Flow and use it directly here**. Both changes ship in the
same release train, so there is never a window where the hook is missing.

That is nearly always better than the alternatives:

- reflection into a private field, which breaks silently on the next Flow
  refactor and hides the fact that the API gap exists;
- copying Flow logic into a tester, which drifts from the component the moment
  the component changes;
- asking the user to reach around the tester.

The practical shape of the work is: open the Flow pull request, build the Flow
branch locally (`mvn clean install -DskipTests -pl flow-server -am` in a Flow
checkout), write the Browserless Test change against the local snapshot, and
land the Flow side first. Mention the Flow pull request in the Browserless Test
one so a reviewer can see the pair.

### No compatibility branches

Do not write code whose purpose is to work against more than one Flow version:

- no `Class.forName` / `try { getMethod(…) }` probes to detect whether an API
  exists;
- no `if` on a Flow version number or on the presence of a class;
- no reflective fallback "for older Vaadin";
- no deprecation cycle kept alive because an older Flow needed it.

When you touch code that still has such a branch, delete the branch instead of
extending it. Pull request #211 is the model: adding `isValid()` to the picker
testers removed the reflection-based Vaadin 24.4 / 24.5 branch they carried,
and the testers now call `getDefaultValidator()` directly.

### Reflection is a last resort, and it is documented

`ComponentTester` has `getField(…)` / `getMethod(…)` helpers, and a handful of
testers still use them (`ComboBoxTester`, `UploadTester`, `TreeGridTester`,
`VirtualListTester` and a few others). Before adding another one, check whether
Flow can expose the state properly. If reflection really is the only way for
now, say in a comment which Flow API is missing, so the next person knows what
to ask Flow for.

### Deprecated Flow API

Because the Flow version is pinned, Flow deprecations are a signal to act, not
to wait. When Flow deprecates something a tester uses, move to the replacement
on the branch that first sees the deprecation, rather than carrying the old
call until it is removed.

## What this does *not* license

The version coupling is about Flow, not about this framework's own users.
Applications compile against `ComponentTester`, the testers, the locators and
the `BrowserlessTest` base classes, so those remain real public API — see
[Design](design.md) and the Public API section of
[`CONVENTIONS.md`](../CONVENTIONS.md).
