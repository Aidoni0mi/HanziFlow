# Android regression plan — 0.4.x alpha

## Baselines

- User device QA for 0.2.0: **63 passed / 13 failed / 19 untested** out of 95 checks.
- Automated baseline for commit `697aab1`: GitHub Actions run `45698885382` passed data validation, Kotlin unit tests, debug APK assembly, and Android Lint.
- Automated 0.4.0 implementation: push run `46061710393` and pull-request run `46061715516` passed both validators, 27 Kotlin tests, debug APK assembly, and Android Lint; use the pull-request run's `HanziFlow-debug` artifact for device QA.
- Automated success does not change a device item to passed. The items below remain pending until observed on Android.

## Retest the 13 previous failures first

| Area | 0.2.0 report | 0.3.x evidence | Required device result |
|---|---|---|---|
| Vocabulary | HSK 2 count failed | Data check proves 200 new / 500 cumulative; UI has explicit new/cumulative selection | Both modes show the correct count after level changes |
| Dictionary | Russian search `учиться` failed | `学习` is HSK 1 serial 249 with `учиться; изучать`; search includes the Russian field | Query returns `学习` at HSK 1 and cumulative higher levels |
| Hanzi | HSK 2 count failed | Data check proves 125 new / 371 cumulative | Reading mode shows 125 new and 371 cumulative |
| Grammar | Seven cumulative counts failed | All 593 category/headings load; category-only rows are no longer dropped | Counts are 70, 148, 244, 339, 409, 459, 593 |
| Review | Cards did not open | Review pool/navigation and saved-card view were reworked | A saved card opens in due and all-saved modes |
| Review | Four grades failed | Four explicit grade actions use serialized persistence | Each grade changes only the selected card and shows its next due date |
| Review | Progress did not persist | Review rows are stored in SQLite and reloaded | State survives app restart and device process recreation |

## Retest the 19 previously untested checks

### Grammar — 4

- Search by Chinese construction.
- Search by Russian category.
- Open a grammar card, including a category-only syllabus row.
- Distinguish the small set of original explanations from the complete 593-heading index.

### Local library — 7

- Import a generated PDF fixture and render all pages.
- Move between pages and restore the last page after restart.
- Change and restore the user-entered HSK/book version label.
- Import a generated UTF-8 TXT fixture.
- Delete each fixture and confirm its private copy is removed.
- Reject unsupported, malformed, oversized, or binary-as-TXT files without leaving partial files.
- Confirm no imported fixture is added to Git or exported by the application.

### Profile and offline behavior — 8

- Save a valid exam date and restore it after restart.
- Change daily study minutes and restore them.
- Confirm review, quiz, and library statistics update after each action.
- Use the dictionary in airplane mode.
- Use the grammar index in airplane mode.
- Record and analyze an isolated tone in airplane mode.
- Restore all supported progress after process restart.
- Run a longer navigation/import/review/audio session without a crash or unreleased microphone/recognizer.

## Speech-specific acceptance rules

- For character, word, and sentence practice, label the result as recognized-text comparison.
- A perfect text match must explicitly say it is not a tone or phoneme score.
- Recognition errors and homophones must not be presented as definite pronunciation errors.
- F0 feedback may describe a measured contour direction only when enough stable voiced frames exist.
- Do not show a percentage or pass/fail pronunciation grade until a validated Mandarin learner-speech model and evaluation set exist.

## Life-situation course — new in 0.4.0

- The course list shows 16 original situations; “all” and “up to my level” filters update without losing or duplicating entries.
- Every situation opens one four-line dialogue and one two-line story. Across the course, all 96 lines display Hanzi, tone-marked pinyin, and Russian translation.
- Normal and 0.7× Mandarin TTS work for an individual line; full-text playback follows the visible line order.
- Opening “read aloud” selects the intended line and resets recognition and recording results when the line changes.
- On Android 12+ with an installed on-device Mandarin recognizer, recognized text and cautious comparison feedback appear after an explicit microphone action.
- A separate recording uses a five-to-fifteen-second window based on line length, can be played back, and displays the full measured F0 phrase contour without presenting syllable alignment or a pronunciation score.
- Denied permission, missing offline TTS/recognition packages, silence, noisy input, cancellation, and leaving the screen all recover without a crash or a retained microphone session.
- The course remains usable in airplane mode after required Mandarin system voice and recognition packages have been installed.

## Release gate

A 0.4.x APK can be called device-verified only after:

1. The current commit passes the same CI gates as run `45698885382`.
2. The 13 previous failures are retested on Android.
3. The 19 untested checks have recorded observations.
4. The life-situation and guided-reading checks above have recorded observations.
5. No private textbook, workbook, recording, signing key, or credential appears in the Git diff or artifacts.
