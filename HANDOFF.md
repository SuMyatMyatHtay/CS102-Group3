# CS102 Auto Grading System — continuation handoff

Updated 10 October 2026 (Asia/Tokyo).

## Start here

The preparation foundation was accepted by Boey after 30 passing tests and a successful local build/run. In this continuation Boey confirmed identity and said: **“Approved lets go”**, approving the recorded remaining MVP plan. Compilation, per-case execution, score aggregation and CSV integration are now implemented; integration verification has passed and Boey has supplied a successful clean build and grading run, satisfying the remaining MVP completion gate (DONE). Do not rebuild the foundation or request approval for the same implementation plan again.

At a new workflow follow the README kickoff and agents.md session protocol: read the six core context documents, summarize the MVP and live bonus board, then ask **“Who am I speaking with? Please choose your recorded team name: Boey, Sera, William, Kai Cong, Kiya, or Gabriel.”** Confirm nickname/variant answers against the list rather than guessing. Read-only onboarding may precede identity confirmation; claims, edits, installs, builds and implementation must wait for it. Do not infer the new driver from the previous driver. Boey's build-confirmation gate is satisfied for the core MVP; future features still need their own confirmation.

## Human decisions to preserve

- Substantial new work requires a concise inspected plan and approval; the remaining MVP implementation already has approval
- Use simple, readable Java, clear responsibilities and ordinary control flow; avoid unnecessary frameworks
- Keep operational settings in `config.properties`
- Comments must be minimal, lowercase and without full stops
- **Do not add AI-attribution comments to Java**: Boey explicitly retained this rule; the human team will review/modify the work and manually add brief-required method declarations and the presentation AI Use appendix before submission
- Preserve all supplied ZIPs, instructor testers, starter files, score sheets and reference results
- Do not restore the removed legacy grader
- No automatic commit or push is requested
- `.DS_Store` is an unrelated pre-existing modification; preserve it
- README, agents.md and docs/REQUIREMENTS.md already had handoff-related changes before this continuation; preserve their context

## Implemented flow

1. AppConfig validates preparation settings and roster/assessment metadata
2. ExecutionConfig validates grading output location, executable paths and positive limits
3. InstructorTestCaseProvider validates the fixed five instructor testers and supplies 20 case definitions
4. The CLI checks Java tools/resources before preparing submissions
5. SubmissionProcessor extracts archives safely, reconciles identities, prepares corrected working copies and flags duplicates
6. GradingPipeline withholds unresolved identities, duplicates and unusable preparations; ordinary name warnings do not block grading a safely matched student
7. StudentCompiler compiles each part independently using only the answer, required companion sources, configured support classes and adapter
8. CaseExecutor runs every case in a fresh directory/JVM with its fixture copies
9. ProcessRunner bounds capture of stdout/stderr, drains both pipes and enforces deadlines with best-effort process cleanup
10. StudentGrade aggregates scores and CsvScoreReportWriter exports eligible marks; diagnostic CSVs record every case and export decision

A mismatched answer is FAILED. Unexpected exceptions and invalid/missing result files are RUNTIME_ERROR. A case deadline is TIMED_OUT. A missing/invalid student source gives all that part’s cases COMPILE_ERROR. A blocked submission gets NOT_RUN records. Only PASSED earns configured points. A runtime case cannot erase other case marks.

Infrastructure I/O errors withhold the affected student's export while retaining diagnostics and continuing unrelated work. Withheld and absent roster rows retain their original numerator, which may be blank or an old score. Always read `submissions.csv` with the score report.

## Files added for this MVP

