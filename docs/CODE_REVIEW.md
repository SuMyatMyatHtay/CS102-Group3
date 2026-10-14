# Merged-main code review and modularisation proposals

Reviewed on 2026-10-14 for Boey's approved review/documentation phase. Source baseline: `a99190f49ef6ec307f169f0fe4eeb2c5c37dd841` (accepted PR #2 merged into main). Working branch: `code-modularisation`. These are review findings and a proposed implementation sequence; Java refactoring and bug fixes have not been approved or performed in this phase.

## Scope, ownership and evidence

The review covers every tracked application Java file, Java resource adapter, dashboard asset, test file, `pom.xml`, `config.properties` and `.gitignore`: 46 application Java files (4,919 lines), 15 test files (2,327 lines), 5 adapter resources (243 lines), 2 browser assets (246 lines), and 3 build/configuration files. The inventory below records all 71 reviewed files. Supplied assessment/student inputs, unrelated files, generated output and teammates' unmerged branches are outside the code-edit scope.

Main reviewed application/pipeline integration, APIs, shared models, their tests and build/settings; it owns all documentation and final design decisions. One bounded Luna reconnaissance agent reviewed reporting/configuration/CSV/roster/assets/tests; one bounded Luna/xhigh agent reviewed execution/submission/provider/adapters/tests. Neither agent edited files or spawned agents. Main inspected their evidence and the cross-component boundaries.

This is a source review, not a new passing build or browser result. No Maven tests, grading run or browser checks were run during this phase. The one new runtime probe used a disposable temporary directory to establish local filesystem case aliasing; it did not execute the application. Documentation checks and preservation hashes are reported separately in the PR.

The accepted MVP/original-dashboard evidence and completed heatmap agent evidence remain historical: heatmap focused tests 14/14, clean build 67 tests, mock-DOM fixture 2/2, six expected totals and 120 case records. The DOM fixture is not a real browser/keyboard pass. Actual Java 17 runtime and Windows verification were not performed; a separate new human heatmap clean-build/display transcript remains uncaptured despite PR acceptance and merge. This review does not replace or expand those observations.

## Main findings

| ID | Priority | Finding | Proposed treatment |
| --- | --- | --- | --- |
| M1 | First batch | `OfflineHtmlReportGenerator` combines document/file orchestration, summaries, rows, diagnostics, analysis and presentation helpers in 681 lines | Extract cohesive report components; retain the public generator and current markup/behaviour |
| M2 | Second batch | `AppConfig` combines immutable settings, properties parsing, assessment loading and path policy in 611 lines | Delegate loading and validation while retaining `load(Path)`, getters and the existing canonicalisation bridge |
| M3 | Third batch | `IdentityResolver` collects evidence, matches a roster, plans repairs and writes corrected files in 578 lines | Separate evidence, matching, repair planning and mutation; retain the resolution facade/result |
| M4 | Later bounded batch | `GradingPipeline` is only 193 lines, but `grade` combines nested per-submission/per-question execution and `writeReports` owns three CSV projections | Extract diagnostic persistence and clarify execution helpers while keeping eligibility and ordering authoritative |
| S1 | Scoped cleanup | Some existing Java files use wildcard imports or tightly grouped getters; new style calls for explicit imports and consistent spacing | Correct only deliberately edited areas; no broad formatting-only rewrite |
| B1 | Separate correctness approval | Case variants of reserved CSV filenames can overwrite diagnostics on a case-insensitive filesystem | Add explicit validation only after a separate bug-fix approval; do not mix it into pure extraction |

No other material correctness defect was established in the reviewed evidence. This is not a guarantee that no defects exist. The known controlled-course execution limits remain documented; changing them is outside this modularisation proposal.

## Proposed extraction boundaries

Class names below are concrete proposed names for the later implementation plan. All implementation collaborators should be package-private unless a genuine caller outside the package needs them. Keep shared APIs/models and runtime dependencies unchanged initially. Use composition; retain the current Java 17/Maven layout. Each batch requires its concrete plan and approval before Java edits.

