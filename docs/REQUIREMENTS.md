# Requirements and evidence

This checklist is based on the seven-page project brief. It separates the accepted preparation foundation from the remaining MVP, whose implementation is verified and accepted through Boey’s successful clean build/run on 2026-10-10. No feature becomes DONE until a team member confirms the documented clean build.

## Marking priorities

The brief gives tentative marks of code 7, application 16, presentation 7. It specifies possible penalties up to 10% for usability/input-validation issues and up to 50% for application Java compilation/run failures. The highest rubric band emphasizes understandable object responsibilities, consistent style, external configuration, well-handled edge cases, intuitive operation, a clear presentation, and accurate team-wide answers.

Bonus credit depends on usefulness, correctness, and design quality; there is no stated per-feature entitlement or bonus ceiling. Keep ordinary loops and readable conditionals. Complexity of implementation is not a goal.

## Core acceptance checklist

| Requirement | Implementation / planned work | Evidence required | State |
| --- | --- | --- | --- |
| Batch intake | SubmissionProcessor | All six supplied archives in one run; corrupt archive does not block the next | Foundation verified; user confirmed |
| Safe extraction | SafeZipExtractor | Traversal, entry/byte limits, checksum corruption, cleanup, repeat runs | Foundation verified; user confirmed |
| Required folders and source names | Assessment configuration and submission validator | Missing Q folder, missing required source, wrong filename case | Foundation verified; user confirmed |
| Correct common layout errors | Working-copy preparation | Direct Q folders and wrong wrappers become canonical copies; originals unchanged | Foundation verified; user confirmed |
| Identify students / missing headers | Roster reconciliation and per-file checks | Missing fields corrected only for a unique owner; conflicting evidence withheld | Foundation verified; user confirmed |
| Duplicate submissions | Batch identity checks | Both duplicates require review; no silent replacement | Foundation verified; user confirmed |
| Compile against instructor testers | StudentCompiler with explicit instructor adapters | Required support classes/fixtures, compile error in one question leaves others usable | DONE; verified and user confirmed |
| Stop non-terminating tests fairly | CaseExecutor and independent child processes | Pass-loop-pass yields two passing cases; timeout kills work and later cases run | DONE; verified and user confirmed |
| Capture diagnostic output | ProcessRunner | stderr and stdout captured with limits; output flood cannot block a run | DONE; verified and user confirmed |
| Compute scores | GradingPipeline and shared result models | Five components have maxima 3,3,5,5,4; independently checked total 20 | DONE; verified and user confirmed |
| Exact supplied CSV format | CSV codec, roster repository and score writer | Preserve column order, raw roster fields and end marker; only supplied scores change | DONE; component/pipeline verified and user confirmed |
| Instructor progress/errors/results | CLI and ProgressListener | Clear stage progress, review status, log location, input error messages | Foundation verified; user confirmed |
| Multiple assessments | External assessment definitions | A second synthetic assessment changes required filenames and scoring without Java changes | Foundation verified; fixed instructor adapters only; arbitrary testers remain out of MVP scope |
| Complete grading run | GradingPipeline and CLI integration | Six supplied submissions plus synthetic failures, independently expected scores | DONE; verified and user confirmed |

## Bonus acceptance targets

| Feature | Owner | Demonstration required before completion |
| --- | --- | --- |
| 1: token similarity | Kiya | Known similar and unrelated pairs; starter-code exclusion; evidence; configurable warning threshold; no automatic accusation |
| 2: offline HTML report | Boey | Opens without a server/network; trace a result to the failed case or anomaly; escape student-controlled content |
| 3: starter-code comparison | Sera | Unchanged starter submission detected; genuine changes distinguished; explain comparison limitations |
| 4: parameterized cases | William | New inputs catch a hard-coded solution; invalid case data gives useful errors; retain intended mark weights |
| 5: Markdown feedback | Unclaimed | Explain compilation failure, timeout, mismatch, and missing files using recorded evidence; no invented diagnosis |
| 6: terminal dashboard | Kai Cong (review) | Progress and cancellation; readable non-ANSI fallback; one display failure cannot stop grading |

The contracts exist before implementations. Bonus code must consume common results rather than re-run grading independently. No bonus implementation begins before the core MVP is tested.

## Submission checklist

- Build from a clean extraction using only the README commands on another team member's machine
- Verify Java/Maven versions and first-build dependency needs; no reliance on a developer's temporary installation
- Package source, pom.xml, configuration, required resources, README, and the GX-TY presentation deck (actual group/section identifier to be supplied by the team)
- Maven is explicitly permitted in place of compile.sh/run.sh and the alternate classes/lib layout
- Keep generated build workspaces, temporary archives, logs, and local caches out of the submission
- Intentionally broken student fixtures are test data, not application source compiled by Maven
- Use free/open-source dependencies only; list choices and licenses
- Human reviewers complete the method-level AI declarations and presentation appendix after reviewing code
- Submit via eLearn Assignments; the brief says email submissions are ignored
- Brief deadline: 11 November, 11:59 PM, Week 13; team should check course announcements for any changes

## Presentation checklist

Prepare 10–15 minutes of presentation/demonstration and 5 minutes of Q&A. Explain architecture/OOP, library choices, algorithms considered with trade-offs, and useful features. Use readable diagrams and fonts. Demonstrate on one prepared machine with a rehearsed dataset and avoid unnecessary app/laptop switching. Every member must explain the whole pipeline, including code written with assistance. Record genuine contributions and cross-member reviews; the brief considers peer and interview evidence.

## Foundation verification record

The final foundation verification passed 30 Maven tests. The packaged preparation command processed six supplied archives with 4 READY, 2 REVIEW_REQUIRED, 0 FAILED and 22 working-copy corrections. All 63 supplied input file hashes were unchanged. David and Xing remain flagged for nonempty source-name headers that differ from the roster; the tool did not overwrite those names. This is the accepted historical foundation baseline. The completed grading verification record follows below.

## Remaining MVP verification

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
