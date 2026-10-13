# Agent Synchronization

## Session protocol

At the start of each new workflow, read this file, README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md, docs/TEAM_WORKFLOW.md and the project brief `CS102-Project-Auto Grading System.pdf`. Read-only context gathering and a concise project/bonus summary may precede identity confirmation. Show all six bonus features with their purpose, current owner, status and availability from this board, then ask:

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
| 2: Offline HTML dashboard | Boey | TODO |
| 3: Starter code / boilerplate diff detector | Sera (locked) | TODO |
| 4: Dynamic parameterized test ingestion | William (locked) | TODO |
| 5: Markdown student feedback | Unclaimed | TODO |
| 6: ANSI terminal dashboard | Kai Cong | REVIEW |

Allowed statuses: TODO, IN_PROGRESS, REVIEW, DONE.

## Gatekeeper and ownership

Boey has claimed feature 2 (offline HTML dashboard); implementation has not started. Kai Cong has claimed feature 6; its branch is awaiting team review and human build confirmation. Feature 5 remains available. Only Sera may claim or implement feature 3; only William may claim or implement feature 4. No bonus implementation begins until the MVP is functional and tested.

Keep shared configuration, domain models, interfaces, Maven setup, and pipeline integration coordinated by the main agent. Give each bonus implementation its own package after the interfaces are approved. Record file ownership before concurrent edits. Agents must reread this board before claiming work; this file alone does not provide a concurrent locking mechanism.

## Completion gate

Move implemented features to REVIEW. Provide exact Maven commands for compilation and execution of the relevant feature, then explicitly request the user's confirmation of a clean build. Never mark DONE before that confirmation. On marking a bonus DONE, create <feature_name>_architecture.md with implementation details, actual OOP principles used, and Slide Deck Talking Points.

## Current Context

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
- As of 2026-10-10, all six bonuses were TODO; later status updates are recorded below and in the feature board
- No bonus is DONE; no per-bonus architecture document is due
- Maven is temporarily available at /tmp/apache-maven-3.9.9/bin/mvn with -Dmaven.repo.local=/tmp/cs102-maven-repository
- Exact commands: mvn clean verify; java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
- Fallback build command: /tmp/apache-maven-3.9.9/bin/mvn -Dmaven.repo.local=/tmp/cs102-maven-repository clean verify
- Next: onboard teammates using the README kickoff, confirm canonical identity and bonus ownership, then seek approval for the selected feature plan; do not repeat the accepted MVP build-confirmation gate
- Read README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md and docs/TEAM_WORKFLOW.md for detailed behavior and responsibilities
- Boey approved reconciling remote history and publishing the current code on 2026-10-10; pre-existing unrelated .DS_Store change preserved

- Documentation onboarding update approved by Boey: kickoff placed first in README; identity prompts list every group member; read-only context/summary may precede identity confirmation
- Boey-confirmed run reports: build/grading/run-9005748573100915500/{scorereport,cases,submissions}.csv

- Remote September work-division assignment preserved: Kiya owns feature 1; WORK_DIVISION.md now maps proposals to current contracts

- Boey claimed feature 2 (offline HTML dashboard) on 2026-10-10; owner recorded, status TODO because implementation has not started
- Current authorized work is claim documentation and continuation handoff only; feature 2 implementation plan still needs explicit approval
- Next chat: read this board, README.md and the project docs; confirm the driver using the full member list, inspect relevant reporting contracts and the PDF rubric, then propose a concise feature 2 plan for approval

- 2026-10-14: Kai Cong identified himself as feature 6 owner and requested work on branch `kaicong/6-terminal-dashboard`
- Feature 6 adds a `ProgressListener` implementation, intake/grading/reporting progress events, plain-text fallback, and interactive q + Enter cancellation; branch status REVIEW pending team review and human build confirmation
