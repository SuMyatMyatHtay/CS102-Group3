# Coding style and modularisation

Approved by Boey on 2026-10-14 for application code and future agent-generated changes. These rules support the project brief's page 6 design criteria and page 7 robustness/usability criteria. Read this document before planning or editing code. [agents.md](../agents.md) governs identity, ownership, approval and publication; [TEAM_WORKFLOW.md](TEAM_WORKFLOW.md) governs collaboration and human acceptance.

## Responsibilities and dependencies

- Give each class one coherent responsibility that can be explained in a sentence. Keep validation, execution, grading policy, persistence and presentation in clear components
- Keep coordinating classes focused on the sequence of work. Delegate substantial parsing, rendering or file-writing logic to collaborators with explicit inputs and outputs
- Give each method one logical operation and a name that describes its purpose. Keep related steps together when extraction would obscure the flow
- Use composition for collaboration. Introduce interfaces when alternative implementations or substitution have a concrete benefit. Explain any inheritance relationship with an actual use case
- Keep public APIs small. Prefer package-private implementation collaborators and private helpers; preserve existing public contracts unless an approved plan changes them
- Put related implementation classes in the existing feature package. Coordinate shared models, configuration, contracts and pipeline wiring before changing them
- Keep presentation dependent on completed results. Grading and identity decisions belong to their owning services; renderers and feedback generators consume those decisions
- Give shared helpers a cohesive concern, such as HTML escaping or configuration paths, and consistent semantics for their callers
- Use one top-level class per Java file. Small nested records/enums or private implementation types can remain with their owner when useful only there

## Size review guidelines

These are review triggers, not mandatory caps. Existing files are assessed through the approved review and subsequent refactoring plans; exceeding a trigger does not authorise an edit.

| Scope | Review trigger | Counting convention |
| --- | --- | --- |
| Production Java file | More than 300 lines | Total physical lines, including imports, comments and blank lines |
| Java method or constructor | More than 40 lines | Nonblank, noncomment lines from declaration through closing brace; multiline statements count as multiple lines |
| Test files and non-Java assets | No automatic file cap | Review readability, scenario separation and responsibilities |

Java adapter resources are production source when compiled into a student workspace. Embedded text-block content is data; assess a long fixture/template separately rather than treating its contents as Java statements. CSS and JavaScript have no Java-specific numeric cap; review coherent selectors, interactions and functions.

When a trigger is exceeded, inspect responsibility boundaries, nesting, repeated logic, shared mutable state and dependencies. Extract a component when it owns a meaningful concern. A cohesive state machine, immutable data aggregate, constructor or integration scenario may justify an exception.

Record an exception in the review report or PR using this form:

```text
File/method:
Size and counting convention:
Responsibility:
Why keeping it together helps:
Relevant checks and remaining limits:
```

Do not add Java comments just to record size exceptions. Preserve method-level AI attribution for human reviewers.

## Java conventions

- Use the existing Java 17 target and four-space indentation consistently
- Use meaningful names, ordinary loops and clear conditionals when they express the operation directly
- Keep imports explicit and consistently grouped; avoid wildcard imports in new or deliberately edited imports
- Prefer final fields and immutable result/view objects. Copy mutable collections at ownership boundaries and avoid exposing writable internal collections
- Keep local mutation limited to the operation that owns it; return explicit results rather than sharing partially populated mutable state across unrelated services
- Externalise changeable operational policy. Use named constants for fixed protocol/format mechanics where a name clarifies the value
- Preserve decimal scoring semantics. Reporting derives views from recorded points and configured maxima; it does not introduce a second grading policy
- Catch exceptions at the layer that can handle or report them. Retain contextual diagnostics, propagate interruptions and preserve the distinction between student failures and infrastructure failures
- Use try-with-resources for owned streams and other closeable resources
- Keep comments minimal, lowercase and without full stops, following the existing project convention. Explain a necessary reason or constraint rather than restating the code
- Agents must not insert AI-attribution comments or prepare human declarations. The team owns method-level attribution and the presentation AI Use appendix required by the brief

Formatting changes stay within the intended scope of a reviewed edit. A behaviour-preserving extraction must not silently alter validation, defaults, error messages, ordering or exception handling.

## Configuration and file handling

Keep relative paths resolved against the configuration file's directory. Preserve canonicalisation, symlink-aware input protection, overlap checks, extraction limits and safe output filenames. Keep supplied ZIPs, testers, starters, support classes, fixtures, score sheets and references unchanged; corrections belong in separate working copies.

Configuration loaders can delegate parsing and path policy while retaining the current `AppConfig.load(Path)` and getter surface. Changing property names, defaults or public contracts requires an explicit approved plan and coordination with callers.

## Reporting and browser code

Keep HTML self-contained with inline CSS and vanilla JavaScript. Escape all student-controlled text and attributes through one consistent implementation. Keep student data out of executable scripts; use DOM text APIs for display changes.

Use the authoritative withholding map. Exported-only aggregates exclude withheld diagnostic scores, while preparation warnings remain a separate status. Preserve real case statuses, score labels, configured question order, ordinal anchors, row/detail pairing, filters, sorting and question navigation.

Display only recorded evidence. Do not invent expected/actual assertions, absent roster students, analysis results, truncation flags or whole-run timing. CSV persistence and HTML generation retain their existing order and failure behaviour.

## Tests and review evidence

- Organise tests around observable behaviour and failure scenarios. Keep readable fixtures near their tests; share fixture helpers only where they reduce repeated setup without hiding the scenario
- Preserve meaningful public-boundary and integration assertions during extraction. Add focused tests when a new seam exposes a real regression risk; avoid tests that only assert delegation
- Prefer static checks and targeted unit/integration tests. Use DOM/browser checks when changed interactions or rendering require them, subject to tool restrictions
- Run the narrowest useful checks after each affected batch. Use a clean build and representative sample run at the final integration boundary for substantial Java refactoring
- Compare semantic results across separate runs: scores, decisions, case statuses and required fields. Paths, durations and run IDs can differ. Byte-identical CSV assertions apply to the same completed run before/after HTML rendering
- Report commands actually run, results and limits. Source inspection, a mock DOM fixture, a real browser pass and human acceptance are different evidence
- Keep correctness findings separate from behaviour-preserving refactoring. Record trigger, location, consequence and confidence; seek approval before fixing them

## Agent and PR checklist

Before editing, confirm the current branch/owner, inspect local work, read relevant code/tests and propose a concise plan for substantial changes. Each new task uses a descriptive branch without a `codex/` prefix, such as `code-modularisation` or `kiya-feature-1-plagiarism`; preserve existing branches until their owner authorises cleanup.

Main retains architecture, shared decisions and integration. Delegate bounded useful work under the current Agent Workflow, give agents explicit file ownership, use at most two concurrent subagents and prohibit subagent spawning. Inspect delegated findings and diffs before integration.

Before publication, review responsibilities, size exceptions, API compatibility, preserved behaviour, actual test evidence and documentation. Commit only intended files, excluding supplied inputs, generated output, local handoffs and unrelated metadata. Publish approved reviewed work through a PR into `main`, give the driver its URL and ask them to contact the group. Teammate messages and merging require explicit authorisation.

The current review is recorded in [CODE_REVIEW.md](CODE_REVIEW.md). Boey directly approved the M1-M5 behaviour-preserving implementation; its code and checks are recorded there. Further substantial recommendations require their own plan and approval. These guidelines do not approve optional bonuses or broad rewrites.
