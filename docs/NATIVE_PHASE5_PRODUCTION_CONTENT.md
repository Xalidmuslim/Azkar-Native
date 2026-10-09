# Native Phase 5 — Production Content + Periods

## Scope

Phase 5 replaces golden/demo data as a production content source and implements canonical Morning/Evening reader periods without integrating a production launcher or MainActivity.

## Production catalog

- Package: `app.xalidmuslim.azkar.content`
- Immutable catalog: `AzkarCatalog`
- Stable IDs: exactly 16
- Morning: 14
- Evening: 13
- Progress identity remains `LocalDate + stable dhikr id`; period is not part of the persistence key.
- `kingdom` and `by-you` resolve period-specific Arabic and Russian variants under one stable ID.
- Golden fixtures remain available only for regression/evidence screens and are not the production dataset.

## Period state

`AzkarPeriodReaderController` owns the current period and the navigation controller for that filtered period.

Canonical switch behavior:
- switching creates a new filtered navigation controller;
- active index becomes 0;
- reader history is empty;
- reading scroll starts at 0;
- Compose period subtree is keyed by period so Cards/List scroll state is not inherited;
- shared `AzkarReaderUiController` is retained, preserving Settings, ViewMode and daily progress;
- reset receives only currently visible period IDs.

Default period in Phase 5 is deterministic **Morning**. No time-based auto-selection is introduced because the canonical docs do not require it in this phase.

## Filtering

- Contents receives only the current period entries.
- Cards totals and bounds use the filtered list: 14 Morning / 13 Evening.
- List renders only the current period entries.
- Header progress counts completed items, not repetitions, over the filtered denominator.
- Shared stable IDs use one daily progress record across periods.

## Explanation model

Production content supports:
- Смысл
- Связанный случай
- Примечания учёных
- Источники разбора

Optional sections are rendered only when non-empty. “Связанный случай” is populated only where a transmitted report was retained during content audit.

## Excluded from Phase 5

- production MainActivity / launcher
- splash
- release signing
- Play Store
- release APK/AAB
- notifications
- widgets
- cloud sync
- account/login
- Phase 6 work

`main` is not modified.
