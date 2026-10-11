# Offline HTML dashboard architecture

Feature 2 — owner: Boey — status: DONE

The original dashboard was accepted on 10 October 2026 after Boey supplied a clean 59-test build, the expected sample grading run and confirmation that the displayed dashboard looked good. Boey approved the separate question heatmap revision on 12 October 2026; it is REVIEW pending his clean-build and local display/interaction acceptance. Attention overview and score-distribution charts remain outside the approved scope.

## Purpose and flow

The dashboard lets an instructor inspect completed grading results, preparation warnings, corrections and recorded case output from one local HTML file.

```text
GradingPipeline.grade(submissions)
  -> GradingRun(batch, run directory, withheld reasons)
  -> completed scorereport.csv, cases.csv and submissions.csv
  -> App.generateHtmlReport(...)
  -> GradingReportContext(batch, withheld reasons)
  -> HtmlReportGenerator.generate(context, run directory)
  -> dashboard.html
```

The report consumes completed results. It does not compile submissions, execute cases or decide export eligibility. CSV generation succeeds before App invokes the renderer.

## Components and responsibilities

| Component | Responsibility |
| --- | --- |
| `HtmlReportGenerator` | Defines the HTML output contract and returns the generated path |
| `GradingReportContext` | Carries the batch and an immutable copy of authoritative withholding reasons; rejects unrelated submissions and blank reasons |
| `HtmlReportConfig` | Loads enablement and validates a safe `.html` filename |
| `OfflineHtmlReportGenerator` | Renders assessment information, summaries, submission details and analysis evidence; escapes text and writes the report |
| `QuestionOutcome` | Immutable report-local view data for question scores, recorded status counts, evidence warnings and credit bands |
| `dashboard.css` | Styles the report, readable statuses, focus indicators and scrollable tables |
| `dashboard.js` | Searches, filters and sorts existing rows; opens and focuses submission details |
| `App` | Coordinates optional generation, prints the path and reports generation failure |

Implementation lives in `src/main/java/autograder/report/html`. Browser resources live in `src/main/resources/autograder/html` and are packaged in the JAR. The generator embeds their contents into one UTF-8 report using Java standard-library APIs. No new application runtime library was added.

## Result and eligibility semantics

- A submission in the context's withheld map is WITHHELD; other submissions in a successfully exported run are EXPORTED. Preparation status is displayed separately, so REVIEW_REQUIRED warnings can coexist with an exported grade.
- Withheld computed totals remain visible as diagnostic scores and are excluded from exported-only averages and medians. Empty eligible sets display N/A. The mean is rounded to four decimal places with a visible explanation; individual scores and maxima use the existing model accessors.
- Each received archive has its own row and ordinal, including duplicate identities. Matched roster names/usernames identify students; details retain reported headers, roster fields, paths and warnings.
- Assessment/question maxima and order come from the assessment model. Case points come from `TestCaseResult.getScore()`; the renderer does not duplicate scoring rules.
- All six case statuses remain visible: PASSED, FAILED, TIMED_OUT, COMPILE_ERROR, RUNTIME_ERROR and NOT_RUN. Output is the existing captured stdout/stderr; expected/actual assertion values are not invented.
- Question cells show existing earned/max points and nonzero recorded status counts. Six blue bands distinguish zero, above-zero/below-25%, 25–below-50%, 50–below-75%, 75–below-100% and full credit. Decimal boundary comparisons determine the band without rounding scores. Color indicates credit only, and withheld question scores explicitly say Diagnostic.
- Missing or mismatched case records, all-NOT_RUN questions and unusable score ranges have neutral styling and truthful explanations. Configured question groups have safe numeric anchors; unmatched question IDs remain in an additional recorded-cases group. The report does not interpret matching record counts as proof that all cases executed.
- Durations are recorded per result. COMPILE_ERROR durations can repeat because they refer to one compilation attempt; the renderer does not sum them into a grading-run duration.
- The batch contains received submissions, so absent roster students are not inferred. Empty analysis lists say no analysis data was supplied, without claiming plagiarism or starter comparison ran.

## Configuration and failure behavior

```properties
grading.html.enabled=true
grading.html.file=dashboard.html
```

The supplied configuration enables the report. Older files that omit HTML settings default to disabled generation. The filename must be a simple `.html` basename using letters, digits, dots, underscores or hyphens. Output stays inside the existing unique grading run directory, whose protection is validated by `ExecutionConfig`.

The generator uses CREATE_NEW and does not overwrite an existing destination. If enabled generation throws IOException, App prints the failed dashboard location and confirms that completed CSVs remain in the run directory, then returns nonzero. Disabling generation preserves the existing successful CLI result.

## Safe rendering and interaction

Names, archives, paths, warnings, corrections, case identifiers and output are escaped for HTML text and quoted attributes, including ampersands, angle brackets and both quote characters. Student strings are not embedded into executable JavaScript or JSON script blocks. Browser code reads existing DOM data and updates the result count through `textContent`.

CSS and JavaScript are inline; there are no external assets, server calls or fetch requests. Search covers names, usernames and archive paths. Separate filters cover export decisions and preparation warnings. Sorting supports names and computed totals in both directions. Native details remain readable without JavaScript; filtering and sorting require it. Labels, text statuses and focus styles support keyboard use. Wide result/diagnostic tables scroll within their containers.