### M1: report rendering

Keep `OfflineHtmlReportGenerator` as the public boundary: its constructor validates the filename, and `generate` validates the supplied context/output directory, creates the directory and writes the report. Keep document/resource composition in the generator. Proposed collaborators in `autograder.report.html`:

| Proposed component | Existing logic to move | Preservation constraints |
| --- | --- | --- |
| `HtmlSummaryRenderer` | `appendSummary`, `metric`, `median` and exported-score summary calculations (generator lines 100-143, 580-589) | Same authoritative withheld map; exported-only mean/median; existing rounding/captions |
| `HtmlSubmissionTableRenderer` | Controls/table assembly, `appendStudentRow` and row identity/diagnostic display (145-263) | Same search attributes, row IDs, control IDs, sort values, received/archive rows and withheld labels |
| `HtmlSubmissionDetailsRenderer` | `appendDetailRow`, corrections and detail metadata (264-340, 541-545) | Same roster/raw identity, warnings, corrections, paths and native details |
| `HtmlCaseRenderer` | Case groups/tables/output, score cells/status counts, question grouping and unmatched cases (341-496) | Same configured order, ordinal/index anchors, neutral states, mixed status counts and all recorded evidence |
| `HtmlAnalysisRenderer` | `appendAnalysisSection` (497-540) | Display supplied findings only; retain no-data explanation |
| `HtmlEscaper` | `escape` (650-667) | One escaping implementation for every untrusted text/attribute value |
| `HtmlScoreFormatter` | Decimal formatting, unavailable-value handling and recorded-score/max conversion/access helpers (567-579, 590-610) | Same decimal values and display formatting; no new scoring policy |
| `QuestionSection` | Existing private record (678-680) | Extract package-locally with a defensive-copy constructor for recorded cases, preserving the current immutable-list construction; anchor ordinals allocated by one owner |

Display-name/path helpers belong with the submission renderers; share only genuinely reused display logic in a focused submission-presentation helper if review of the moved code requires it. `QuestionOutcome` remains unchanged as the existing immutable evidence/credit-band helper. Retain resource loading inside the generator unless a concrete reuse need appears. Keep CSS/JS inline and preserve their selectors; no visual redesign or script behaviour change is proposed.

Existing renderer, outcome, context and pipeline tests remain black-box regression coverage. Add focused tests only for meaningful extraction seams or missing invariants; avoid tests that merely assert collaborator calls. A targeted interaction/manual pass may be needed if markup changes cannot be avoided, within the established browser restrictions.

### M2: configuration loading

Keep `AppConfig` as the immutable aggregate and compatibility facade. `load(Path)` delegates without changing property names, defaults, getter signatures or error semantics. Proposed collaborators in `autograder.config`:

| Proposed component | Existing logic to move | Preservation constraints |
| --- | --- | --- |
| `AppConfigLoader` | File/properties loading, top-level validation sequence and aggregate construction (112-191), score-column mapping (193-200) | Same configuration-file base directory, required keys and immutable aggregate |
| `AssessmentConfigLoader` | `loadAssessment`, assessment canonicalisation (201-266) | Same configured question order, source/companion names, counts, weights and protected resources |
| `ConfigPathPolicy` | `canonicalize`, `validateOutputPaths`, `requireDisjoint`, `pathList`, `resolvePath` (268-365, 396-403) | Same real-path/symlink handling, protected-input disjointness and path resolution |
| `PropertyParsers` | Required values, name lists, prefixes, regex capture requirements and numeric parsers (366-395, 404-479) | Preserve validation details and exception behaviour; share with other config loaders only after reviewing compatibility |

`ExecutionConfig` calls package-private `AppConfig.canonicalize`; retain that delegating bridge initially. Keep the small `ExecutionConfig` and `HtmlReportConfig` records together. Avoid introducing grouped settings objects that force changes across all consumers in this first pass.

