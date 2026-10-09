# Native Phase 6 App Integration

## Scope

Phase 6 integrates the already validated Phase 3–5 reader, content, period and persistence architecture into a real installable native Android application.

This phase does not change religious content and does not start Phase 7 or Play publishing.

## Production entry point

- Activity: `app.xalidmuslim.azkar.MainActivity`
- Activity base: `ComponentActivity`
- UI: Jetpack Compose via `setContent`
- Production root: `AzkarAppRoot`
- Default period: Morning
- Package/applicationId: `app.xalidmuslim.azkar`
- App label: `Азкар`

`MainActivity` contains no reader business logic.

## Production dependency wiring

`AzkarAppRoot` creates the production repository once per Activity composition with `remember` and uses a single application-context Preferences DataStore:

- DataStore name: `azkar_preferences`
- Repository: `DataStoreAzkarPreferencesRepository`
- Date provider: `SystemAzkarDateProvider`
- Reader: existing `AzkarProductionReaderScreen`
- Catalog: existing `AzkarCatalog`

The UI does not access DataStore directly.

Hydration remains owned by `AzkarReaderUiController`. While persisted state is loading, the reader body is withheld, preventing default reader settings from flashing before persisted settings arrive.

## Launcher and manifest

The production manifest contains:

- `MAIN`
- `LAUNCHER`
- `android:exported="true"`
- label `Азкар`
- no INTERNET permission
- no AlFatiha package name

A temporary native adaptive launcher icon is included because the repository did not contain a separately approved Azkar launcher icon at the Phase 6 base HEAD. Final release icon approval remains a Phase 7/release task.

## Offline contract

Runtime reader/content/persistence are local:

- production content is compiled into the app;
- preferences use local DataStore;
- fonts are packaged as Android resources by the existing verified font build pipeline;
- no WebView is used;
- no runtime network client is added;
- INTERNET permission is absent.

## Back contract

Existing reader Back handlers remain authoritative:

1. close Settings / Contents / Explanation;
2. reader history;
3. fall through to standard Android Activity Back at root.

System edge Back is not repurposed as a custom dhikr swipe gesture.

## Phase 6 validation

The Phase 6 workflow validates in one full run:

- compile;
- unit tests;
- lint;
- Phase 3A / 3B / 3C;
- Phase 4;
- Phase 5;
- production MainActivity integration;
- Phase 5 visual regression evidence;
- ten real-app screenshots through MainActivity;
- merged/APK manifest identity;
- no INTERNET permission;
- local packaged fonts;
- debug APK creation;
- APK install and launcher smoke test on API 35 with KVM.

Closeout status is recorded separately only after that full workflow succeeds and screenshot evidence is inspected.