Question links open the existing submission details, scroll to the question group and focus its heading. Submission ordinals and question indexes form the identifiers; names and other untrusted strings do not. Navigation preserves active filters, sorting and row/detail pairing. Modified link clicks retain normal browser behavior. The existing Open details link continues to focus the submission summary.

HTML escaping protects report presentation. It does not change the core grader's execution-isolation limits; student JVMs still run with the current account's privileges.

## Actual OOP principles used

- **Encapsulation and single responsibility:** configuration validation, reporting context and rendering have separate responsibilities. Private renderer helpers handle summaries, rows, corrections, cases, analysis and escaping.
- **Immutable view data:** `QuestionOutcome` groups recorded evidence and classifies presentation without changing shared result models or their scoring policy. The renderer handles HTML formatting separately.
- **Abstraction:** App invokes the `HtmlReportGenerator` contract rather than requiring callers to know template or resource mechanics.
- **Polymorphism:** `generateHtmlReport` accepts the interface; the production generator and test generators are dispatched through the same contract. Tests substitute a failing generator to verify CLI failure handling.
- **Composition and separation of model/view:** the renderer composes existing assessment, grade, submission and case models into a view. The pipeline owns grading and eligibility; the renderer owns presentation.
- **Protected shared state:** the context copies its withholding map and existing batch/result models expose protected collections. Rendering cannot mutate those shared results through their accessors.

No artificial class inheritance was introduced for this reporting feature.

## Verification and acceptance

The following 59-test record is the accepted original dashboard. The heatmap revision's separate evidence follows it.

- Focused tests: 15 passed, zero failures/errors/skips.
- Clean Maven verification: 59 passed, zero failures/errors/skips, on JDK 25 with Java 17 compiler target. Actual Java 17 runtime and Windows execution were not tested.
- Packaged sample run: Chee 12, David 10, Fen 10, Jing 0, Ping 20 and Xing 1; 120 case records and six submission records.
- Rendering tests cover inline resources, hostile text, all statuses, decimals, withholding and partial scores, exported review warnings, roster/header differences, duplicate/missing identities, empty/all-withheld batches, analysis evidence and write failure. App tests cover disabled generation and preserving CSV evidence after a renderer error.
- Integration fixtures compare all three CSV files byte-for-byte before and after HTML generation. All 64 snapshotted supplied inputs remained unchanged.
- Boey supplied the successful clean build/run and viewed and accepted `build/grading/run-1332949390012507172/dashboard.html`. Automated browser checks were unavailable because isolated Chrome failed and the browser tool prohibited file:// navigation. No automated network-disabled or viewport assertions are claimed.

For the heatmap revision on 12 October 2026, focused tests passed 14/14 and the final clean build passed 67 tests with zero failures/errors/skips. Tests cover exact decimal credit boundaries, mixed statuses, missing/excess/absent records, all-NOT_RUN, unusable maximums/out-of-range scores, safe question anchors and retained hostile/unmatched case evidence, alongside existing withholding/empty/duplicate/identity tests. Pipeline assertions verify generated question links and unchanged CSV bytes. A built-in-Node DOM API fixture passed 2/2 scenarios against the actual script; it checks navigation/focus/control preservation but does not establish actual browser rendering or keyboard behavior. Scoped text contrast is at least 5.45:1 and cell focus contrast at least 3.84:1 across the six bands.

The revised packaged run produced `build/grading/run-3551798997316566219/dashboard.html`, the same six scores, six submissions and 120 cases. HTML checks verified all 30 links resolve to unique question headings and each recorded case is retained once; Ping has full-credit cells and Xing's Q2a shows 1/5 with one pass/four timeouts. All 67 Project files matched the new input snapshot. No real-browser, Java 17 runtime or Windows check was performed; Boey's new clean-build/display acceptance is pending. No commit/push/merge or branch synchronization occurred.

Build and run from the repository root:

```sh
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

Boey's available temporary Maven fallback is `/tmp/apache-maven-3.9.9/bin/mvn -Dmaven.repo.local=/tmp/cs102-maven-repository clean verify`.

## Slide Deck Talking Points

- Explain the reporting flow: grading and CSV export finish first; an immutable context supplies the same results and withholding decisions to the HTML view.
- Demonstrate opening the saved report, locating Ping's full marks, and expanding Xing's Q2a cases to show one passing case alongside timeouts.
- Scan the question heatmap, then activate Xing's Q2a 1/5 cell to reach its one pass and four timeouts. Explain that color represents credit, while the recorded status counts explain the outcome. Show that search/filter/sort settings survive the navigation.
- Filter preparation warnings and explain why David/Xing can be exported while still requiring review. Use synthetic duplicate/partial-score fixtures to explain withholding.
- Explain how escaping and DOM text handling keep student-controlled content from becoming executable HTML/JavaScript.
- Show interface abstraction, composition and private rendering responsibilities; explain the choice of standard-library Java plus inline browser assets.
- Present the actual 59-test/build/run evidence and its browser/platform verification limits accurately. Rehearse the local-file, network-disabled and narrow-window demonstrations before presentation.

Human team review, method-level attribution and the presentation AI Use appendix remain release responsibilities under the project brief. This document does not complete those declarations for the team.