Add focused `AppConfigTest` coverage for valid/invalid loading and path/regex/numeric policies extracted from ingestion integration fixtures. Preserve existing execution/HTML config tests and ingestion tests rather than relocating them merely to match implementation classes.

### M3: identity and corrections

Keep `IdentityResolver.resolve(...)`, `applyCorrections(...)` and public nested `Resolution` initially. The two public methods delegate to focused collaborators in `autograder.submission` so current callers continue to work:

| Proposed component | Existing logic to move | Preservation constraints |
| --- | --- | --- |
| `IdentityEvidenceCollector` | Archive/wrapper evidence, required-source checks and bounded header parsing (142-298) | Same strong-evidence classification, exact-case file checks, header limit and anomaly order |
| `RosterIdentityMatcher` | Unique roster matching, evidence matching and roster-prefix handling (300-358) | Same ambiguous/unresolved outcomes; no guessed ownership |
| `HeaderCorrectionPlanner` | Header reconciliation, field correction proposals, unresolved anomalies and canonical roster names (360-431, 459-480) | Repair only missing values for a unique owner; preserve nonempty conflicting values and warnings |
| `HeaderCorrectionApplier` | `applyCorrections`, field patterns and labels (81-140, 433-457) | Mutate only working copies; preserve containment, bounded matching, inserted header order and returned applied corrections |

Move small evidence/header/source data types to their owning stage; use immutable package-local transfer data where multiple stages consume it. Preserve existing list order and `Resolution` outputs. `WorkingCopyPreparer` may retain its current call through the facade initially; remove that coupling only if an approved implementation plan explicitly wires the applier directly. Unused stored `ParsedSource.sourcePath` is optional cleanup, not a bug.

Use focused collection/matching/planning/applier tests for the new boundaries, with existing ingestion and pipeline tests preserving end-to-end identity, warnings, correction logs and withholding decisions. Source input hashes remain protected.

### M4: pipeline responsibilities

Propose a package-private `GradingDiagnosticsWriter` to own the `cases.csv` and `submissions.csv` projections and score-writer invocation from `writeReports` (159-192). It consumes the completed `GradingRun` and configuration; it does not decide eligibility. Existing `CsvScoreReportWriter` remains responsible for schema/score validation and atomic score-sheet output.

Keep batch sequencing, blockers and infrastructure withholding in `GradingPipeline`. If its long `grade` method remains hard to follow after report extraction, introduce focused per-submission/per-question private helpers in a bounded follow-up. Preserve all expected case records, independent compilation, per-case timeouts, continuation after infrastructure errors and report order. Do not introduce a new provider/injection architecture or implement William's feature 4 in this work.

`App` remains the CLI coordinator. Its linear `run` method is a reasonable size-review exception at this stage: it keeps the startup/preparation/grading/report flow visible, with error handling at the CLI boundary. Extract console formatting later only if it materially improves readability. Existing `AppTest` and `GradingPipelineTest` remain the observable contract checks. Exception record: `App.java:32-79`, 48 physical lines, 47 nonblank/noncomment lines; responsibility is end-to-end CLI coordination; retaining this sequence helps readers see stage order and the exit/error boundary. Existing CLI/pipeline tests were inspected, not rerun; live runtime/platform limits remain as recorded above.

## Separate correctness and hardening findings

### B1: case-variant report names can replace diagnostic CSVs

Priority: P2. Confidence: high for the code-level validation gap and filesystem aliasing; no full grading reproduction was run.

Trigger: configure `grading.report.file=Cases.csv` or `Submissions.csv` on a case-insensitive filesystem. `ExecutionConfig.load` rejects only exact `cases.csv`/`submissions.csv` strings at lines 36-40, so these `.csv` variants pass that filename guard. `GradingPipeline.writeReports` writes the lowercase diagnostic files first at lines 187-188, then passes the configured name to `CsvScoreReportWriter` at line 191. The writer's atomic move uses `REPLACE_EXISTING` at lines 134-137. The case-variant name therefore targets the same file and can replace diagnostic content with the score-sheet CSV.

