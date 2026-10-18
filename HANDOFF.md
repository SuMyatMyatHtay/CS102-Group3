# CS102 Auto Grading System — continuation handoff

Updated 10 October 2026 (Asia/Tokyo), after Boey claimed feature 2.

**Immediate continuation: core MVP DONE; feature 2 (offline HTML dashboard) claimed by Boey, status TODO. No HTML implementation or approved feature 2 implementation plan exists yet. Read the feature 2 continuation sections below after the project context.**

## Start here

The preparation foundation was accepted by Boey after 30 passing tests and a successful local build/run. In this continuation Boey confirmed identity and said: **“Approved lets go”**, approving the recorded remaining MVP plan. Compilation, per-case execution, score aggregation and CSV integration are now implemented; integration verification has passed and Boey has supplied a successful clean build and grading run, satisfying the remaining MVP completion gate (DONE). Do not rebuild the foundation or request approval for the same implementation plan again.

At a new workflow follow the README kickoff and agents.md session protocol: read the six core context documents and `CS102-Project-Auto Grading System.pdf` (including its rubrics and restrictions), summarize the MVP and live bonus board, then ask **“Who am I speaking with? Please choose your recorded team name: Boey, Sera, William, Kai Cong, Kiya, or Gabriel.”** Confirm nickname/variant answers against the list rather than guessing. Read-only onboarding may precede identity confirmation; claims, edits, installs, builds and implementation must wait for it. Do not infer the new driver from the previous driver. Boey's build-confirmation gate is satisfied for the core MVP; future features still need their own confirmation.

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

All six bonus implementations remain TODO. Sera owns feature 3 (boilerplate comparison) and William owns feature 4 (parameterized tests). Kiya owns feature 1 (token similarity), preserving the assignment in the remote work-division document. Boey has claimed feature 2 (HTML). Features 5 (Markdown feedback) and 6 (ANSI dashboard) remain unclaimed. Shared MVP work was explicitly permitted while claims remain pending.

Main agent retains architecture/integration. This implementation delegated adapters/provider/tests and process services/tests with non-overlapping ownership recorded in agents.md. Only main may spawn agents; no Astra subagents; at most two concurrent subagents. All delegated work is complete; no agents are actively editing.

No bonus implementation begins before the core MVP is functional and tested. No feature is DONE before the driver's clean-build confirmation. A bonus architecture document is due only when that bonus is marked DONE.

## Project submission responsibilities

Read README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md and docs/TEAM_WORKFLOW.md for current behavior and responsibilities. The seven-page brief is `CS102-Project-Auto Grading System.pdf`; read it during initial onboarding and review relevant highest-band rubric descriptors and restrictions before planning new implementations; cite relevant pages and reuse unchanged context where appropriate. It permits Maven. Tentative rubric: code 7, application 16, presentation 7; presentation 10–15 minutes plus 5 minutes Q&A. Humans must complete method attributions/AI appendix, cross-member understanding, presentation and release review. The exact GX-TY group/section identifier still needs the team. Do not promise a grade.

## Next action

Continue with feature 2 planning for Boey, not a rebuild of the accepted MVP. Read the continuation details below, inspect the narrow reporting/configuration integration surface and the relevant PDF rubric sections, then present a concise implementation plan for approval. The request to claim feature 2 and prepare this handoff was not approval to implement it. No bonus architecture document is due yet. No commit or push is requested for this claim/handoff task.

## 1. Latest human request and approval boundary

Boey asked: “Lets do 2. Just mark it as claimed in the markdown files first, then create a detailed handoff document that details the full project context, what we have done, where we are at, next steps.” The goal is to move to a fresh chat before the current context fills.

Actions completed for that request:

- Recorded Boey as owner of feature 2 in agents.md, README.md, WORK_DIVISION.md, docs/REQUIREMENTS.md and docs/TEAM_WORKFLOW.md, and updated this handoff
- Removed Boey from the list of people who still need a claim; only features 5 and 6 remain available for Kai Cong and Gabriel
- Kept all six bonus statuses TODO, because no bonus implementation has started
- Preserved Sera's feature 3 and William's feature 4 locks, and Kiya's feature 1 claim
- Prepared this detailed continuation context without changing Java or running an unnecessary rebuild

Do not interpret “Lets do 2” in isolation as permission to skip planning: Boey explicitly limited this turn to claiming and writing the handoff. Read-only inspection and a concrete plan are the next steps. Wait for explicit approval before implementing substantial feature 2 changes.

## 2. Recommended prompt to paste into a new chat

```text
Continue this CS102 project from HANDOFF.md. I am Boey, and I have claimed feature 2: the offline HTML dashboard. The core MVP is DONE; my clean build passed 48 tests and the sample grading run succeeded. Read agents.md, HANDOFF.md, README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md, docs/TEAM_WORKFLOW.md, WORK_DIVISION.md, and the project brief CS102-Project-Auto Grading System.pdf. Inspect git status and the narrow reporting/configuration surface described in the handoff.

Do not rebuild the MVP or start another bonus. First summarize the current state and propose a concise feature 2 implementation plan with relevant PDF rubric page references, reporting/withholding semantics, output configuration, offline behavior, safe rendering, and targeted verification. Wait for my explicit approval before implementing. Do not add AI-attribution comments or complete AI-use declarations; our human team will add the required declarations manually before submission. Do not commit or push unless I explicitly ask.
```

If the next message includes that explicit “I am Boey” identity, validate it against the team list rather than asking redundantly. Otherwise use the full list in the session identity prompt. Prior driver identity alone is not sufficient in a new workflow.

## 3. GitHub synchronization and recent documentation history

Repository: https://github.com/boeyboeyboeyboey/CS102-Group3

Local checkout used in this conversation: `/Users/boey/Documents/SMU/2.1/cs102/group project/CS102-Group3`. A teammate's checkout will have a different absolute path; resolve paths relative to its root.

Earlier in this chat the online repository still showed September code. Local main had two October MVP commits while remote main had two September planning commits. Those remote commits added/revised WORK_DIVISION.md and contained no Java changes relative to the common ancestor. A normal merge preserved both histories; no force push was used. Boey explicitly authorized publishing the correct code, and merge commit `6dccb4c` was pushed and verified against GitHub's main tip. This was task-specific permission, not perpetual permission to publish future work.

Kiya's ownership of feature 1 was found in the remote planning document and preserved during reconciliation. The old proposals named obsolete interfaces and packages; WORK_DIVISION.md was rewritten to point at the actual current contracts and agents.md as the authoritative board. Do not restore the old source tree or outdated interfaces from the online history.

At the start of this handoff task, local history was:

- `db0f2f1` — added setup/build instructions to readme
- `174e99c` — added more agentic scope management into markdown files for better workflow
- `6dccb4c` — Merge remote planning history and reconcile bonus ownership

`git status --short --branch` showed main aligned with the locally recorded origin/main and only `.DS_Store` modified before the claim edits. The live remote was not queried again for this handoff. The current claim/handoff edits are uncommitted local Markdown changes unless the human commits/pushes them afterward. Check actual Git status at continuation; do not assume this snapshot proves the current remote state.

Never stage the unrelated `.DS_Store` modification as part of feature work. Preserve user edits. Do not reset or clean the checkout to simplify onboarding.

## 4. Feature 2: established intent versus proposals

Established intent: an offline HTML dashboard/report generated from real grading results, owned by Boey. It must fit the existing grader and remain usable without a running server or internet connection. It should explain results, including failures and review conditions, while preserving current grading and CSV behavior.

WORK_DIVISION.md describes scores, filters and expandable diagnostics. Earlier proposals included cohort totals/average/median, simple score or pass-rate charts, search/sorting and expandable student details. These are useful candidates for the implementation plan, not a separately approved exhaustive UI specification. Do not invent a design preference, framework requirement, chart requirement or export policy on Boey's behalf. A small self-contained HTML/CSS/vanilla-JavaScript report is a plausible approach, but its exact implementation is still to be approved.

