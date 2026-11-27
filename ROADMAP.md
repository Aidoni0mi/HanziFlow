# Roadmap and release gates

| Phase | Status | Required exit criteria |
|---|---|---|
| 0 Research & architecture | Partial | Exact CTI exam specs/level lists, rights review, corpus and source coverage checks; initial architecture recorded |
| 1 Foundation | Source written, blocked on SDK | Android build, lint, install and navigation smoke test; DB lifecycle; import tests |
| 2 Vocabulary & reader | Partial source | Licensed bilingual data and level mapping, editorial review, indexed search, real context/pinyin and unknown handling |
| 3 Tone engine | JVM prototype tested; Android integration unverified | Device audio recording/replay, chart, reference recordings, noise/voice-range tests, measured accuracy; no unjustified score |
| 4 HSK 4 learning | Not complete | Verified list + grammar, lessons, workbook links/import, original exercises, SRS, content report |
| 5 Exam center | Starter quiz only | Verified exam version/center, full timed original practice, scoring spec, HSKK recording/timers |
| 6 Levels 1–9 | Goal selectors only | Separate coverage and cumulative lists; all levels independently verified |
| 7 AI & personalization | Not implemented | Optional, consent, backend secrets, no core dependency, evaluation |
| 8 Release | Blocked | Android build, device test, unit/integration/UI/database/import/audio/regression suite, documented APK |

Immediate next gate: run CI for the 0.4.0 candidate, install that exact APK on Android, and execute `docs/QA_0_3_REGRESSION.md`, beginning with the 13 previous failures and 19 untested checks and adding guided-reading checks for TTS, local recognition and tone-contour recording. No phase is declared passed solely because code or an automated build exists. After device regression, continue native Russian editorial review beyond HSK 1 and expand the original life-situation course. Sunday mock will only become a full mock once a verified exam engine and enough original questions exist.
