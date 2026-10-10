# Foundation architecture

## What runs today

The entry point loads an external properties file, reads the supplied class list, processes the submission ZIPs, checks ownership and required files, prepares corrected working copies where safe, and reports problems. It does not yet compile or execute student programs.

The archive and supplied inputs are evidence. Extraction output is a raw copy. Corrections belong in a separate working copy, with recorded reasons. Conflicting ownership must not be converted into a guessed grade. An extracted archive, a valid question, and a graded student are separate states.

## Responsibilities

- `App`: accepts the config filename, shows progress and the final summary
- `AppConfig`: loads paths, identity rules, extraction limits, CSV column mappings, and assessment settings
- `AssessmentDefinition` / `QuestionDefinition`: describe the assessment without fixed Java question names or marks
- `ScoreSheetRepository`: validates the supplied class list and retains its original CSV data
- `SafeZipExtractor`: bounded ZIP extraction with unsafe path and corruption rejection
- `SubmissionProcessor`: coordinates the batch and records per-submission outcomes
- `SubmissionValidator`: selects an unambiguous question root and checks question folders
- `IdentityResolver`: checks required sources/headers and matches identity evidence against the roster
- `WorkingCopyPreparer`: creates a canonical copy under the roster username and applies approved identity repairs
- `CorrectionLog` / `AnomalyLog`: keep corrections separate from problems
- `SubmissionResult`: shares the original/working locations, identity, status, problems, and corrections
- `CsvCodec` / `CsvScoreReportWriter`: parse/write the supplied schema safely; the CLI does not invent grades to call the writer
- `ProgressListener` / `ConsoleProgressListener`: separate progress events from their presentation

Small immutable data classes pass results between components. Lists exposed by result models cannot be modified by consumers. Composition keeps responsibilities separate. Interfaces allow later feature implementations to fit into the pipeline without changing its core responsibilities. Java class inheritance is not being added artificially; the supplied instructor tester classes themselves extend student question classes, which the later compilation design must respect.

## Bonus connections

| Contract | Input | Output |
| --- | --- | --- |
| PlagiarismDetector | Submissions and assessment | Similarity findings with evidence |
| HtmlReportGenerator | Completed batch results and output directory | Generated report path |
| BoilerplateDetector | Submission and starter directory | Findings identifying the source file and reason |
| TestCaseProvider | Assessment | Independently runnable test definitions |
| FeedbackGenerator | Student grading results and output directory | Feedback file path |
| ProgressListener | Stage, item, completed count and total | Display update |

These are Java contracts, not completed bonus implementations. Sera and William retain ownership of their implementation logic. Generation of runner source files will be agreed when the execution adapter is designed; a TestCaseDefinition describes the runner class and arguments to execute, not a general Java source parser.

## Scoring foundation

`TestCaseDefinition` names a globally unique test, question, runner class, arguments, and configured maximum points. `TestCaseResult` distinguishes pass, fail, timeout, compilation error, runtime error, and not-run. Only a passed case earns its configured points. `StudentGrade` sums recorded case scores using decimal arithmetic and rejects duplicate case identifiers. `BatchResult` groups assessment, grades and analysis findings for report features.

These models do not prove execution correctness. The future executor must record every expected case, including skipped/unrunnable cases, validate case/question mappings against the assessment, and keep compilation errors from blocking independent questions. The report pipeline must check all expected cases before exporting final marks and must not choose between duplicate student submissions automatically.

## Decisions required for the next MVP stage

The supplied testers run multiple cases within one main method and print their total at the end. A timeout on the whole method is insufficient for fair continuation. Before implementing the execution stage, build a small adapter experiment preserving the supplied assertions and point values. Demonstrate a pass–loop–pass sequence that retains both available marks.

Prefer explicit, understandable case runners to brittle text rewriting of arbitrary Java. Evaluate reusable runner generation versus small documented adapters for these tester files, including maintenance and multi-assessment support. Preserve instructor originals and test equivalence against independently verified expected outcomes. William's later feature can generate additional configured cases through the same contract.

Run compilation/execution in separate processes with deadlines and bounded stdout/stderr. Clean up child processes after timeout/cancellation. A child JVM and timeout are not a security sandbox: state the supported trust boundary honestly and decide stronger isolation before claiming protection from deliberately hostile code.

## Configuration and simplicity

Externalize settings the instructor or team may change: paths, assessment identifiers, required files, case counts, marks, matching rules, extraction limits, and future execution limits. Language/protocol mechanics such as zero-based indexes, CSV quoting characters, and a success exit code are not configurable grading policy. Keep comments minimal, lowercase, and without full stops; agents must not add AI attribution comments.

## Dependency choices

Java 17 standard library provides file/ZIP/process primitives. JUnit Jupiter is a test-only dependency; there are no third-party application runtime libraries in the current foundation. Maven compiler, test, and JAR plugins build the application. JUnit is EPL-2.0 and the selected Apache Maven plugins use Apache-2.0; the final team release must verify the complete dependency/license inventory if libraries change.
