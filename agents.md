# Agent Synchronization

## Session protocol

Ask "Who am I speaking with?" at the start of each new workflow. Validate the answer against Boey, Sera, William, Kai Cong, Kiya, and Gabriel. Read this file before build work and state the current context. Get approval for substantial implementation plans before changing Java code.

## Feature status board

| Feature | Owner | Status |
| --- | --- | --- |
| 1: Token-based plagiarism detection | Unclaimed | TODO |
| 2: Offline HTML dashboard | Unclaimed | TODO |
| 3: Starter code / boilerplate diff detector | Sera (locked) | TODO |
| 4: Dynamic parameterized test ingestion | William (locked) | TODO |
| 5: Markdown student feedback | Unclaimed | TODO |
| 6: ANSI terminal dashboard | Unclaimed | TODO |

Allowed statuses: TODO, IN_PROGRESS, REVIEW, DONE.

## Gatekeeper and ownership

Boey's feature claim is pending. Available features: 1, 2, 5, 6. Ask unassigned members to claim an available feature before feature work. Only Sera may claim or implement feature 3; only William may claim or implement feature 4. No bonus implementation begins until the MVP is functional and tested.

Keep shared configuration, domain models, interfaces, Maven setup, and pipeline integration coordinated by the main agent. Give each bonus implementation its own package after the interfaces are approved. Record file ownership before concurrent edits. Agents must reread this board before claiming work; this file alone does not provide a concurrent locking mechanism.

## Completion gate

Move implemented features to REVIEW. Provide exact Maven commands for compilation and execution of the relevant feature, then explicitly request the user's confirmation of a clean build. Never mark DONE before that confirmation. On marking a bonus DONE, create <feature_name>_architecture.md with implementation details, actual OOP principles used, and Slide Deck Talking Points.

## Current Context

- Date: 2026-10-10 (Asia/Tokyo)
- Validated driver: Boey
- Approved work: revise the foundation after reviewing the seven-page project brief; strengthen Step 1, add shared contracts and roster/CSV components, improve workflow and documentation
- Foundation status: DONE (preparation foundation; Boey supplied successful build/run output); student compilation, per-case execution, end-to-end grading, and bonus implementations remain TODO
- Ownership: delegated implementation complete; main agent reviewed and integrated all changes; no agents are actively editing
- Original ZIPs, supplied testers, starter files, and source score sheet must remain unchanged
- Do not generate AI attribution comments in Java code: Boey explicitly retained that rule on 2026-10-10
- Human team will review/modify code, then manually add the brief-required method attributions and presentation AI-use declarations before submission
- Human declaration completion is a release-checklist item, not permission for agents to add attribution comments
- Bonus ownership remains unchanged; Boey's claim is pending, with 1, 2, 5, 6 available
- No feature is DONE until the user confirms a clean Maven build
- Maven is available temporarily at /tmp/apache-maven-3.9.9/bin/mvn with -Dmaven.repo.local=/tmp/cs102-maven-repository
- Final verification: Maven clean verify passed with 30 tests, zero failures/errors; executable JAR processed all six supplied archives
- Runtime results: 4 READY, 2 REVIEW_REQUIRED (David and Xing: name headers do not corroborate roster), 0 FAILED; 22 working-copy corrections in the final run
- Original-input verification: all 63 recorded SHA256 checksums unchanged; student code has not been executed
- Six bonus interfaces and shared assessment/test/result models exist; the CSV writer is component-tested but not connected to a grading pipeline
- Missing Username is optional; required Name/Email headers may be filled in working copies only when strong identity evidence has one roster match
- Read README.md and docs/REQUIREMENTS.md, docs/ARCHITECTURE.md, docs/TEAM_WORKFLOW.md for current behavior, evidence, remaining work, and human responsibilities
- Exact verification commands: mvn clean verify; java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
- Next: propose the compiler/test-adapter implementation for approval, including fair per-case timeout handling before implementing student execution
- No bonus has been marked DONE; no per-bonus architecture document is due yet
- Boey supplied terminal output confirming BUILD SUCCESS, 30 tests with no failures/errors, and the expected six-submission preparation results on 2026-10-10; build-confirmation gate satisfied for the foundation only