- `src/main/java/autograder/config/ExecutionConfig.java`: tool paths, time/output limits, grading directory/report filename
- `src/main/java/autograder/grading/InstructorTestCaseProvider.java`: fixed original-test validation and per-case definitions
- `src/main/java/autograder/grading/GradingPipeline.java`: grading/report orchestration
- `src/main/java/autograder/grading/GradingRun.java`: completed batch, output directory and withheld reasons
- `src/main/java/autograder/execution/ProcessRunner.java`, `ProcessResult.java`: bounded child-process lifecycle and captured result
- `src/main/java/autograder/execution/StudentCompiler.java`: separate part compilation
- `src/main/java/autograder/execution/CaseExecutor.java`: individual fixture/result protocol execution
- `src/main/resources/autograder/adapters/`: five Java adapter resources, packaged inside the JAR
- `src/test/java/autograder/config/ExecutionConfigTest.java`
- `src/test/java/autograder/execution/ExecutionServicesTest.java`
- `src/test/java/autograder/grading/InstructorTestCaseProviderTest.java`, `AdapterEquivalenceTest.java`, `GradingPipelineTest.java`
- `src/test/resources/autograder/adapters/QXAdapter.java`: synthetic compiler-test resource only

App.java now invokes grading. AppConfig only exposes its existing path canonicalizer within its package for ExecutionConfig. Existing foundation models and bonus contracts are retained.

## Fixed adapters and limits

The parts are Q1a (3 cases), Q1b (3), Q2a (5), Q2b (5), Q3 (4), totaling 20 at the supplied weights. The adapter provider verifies normalized original tester hashes (line-ending differences are accepted) so edited assertions cannot silently use an outdated adapter. Other instructor testers need new reviewed adapters; configurable assessment metadata alone is not arbitrary Java test ingestion. This is core MVP, not William's dynamic parameterized-case feature.

Q2a uses personstester.txt. Q2b uses studentstester.txt and DataException.class. Q3 uses supplied Shape/Circle/Rectangle classes plus the student's ShapeComparator.java. Missing answers/companions are never replaced with starter answers. Each case starts with fresh static state and fixtures. That intentional isolation can differ from running a stateful student answer through the original single-process tester.

The adapter writes a small PASSED/FAILED/ERROR result file; stdout is diagnostic only. `System.exit(0)` without a result is a runtime error. Timeouts override written results. Unexpected exceptions preserve zero marks with stack traces; Q2b's expected DataException cases pass.

This is not an OS security sandbox. Programs run with the current account's privileges. Hostile code could access files/network, tamper with the result file or spawn children. Descendant termination is best effort where enumeration/termination are permitted. Use external isolation for adversarial inputs; no hostile-code isolation is claimed.

Defaults in config.properties: current JDK tools (`JDK`), compile deadline 15000ms, case deadline 2000ms, captured output 65536 bytes per stream. Tool values can also be names on PATH or configured paths. Relative paths resolve against the configuration directory.

## Running and output

```sh
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

Temporary Maven available on Boey's machine (check existence before use):

```sh
/tmp/apache-maven-3.9.9/bin/mvn -Dmaven.repo.local=/tmp/cs102-maven-repository clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

Runtime requires a full JDK. Maven targets Java 17; local execution has used JDK 25. Do not claim Windows or an actual Java 17 runtime was tested without evidence. Only Java standard-library runtime dependencies are used; JUnit is test-only.

Outputs are ignored by Git:

- `build/extracted/run-*`: raw extraction
- `build/working/run-*`: corrected working copies
- `build/grading/run-*`: independent compiler/case workspaces and reports
- `scorereport.csv`: exact required roster schema, updated eligible numerators
- `cases.csv`: archive, username, case, status, score, maximum, duration, stdout/stderr
- `submissions.csv`: EXPORTED/WITHHELD decisions, reasons, computed score and preparation warnings
- Existing correction/anomaly logs append across runs

Each run creates fresh output directories. A completed batch may return exit zero with student failures or withheld results; inspect reports. Startup/configuration or report-write failure returns nonzero.

## Verification evidence

Historical foundation: 30 tests passed; 6 submissions prepared, 4 READY, 2 REVIEW_REQUIRED (David/Xing names differ from roster), 0 FAILED, 22 working-copy corrections. Boey already confirmed this foundation build. All 63 historical input hashes remained unchanged.