This is an output artifact of the Java grading application, not a hosted website project. Do not turn it into an unrelated web service, add a server/framework solely for the dashboard, or publish student reports to external hosting. The application currently has no third-party runtime dependencies. Any proposed addition needs a clear justification and compliance with the brief's restrictions.

## 5. Exact existing reporting surface to inspect

Start with these files instead of exploring the entire repository:

| File | Why it matters for feature 2 |
| --- | --- |
| src/main/java/autograder/api/HtmlReportGenerator.java | Existing public HTML extension contract |
| src/main/java/autograder/model/BatchResult.java | Assessment, student grades, analysis findings and similarity findings |
| src/main/java/autograder/model/StudentGrade.java | Submission, all case results, total and per-question score calculations |
| src/main/java/autograder/model/SubmissionResult.java | Identity, preparation status, warnings, corrections and source/working locations |
| src/main/java/autograder/model/TestCaseDefinition.java | Case/question identifiers and maximum marks |
| src/main/java/autograder/model/TestCaseResult.java | Status, earned points, duration, bounded stdout/stderr |
| src/main/java/autograder/model/AssessmentDefinition.java and QuestionDefinition.java | Names, ordering and configured maxima |
| src/main/java/autograder/grading/GradingRun.java | Batch result plus output directory and explicit withheld-reason map |
| src/main/java/autograder/grading/GradingPipeline.java | Creates grades, decides export eligibility, and writes existing CSV reports |
| src/main/java/autograder/App.java | Coordinates preparation/grading and prints output paths |
| src/main/java/autograder/config/ExecutionConfig.java | Current grading output/tool/deadline settings and path validation |
| config.properties | External operational settings; there are no HTML settings yet |
| src/test/java/autograder/grading/GradingPipelineTest.java | Meaningful eligibility, failure and CSV integration fixtures |

The current interface is:

```java
Path generate(BatchResult batch, Path outputDirectory) throws IOException;
```

Its package is `autograder.api`. It is an interface only; there is no concrete HTML implementation, template/resource, dashboard test or CLI HTML invocation yet.

`GradingRun` is a record containing `BatchResult batch`, `Path directory`, and `Map<SubmissionResult, String> withheld`. The map is copied when constructing the result. `GradingPipeline.grade(...)` builds it after processing submissions, calls its current private `writeReports(...)`, and returns it. `App` then prints scores or withholding reasons and the three CSV locations.

`BatchResult` contains assessment/grades/findings/similarities but **does not contain the withheld map or roster rows for absent students**. The current pipeline populates findings/similarities with empty lists because those analysis bonuses do not exist yet. Do not display empty findings as evidence that plagiarism/starter analysis has run and found nothing.

## 6. Important design issue: withheld versus graded scores

Settle this in the plan before implementing the dashboard. The existing HTML interface receives only BatchResult, whereas export eligibility is held separately in GradingRun.withheld. A renderer using only total points or preparation status cannot reliably infer whether a score was exported.

Examples:

- David and Xing have REVIEW_REQUIRED preparation warnings but valid identities and graded/exported scores
- Duplicate submissions can contain valid-looking identities and usable files but must both be withheld
- Infrastructure I/O errors can occur after some cases have earned marks; the computed partial score is retained for diagnostics but the student's export is withheld
- Unresolved identity or failed preparation produces NOT_RUN records and no final exported grade
- A roster student with no submission may not appear in BatchResult at all; the CSV writer preserves that template row

Therefore do not equate REVIEW_REQUIRED with withheld, zero with not graded, or a computed partial total with an accepted final score. Do not compute class averages that silently include withheld partial totals as official grades. Decide and label the cohort/eligible-submission denominator explicitly. If showing roster-wide absence, obtain that data deliberately rather than inventing absent rows from BatchResult.

