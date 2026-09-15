# Documenting

## Write for someone who cannot see the browser

A tester's Javadoc is read by someone who is trying to reproduce a user
interaction without a UI in front of them. So it says what the *user action*
is, not what the method calls:

> Toggles details visibility, as if the summary is clicked on the browser.

That single clause tells a reader which real interaction the method stands for,
which is the only thing they need to pick the right method. Explain, in this
order:

1. what interaction the method performs;
2. what state the component is left in;
3. when it throws.

## Javadoc rules

- **`@throws IllegalStateException` is part of the contract, not a detail.**
  Any method that calls `ensureComponentIsUsable()` documents it, and says what
  "not usable" covers when the tester adds its own condition (details already
  open, clear button hidden, …).
- **Say what is committed and what is refused.** For a value tester, the
  Javadoc of `setValue` states that the value is committed the way the browser
  would commit it and that validity is asserted separately with `isValid()`.
  A reader who expects an exception needs to find out here, not from a failing
  test.
- **Explain non-obvious mechanics once, where they are met.** The
  signal-queue behavior is documented on `TestSignalEnvironment` and
  `runPendingSignalsTasks()`, including the fact that blocking on a
  `SignalOperation` without draining the queue always times out. That is the
  kind of thing a user cannot guess and will otherwise file as a bug.
- **Do not add `@since` tags.** They are reconciled against the published
  release history in a separate pass before a release — the `Update @since
  tags` workflow. The full reasoning is in
  [`CONTRIBUTING.md`](../CONTRIBUTING.md).
- **Javadoc describes the code today**, not what changed. Change history
  belongs in commit messages.
- **Mark internal API as internal.** "For internal use only. May be renamed or
  removed in a future release." on classes like `BaseBrowserlessTest` is what
  keeps them changeable.

## Documenting the mocks

A mock's Javadoc says which real behavior it reproduces and where it stops. The
useful sentence is the boundary one: which container behavior is modeled, which
failure modes are reproduced, and what a test may therefore assert. A mock
documented only as "mock implementation of X" leaves a user guessing whether a
missing behavior is deliberate.

## README and CONTRIBUTING

The README is user-facing documentation, and it is long on purpose: a new
capability that a user is expected to reach for gets a section with a runnable
snippet, not only a Javadoc entry. Features that only tester authors touch stay
out of it.

When behavior a document describes changes, update the document in the same
pull request. `README.md`, `CONTRIBUTING.md`,
[`CONVENTIONS.md`](../CONVENTIONS.md) and these guidelines are part of the
deliverable, not a follow-up.
