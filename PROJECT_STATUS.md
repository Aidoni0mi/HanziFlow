# Project status — 0.4.0-alpha

- User device QA for 0.2.0 reported 63 pass, 13 fail and 19 untested. The retest plan is `docs/QA_0_3_REGRESSION.md`; source or CI checks do not mark device items passed.
- Published 0.3.0 commit `697aab1` passed data validation, Kotlin tests, debug APK assembly and Android Lint in GitHub Actions run 45698885382.
- Preserved all 11,000 words, 3,088 characters, 1,200 writing entries and 593 grammar index entries. Automated checks now assert every cumulative count, including HSK2 totals of 500 words, 371 recognition characters and 148 grammar headings.
- 300 original HSK1 Russian glosses plus previous matching glosses on other levels. Not native-editor reviewed. Remaining translations not complete.
- Russian regression fixture confirms `учиться` maps to `学习`; a physical-device search retest is still required.
- Four review presentation modes share a hardened SRS. Intervals grow monotonically, malformed legacy rows no longer break the queue, writes are serialized and batch additions are deduplicated.
- Added daily three-step lesson, four original dialogues, paste/TXT local private-dialogue input, listening drill, and device-only Android speech-to-text comparison for HSK characters/words and custom sentences. Private dialogues can be deleted and are never bundled. Speech text comparison is not phoneme or tone scoring; Android 12+ and a suitable local Mandarin recognizer are required.
- Isolated-tone F0 feedback uses the longest continuous voiced segment. Guided reading shows all stable voiced sections on one relative timeline and preserves pauses; neither mode performs syllable alignment or calibrated pronunciation scoring.
- Added a scalable original life-situation course: 16 high-frequency situations, 32 dialogue/story texts and 96 lines. Every line carries Hanzi, tone-marked pinyin and Russian translation, with normal/slow TTS, a separate on-device speech-text comparison and qualitative F0 contour practice.
- The bundled life-course TSV has its own parser, unit tests and CI validator. It contains no copied textbook passages, personal imports or recordings.
- No textbook dialogue extraction, licensed native audio, OCR, stroke grading, full grammar lessons, adaptive AI tutor or official mocks yet.
- The 0.4.0 implementation passed 27 JUnit tests, both data validators, debug APK assembly and Android Lint locally and in GitHub Actions runs 46061710393 / 46061715516 on 2026-11-28. Physical-device QA remains pending, including Mandarin TTS, local recognition, 5–15-second microphone recording and F0-analysis performance on a slower device.
- No authorized Gemini connection or local Gemini client was available. `docs/GEMINI_HANDOFF.md` contains the public-safe independent-review brief; it excludes private books, recordings and secrets.