The plan must propose how to pass enough reporting context to the HTML implementation while coordinating shared-contract changes. Possible approaches involve enriching shared reporting data or explicitly supplying run-level eligibility, but none is approved yet. Do not casually break the interface or rederive a conflicting eligibility policy in the renderer. Existing CSV schema and withholding behavior must remain intact.

## 7. Safe, truthful and useful HTML output

Acceptance considerations for the plan and tests:

- Open the saved report directly using file:// with networking unavailable; no CDN assets, external fonts, server-only imports or fetch calls that require a web server
- Render student names, archive names, source paths, warnings, corrections and stdout/stderr as untrusted text; escape HTML and attributes and avoid injecting strings into executable JavaScript
- Prefer safe DOM text insertion for dynamic labels; if JSON is embedded inside a script element, handle script-closing sequences and context-specific escaping properly
- Test text containing `<script>`, `</script>`, quotes, ampersands, angle brackets and multiline diagnostics, verifying it displays as text and cannot execute
- Preserve every relevant case status: PASSED, FAILED, TIMED_OUT, COMPILE_ERROR, RUNTIME_ERROR, NOT_RUN
- Show configured point maxima and decimal scores accurately; do not hardcode the supplied assessment's totals throughout the renderer
- Avoid presenting review-required identities or withheld grades as clean final results
- Use existing diagnostic records; do not rerun grading just to generate HTML
- Do not invent expected/actual outputs or assertion inputs that are not stored in TestCaseResult. Current adapters generally record only status and output/exception diagnostics, not a structured assertion comparison
- Generated timestamps, grading duration, truncation labels and summary counts should be supported by actual data. Summing case durations can overcount a compilation duration copied into multiple COMPILE_ERROR records
- Consider empty results, long names, no diagnostics, duplicate submissions, missing identity and future nonempty analysis findings without claiming unavailable features
- Keep any output path/enablement setting externalized and consistent with current protection of input files and unique run directories
- Decide how an optional HTML-write failure affects the CLI result without losing existing grading/CSV evidence; do not silently swallow it
- Prefer usable keyboard controls, readable status text rather than color alone, and a minimal representative responsive check

These are planning/verification concerns, not permission to broaden feature 2 into unrelated analysis or reporting bonuses.

## 8. Relevant verification strategy for the next implementation

The current 48-test baseline and successful real run are accepted evidence. Do not rerun them merely to rediscover the same state before any code changes.

After an approved implementation, first use focused tests for rendering/escaping, calculated summaries, export eligibility and generated-file behavior. Add a targeted integration check proving the CLI produces HTML from the completed run without changing CSV scores. If shared reporting contracts change, exercise directly affected consumers. A final clean Maven verification at integration is appropriate.

Because this feature is visual/interactive, perform one targeted browser pass after deterministic checks: open the local HTML, verify it works offline, inspect key content and search/sort/expand behavior that was actually implemented, and check representative layout/console errors. Fix discovered issues and recheck only affected behavior. Delegate routine execution/browser checks when useful; do not spawn agents for tiny mechanical tasks or perform repetitive screenshots for reassurance.

When done, report exact commands and observed evidence, mark feature 2 REVIEW and ask Boey for its clean-build/run confirmation. The previous MVP confirmation does not automatically cover new HTML code. Only after feature 2 confirmation may it be marked DONE and its required architecture document created, including actual OOP principles and Slide Deck Talking Points.

## 9. Brief, rubric and AI disclosure instructions

Boey explicitly asked teammate agents to read the full project brief PDF during onboarding and review relevant highest grade-band rubric descriptors/restrictions before each new implementation plan. Cite real page numbers from the PDF and explain how planned behavior and verification address the criteria. Check allowed dependencies, deliverables, submission requirements and scope. Repository summaries do not replace the source brief. If the PDF cannot be read or materially conflicts with the intended plan, ask before dependent implementation.