A disposable local Python filesystem probe created `cases.csv`, confirmed `Cases.csv` existed as the same file, and confirmed writing through the variant replaced its contents. The probe directory was removed. This establishes local path aliasing, not a new Java/Windows test result. Default `scorereport.csv` does not trigger the issue.

Proposed separately approved fix: reject reserved filenames case-insensitively, with targeted configuration regression tests covering case variants of both names and a valid custom score filename. Consider constructor invariants only in an explicitly approved API review; do not silently alter programmatically constructed `ExecutionConfig` behaviour during extraction.

### H1: conditional multiline anomaly records

`AnomalyLog.java:17` removes tabs from an archive basename but retains CR/LF. A custom archive regex allowing line breaks, or another direct logger caller, could split a record. The supplied archive regex uses Java's default dot behaviour and rejects line-break basenames, so this was not established as a defect in the supplied intake flow. Treat filename normalisation as optional hardening requiring separate approval, not part of pure modularisation.

### Existing limits and evidence gaps

- Current-user JVM permissions, protocol tampering and best-effort descendant cleanup remain documented course-grading limits; stronger adversarial isolation is external
- Fixed adapters support reviewed supplied testers, not arbitrary edited Java testers. Per-case fresh JVM state can differ from an original single-process stateful tester
- Adapter equivalence tests compare aggregate outcomes on a known-good submission and limited wrong-answer/expected-exception cases; they do not cover every varied submission per case
- Execution tests cover a looping child and interruption, but do not establish cleanup of an arbitrary process tree
- Dashboard CSS has a 390px minimum body width; arbitrarily narrow layout, network-disabled demonstrations and real browser keyboard behaviour remain unverified, as already documented
- Some model constructors rely on validated upstream data and allow nullable preparation paths. Document those contracts before considering stricter validation; changing invalid-input behaviour is separate from extraction

## Proposed verification and publication for later refactoring

| Batch | Targeted existing checks | New checks justified by extraction |
| --- | --- | --- |
| M1 | `OfflineHtmlReportGeneratorTest,QuestionOutcomeTest,GradingReportContextTest,AppTest,GradingPipelineTest` | Same fixture output/anchors/eligibility and hostile text; targeted navigation if markup changes |
| M2 | `ExecutionConfigTest,HtmlReportConfigTest,SubmissionProcessorTest,GradingPipelineTest` | Direct AppConfig loading/path/regex/numeric regression scenarios at the new parsing boundaries |
| M3 | `SubmissionProcessorTest,GradingPipelineTest` | Focused evidence/matching/planning/application tests where independent seams merit them |
| M4 | `GradingPipelineTest,AppTest,CsvScoreReportWriterTest` | Same case/submission CSV fields, export eligibility, diagnostic persistence and failure behaviour |
| B1 (separate approval) | `ExecutionConfigTest` | Case variants of reserved names rejected; valid custom name remains accepted |

After approved Java batches, run `mvn clean verify` and `java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties` once at final integration. Expected supplied totals remain Chee 12, David 10, Fen 10, Jing 0, Ping 20 and Xing 1, with six submissions and 120 case records. Compare semantic decisions/statuses/fields across independent runs; timings and run paths can vary. Existing fixtures assert the three completed CSVs remain byte-identical during HTML rendering. Check supplied-input hashes and record actual platform/runtime/browser limits.

Keep refactoring and bug fixes in focused branches/PRs and coordinate shared-file ownership. Publish reviewed approved files to `main` through a PR, give Boey its URL and ask him to contact the group. Human clean-build/behaviour acceptance remains required for substantial implemented revisions. No automatic merge, teammate messages, optional enhancements, bonus claim changes or human disclosure writing are authorised by this review.

## Brief/rubric alignment

