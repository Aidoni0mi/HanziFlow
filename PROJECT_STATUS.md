# Project status — 0.2.0-alpha, 2026-11-25

- 0.1.0-alpha CI passed (run 45625738536); installed and tested by user, 22/55 checks confirmed. 0.1.1 fixes committed, device/CI status not confirmed here.
- 0.2.0 imports full HSK 2025 vocabulary (11000), recognition characters (3088), writing subset (1200) and grammar index (593), verified counts and cross-source comparisons; 3088 pinyin lists.
- New/cumulative scope selection, dictionary search, hanzi reading/writing filters, grammar index and source labels; original practice preserved. Data parsed and indexed off main thread; LazyColumn renders visible rows. In-memory compact syllabus index, not a complete paginated SQLite dictionary.
- Existing review IDs preserved; new records have stable hsk25-N IDs. Existing database schema unchanged.
- Russian translations and detailed grammar lessons incomplete; no licensed full textbook or workbook bundle, no copied official example sentences.
- Data checks added to CI. 0.2.0 build and real-device QA pending. See CONTENT_COVERAGE.md.
