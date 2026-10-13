# Terminal dashboard: feature 6

## Purpose and flow

`App` creates one `TerminalDashboard` and passes it through the existing `ProgressListener` interface to submission intake and grading. Intake reports each archive before and after preparation. The grading pipeline reports each submission before and after its cases, then reports CSV writing. The existing final score and diagnostic paths still appear after the progress lines. This uses actual completed work; percentages do not estimate time remaining.

`TerminalDashboard` handles presentation only. It prints a bounded, append-only status line with the stage, count, percentage, bar, and current archive. Append-only lines keep warnings visible and make redirected logs readable. It replaces terminal control characters from archive names before printing them. ANSI color is optional; `DashboardSettings` reads `dashboard.mode` from the properties file. `auto` uses color only on an interactive console without `NO_COLOR` or `TERM=dumb`. `plain` forces uncolored output, and `ansi` forces color for a compatible terminal.

`TerminalCancellation` listens for `q` followed by Enter only when Java has an interactive `Console`. It interrupts the grading thread. Intake checks cancellation between archives; grading checks between submissions, questions, and cases. The child-process runner already terminates the active child and attempts descendant cleanup when interrupted. Cancellation exits with code 130. A run directory can contain partial files if cancellation occurs during reporting; instructors should use only a completed report.

Both intake and grading catch runtime display errors and continue their work without further progress updates. A broken display cannot change a student's score or stop the batch. The console is not a security sandbox for submitted Java code.

## Design and rubric discussion

- **Abstraction:** the existing `ProgressListener` contract keeps pipeline work separate from presentation. The previous `ConsoleProgressListener` remains available.
- **Single responsibility:** settings parsing, progress rendering, and cancellation input live in separate classes under `autograder.ui`.
- **Usability:** counts and stage labels explain what is happening; plain output works in redirected logs; diagnostics and final result paths remain visible.
- **Robustness:** display exceptions are isolated from grading, and cancellation uses the pipeline's existing interruption cleanup.

The dashboard intentionally reports archive-level grading progress rather than guessing case duration or hiding error messages behind a screen redraw. It requires no external runtime library.

## Verification and demo

Run `mvn clean verify`, then `java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties`. Demonstrate the default output on the six supplied submissions, set `dashboard.mode=plain` for a redirected-log example, and show `q` plus Enter during an interactive run. Explain that a non-interactive terminal cannot accept the cancellation command. The test suite checks plain/ANSI selection, escaped archive labels, invalid settings, cancellation signaling, and grading/report progress events.