The actual `CS102-Project-Auto Grading System.pdf` was read, and relevant pages were rechecked before planning. Page 6 calls for separated major responsibilities, focused methods, clear packages, consistent conventions, external configuration and understandable class relationships; M1-M4 address those concerns through composition and explicit boundaries. Page 7's edge-case handling/usability criteria motivate preserving identity, export, failure and navigation behaviour through observable regression checks. Page 1's demonstration expectations favour a stable rehearsable pipeline and explainable design. Page 3 permits Maven and only free/open-source libraries; no new runtime or style-tool dependency is proposed. Human attribution/AI Use disclosure remains a release duty under the repository's explicit agent exception.

See [CODING_STYLE.md](CODING_STYLE.md) for future coding rules. The inventory below includes deliberate keep decisions; class count and line count alone do not measure design quality.

## Complete reviewed-file inventory

Line counts are physical lines at source baseline `a99190f`; paths resolve from the repository root. Each listed file was read by main or its bounded reviewer and integrated into this report. Test size is assessed by scenario readability rather than a production-file cap.

### Application Java

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [src/main/java/autograder/App.java](../src/main/java/autograder/App.java) | 133 | CLI orchestration, exit status and report coordination | Keep coordinator; method-size exception recorded in M4 |
| [src/main/java/autograder/api/BoilerplateDetector.java](../src/main/java/autograder/api/BoilerplateDetector.java) | 12 | Starter-analysis extension contract | Keep; Sera owns implementation |
| [src/main/java/autograder/api/FeedbackGenerator.java](../src/main/java/autograder/api/FeedbackGenerator.java) | 9 | Student-feedback extension contract | Keep; coordinate feature integration |
| [src/main/java/autograder/api/HtmlReportGenerator.java](../src/main/java/autograder/api/HtmlReportGenerator.java) | 9 | Completed-context HTML output contract | Keep public boundary |
| [src/main/java/autograder/api/PlagiarismDetector.java](../src/main/java/autograder/api/PlagiarismDetector.java) | 12 | Similarity-analysis extension contract | Keep; Kiya owns implementation |
| [src/main/java/autograder/api/ProgressListener.java](../src/main/java/autograder/api/ProgressListener.java) | 5 | Progress event contract | Keep; coordinate richer events |
| [src/main/java/autograder/api/TestCaseProvider.java](../src/main/java/autograder/api/TestCaseProvider.java) | 10 | Runnable-case provider contract | Keep; coordinate William’s feature |
| [src/main/java/autograder/config/AppConfig.java](../src/main/java/autograder/config/AppConfig.java) | 611 | Immutable settings plus properties/assessment/path loading | Extract loading/policy (M2); retain facade |
| [src/main/java/autograder/config/ExecutionConfig.java](../src/main/java/autograder/config/ExecutionConfig.java) | 86 | Execution settings and grading-output validation | Keep cohesive record; B1 requires separate fix |
| [src/main/java/autograder/config/HtmlReportConfig.java](../src/main/java/autograder/config/HtmlReportConfig.java) | 29 | Optional report enablement and safe basename | Keep cohesive record |
| [src/main/java/autograder/csv/CsvCodec.java](../src/main/java/autograder/csv/CsvCodec.java) | 128 | CSV quoting, records, parsing and serialization | Keep focused codec |
| [src/main/java/autograder/execution/CaseExecutor.java](../src/main/java/autograder/execution/CaseExecutor.java) | 92 | Fixture copying, one case and result protocol | Keep execution boundary |
| [src/main/java/autograder/execution/ProcessResult.java](../src/main/java/autograder/execution/ProcessResult.java) | 5 | Immutable child-process outcome | Keep small value record |
| [src/main/java/autograder/execution/ProcessRunner.java](../src/main/java/autograder/execution/ProcessRunner.java) | 225 | Bounded streams, deadlines and cleanup | Keep coupled lifecycle service |
| [src/main/java/autograder/execution/StudentCompiler.java](../src/main/java/autograder/execution/StudentCompiler.java) | 92 | Part workspace and selected compiler inputs | Keep compilation boundary |
| [src/main/java/autograder/grading/GradingPipeline.java](../src/main/java/autograder/grading/GradingPipeline.java) | 193 | Batch/part execution, eligibility and CSV projections | Extract diagnostics; clarify private helpers (M4) |
| [src/main/java/autograder/grading/GradingRun.java](../src/main/java/autograder/grading/GradingRun.java) | 13 | Completed batch, run path and copied decisions | Keep immutable result boundary |
| [src/main/java/autograder/grading/InstructorTestCaseProvider.java](../src/main/java/autograder/grading/InstructorTestCaseProvider.java) | 91 | Fixed tester validation and case definitions | Keep supported-assessment boundary |
| [src/main/java/autograder/model/AnalysisFinding.java](../src/main/java/autograder/model/AnalysisFinding.java) | 26 | Recorded source-analysis evidence | Keep small immutable model |
| [src/main/java/autograder/model/AssessmentDefinition.java](../src/main/java/autograder/model/AssessmentDefinition.java) | 46 | Assessment metadata and decimal maximum | Keep coherent aggregate |
| [src/main/java/autograder/model/BatchResult.java](../src/main/java/autograder/model/BatchResult.java) | 32 | Completed grades and supplied analysis lists | Keep copied result aggregate |
| [src/main/java/autograder/model/GradingReportContext.java](../src/main/java/autograder/model/GradingReportContext.java) | 22 | Batch and validated copied withholding decisions | Keep authoritative reporting boundary |
| [src/main/java/autograder/model/QuestionDefinition.java](../src/main/java/autograder/model/QuestionDefinition.java) | 78 | Question sources, resources, counts and weights | Keep coherent metadata model |
| [src/main/java/autograder/model/ScoreSheet.java](../src/main/java/autograder/model/ScoreSheet.java) | 56 | Raw roster schema, rows and student mapping | Keep protected nested collections |
| [src/main/java/autograder/model/SimilarityFinding.java](../src/main/java/autograder/model/SimilarityFinding.java) | 36 | Validated pairwise similarity/evidence | Keep focused result model |
| [src/main/java/autograder/model/StudentGrade.java](../src/main/java/autograder/model/StudentGrade.java) | 49 | Recorded-case score aggregation and duplicate check | Keep scoring responsibility |
| [src/main/java/autograder/model/StudentIdentity.java](../src/main/java/autograder/model/StudentIdentity.java) | 85 | Observed headers, matched record and evidence | Keep identity result model |
| [src/main/java/autograder/model/StudentRecord.java](../src/main/java/autograder/model/StudentRecord.java) | 37 | Roster identity fields | Keep small model |
| [src/main/java/autograder/model/SubmissionCorrection.java](../src/main/java/autograder/model/SubmissionCorrection.java) | 53 | Recorded proposed/applied correction data | Keep shared audit model |
| [src/main/java/autograder/model/SubmissionResult.java](../src/main/java/autograder/model/SubmissionResult.java) | 89 | Preparation paths/status/anomalies/corrections | Keep preparation result aggregate |
| [src/main/java/autograder/model/SubmissionStatus.java](../src/main/java/autograder/model/SubmissionStatus.java) | 7 | Preparation-state enum | Keep distinct from test states |
| [src/main/java/autograder/model/TestCaseDefinition.java](../src/main/java/autograder/model/TestCaseDefinition.java) | 43 | Validated runnable case and maximum points | Keep execution contract |
| [src/main/java/autograder/model/TestCaseResult.java](../src/main/java/autograder/model/TestCaseResult.java) | 46 | Status, captured output, timing and earned points | Keep scoring/result boundary |
| [src/main/java/autograder/model/TestStatus.java](../src/main/java/autograder/model/TestStatus.java) | 10 | Recorded execution-status enum | Keep all six states |
| [src/main/java/autograder/report/CsvScoreReportWriter.java](../src/main/java/autograder/report/CsvScoreReportWriter.java) | 143 | Schema-preserving score export and atomic write | Keep persistence/safety boundary |
| [src/main/java/autograder/report/html/OfflineHtmlReportGenerator.java](../src/main/java/autograder/report/html/OfflineHtmlReportGenerator.java) | 681 | Document, sections, summaries and HTML persistence | Extract render collaborators (M1) |
| [src/main/java/autograder/report/html/QuestionOutcome.java](../src/main/java/autograder/report/html/QuestionOutcome.java) | 175 | Recorded question evidence and credit bands | Keep existing report-local helper |
| [src/main/java/autograder/roster/ScoreSheetRepository.java](../src/main/java/autograder/roster/ScoreSheetRepository.java) | 144 | CSV roster import and semantic validation | Keep import boundary |
| [src/main/java/autograder/submission/CorrectionLog.java](../src/main/java/autograder/submission/CorrectionLog.java) | 43 | Correction audit persistence | Keep focused logger |
| [src/main/java/autograder/submission/IdentityResolver.java](../src/main/java/autograder/submission/IdentityResolver.java) | 578 | Evidence, matching, repair planning and mutation | Extract staged collaborators (M3); retain facade |
| [src/main/java/autograder/submission/SafeZipExtractor.java](../src/main/java/autograder/submission/SafeZipExtractor.java) | 144 | Bounded ZIP extraction, CRC and containment | Keep safety boundary |
| [src/main/java/autograder/submission/SubmissionProcessor.java](../src/main/java/autograder/submission/SubmissionProcessor.java) | 268 | Intake coordination, duplicates and progress | Keep coordinator; reconsider only if growth warrants |
| [src/main/java/autograder/submission/SubmissionValidator.java](../src/main/java/autograder/submission/SubmissionValidator.java) | 113 | Question-root selection and folder validation | Keep narrow package-local service |
| [src/main/java/autograder/submission/WorkingCopyPreparer.java](../src/main/java/autograder/submission/WorkingCopyPreparer.java) | 118 | Canonical working copy and layout corrections | Keep copy concern; delegate repairs through facade initially |
| [src/main/java/autograder/ui/ConsoleProgressListener.java](../src/main/java/autograder/ui/ConsoleProgressListener.java) | 10 | Plain console progress rendering | Keep ProgressListener adapter |
| [src/main/java/autograder/util/AnomalyLog.java](../src/main/java/autograder/util/AnomalyLog.java) | 32 | Anomaly audit persistence and stderr | Keep; conditional hardening H1 |

