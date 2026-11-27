# Gemini review handoff — HanziFlow

This document is a public-safe handoff for an independent Gemini code review. It does not contain private textbooks, recordings, credentials, account details, or unpublished user content.

## Repository and verified baseline

- Repository: `Aidoni0mi/HanziFlow`
- Baseline commit: `697aab1e2ea22dc0feab517ff49c9c9adaaac753`
- Baseline version: `0.3.0-alpha`
- GitHub Actions run: `45698885382`
- Result: success on 2026-11-25
- Passed gates: HSK data validation, Kotlin unit tests, debug APK build, Android Lint
- APK artifact: `HanziFlow-debug`, SHA-256 `069fdc64022b983014e7b6f99cea83c44b6ec66da4b21ccc7fcb1a80573b7a5e`

The successful CI run proves that the baseline compiles and passes the automated checks. It does not replace Android device QA.

## Product constraints

- Offline-first Android application; no internet permission.
- Public repository must contain only redistributable or original content.
- Never add private textbook pages, workbook answers, pasted user dialogues, recordings, API keys, tokens, signing keys, or local paths containing personal data.
- Private textbook material must remain device-local through the existing PDF/TXT library or personal-dialogue input.
- Do not claim phoneme, tone, or pronunciation accuracy from Android speech-to-text. Text recognition and F0 direction hints are separate, limited signals.
- Do not replace HSK 2025 syllabus counts with legacy HSK or GF0025-2021 counts.

## User QA baseline

Android 0.2.0 device QA reported 63/95 passing, 13 failing, and 19 untested. The 13 failures were:

1. HSK 2 vocabulary count display.
2. Russian dictionary search for `учиться`.
3. HSK 2 recognition-character count display.
4–10. All seven cumulative grammar-count displays.
11. Opening review cards.
12. Applying all four review grades.
13. Persisting review progress.

Several fixes landed before the baseline commit: category-only grammar rows are retained, new/cumulative counts are explicit, review navigation and serialized writes were reworked, and all 300 HSK 1 entries now have Russian glosses. These items still require device regression testing; do not mark them passed from source inspection alone.

## Requested independent review

Review the current branch without changing product scope. Prioritize correctness defects and missing regression coverage in this order:

1. **Curriculum and search**
   - Verify cumulative/new word counts `[300, 500, 1000, 2000, 3600, 5400, 11000]`.
   - Verify cumulative recognition counts `[246, 371, 655, 1096, 1527, 1940, 3088]`.
   - Verify cumulative grammar counts `[70, 148, 244, 339, 409, 459, 593]`.
   - Confirm Russian search finds `学习` for `учиться` at HSK 1 and cumulative higher levels.
   - Check that pinyin search handles tone marks, unmarked pinyin, `ü`, `u:`, and `v` consistently.

2. **Local private content**
   - Review PDF/TXT import size limits, cleanup on failure, UTF-8 validation, page bounds, deletion, and process-death persistence.
   - Review personal-dialogue input limits and local storage behavior.
   - Propose tests using generated fixtures only. Do not request or embed real textbook content.

3. **SRS and persistence**
   - Review `Again`, `Hard`, `Good`, and `Easy` transitions, due dates, lapse counts, interval bounds, duplicate additions, and concurrent UI actions.
   - Check recovery from malformed legacy rows without silently losing all review data.
   - Confirm grading one card cannot mutate another card.

4. **Speech assessment honesty and robustness**
   - Review Chinese text normalization and edit-distance comparison.
   - Check multiple recognition hypotheses, partial results, cancellation, recognizer errors, and lifecycle cleanup.
   - Review F0 extraction edge cases, but preserve the current truthful limitation: it is a visual/qualitative hint, not a calibrated pronunciation score.

5. **Tests and release gate**
   - Prefer deterministic unit tests and generated fixtures.
   - Identify which claims require a physical Android device and a locally installed Mandarin recognizer/TTS voice.
   - Treat the 19 previously untested device checks as pending until observed on a device.

## Expected response format

Return:

1. Findings ordered by severity, with file and line references.
2. A minimal patch or commit containing only justified changes.
3. Tests executed and exact results.
4. Remaining device-only checks and product limitations.
5. A statement confirming that no private textbook content or secrets were accessed or added.