The remaining MVP is **DONE**. Boey supplied a successful clean build on 2026-10-10 with **48 tests, zero failures/errors/skips**, followed by the expected six-submission grading output (12, 10, 10, 0, 20, 1). The human build-confirmation gate is satisfied. `mvn clean verify` passed **48 tests, zero failures/errors**. After final diagnostic wording/formatting fixes, the affected execution suite passed **7/7** and the executable JAR was rebuilt successfully. The packaged CLI graded all six supplied submissions in one fresh run, producing 120 case records (20 per submission). All scores reconciled between case points, submission summaries and exported CSV numerators. All **67** supplied-file hashes in the new snapshot remained unchanged.

| Student | Q1a | Q1b | Q2a | Q2b | Q3 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Chee | 3 | 3 | 5 | 1 | 0 | 12 |
| David | 3 | 3 | 4 | 0 | 0 | 10 |
| Fen | 1 | 0 | 5 | 4 | 0 | 10 |
| Jing | 0 | 0 | 0 | 0 | 0 | 0 |
| Ping | 3 | 3 | 5 | 5 | 4 | 20 |
| Xing | 0 | 0 | 1 | 0 | 0 | 1 |

David and Xing retain name-header warnings while their uniquely matched submissions are graded. Xing passes Q2a's missing-file case; the remaining four Q2a cases time out. Inspection shows the loop advances to the next line only when a surname matches, so an unmatched line can repeat forever. The other parts fail their assertions. The reference file's components sum to 15 but its stated total is 1; actual execution independently awards 1. Reference files were not changed.

Verification includes original-tester equivalence on Ping (20/20), wrong-answer and expected-exception equivalence, pass–loop–pass retaining 2 points, independent compilation failure, bounded output, interrupted-process cleanup, missing results/System.exit(0), duplicate/unresolved withholding, missing sources, configured-path checks and exact CSV integration. The final reports are under `build/grading/run-10952970798376735826`. Tests ran on the available JDK 25 with Maven's Java 17 compilation target; actual Java 17 runtime and Windows behavior have not been verified.

The input snapshot is `/tmp/cs102-mvp-input-hashes.json`; temporary build/run logs are `/tmp/cs102-final-{verify,targeted,package,grading}.log`. Temporary files may disappear. The generated report directory may also be removed during ordinary cleanup.

## Team and bonus status

All six bonus implementations remain TODO. Sera owns feature 3 (boilerplate comparison) and William owns feature 4 (parameterized tests). Features 1 (token similarity), 2 (HTML), 5 (Markdown feedback), 6 (ANSI dashboard) remain unclaimed. Boey's bonus claim is pending. Shared MVP work was explicitly permitted while claims remain pending.

Main agent retains architecture/integration. This implementation delegated adapters/provider/tests and process services/tests with non-overlapping ownership recorded in agents.md. Only main may spawn agents; no Astra subagents; at most two concurrent subagents. All delegated work is complete; no agents are actively editing.

No bonus implementation begins before the core MVP is functional and tested. No feature is DONE before the driver's clean-build confirmation. A bonus architecture document is due only when that bonus is marked DONE.

## Project submission responsibilities

Read README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md and docs/TEAM_WORKFLOW.md for current behavior and responsibilities. The seven-page brief is `CS102-Project-Auto Grading System.pdf`; do not reread it routinely unless exact wording matters. It permits Maven. Tentative rubric: code 7, application 16, presentation 7; presentation 10–15 minutes plus 5 minutes Q&A. Humans must complete method attributions/AI appendix, cross-member understanding, presentation and release review. The exact GX-TY group/section identifier still needs the team. Do not promise a grade.

## Next action

The core MVP is DONE following Boey's successful 48-test clean build and six-submission grading run on 2026-10-10. Do not ask for that confirmation again. Use the kickoff prompt at the start of README.md to onboard teammates, summarize current bonus ownership, and ask them to choose their canonical team name. Confirm or coordinate a feature claim, inspect the relevant code and obtain explicit approval for a substantial feature plan before implementation. No bonus has been marked DONE; no bonus architecture document is due yet. Do not automatically commit or push.
