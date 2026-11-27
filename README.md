# HanziFlow — Chinese Mastery

**0.3.1-alpha / engineering checkpoint, 2026-11-27.** Full HSK 2025 syllabus lists: 11,000 words, 3,088 recognition characters, 1,200 writing characters, 593 grammar headings. Levels filter new and cumulative material. English CC-CEDICT glosses for 10,734 entries; 300 original HSK1 Russian glosses plus existing matched starter entries. Russian glosses and detailed grammar lessons are incomplete. No copyrighted books included. The published 0.3.0 baseline passed CI; current 0.3.1 source passes local unit tests, APK assembly and Android Lint but still needs device QA. Includes daily lessons, dialogues, local private-dialogue TXT import, review modes and local speech text comparison on compatible Android 12+ devices.

## Run on a development computer

1. Install Android Studio with JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. Android 8.0+ device (API 26).
2. Open the directory containing `settings.gradle.kts`. Set Gradle JDK to 17; use Gradle 8.9. A wrapper binary is not supplied because it could not be downloaded in the source environment. Use an installed Gradle 8.9 to run `gradle wrapper --gradle-version 8.9` if you want the wrapper.
3. Sync Gradle. Run `gradle :core-domain:test :app:lintDebug :app:assembleDebug` (or `./gradlew` after generating the wrapper).
4. Install `app/build/outputs/apk/debug/app-debug.apk` after successful compilation: `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or Run in Android Studio. Grant microphone permission only when recording. Install an offline mainland Mandarin TTS voice on the device if needed.
5. On first run select HSK 4, add words to review, import your personal PDF or UTF-8 TXT from the file picker; tag version yourself. A personal dialogue can also be pasted or loaded from a small UTF-8 TXT and remains in app-private storage. Tone practice: choose one syllable, play TTS, record for 3 seconds and compare schematic/reference and measured F0. Try in a quiet room. A full pronunciation score is not implemented.

## GitHub

Canonical repository: https://github.com/Aidoni0mi/HanziFlow. Public visibility explicitly confirmed by owner. Do not commit personal books, recordings or secrets. Published 0.3.0 commit `697aab1` passed Android CI and produced an APK in run 45698885382. Current 0.3.1 changes require a new CI run before publication. Visit Actions → Android verification → successful run → Artifacts to download HanziFlow-debug. No public redistribution license has been selected.

## Verified in this environment

On 2026-11-27 the current 0.3.1 source passed 14 JUnit tests, HSK data checks, debug APK assembly and Android Lint with Gradle 8.9 and Android API 35. It has not been installed or retested on a physical Android device. See `PROJECT_STATUS.md` and `docs/QA_0_3_REGRESSION.md`.

## Documentation

- `SOURCES.md`, `CONTENT_COVERAGE.md`: provenance and actual content gaps.
- `ARCHITECTURE.md`, `ROADMAP.md`, `PROJECT_STATUS.md`: implementation status and gates.
- `PRIVACY.md`, `SECURITY.md`, `THIRD_PARTY_NOTICES.md`: privacy/security/licensing.
- `CONTRIBUTING.md`, `CHANGELOG.md`: development process.

No guarantee of exam results or pronunciation accuracy. For personal learning only pending content and device verification.
