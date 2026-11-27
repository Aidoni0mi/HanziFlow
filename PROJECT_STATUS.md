# Project status — 0.3.1-alpha

- User device QA for 0.2.0 reported 63 pass, 13 fail and 19 untested. The retest plan is `docs/QA_0_3_REGRESSION.md`; source or CI checks do not mark device items passed.
- Published 0.3.0 commit `697aab1` passed data validation, Kotlin tests, debug APK assembly and Android Lint in GitHub Actions run 45698885382.
- Preserved all 11,000 words, 3,088 characters, 1,200 writing entries and 593 grammar index entries. Automated checks now assert every cumulative count, including HSK2 totals of 500 words, 371 recognition characters and 148 grammar headings.
- 300 original HSK1 Russian glosses plus previous matching glosses on other levels. Not native-editor reviewed. Remaining translations not complete.
- Russian regression fixture confirms `учиться` maps to `学习`; a physical-device search retest is still required.
- Four review presentation modes share a hardened SRS. Intervals grow monotonically, malformed legacy rows no longer break the queue, writes are serialized and batch additions are deduplicated.
- Added daily three-step lesson, four original dialogues, paste/TXT local private-dialogue input, listening drill, and device-only Android speech-to-text comparison for HSK characters/words and custom sentences. Private dialogues can be deleted and are never bundled. Speech text comparison is not phoneme or tone scoring; Android 12+ and a suitable local Mandarin recognizer are required.
- F0 feedback uses the longest continuous voiced segment and remains a qualitative acoustic hint, not a calibrated pronunciation score.
- No textbook dialogue extraction, licensed native audio, OCR, stroke grading, full grammar lessons, adaptive AI tutor or official mocks yet.
- Current source passed 14 JUnit tests, data validation, debug APK assembly and Android Lint locally on 2026-11-27. A new CI run and physical-device QA are still pending.
- No authorized Gemini connection or local Gemini client was available. `docs/GEMINI_HANDOFF.md` contains the public-safe independent-review brief; it excludes private books, recordings and secrets.