### Tests

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [src/test/java/autograder/AppTest.java](../src/test/java/autograder/AppTest.java) | 74 | Help/startup, optional HTML and retained CSV errors | Keep public CLI behaviour tests |
| [src/test/java/autograder/config/ExecutionConfigTest.java](../src/test/java/autograder/config/ExecutionConfigTest.java) | 60 | Tool/path resolution, overlaps and invalid settings | Keep; add B1 cases only after fix approval |
| [src/test/java/autograder/config/HtmlReportConfigTest.java](../src/test/java/autograder/config/HtmlReportConfigTest.java) | 39 | Report defaults, enablement and safe name | Keep public config tests |
| [src/test/java/autograder/execution/ExecutionServicesTest.java](../src/test/java/autograder/execution/ExecutionServicesTest.java) | 181 | Timeout/output/interruption/compile/result protocol | Keep observable execution scenarios |
| [src/test/java/autograder/grading/AdapterEquivalenceTest.java](../src/test/java/autograder/grading/AdapterEquivalenceTest.java) | 224 | Original/adapted scoring and exception comparisons | Keep; limitations recorded separately |
| [src/test/java/autograder/grading/GradingPipelineTest.java](../src/test/java/autograder/grading/GradingPipelineTest.java) | 268 | Fair timeouts, independent parts, withholding and CSV/HTML | Keep end-to-end assertions |
| [src/test/java/autograder/grading/InstructorTestCaseProviderTest.java](../src/test/java/autograder/grading/InstructorTestCaseProviderTest.java) | 109 | Case metadata, hashes and CRLF tester handling | Keep; includes already-merged CRLF correction |
| [src/test/java/autograder/model/GradingReportContextTest.java](../src/test/java/autograder/model/GradingReportContextTest.java) | 40 | Copied authoritative/nonblank batch decisions | Keep immutable-context invariants |
| [src/test/java/autograder/model/StudentGradeTest.java](../src/test/java/autograder/model/StudentGradeTest.java) | 53 | Independent marks, duplicate IDs and decimals | Keep score invariants |
| [src/test/java/autograder/model/TestCaseResultTest.java](../src/test/java/autograder/model/TestCaseResultTest.java) | 34 | Only passes earn points; invalid maximum/duration | Keep status/score invariants |
| [src/test/java/autograder/report/CsvScoreReportWriterTest.java](../src/test/java/autograder/report/CsvScoreReportWriterTest.java) | 110 | Schema/BOM preservation, source protection and bounds | Keep persistence invariants |
| [src/test/java/autograder/report/html/OfflineHtmlReportGeneratorTest.java](../src/test/java/autograder/report/html/OfflineHtmlReportGeneratorTest.java) | 371 | Inline assets, escaping, eligibility, links and edge cases | Keep cohesive black-box report assertions; test files have no automatic cap |
| [src/test/java/autograder/report/html/QuestionOutcomeTest.java](../src/test/java/autograder/report/html/QuestionOutcomeTest.java) | 147 | Exact bands, mixed statuses and neutral states | Keep calculation/evidence invariants |
| [src/test/java/autograder/roster/ScoreSheetRepositoryTest.java](../src/test/java/autograder/roster/ScoreSheetRepositoryTest.java) | 136 | Roster/schema/quoting and invalid-row validation | Keep import scenarios |
| [src/test/java/autograder/submission/SubmissionProcessorTest.java](../src/test/java/autograder/submission/SubmissionProcessorTest.java) | 481 | Extraction, identity, repairs, duplicates and progress | Keep ingestion integration scenarios; review organisation when edited |

