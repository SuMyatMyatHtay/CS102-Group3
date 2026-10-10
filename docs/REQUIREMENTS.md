# Requirements and evidence

This checklist is based on the seven-page project brief. It distinguishes implemented foundation work from the complete grader still to be built. Passing foundation tests does not establish end-to-end grading correctness. No feature becomes DONE until a team member confirms the documented clean build.

## Marking priorities

The brief gives tentative marks of code 7, application 16, presentation 7. It specifies possible penalties up to 10% for usability/input-validation issues and up to 50% for application Java compilation/run failures. The highest rubric band emphasizes understandable object responsibilities, consistent style, external configuration, well-handled edge cases, intuitive operation, a clear presentation, and accurate team-wide answers.

Bonus credit depends on usefulness, correctness, and design quality; there is no stated per-feature entitlement or bonus ceiling. Keep ordinary loops and readable conditionals. Complexity of implementation is not a goal.

## Core acceptance checklist

| Requirement | Implementation / planned work | Evidence required | State |
| --- | --- | --- | --- |
| Batch intake | SubmissionProcessor | All six supplied archives in one run; corrupt archive does not block the next | Foundation review |
| Safe extraction | SafeZipExtractor | Traversal, entry/byte limits, checksum corruption, cleanup, repeat runs | Foundation review |
| Required folders and source names | Assessment configuration and submission validator | Missing Q folder, missing required source, wrong filename case | Foundation review |
| Correct common layout errors | Working-copy preparation | Direct Q folders and wrong wrappers become canonical copies; originals unchanged | Foundation review |
| Identify students / missing headers | Roster reconciliation and per-file checks | Missing fields corrected only for a unique owner; conflicting evidence withheld | Foundation review |
| Duplicate submissions | Batch identity checks | Both duplicates require review; no silent replacement | Foundation review |
| Compile against instructor testers | Separate compiler service, not implemented | Required support classes/fixtures, compile error in one question leaves others usable | TODO |
| Stop non-terminating tests fairly | Per-case execution adapter and child processes, not implemented | Pass-loop-pass yields two passing cases; timeout kills work and later cases run | TODO |
| Capture diagnostic output | Process runner, not implemented | stderr and stdout captured with limits; output flood cannot block a run | TODO |
| Compute scores | Shared result models exist; execution/integration pending | Five components have maxima 3,3,5,5,4; independently checked total 20 | TODO |
| Exact supplied CSV format | CSV codec, roster repository and score writer | Preserve column order, raw roster fields and end marker; only supplied scores change | Component review; pipeline TODO |
| Instructor progress/errors/results | CLI and ProgressListener | Clear stage progress, review status, log location, input error messages | Foundation review |
| Multiple assessments | External assessment definitions | A second synthetic assessment changes required filenames and scoring without Java changes | Foundation review; full grading TODO |
| Complete grading run | Wire compilation, execution, scoring and CSV writer | Six supplied submissions plus synthetic failures, independently expected scores | TODO |

## Bonus acceptance targets

| Feature | Owner | Demonstration required before completion |
| --- | --- | --- |
| 1: token similarity | Unclaimed | Known similar and unrelated pairs; starter-code exclusion; evidence; configurable warning threshold; no automatic accusation |
| 2: offline HTML report | Unclaimed | Opens without a server/network; trace a result to the failed case or anomaly; escape student-controlled content |
| 3: starter-code comparison | Sera | Unchanged starter submission detected; genuine changes distinguished; explain comparison limitations |
| 4: parameterized cases | William | New inputs catch a hard-coded solution; invalid case data gives useful errors; retain intended mark weights |
| 5: Markdown feedback | Unclaimed | Explain compilation failure, timeout, mismatch, and missing files using recorded evidence; no invented diagnosis |
| 6: terminal dashboard | Unclaimed | Progress and cancellation; readable non-ANSI fallback; one display failure cannot stop grading |

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

The final foundation verification passed 30 Maven tests. The packaged preparation command processed six supplied archives with 4 READY, 2 REVIEW_REQUIRED, 0 FAILED and 22 working-copy corrections. All 63 supplied input file hashes were unchanged. David and Xing remain flagged for nonempty source-name headers that differ from the roster; the tool did not overwrite those names. Tests do not yet prove real compilation, process timeouts, or complete report export from student execution. Those checklist rows remain TODO.
