# Project status — 0.1.0-alpha, 2026-11-25

**Not release-ready.** Source snapshot and local Git history created. GitHub account Aidoni0mi lookup successful; no create-repository API exposed, remote now exists; initial source import in progress. Environment: Java 21.0.12, Kotlin CLI 1.9.0, Git 2.47.3; no Gradle, Android SDK, adb, emulator; network DNS failure. No APK, no executed GitHub Actions. Android/Compose type checking and runtime behavior unverified. Kotlin-only syntax scan without Android dependencies is not a build.

## Implemented in source
- Compose Russian UI, five tabs, light/dark/system, HSK 1–9 goal selectors (default 4), date/time plan and summary.
- 30 original dictionary records with Russian, pinyin, traditional, POS and example; exact/substring search including tone-mark-stripped pinyin; longest-match reader and unknown fallback. Not a complete dictionary or context model.
- Local PDF/TXT import, private copy, size/header checks, page rendering / TXT tap, resume page, edition manual tag and delete. PDF rendering only, no OCR.
- SQLite review cards/history/book metadata/quiz; scheduler and due list. No Room yet.
- Three-second microphone capture on user request, playback, local pitch computation, schematic/actual overlay, qualitative hints, basic sandhi function. No model confidence, phoneme assessment, ASR, multi-syllable alignment, real reference contours, or saved audio history.
- Five original grammar points, five-item quiz, one self-practice speaking prompt, starter 30-day distribution. No full official exam or scoring. No full adaptive plan/diagnostics.
- GitHub Actions workflow and JVM tests supplied; workflow not executed.

## Executed tests (Kotlin CLI)
`bash scripts/core-check.sh` passed 13 labeled checks: 5 steady synthetic pitches (90, 140, 220, 360, 480 Hz, absolute difference of the mean estimated F0 from target under 3 Hz in each test), silence, empty signal, rise, fall, longest-match/Unicode, four basic sandhi assertions, SRS and sprint. Real speech, microphone, performance, Android unit/instrumentation, UI, DB migrations, import and offline device tests **not run**. No human accuracy claim.

## Main remaining risks
Unverified Compose/API compile, audio lifecycle and TTS device availability, native PDF parsing, dictionary ambiguity, editorial/level mappings, large data performance, limited normalization, no human validation. Source code has no public redistribution license selected; no third-party books or personal recordings included. See ROADMAP and CONTENT_COVERAGE for all deferred items.
