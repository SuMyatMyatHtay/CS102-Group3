# Starter-code comparison (Sera, feature 3)

The implementation compares configured answer files and student-written companions against the starter directory. It reports warnings in the terminal, `starter-analysis.csv` and the HTML dashboard's **Analysis findings** section. It does not change scores, preparation status or export eligibility.

## Build and run

Run from the repository root:

```powershell
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

If Maven is not on PATH, the temporary installation used for this checkout can be invoked with:

```powershell
& "$env:TEMP\cs102-sera-tools\apache-maven-3.9.9\bin\mvn.cmd" clean verify
```

Wait for BUILD SUCCESS before running the JAR. Read the output directory printed by this run, then open its `dashboard.html` and scroll to **Analysis findings**. Inspect `starter-analysis.csv` for archive, source file, finding code and explanation. Expected sample scores remain Chee 12, David 10, Fen 10, Jing 0, Ping 20 and Xing 1.

## Focused behaviour checks

```powershell
mvn "-Dtest=StarterCodeDetectorTest,BoilerplateConfigTest,AppTest" test
```

Substitute the temporary Maven command above for `mvn` if needed. Tests create temporary synthetic files; do not edit the supplied files under `Project` to test this feature.

The tests cover:

- An identical starter or changes only to comments, identity headers and formatting produce `UNCHANGED_STARTER`
- A small token edit produces `MINIMAL_STARTER_CHANGES` when it meets the configured threshold; substantial implementation changes do not
- String/character literals, text blocks and multi-character operators retain meaningful differences
- Missing files, malformed/unsupported syntax, invalid UTF-8 and resource-limit failures produce `STARTER_COMPARISON_UNAVAILABLE`; other files/submissions continue
- Configured student companions are compared; unrelated support files are excluded
- Disabled analysis does not run; findings reach escaped HTML while grades, withholding and the three existing CSVs are preserved

## Configuration and limitations

`grading.boilerplate.enabled=true` enables the feature; older configurations without this key default to disabled. `grading.boilerplate.starter.directory` resolves relative to the configuration file. The default similarity threshold is 0.98; valid values are greater than zero and at most one. Byte and token limits bound reading and comparison work.

Similarity is `1 - token edit distance / max(starter token count, answer token count)`. Insertion, deletion and substitution each cost one. Comparisons ignore comments and whitespace outside literals, but preserve names, literals and operator boundaries. This is a conservative lexical comparison, not a complete Java parser: Unicode escape syntax requires manual comparison. Whole-file similarity can be high because of supplied demo code; warnings require human review and do not establish correctness or misconduct. A low similarity does not prove a complete answer.

The implementation uses the unchanged `BoilerplateDetector` interface and immutable shared findings; a separate package owns comparison logic and external properties own operational settings. These support the brief's page 6 modularity/configuration and anomaly-warning criteria. Edge-case tests and the CLI/report demonstration support page 7 robustness/usability criteria. No runtime library was added. Human review, clean-build acceptance, submission documentation and AI declarations remain team responsibilities.
