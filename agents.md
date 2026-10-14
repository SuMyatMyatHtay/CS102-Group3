# Agent Synchronization

## Session protocol

At the start of each new workflow, read this file, README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md, docs/TEAM_WORKFLOW.md, docs/CODING_STYLE.md and the project brief `CS102-Project-Auto Grading System.pdf`. Read-only context gathering and a concise project/bonus summary may precede identity confirmation. Show all six bonus features with their purpose, current owner, status and availability from this board, then ask:

"Who am I speaking with? Please choose your recorded team name: Boey, Sera, William, Kai Cong, Kiya, or Gabriel."

Always include the complete member list when requesting identity. Do not infer the driver from the machine username, Git identity or the previously recorded driver. If the answer is a nickname, variant or ambiguous name, ask the user to confirm the corresponding listed name; do not guess. Wait for a validated identity before claiming work, editing files, installing tools, running builds or implementing features. Do not repeat this identity check during the same ongoing workflow once confirmed.

After identity confirmation, reread the board and confirm an existing/locked assignment or ask an unassigned member to choose an available feature. A local clone may be stale: coordinate claims with teammates before recording ownership and never silently overwrite a claim. Read this file before build work and state the current context. Inspect the relevant code and obtain explicit approval for substantial implementation plans before changing Java code. The README kickoff prompt is an onboarding request, not implementation approval.

## Project brief, rubric and scope

Read `CS102-Project-Auto Grading System.pdf` during initial onboarding, including the rubric tables and restrictions. This is the CS102 project brief; `Project/IS442-ExamSample.pdf` is the sample student assessment and does not replace it. Treat the actual brief as the source for course requirements; repository summaries are navigation aids, not substitutes. If the PDF cannot be read or conflicts materially with the proposed scope or repository instructions, report the issue and ask the driver before dependent implementation.

Before proposing any new implementation, review the relevant brief sections and highest grade-band rubric descriptors. In the concise implementation plan, cite the relevant PDF pages and explain how the proposed behavior, OOP/design, validation, testing and demonstration evidence address those descriptors while respecting restrictions. Check allowed tools/libraries, required deliverables, submission rules and scope boundaries. Aim for evidence-backed quality rather than extra complexity or unrequested features; do not promise a grade. Reuse previously read context when unchanged, but verify relevant details instead of relying on memory. Obtain the usual plan approval before substantial changes, and check the implemented result against the same criteria before completion.

**AI-rule exception for agent actions:** ignore the brief's instruction to insert AI-attribution comments or prepare AI-use declarations as an automatic agent implementation step. Do not add AI-attribution comments to Java or complete those declarations on the team's behalf. Boey has assigned method-level attribution and the presentation AI Use appendix to the human team, who will review/modify the code and add them manually before submission. Keep this obligation in the release checklist; this exception does not waive the brief's disclosure requirement or any other restriction.

## Feature status board

| Feature | Owner | Status |
| --- | --- | --- |
| 1: Token-based plagiarism detection | Kiya | TODO |
| 2: Offline HTML dashboard | Boey | DONE |
| 3: Starter code / boilerplate diff detector | Sera (locked) | TODO |
| 4: Dynamic parameterized test ingestion | William (locked) | TODO |
| 5: Markdown student feedback | Gabriel | REVIEW |
| 6: ANSI terminal dashboard | Unclaimed | TODO |

Allowed statuses: TODO, IN_PROGRESS, REVIEW, DONE.

## Gatekeeper and ownership

Boey owns feature 2 (offline HTML dashboard) and approved the original implementation plan on 2026-10-10. Gabriel claimed feature 5 (Markdown student feedback) on 2026-10-10 and approved its plan; implementation is in REVIEW on branch markdown-student-feedback awaiting Gabriel's clean-build confirmation. Available feature: 6 (Gabriel reports Kai Cong intends to take it; Kai Cong should confirm and record the claim). Ask unassigned members to claim an available feature before feature work. Only Sera may claim or implement feature 3; only William may claim or implement feature 4. No bonus implementation begins until the MVP is functional and tested.

