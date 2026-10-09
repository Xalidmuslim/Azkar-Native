# Native Phase 4 — Persistence + Daily Progress

## Scope

Phase 4 adds offline persistence without integrating a production launcher or implementing Morning/Evening switching.

Persistent state:

- reader settings;
- Cards/List view mode;
- daily dhikr progress.

Session-only state remains non-persistent:

- active dhikr/index;
- navigation history;
- reading/list scroll positions;
- active sheet;
- selected explanation;
- period;
- animation and gesture state.

## DataStore architecture

AndroidX Preferences DataStore is accessed only through `AzkarPreferencesRepository`.

Production implementation:

- `persistence/AzkarPreferencesRepository.kt`
- `persistence/DataStoreAzkarPreferencesRepository.kt`
- `persistence/AzkarPreferenceKeys.kt`
- `persistence/AzkarDateProvider.kt`

UI composables never read or write DataStore directly. `AzkarReaderUiController` is the state-holder boundary and accepts an injectable repository, date provider and coroutine scope.

## Keys and defaults

Stable settings keys store enum IDs, numeric values and flags. Progress uses deterministic keys:

`progress_<LocalDate>_<stable dhikr id>`

Titles are never used as progress keys.

Canonical defaults remain unchanged:

- Arabic font: Noto Naskh Arabic
- Russian font: Literata
- Arabic size: 32
- Russian size: 17
- line height: 1.65
- reader style: Book
- translation/sources/notes: ON
- theme: System
- view mode: Cards

## Validation and fallback

Read-time validation:

- Arabic size: 22..44
- Russian size: 13..25
- line height: 1.25..2.00
- unknown font IDs: canonical defaults
- unknown theme: System
- unknown reader style: Book
- unknown view mode: Cards

Non-finite numeric values fall back to canonical defaults. Read failures emit safe defaults rather than crashing the reader.

## Date scoping

`SystemAzkarDateProvider` resolves `LocalDate` using the system timezone. The controller captures the session date when it is created. A new controller/session on a new local calendar date reads a separate progress namespace. Old-day values may remain in DataStore.

## Progress model

Progress is one shared map keyed by stable item ID. Cards and List therefore render the same count/completed state.

Increment semantics:

- one action increments by exactly one;
- values clamp at target;
- completed entries ignore further increment requests;
- UI updates immediately while DataStore writes asynchronously.

The progress header counts completed visible items, not the sum of repetitions.

## Reset semantics

Reset receives only the IDs currently visible to the reader and sets those date-scoped entries to zero. Unrelated entries are untouched, preserving future period-scoped reset behavior.

## Hydration and failures

Persistent controllers begin in a non-hydrated state. Reader content is not rendered until the first repository snapshot is available, avoiding a default-state-to-persisted-state visual flash in evidence harnesses.

Production UI uses no `runBlocking` and performs no direct disk I/O. Repository read/write failure is contained and never exposes a stacktrace to the user.

## Phase boundary

Phase 4 does not add the production dataset, Morning/Evening switching, automatic period selection, `MainActivity`, launcher, splash, APK/AAB release, notifications, widgets, sync, login, or Phase 5 behavior.

The canonical contract is not rewritten by this document.
