# HanziFlow — Chinese Mastery

**0.3.0-alpha / engineering checkpoint, 2026-11-25.** Full HSK 2025 syllabus lists: 11,000 words, 3,088 recognition characters, 1,200 writing characters, 593 grammar headings. Levels filter new and cumulative material. English CC-CEDICT glosses for 10,734 entries; 300 original HSK1 Russian glosses plus existing matched starter entries. Russian glosses and detailed grammar lessons are incomplete. No copyrighted books included. Android 0.2.0 verified; 0.3.0 pending. Includes daily lessons, dialogues, review modes and local speech text comparison on compatible Android 12+ devices.

## Run on a development computer

1. Install Android Studio with JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. Android 8.0+ device (API 26).
2. Open the directory containing `settings.gradle.kts`. Set Gradle JDK to 17; use Gradle 8.9. A wrapper binary is not supplied because it could not be downloaded in the source environment. Use an installed Gradle 8.9 to run `gradle wrapper --gradle-version 8.9` if you want the wrapper.
3. Sync Gradle. Run `gradle :core-domain:test :app:lintDebug :app:assembleDebug` (or `./gradlew` after generating the wrapper).
4. Install `app/build/outputs/apk/debug/app-debug.apk` after successful compilation: `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or Run in Android Studio. Grant microphone permission only when recording. Install an offline mainland Mandarin TTS voice on the device if needed.
5. On first run select HSK 4, add words to review, import your personal PDF or UTF-8 TXT from the file picker; tag version yourself. Tone practice: choose one syllable, play TTS, record for 3 seconds and compare schematic/reference and measured F0. Try in a quiet room. A full pronunciation score is not implemented.

## GitHub

Canonical repository: https://github.com/Aidoni0mi/HanziFlow. Public visibility explicitly confirmed by owner. Do not commit personal books, recordings or secrets. Initial source import completed; 0.1.0 Android CI and APK verified (run 45625738536); 0.2.0 verification pending. Visit Actions → Android verification → successful run → Artifacts to download HanziFlow-debug. No public redistribution license has been selected.

## Verified in this environment

Java 21, Kotlin CLI 1.9 and Git installed; Android SDK, Gradle and adb absent, DNS blocked to Gradle/GitHub/Google download servers. `bash scripts/core-check.sh` passed 13 checks on JVM (synthetic signal tests, SRS, segmentation, sandhi, plan). Android code was syntax-inspected but not compiled against Android/Compose, installed or tested on device. See `PROJECT_STATUS.md`.

## Documentation

- `SOURCES.md`, `CONTENT_COVERAGE.md`: provenance and actual content gaps.
- `ARCHITECTURE.md`, `ROADMAP.md`, `PROJECT_STATUS.md`: implementation status and gates.
- `PRIVACY.md`, `SECURITY.md`, `THIRD_PARTY_NOTICES.md`: privacy/security/licensing.
- `CONTRIBUTING.md`, `CHANGELOG.md`: development process.

No guarantee of exam results or pronunciation accuracy. For personal learning only pending content and device verification.