### Runtime resources

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [src/main/resources/autograder/adapters/Q1aAdapter.java](../src/main/resources/autograder/adapters/Q1aAdapter.java) | 59 | Fixed isogram cases/oracles and result protocol | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q1bAdapter.java](../src/main/resources/autograder/adapters/Q1bAdapter.java) | 42 | Fixed even-sum cases/oracles and result protocol | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q2aAdapter.java](../src/main/resources/autograder/adapters/Q2aAdapter.java) | 40 | Fixture-backed person cases and missing-file oracle | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q2bAdapter.java](../src/main/resources/autograder/adapters/Q2bAdapter.java) | 47 | Student-file cases and expected DataException oracle | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q3Adapter.java](../src/main/resources/autograder/adapters/Q3Adapter.java) | 55 | Shape sorting cases and expected output | Keep standalone compiler resource |
| [src/main/resources/autograder/html/dashboard.css](../src/main/resources/autograder/html/dashboard.css) | 146 | Offline layout, statuses, credit/focus classes and breakpoints | Keep selectors/layout; documented 390px limit |
| [src/main/resources/autograder/html/dashboard.js](../src/main/resources/autograder/html/dashboard.js) | 100 | Search/filter/sort pairing and detail/question navigation | Keep interactions; preserve emitted IDs/data |

### Build and settings

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [pom.xml](../pom.xml) | 48 | Java 17/JUnit/Maven executable packaging | Keep build/dependencies; no new tooling proposed |
| [config.properties](../config.properties) | 93 | Operational settings and supplied assessment mapping | Keep unchanged; defaults do not trigger B1 |
| [.gitignore](../.gitignore) | 8 | Generated output and exact local-handoff ignore rules | Keep; tracked metadata remains unrelated |
