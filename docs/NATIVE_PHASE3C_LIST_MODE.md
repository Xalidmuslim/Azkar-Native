# Native Phase 3C — Reader List Mode

## Scope

Phase 3C adds the second native reader presentation mode without starting Phase 4.

Modes:

- `Cards` — existing Phase 3A/3B one-item reader;
- `List` — all visible reader entries in one vertical list.

State is in memory only. No DataStore or persistence is added in this phase.

## Architecture

The existing reader keeps one shared navigation controller and one shared UI controller.

`AzkarReaderViewMode` is part of `AzkarReaderUiState`:

- default: `Cards`;
- `List` is selected from Contents;
- mode switches do not append reader history.

The active/current dhikr anchor remains `AzkarReaderNavigationController.state.activeIndex`.

History-free explicit selection in List uses `selectAnchor(index)`; sequential Cards navigation continues to use the existing `navigateTo/next/previous/back` history path.

## Scroll ownership

### Cards

Phase 3A behavior is retained:

- active card owns its internal long-dhikr vertical scroll;
- horizontal paging is enabled only in Cards;
- a newly opened Cards dhikr starts at the top.

### List

`LazyColumn` is the sole vertical scroll owner.

- cards do not have nested vertical scroll;
- stable keys use `dhikr.id`;
- horizontal paging is disabled;
- manual list scroll does not update reader history or current anchor;
- the current anchor changes only through explicit reader actions in this phase.

The canonical `AzkarDhikrCard` visual component is reused directly for List items.

## Contents behavior

Contents contains a real mode switch:

- «По одному»;
- «Список».

Cards → List:

- keeps the active dhikr anchor;
- closes Contents;
- scrolls the LazyColumn to that dhikr.

List → Cards:

- keeps the selected anchor;
- closes Contents;
- opens that dhikr in Cards at its top.

A Contents item jump while in List:

- remains in List;
- updates the history-free selected anchor;
- closes Contents;
- smooth-scrolls to the selected dhikr.

## Back behavior

Priority remains canonical:

1. open sheet closes first;
2. actual reader history is consumed when it exists;
3. otherwise standard Android Back.

List pixel scroll does not create history. Contents jumps in List and view-mode changes do not append history.

## Settings and Explanation

Settings apply live to the List cards using the same reader settings:

- Arabic/Russian fonts;
- sizes;
- line height;
- book/compact style;
- translation;
- sources;
- notes;
- theme.

Opening/closing Settings keeps List mode and LazyColumn position.

Explanation opens from the selected List card, keeps List mode, and returns to the same list position on Back.

## Test coverage

Phase 3C adds:

- unit coverage for default mode, mode switching, anchor preservation, history isolation, Settings/Explanation mode preservation, and paging gating;
- 15 List-mode instrumentation scenarios covering A–O from the Phase 3C acceptance contract;
- existing Phase 3A Navigation 10/10 regression;
- existing Phase 3B Sheets 8/8 regression;
- existing Cards golden 5/5 regression;
- six new physical List evidence screenshots.

Final CI/run status is recorded in the Phase 3C closeout report after validation.
