# Project status — 0.1.1-alpha, 2026-11-25

**0.1.0-alpha CI passed** (run 45625738536, Kotlin tests, APK, Lint, dependency report). APK installed and user reported 22 of 55 checks working. Remaining 33 checks are unconfirmed, not automatically defects.

## User QA findings
- User reports missing fourth/neutral tone visualization/examples and repetitive reading/grammar/characters.
- Existing fourth-tone numeric template was present; neutral template intentionally empty; cause of fourth-tone display issue not reproduced on device.
- 0.1.1 adds independent padded pedagogical diagrams for four lexical tones plus context-specific neutral tone; 16 TTS targets; short-syllable F0 gate and regression tests.
- 50 original dictionary records, 5 selectable readings, 10 grammar entries, 16 character cards; daily rotation and manual browsing. No verified HSK mapping, no official textbook copying, no OCR or handwriting.
- CI and device test for 0.1.1 pending. No claim of phonetic accuracy or native recordings.

## Remaining
Confirm levels, dictionary search and SRS, playback and measured F0, library, statistics, persistence, offline mode. No complete HSK course or full mocks. No human accuracy validation. See CONTENT_COVERAGE.md and ROADMAP.md. Public repository, no redistribution license selected.
