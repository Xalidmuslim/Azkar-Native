# Native Phase 3C Closeout — Reader List Mode

## Status

**PHASE 3C LIST MODE = CLOSED / PASS**

Branch: `azkar-native-phase3c-list-mode-2026-09-29`

Final validated production HEAD: `51731c681d1bad2830757a1d59374129ee4ae3b4`

Final CI run: `36604372068` — SUCCESS

Evidence artifact: `azkar-phase3c-list-evidence`

Evidence artifact ID: `11050563606`

Earlier validation run `36603012168` / artifact `11050337256` also passed on HEAD `f0422f836163f8c06b5798f3b0fe846049421569`; the final validated run above covers the later Phase 3C production fix that keeps List jump targets below the sticky toolbar.

## Build and regression

- Compile Compose/Kotlin: PASS
- Unit tests: PASS
- Lint: PASS
- Phase 3A navigation regression: 10/10 PASS
- Phase 3B sheet regression: 8/8 PASS
- Phase 3C List instrumentation: 15/15 PASS
- Existing Cards golden regression: 5/5 PASS
- List visual evidence: 6/6 PASS
- KVM: WORKING
- Startup ANR: NO

No `ANR`, `FATAL EXCEPTION`, `Process crashed`, `INSTRUMENTATION_FAILED`, `ClassNotFoundException`, or `NoClassDefFoundError` was found in the final diagnostics artifact.

## List Mode architecture

Phase 3C adds a second in-memory reader presentation mode:

- `Cards` retains the existing Phase 3A/3B single-dhikr reader and horizontal swipe navigation.
- `List` renders reader entries in one `LazyColumn` and owns vertical scrolling.
- `AzkarReaderViewMode` is held in `AzkarReaderUiState`; no DataStore or persistence is introduced.
- Cards and List share the existing navigation controller and UI controller.
- The active dhikr anchor remains the navigation controller's `activeIndex`.
- Explicit selection in List uses history-free anchor selection; list pixel scrolling does not invent reader Back history.
- Horizontal paging is disabled in List and remains enabled in Cards.
- Contents can switch Cards/List and jump to a dhikr while preserving the intended reader context.
- Settings and Explanation preserve List mode and list position.
- Back priority remains sheet first, then real reader history, then Android Back.
- The final Phase 3C fix applies the canonical top margin when programmatically scrolling a List target so it remains visible below the sticky toolbar.

## Scope guard

- `main` was not changed by Phase 3C closeout.
- No DataStore was added.
- No persistence was added.
- Religious content was not changed.
- Phase 4 has not started.
- No release APK/AAB work is part of this closeout.

**PHASE 4 READY = YES**
