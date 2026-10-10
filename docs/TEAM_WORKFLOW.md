# Team workflow

## Starting work

Use the kickoff prompt at the start of README.md. Read the shared context documents listed there and `CS102-Project-Auto Grading System.pdf` (including rubric tables and restrictions), then summarize the MVP and live bonus board, then ask the driver to choose from Boey, Sera, William, Kai Cong, Kiya, and Gabriel. Confirm nickname/variant answers against the list; do not infer identity. Read-only onboarding may precede identity confirmation, but claims, edits, installs, builds and implementation must wait. Follow agents.md for the full session protocol. Sera owns feature 3 and William owns feature 4; those assignments are locked. Kiya owns feature 1. Boey owns feature 2. Gabriel owns feature 5. Kai Cong must claim feature 6 before bonus implementation. Shared MVP work can proceed while claims are pending.

For substantial changes, present the intended behavior and affected files and wait for the driver's approval. Boey approved the remaining MVP plan on 2026-10-10: explicit instructor adapters, isolated compilation, bounded per-case processes, scoring/CSV integration and verification. This does not authorize bonus implementation. Keep supplied materials intact.

Before a new implementation plan, review the relevant highest grade-band rubric descriptors and restrictions in the project brief. Cite PDF pages and connect the planned behavior and verification to those criteria without expanding scope unnecessarily. Follow the project-brief policy in agents.md, including its explicit exception for agent-written AI attribution; human disclosure duties remain on the release checklist.

## Avoiding conflicting edits

Each task declares owned files before editing. Feature implementations go under separate feature packages. Shared models, config, interfaces, Maven setup, and pipeline wiring require coordinated review by the main agent and human team. Read the latest shared branch before claiming work. A Markdown board is a record, not a lock: communicate claims and do not assume separate clones synchronize automatically.

Use focused branches such as codex/feature-name and review small changes. Do not create extra user-facing Codex chats without an explicit request. Subagents receive bounded work and may not spawn more agents.

## Review and completion

1. Explain the change in plain language, including its inputs, outputs, and failure behavior
2. Run the narrow tests appropriate to the change, then a clean build at integration/release boundaries
3. Have a human review and, when needed, modify the code
4. Ask another member to explain the change back, including failure cases and design trade-offs
5. Record the exact build/run commands and ask the driver to confirm an error-free build
6. Move a feature to DONE only after that confirmation
7. For a completed bonus, create its architecture document with implementation, actual OOP principles, and slide talking points

## AI declarations: human-owned release task

Boey explicitly instructed agents not to write Java comments mentioning AI. This remains in force. Agents may generate and revise code within approved scope, but must not insert attribution comments.

After human-led review and modification, the team will diligently add the method-level declarations required by page 3 of the brief and prepare the presentation AI Use appendix. Keep accurate records of tools, major prompts, and architectural decisions as work proceeds. Do not claim that human editing removes the need for declarations. Completing these declarations is a final submission gate, not an agent coding action.

## Presentation preparation

All six members should understand the whole project, not just their feature. Keep explanations alongside implementation and rehearse the same end-to-end demo. Discuss why the chosen algorithm is adequate and what it cannot establish. Do not add inheritance or libraries solely to name an OOP principle; demonstrate a real benefit. Interfaces support abstraction and alternative implementations; data classes protect shared state. If class inheritance is used, explain the actual relationship.