The brief is `CS102-Project-Auto Grading System.pdf` in the repository root. The sample assessment PDF under Project is not the project brief. Do not assert a precise rubric quotation or page reference from memory; inspect the actual document in the next chat.

Boey's explicit exception concerns what agents produce automatically: do not insert AI-attribution comments in Java or complete disclosure declarations on the human team's behalf. Humans will review/modify the code and manually add required method attributions and the presentation AI Use appendix before submission. Keep that requirement in the release checklist. It is not waived, and human editing does not remove it. All other brief restrictions still apply.

## 10. Completion checklist for this handoff itself

- Feature 2 is owned by Boey everywhere the ownership board is duplicated
- Feature 2 stays TODO, not IN_PROGRESS or DONE
- Only features 5 and 6 remain unclaimed
- No HTML source code, shared interfaces, configuration or Maven files were changed by this claim/handoff request
- No new build was needed for Markdown-only changes; check Markdown diffs and conflicting ownership text
- No agents are actively editing; this small documentation task was handled directly
- No commit/push performed for this task; the next human action may publish the claim so teammates see the updated availability
- On resuming, inspect the real Git status and use the current live board instead of treating historical text as a lock

## 11. Feature 4 continuation (2026-10-18) — Dynamic parameterized test ingestion, REVIEW

William's feature 4 was committed earlier but was broken in production: `DynamicTestRunner` required four command-line args and called `System.exit(1)` when it received three, so every dynamic case errored (`RUNTIME_ERROR`); Q2a/Q2b/Q3 had no runner cases and placeholder expected outputs. Fixed 2026-10-18.

**Design decision (resolves the instructor-oracle concern):** only `Project/` is supplied to the team, so the team cannot write answer-key adapters/oracles for the instructor's questions. Feature 4 therefore uses **property-based / metamorphic testing** instead of expected-answer comparison. A default-package `DynamicRunner` is compiled alongside the student's code by `StudentCompiler` and invoked via reflection; it generates random inputs (seeded for reproducibility) and asserts a question invariant/property the spec guarantees — never a pre-computed expected answer. This catches "memorized"/hard-coded solutions without the team acting as an oracle.

**What changed:**
- `src/main/resources/autograder/adapters/DynamicRunner.java` (NEW): reflection-based runner covering all five parts — Q1a (isogram filter), Q1b (sum of even integers), Q2a (mean age for a surname, probing only non-fixed surnames), Q2b (top student for a course, probing only non-fixed courses), Q3 (permutation of inputs sorted by descending area). Writes `PASSED`/`FAILED`/`ERROR`.
- `src/main/java/autograder/grading/DynamicTestCaseProvider.java` (REWRITTEN): emits 5 seeded dynamic cases per question with args `[questionId, seed]` and runner `DynamicRunner`; removed broken serialization/expected-output logic.
- `src/main/java/autograder/grading/DynamicTestRunner.java` (DELETED): obsolete packaged runner that could never run on the student subprocess classpath.
- `src/main/java/autograder/execution/StudentCompiler.java` (EDITED): also compiles `DynamicRunner.java` into the student classes directory so the subprocess can load it.
- `src/test/java/autograder/grading/GradingPipelineTest.java` (EDITED): asserts fixed-only Q1a score still 2.0 and that dynamic cases now run (no longer `NOT_RUN`/`COMPILE_ERROR`); 45 results per submission (20 fixed + 25 dynamic).

**Verification:**
- `GradingPipelineTest` both methods PASS (full pipeline compiles and runs all 45 cases end-to-end, including Q3 with reference support classes).
- Standalone harness (`scratch/verify.sh`) confirms for every question and seeds 7/42/99: correct implementations → PASSED, hard-coded "memorized" implementations → FAILED.
- No supplied `Project/` inputs were modified; the oracle-free design is compatible with "only Project/ provided".

**Status:** REVIEW. Not DONE pending human clean-build confirmation (`mvn clean verify` or the fallback Maven command). On DONE, create `dynamic_test_architecture.md` with actual OOP principles and slide talking points per the completion gate.