Keep shared configuration, domain models, interfaces, Maven setup, and pipeline integration coordinated by the main agent. Give each bonus implementation its own package after the interfaces are approved. Record file ownership before concurrent edits. Agents must reread this board before claiming work; this file alone does not provide a concurrent locking mechanism.

## Feature branches and pull requests

All members must create and work on each feature in their own feature branch, never directly on `main`. Start new branches from the latest `origin/main` after checking and preserving local changes. Use short, descriptive names without the `codex/` prefix, for example `code-modularisation` or `kiya-feature-1-plagiarism`. Use one branch per feature or focused follow-up task; existing feature branches such as Boey's `offline-html-dashboard` may be reused. Branches isolate edits but do not replace ownership coordination for shared files.

After approved implementation and proportionate verification, commit and push only the feature's reviewed files to its branch and create a pull request with that branch as the head and `main` as the base. This workflow authorizes those publication steps for approved feature work; it does not authorize merging into `main`, force-pushing, discarding local work or publishing unrelated changes. Check for an existing PR first and update it rather than creating a duplicate. Use a draft PR when required verification or human acceptance is still pending.

Include the behavior change, scope, exact build/run commands, actual test evidence, limitations and remaining checks in the PR. Immediately tell the driver that the PR was created, provide its URL, and ask them to contact the group to review it. A teammate may use an AI assistant to help review, but a human teammate owns acceptance. Agents must not message teammates, request reviews from named accounts or merge the PR without explicit authorization. Address review feedback on the same branch and PR; resolve conflicts with the owner while preserving shared changes. See [docs/TEAM_WORKFLOW.md](docs/TEAM_WORKFLOW.md#feature-branches-and-pull-requests) for the full sequence.

## Completion gate

Move implemented features to REVIEW. Provide exact Maven commands for compilation and execution of the relevant feature, then explicitly request the user's confirmation of a clean build. Never mark DONE before that confirmation. On marking a bonus DONE, create <feature_name>_architecture.md with implementation details, actual OOP principles used, and Slide Deck Talking Points.

Opening a PR does not mark a feature DONE or merge it. Before merging, require the documented checks, human build/behavior acceptance and review by another teammate. Existing accepted work remains DONE while a separate enhancement can remain REVIEW. Recommend protecting `main` with a required approving review and required build checks when available; changing repository protection settings requires explicit authorization.

## Coding style and code review

Before generating or editing application code, read [docs/CODING_STYLE.md](docs/CODING_STYLE.md). Keep each class responsible for one coherent concern and each method focused on one logical operation. Production Java files above 300 total lines and methods/constructors above 40 nonblank, noncomment lines require a responsibility review, not an automatic split. Record justified exceptions in the PR or review report. Preserve existing public contracts unless an approved plan explicitly changes them; prefer composition and package-private implementation collaborators.

Use [docs/CODE_REVIEW.md](docs/CODE_REVIEW.md) for the merged-main review, proposed extraction boundaries, known evidence limits and separately listed correctness findings. A review recommendation is not implementation approval. Behaviour-preserving refactoring must retain grading, identity, input protection, CSV and offline-report semantics; newly discovered bugs require separate approval. Do not split supplied assessment files or introduce tooling/dependencies simply to enforce line counts.

## Current Context

- Date: 2026-10-14 (Asia/Singapore); validated driver: Boey in this ongoing workflow
- Boey approved a merged-main code review and coding-style/documentation phase, using the branch `code-modularisation` without a `codex/` prefix. Review began at `a99190f`; main advanced while it was in progress, so PR #3's accepted merge was preserved and the review baseline extended to `c769fa93fc25b28b2a8b1af6806949e72de97979`
- Approved changes in this phase are Markdown guidelines, the review report and their onboarding/workflow links. Java, tests, browser assets, configuration and Maven changes require the subsequent concrete implementation plan and Boey's explicit approval
- Main owns all documentation edits and final architecture/integration decisions. Bounded read-only agents cover reporting/configuration/CSV and execution/submission/adapters; they do not edit files or spawn agents. Shared contracts/models/application/pipeline/build settings are reviewed by main
- PR #2 was accepted by Boey and externally merged on 2026-10-14. The original MVP/dashboard remain accepted DONE. Heatmap implementation and recorded agent verification are complete and included in that merge; a separate new human heatmap build/display transcript remains uncaptured. Historical REVIEW evidence is retained without inventing acceptance observations
- PR #3 merged Gabriel's Markdown feedback into main during this review. Preserve its recorded Gabriel/REVIEW ownership and uncaptured human completion gate. Feature 6 remains unclaimed with Kai Cong's intent recorded; coordinate before claiming it. This review does not implement or claim bonuses
- The merged upstream record reports a Windows run with 80/81 tests passing and an InvalidPathException in the existing heatmap test fixture. The review lists that portability defect and the case-variant CSV-name collision separately; neither fix is approved or implemented in this phase
- Preserve supplied inputs, unrelated `.DS_Store` changes and the single ignored `BOEY_FEATURE2_HANDOFF.local.md`. Shared `HANDOFF.md` stays absent. Reviewed Markdown work follows the standing feature-branch/PR publication policy; no merge or teammate message is authorised

### Historical PR creation context (before the accepted merge)

- Date: 2026-10-14 (Asia/Singapore); validated driver: Boey in this ongoing workflow
- Boey requested the branch/PR workflow above and creation of an `offline-html-dashboard` -> `main` PR for group review. Committing/pushing this workflow update and opening the PR are authorized; merging into `main` is not
- Before this update, the dashboard and heatmap were already published on `offline-html-dashboard` at 6efe86b. Remote main was checked at a8d10d7; it contains the earlier handoff cleanup/Windows test fix and a terminal-dashboard change followed by its revert. Neither dashboard nor heatmap is on main
- The original MVP/dashboard remain accepted DONE; the heatmap remains REVIEW pending its human clean-build and local display/interaction acceptance. The PR is to be opened as a draft with those remaining checks recorded
- Preserve local .DS_Store bytes and the single ignored BOEY_FEATURE2_HANDOFF.local.md; do not recreate shared HANDOFF.md. Publication does not authorize attention overview, score distribution, extra feature claims or teammate messages. Gabriel's reported feature-5 branch/claim remains unchecked

## Historical context through heatmap implementation (2026-10-12)

The authorization and Git snapshots below describe earlier sessions. The current context and branch/PR policy above supersede their publication restrictions; historical verification and human acceptance are retained.

- Heatmap revision: Boey approved the detailed plan on 2026-10-12 with “ok lets do it approved”; implementation is REVIEW on offline-html-dashboard, awaiting his clean-build and local display/interaction confirmation for this revision. The original MVP/dashboard remain accepted DONE. Attention overview and score distribution remain outside this approval
- Heatmap file ownership: main integrated pipeline assertions, shared documentation and final review; bounded Luna completed report/html/QuestionOutcome.java, OfflineHtmlReportGenerator.java, dashboard.css, dashboard.js and their focused tests/verification. No agents are actively editing. Shared models/configuration/pipeline contracts are unchanged; branch synchronization and publication remain unauthorized
- Heatmap verification: focused tests 14/14; clean verify 67 tests with zero failures/errors/skips; actual-script DOM API fixture 2/2 (not a real browser pass); sample run scores 12,10,10,0,20,1 with six submissions and 120 cases. All 30 question links resolve to unique headings and recorded case evidence is retained exactly once. Existing pipeline tests preserve all three CSVs byte-for-byte after rendering
- Heatmap report: build/grading/run-3551798997316566219/dashboard.html. All 67 Project file hashes and .gitignore match the new baseline; HANDOFF.md remains absent. .DS_Store changed from its baseline hash during verification without an intentional agent edit; current bytes were left untouched and its concurrent source is unknown
- Heatmap limits: no real-browser, Java 17 runtime or Windows verification; supported DOM fixture and numeric color checks passed. Local main's CRLF fixture fix remains intentionally absent from this branch. No commit/push/merge/fetch was performed; HEAD remains 820394e and staging is empty
- Heatmap review commands: mvn clean verify; java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties. Boey's available fallback is /tmp/apache-maven-3.9.9/bin/mvn -Dmaven.repo.local=/tmp/cs102-maven-repository clean verify. Next: Boey confirms the new revision's clean build/run and checks the displayed heatmap and question navigation
- Preserve the existing unstaged handoff-cleanup mirrors, deleted HANDOFF.md and unrelated .DS_Store; BOEY_FEATURE2_HANDOFF.local.md remains the single ignored local handoff. Gabriel's reported feature-5 branch/claim remains unchecked
- Feature 2: Boey approved the original six-step plan with “Ok lets implement the original plan”; optional heatmap, attention overview and score distribution are outside this implementation
- Active branch: offline-html-dashboard; no commit or push is authorized for feature implementation
- File ownership for feature 2: main integrated the reporting context/API, HTML configuration, App integration, their tests and shared documentation; renderer subagent completed autograder/report/html, autograder/html resources and renderer tests; integration subagent completed GradingPipelineTest HTML/CSV checks and targeted runtime verification; no agents are actively editing
- Keep the existing CSV and withholding policy; HTML is generated afterward from the completed run and authoritative withheld map
- Feature 2 completion gate satisfied: Boey supplied the clean build/run and accepted the displayed dashboard with “ok looks good” on 2026-10-10
- Feature 2 architecture document: offline_html_dashboard_architecture.md (implementation, actual OOP principles and Slide Deck Talking Points)
- Feature 2 verification: focused tests 15/15; clean verify 59 tests with zero failures/errors/skips; packaged grading run retained scores 12,10,10,0,20,1 and produced 120 case records plus dashboard.html
- Feature 2 report: build/grading/run-15042814915555924988/dashboard.html; all 64 files in /tmp/cs102-feature2-input-hashes.json remained unchanged; git diff --check passed
- Automated browser verification was unavailable: isolated Chrome aborted (SIGABRT), and CUA rejected file:// navigation under its browser security policy (http/https only; workarounds prohibited); Boey subsequently viewed and accepted the dashboard; no automated browser pass is claimed
- Boey provided BUILD SUCCESS on 2026-10-10: 59 tests with zero failures/errors/skips and all six expected sample scores; the feature 2 human clean-build/run gate is satisfied
- Boey's latest report: build/grading/run-1332949390012507172/dashboard.html
- Next: Boey reviews and commits the feature on offline-html-dashboard; a requested commit message does not authorize the agent to commit or push. Other bonus ownership and human release declarations remain as recorded

- Date: 2026-10-10 (Asia/Tokyo)
- Validated driver: Boey; identity reconfirmed in this continuation
- Approved work: Boey approved the remaining MVP plan with “Approved lets go”; the accepted MVP behavior and verification are summarized in README.md and the project docs
- Foundation: DONE; Boey previously confirmed 30 tests and successful preparation run
- Remaining MVP: DONE; Boey supplied BUILD SUCCESS, 48 tests with zero failures/errors/skips, and the expected six-submission grading output on 2026-10-10
- Implemented: five explicit per-case instructor adapters, independent compilation, bounded per-case child processes, score aggregation, CSV export and per-case/export-decision diagnostics
- Fixed adapters cover the supplied 20 cases; edited testers require reviewed adapters; original-content validation accepts line-ending differences
- Missing answers/companions are compilation failures for that part only; timeouts preserve other case marks
- Unresolved identities, duplicate submissions, unusable preparations and infrastructure errors withhold export; name warnings alone do not
- Withheld/absent rows retain their template numerator; submissions.csv records decisions and warnings
- Final verification: clean verify passed 48 tests, zero failures/errors; after final diagnostic/formatting fixes, affected execution tests passed 7/7 and the JAR was rebuilt
- Real run: six archives graded; 120 case records, 20 per submission; all exported numerators reconcile with case points
- Scores: Chee 12, David 10, Fen 10, Jing 0, Ping 20, Xing 1
- Xing: Q2a missing-file case passes; four other Q2a cases time out due to a non-advancing scanner loop; remaining cases fail
- groundtruth.txt remains internally inconsistent for Xing (components sum to 15, stated total 1); actual execution independently yields 1; references unchanged
- Preparation still reports 4 READY, 2 REVIEW_REQUIRED (David/Xing name warnings), 0 FAILED; warnings retained in reports
- All 67 supplied-file hashes from the new pre-implementation snapshot remain unchanged
- Reports: build/grading/run-10952970798376735826/{scorereport,cases,submissions}.csv
- No OS security sandbox is claimed; current-user privileges remain, descendant cleanup is best effort; stronger isolation must be external
- Verification used JDK 25 with Java 17 compiler target; actual Java 17 runtime and Windows were not tested
- Ownership: main integrated configuration, pipeline, CLI and docs; delegated adapters/provider/tests and process services/tests are complete; no agents actively editing
- Original ZIPs, instructor tests, starter files and source score sheet remain unchanged
- Do not add AI-attribution comments to Java; Boey retained that rule and humans own review/modification and brief-required declarations before submission
- Feature 2 is DONE after Boey's clean-build/run and dashboard acceptance; Gabriel owns feature 5 (REVIEW); other bonuses remain TODO; Kiya owns feature 1, Sera owns feature 3 and William feature 4; available feature 6
- Feature 2 architecture document is complete; no other per-bonus architecture document is due
- Maven is temporarily available at /tmp/apache-maven-3.9.9/bin/mvn with -Dmaven.repo.local=/tmp/cs102-maven-repository
- Exact commands: mvn clean verify; java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
- Fallback build command: /tmp/apache-maven-3.9.9/bin/mvn -Dmaven.repo.local=/tmp/cs102-maven-repository clean verify
- Next: onboard teammates using the README kickoff, confirm canonical identity and bonus ownership, then seek approval for the selected feature plan; do not repeat the accepted MVP build-confirmation gate
- Read README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md and docs/TEAM_WORKFLOW.md for detailed behavior and responsibilities
- Boey approved reconciling remote history and publishing the current code on 2026-10-10; pre-existing unrelated .DS_Store change preserved

- Documentation onboarding update approved by Boey: kickoff placed first in README; identity prompts list every group member; read-only context/summary may precede identity confirmation
- Boey-confirmed run reports: build/grading/run-9005748573100915500/{scorereport,cases,submissions}.csv

- Remote September work-division assignment preserved: Kiya owns feature 1; WORK_DIVISION.md now maps proposals to current contracts

### Earlier onboarding snapshot before dashboard implementation

The following three entries predate the accepted dashboard and approved heatmap; they do not describe the current feature status.

- Boey claimed feature 2 (offline HTML dashboard) on 2026-10-10; owner recorded, status TODO because implementation has not started
- Current authorized work is claim documentation and continuation handoff only; feature 2 implementation plan still needs explicit approval
- Gabriel (validated driver for this workflow) claimed feature 5 on 2026-10-10 after coordinating with Kai Cong, who is expected to claim feature 6; Gabriel approved the feature 5 plan: MarkdownFeedbackGenerator in autograder.report.feedback implementing the unchanged FeedbackGenerator contract, FeedbackConfig with grading.feedback.* settings, and a small App hook; App/config wiring must be reconciled with the unmerged offline-html-dashboard branch
- Feature 5 implemented on branch markdown-student-feedback: agent-side mvn clean verify passed 62 tests, zero failures/errors/skips; packaged run wrote 6 feedback files under build/grading/run-*/feedback whose totals match scorereport.csv (12, 10, 10, 0, 20, 1); awaiting Gabriel's own clean-build confirmation before DONE
- 2026-10-14: Gabriel had main (including the merged offline HTML dashboard) merged into markdown-student-feedback; App runs the HTML dashboard then feedback and returns nonzero if either fails; config keeps both grading.html.* and grading.feedback.*. Windows 11 (Temurin 25.0.4, Maven 3.9.16): 80 of 81 tests passed; OfflineHtmlReportGeneratorTest.linksQuestionScoresToGroupedEvidenceAndKeepsUnmatchedCasesVisible errors on Windows only (InvalidPathException for '<' in a test tester filename; dashboard test code identical to main, flagged for Boey). Packaged run wrote dashboard.html and 6 feedback files; totals match scorereport.csv (12, 10, 10, 0, 20, 1)
- Next chat: read this board, README.md and the project docs; confirm the driver using the full member list, inspect relevant reporting contracts and the PDF rubric, then propose a concise feature 2 plan for approval
