# Browserless Test Guidelines

These guidelines describe how features in Vaadin Browserless Test — the
browser-free UI testing framework for Vaadin — should be designed and
implemented in the `browserless-test` repository. Chapters can be read
selectively for the topics your work touches.

Treat these as guidelines, not hard rules. They are best practices that should
be followed by default, but can be deviated from when necessary to make
something work.

For the canonical list of checkable conventions see
[`CONVENTIONS.md`](../CONVENTIONS.md). For repository-level commands (build,
test, format) see [`CLAUDE.md`](../CLAUDE.md).

## Chapters

| Chapter                            | Topic                                                                                   |
| ---------------------------------- | --------------------------------------------------------------------------------------- |
| [Repository](repository.md)        | Tech stack, module layout, where things live, Maven and the annotation processor.        |
| [Flow Version](flow-version.md)    | One version per Flow release: no back-compatibility, fix Flow first, delete old branches. |
| [Architecture](architecture.md)    | The mocked Vaadin environment, round trips, signals, contexts, tester resolution.        |
| [Testers](testers.md)              | Writing a tester so its interactions are indistinguishable from a real user's.           |
| [Locators](locators.md)            | The typed locator API and what the annotation processor generates and enforces.          |
| [Design](design.md)                | Shape of new public API: naming, options, visibility, the context hierarchy.             |
| [Documenting](documenting.md)      | Javadoc expectations for testers and for the mocked environment.                         |
| [Testing](testing.md)              | Where tests live, the shared value-tester contracts, debugging failures.                 |
