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

Immediate next gate: create empty private GitHub repository, push bundle history, run CI, resolve actual compiler/lint errors, test APK on Android. No phase declared passed solely because code exists. Then source/license and CTI syllabus ingestion, HSK 4 editorial review. Sunday mock will only become a full mock once verified exam engine and enough questions exist.
