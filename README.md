# HanziFlow — Chinese Mastery

**0.4.0-alpha / engineering checkpoint, 2026-11-28.** Full HSK 2025 syllabus lists: 11,000 words, 3,088 recognition characters, 1,200 writing characters and 593 grammar headings. Levels filter new and cumulative material. English CC-CEDICT glosses cover 10,734 entries; 300 HSK1 entries have original Russian glosses plus existing matched starter entries. Russian glosses and detailed grammar lessons remain incomplete. The app now also includes 16 original everyday situations, each with a dialogue and a short story: 32 texts and 96 lines with Hanzi, tone-marked pinyin and Russian translation. No copyrighted books are bundled.

Each life-situation line can be spoken by an installed offline Mandarin TTS voice at normal or slower speed. Guided reading uses the device's on-device Mandarin recognizer on compatible Android 12+ devices to compare recognized text with the selected line. A separate recording shows the measured F0 contour beside the expected tone sequence as a qualitative practice aid. These are not phoneme grading, per-syllable alignment or a calibrated pronunciation score.

## Run on a development computer

1. Install Android Studio with JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. Android 8.0+ device (API 26).
2. Open the directory containing `settings.gradle.kts`. Set Gradle JDK to 17; use Gradle 8.9. A wrapper binary is not supplied because it could not be downloaded in the source environment. Use an installed Gradle 8.9 to run `gradle wrapper --gradle-version 8.9` if you want the wrapper.
3. Sync Gradle. Run `gradle :core-domain:test :app:lintDebug :app:assembleDebug` (or `./gradlew` after generating the wrapper).
4. Install `app/build/outputs/apk/debug/app-debug.apk` after successful compilation: `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or Run in Android Studio. Grant microphone permission only when recording. Install an offline mainland Mandarin TTS voice on the device if needed.
5. On first run select HSK 4, add words to review and open “Жизненные ситуации” from the learning menu. Select a situation, dialogue or story, then listen to a line or read it aloud. The separate tone recording compares a schematic tone sequence with measured F0; use it in a quiet room. A full pronunciation score is not implemented. Personal PDF/TXT imports and pasted or loaded personal dialogues remain in app-private storage.

## GitHub

Canonical repository: https://github.com/Aidoni0mi/HanziFlow. Public visibility explicitly confirmed by owner. Do not commit personal books, recordings or secrets. Published 0.3.0 commit `697aab1` passed Android CI in run 45698885382. The 0.4.0 implementation passed push run 46061710393 and pull-request run 46061715516; both produced a HanziFlow-debug artifact. Physical-device QA remains required before publication. No public redistribution license has been selected.

## Verified in this environment

On 2026-11-28 the current 0.4.0 source passed 27 JUnit tests, both data validators, debug APK assembly and Android Lint locally and in GitHub Actions. Physical-device QA is still required. See `PROJECT_STATUS.md` and `docs/QA_0_3_REGRESSION.md`.

## Documentation

- `SOURCES.md`, `CONTENT_COVERAGE.md`: provenance and actual content gaps.
- `ARCHITECTURE.md`, `ROADMAP.md`, `PROJECT_STATUS.md`: implementation status and gates.
- `PRIVACY.md`, `SECURITY.md`, `THIRD_PARTY_NOTICES.md`: privacy/security/licensing.
- `CONTRIBUTING.md`, `CHANGELOG.md`: development process.

No guarantee of exam results or pronunciation accuracy. For personal learning only pending content and device verification.
