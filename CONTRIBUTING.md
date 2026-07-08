# Contributing

Thanks for contributing to Vaadin Browserless Test! A few conventions to keep in
mind when opening a pull request.

## Javadoc `@since` tags

Individual pull requests should **not** add `@since` tags to new public API.

`@since` values are added in a single, separate pass just before a release, once
the target version is known. Adding them per-PR is error-prone: the version a
change actually ships in may differ from what is current on the branch (releases
slip, changes get backported, etc.), so tags added during development are often
wrong by the time the API is published. Deferring them to a pre-release pass
keeps the tags accurate.
