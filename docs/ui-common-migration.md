# UI Common source migration

This branch replaces Demo Common directly with [UI Common](https://github.com/lambdawalker/android.ui.common).
`0.0.0-SNAPSHOT` is an unpublished substitution key, not a released dependency. **Do not merge or publish
this migration until the first UI Common release is confirmed and the version catalog is updated.**

For source integration, check out the exact UI Common revision from the integration workflow and run:

```sh
./gradlew :app:assembleDebug -PuiCommonCheckout=/absolute/path/to/android.ui.common
```

The integration workflow pins the shared source by commit. The normal release/build workflows remain
unchanged and need a confirmed dependency before they can pass without this explicit checkout.
Remove the temporary substitution when switching to the published artifact; do not add a global
mavenLocal fallback or a floating Git dependency.

CatalogScreen replaces DemoScreen, ContentCard replaces the content overload of DemoCard, ActionCard
replaces its action overload, and SectionHeading replaces CardTitle. Settings labels/navigation belong
to each app. UI Common inherits the host MaterialTheme. No compatibility wrappers are retained.

The new dependency is restricted to :app. Published library APIs and minimum SDKs are unchanged.

Existing accepted screenshot metadata remains historical evidence for its original source. Re-render
and review affected consumer screens before accepting new baselines; do not rewrite historical
acceptance hashes to make the migration appear validated.

Pinned implementation: `a32dce2342b80f5b793fec6ba8ce75c69228251b` ([UI Common PR #2](https://github.com/lambdawalker/android.ui.common/pull/2)).
