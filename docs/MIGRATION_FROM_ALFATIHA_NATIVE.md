# Migration from AlFatiha-Native

The Azkar native work was separated into its own repository on 2026-09-29.

Source repository used during prototyping: `Xalidmuslim/AlFatiha-Native`.
Source branch at migration: `azkar-native-phase2-golden-reading-2026-09-29`.
Source HEAD at migration: `3a9e39c8159573f249b833ee4cea198b0340de00`.

Only Azkar-specific Android code, tests, design-system files, Phase 0–2 documentation, font licenses, and Phase 2 CI were migrated. Al-Fatiha production code/assets and web deployment files were intentionally excluded.

The native package/namespace was changed from the temporary `app.alfatiha.tafsir.azkar...` location to `app.xalidmuslim.azkar...`.
