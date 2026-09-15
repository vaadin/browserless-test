# Contributing

Thanks for contributing to Vaadin Browserless Test! This page is the entry
point: what to run, what the conventions are, and where the detailed
guidelines live.

## Where things are written down

- [`CONVENTIONS.md`](CONVENTIONS.md) — the canonical list of checkable
  conventions. Read it in full before opening a pull request.
- [`guidelines/`](guidelines/overview.md) — the reasoning behind the
  conventions, one chapter per topic. The two that apply to almost every
  change are [Flow Version](guidelines/flow-version.md) and
  [Testers](guidelines/testers.md).
- [`CLAUDE.md`](CLAUDE.md) — repository overview and the build, test and
  format commands, for both people and coding agents.

## Building and testing

```bash
mvn clean install              # build everything
mvn clean install -DskipTests  # faster
mvn test -pl junit6            # the tests most changes need
mvn spotless:apply             # before every commit
```

Most tests live in the `junit6` module rather than next to the code they cover
— see [Testing](guidelines/testing.md).

## One Flow version per branch

A Browserless Test version targets exactly one Vaadin/Flow version (`1.0` →
25.1, `1.1` → 25.2, `main` → 25.3). There is no backwards compatibility with
older Flow releases, so when a tester needs something Flow does not expose, the
preferred fix is to add it to Flow first and use it directly here — and code
that branches on a Flow version should be deleted rather than extended. See
[Flow Version](guidelines/flow-version.md).

## Commits and pull requests

- Commit subjects follow Conventional Commits: `<type>: <summary>`, under 72
  characters, imperative verb, `!` before the colon for a breaking change.
  Types in use: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`.
- Branch names follow the type: `feat/…`, `fix/…`, `docs/…`, `chore/…`.
- Run `mvn spotless:apply` before committing. If the `Format Check` job fails
  anyway, comment `/format` on the pull request and the formatting is applied
  for you.
- A pull request description uses `## Summary`, `## What changed`,
  `## Use case` (when the change adds API), `## API Changes` (when the public
  API changes) and `## Test summary`, omitting what does not apply.

## Javadoc `@since` tags

Individual pull requests should **not** add `@since` tags to new public API.

`@since` values are added in a single, separate pass just before a release, once
the target version is known. Adding them per-PR is error-prone: the version a
change actually ships in may differ from what is current on the branch (releases
slip, changes get backported, etc.), so tags added during development are often
wrong by the time the API is published. Deferring them to a pre-release pass
keeps the tags accurate.

## Simulating the browser in testers

A tester interaction has to be indistinguishable from the real one: the events
an application observes must report `isFromClient() == true`. Never drive a
component through its plain server-side setter from a tester — use the shared
`ComponentTester` helpers, and call `ensureComponentIsUsable()` first in any
method that changes state.

The helpers, what a `setValue` may refuse, and the steps for adding a tester
are documented in [Testers](guidelines/testers.md).

## Test contracts for value testers

`clear()` and `clickClearButton()` behave the same on every value tester, so
their expectations are asserted once, as `default` methods on `ClearContract`,
`ClearButtonContract` and `CommitsEmptyValueContract` in
`junit6/src/test/java/com/vaadin/browserless/`. If you add or change a value
tester, implement the ones that apply — the compiler will ask you for the
hooks. See [Testing](guidelines/testing.md) for which contract applies when.
