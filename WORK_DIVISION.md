# Bonus work division

Updated 2026-10-10 after reconciling the September planning commits with the completed MVP. The original proposals remain available in Git history. This document describes the current integration points; algorithms and implementation details still require an approved feature plan.

[agents.md](agents.md) is the authoritative ownership/status board. Start with the [README kickoff](README.md#start-here-teammate-agent-kickoff), confirm identity and coordinate claims before editing. The core MVP is DONE following Boey's successful 48-test build and grading run. Kai Cong's feature 6 branch is in review; consult the board and open branches for the other bonuses.

| Feature | Owner | Goal | Existing contract in autograder.api |
| --- | --- | --- | --- |
| 1: Token-based plagiarism detection | Kiya | Compare normalized Java tokens and report similarity evidence without automatically accusing students | PlagiarismDetector.compare(submissions, assessment) returns similarity findings |
| 2: Offline HTML dashboard | Boey | Present cohort scores, filters and expandable diagnostics in a self-contained offline report | HtmlReportGenerator.generate(batch, outputDirectory) returns a report path |
| 3: Starter-code comparison | Sera (locked) | Compare answers with starter files and report unchanged or minimally changed implementations | BoilerplateDetector.analyze(submission, starterDirectory) returns analysis findings |
| 4: Parameterized test ingestion | William (locked) | Load additional varied inputs and expected outcomes through an approved case format | TestCaseProvider.load(assessment) returns case definitions |
| 5: Markdown student feedback | Unclaimed | Explain scores and failure reasons in individual student reports | FeedbackGenerator.generate(grade, outputDirectory) returns a report path |
| 6: ANSI terminal dashboard | Kai Cong (review) | Display grading progress with a readable plain-text fallback and interactive cancellation | ProgressListener.onProgress(stage, item, completed, total) |

Kiya's feature 1 assignment is preserved from the remote September work-division document. Boey has claimed feature 2. Kai Cong has claimed feature 6. Feature 5 remains unclaimed. Coordinate any reassignment rather than overwriting a teammate's claim.

## Implementation boundaries

- Use the existing contracts under src/main/java/autograder/api and shared models under autograder.model; do not recreate the older proposed interfaces or restore the legacy src/autograder tree
- Give each bonus its own implementation package after the plan/interfaces are approved; coordinate shared configuration, models and GradingPipeline integration with the main agent
- Token normalization, n-grams/winnowing, starter normalization and configurable similarity/edit thresholds are candidate approaches, not already implemented requirements
- HTML must work offline and escape student-controlled text; feedback should use recorded outcomes and avoid inventing expected/actual values absent from the case data
- Parameterized cases need an explicit plan for runner generation, compiler integration, result validation and mark weights; TestCaseDefinition alone does not provide these. Preserve original testers and the explicit result-file protocol; do not award marks from printed score markers
- Terminal rendering should tolerate unsupported terminals and display failures. Richer stage events require coordinated pipeline changes; the current ProgressListener is intentionally small
- Externalize operational settings, preserve supplied inputs and review/withholding policy, and consume shared results rather than rerunning grading for reports
- Obtain approval before substantial implementation. Move implemented work to REVIEW, provide exact build/run commands, and wait for human clean-build confirmation before DONE
- When a bonus becomes DONE, create its required architecture document with actual OOP principles and slide talking points

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) and [docs/TEAM_WORKFLOW.md](docs/TEAM_WORKFLOW.md) for the current design, acceptance criteria and human-owned submission responsibilities.
