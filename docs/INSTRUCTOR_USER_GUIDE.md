# Instructor guide: randomized lab-test mode

This guide explains how to generate student practice resources and use seeded randomized grading for the current CS102 lab test. The mode is optional; the original batch grader remains the normal way to process the supplied lab submissions and score sheet.

## What the current version supports

The randomized question definitions are built into the application registry. They cover the current lab-test questions:

| Question | Student method | Main behavior |
| --- | --- | --- |
| Q1a | `getIsogramWords(ArrayList<String>)` | Return words without repeated letters, ignoring case |
| Q1b | `getSumOfEvenIntegers(ArrayList<Object>)` | Sum even `Integer` values and ignore other types |
| Q2a | `getAverageAge(String, String)` | Average matching people-file ages; return `0` for no matches and `-1` for an unavailable file |
| Q2b | `getTopStudent(String, String)` | Return the highest-GPA student for a course; throw `DataException` when there is no matching data |
| Q3 | `sortShapes(List<Shape>)` | Keep shapes with area at most 1000 and sort by descending area, then perimeter |

The application does not discover arbitrary question definitions by scanning a directory. For these five registered questions, the instructor runs one resource-generation command per question; the guide below provides a loop that creates one `studentResources` folder containing all five packs.

## Prepare the instructor machine

Run these commands from the repository root after building the project:

~~~powershell
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --help
~~~

The randomized commands need both `java` and `javac` from a JDK. Their command names can be configured with `randomized.java.command` and `randomized.javac.command` in `config.properties`.

## Generate `studentResources`

In `config.properties`, set the output directory to the folder you want to send:

~~~properties
randomized.resources.directory=studentResources
~~~

Relative paths resolve from the directory containing `config.properties`. The default is `students/resources`. Choose a clean output folder: generation replaces files with the same names.

Run this PowerShell loop from the repository root:

~~~powershell
$questionIds = @("Q1a", "Q1b", "Q2a", "Q2b", "Q3")
foreach ($questionId in $questionIds) {
    java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-resources $questionId config.properties
    if ($LASTEXITCODE -ne 0) {
        throw "Resource generation failed for $questionId"
    }
}
~~~

The generated folder contains a default answer stub and a fixed-example tester for each question. It also contains the Q2 text fixtures, the `DataException.java` helper, and the `Shape`/`Circle`/`Rectangle` sources and `ShapeComparator.java` starter needed by the examples. The answer key is not included. You can send the folder directly or package it yourself:

~~~powershell
Compress-Archive -Path .\studentResources\* -DestinationPath .\studentResources.zip -Force
~~~

Students can compile a question and its tester in the resource folder, then run the tester. For example:

~~~powershell
javac Q1a.java Q1aTester.java
java Q1aTester
~~~

For Q2a and Q2b, keep the generated fixture text files beside the Java files. For Q3, compile `Shape.java`, `Circle.java`, `Rectangle.java`, `Q3.java`, `ShapeComparator.java`, and `Q3Tester.java` together. Students should submit their implemented question source; Q3 submissions also need `ShapeComparator.java`.

## Randomized grading for one student and question

Put the student's source files in one folder, then run one grading command for a selected question:

~~~powershell
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade Q1a students/submissions/alice config.properties 482910374651
~~~

The seed is optional. Save the printed seed to reproduce the same randomized inputs. The output report is written under `randomized.results.directory` (default `grading/results`), for example `alice-Q1a.txt`. Its first line is `PASS` or `FAIL`; the remaining lines show the pass count, seed, and a capped list of failures. A completed grading run exits successfully even when the student gets `FAIL`, so inspect the report instead of relying only on the process exit code.

By default, the grader generates 50 inputs for each declared test case. You can change this with `randomized.runs.per.case`; the total is capped at 10,000 trials per run. The timeout applies to the complete student run. Compilation has its own 60-second minimum deadline.

The randomized result is separate from `scorereport.csv`. This mode currently grades one question and one extracted submission folder at a time; it does not combine these results into the class score sheet.

## Processing ZIP submissions in a batch

The original batch mode can process multiple student ZIP files from a directory configured in `input.directory`. For example, set an absolute path with forward slashes in `config.properties`:

~~~properties
input.directory=C:/courses/CS102/submissions
~~~

Then run the ordinary batch command:

~~~powershell
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
~~~

The configured archive pattern defaults to ZIP files. The batch pipeline validates and extracts each archive, prepares the configured lab questions, runs the fixed and existing dynamic test cases, and writes the normal score and diagnostics reports. Keep the supplied assessment layout, roster, score sheet, and other configured paths available.

This ZIP workflow belongs to the original batch grader. The randomized mode does not currently scan a ZIP directory or batch-grade many archives. To use randomized grading today, extract a student's files into a submission folder and invoke `--randomized-grade` for each question. Automating that ZIP-to-randomized-batch workflow would require a separate feature.

## Results, boundaries, and safe use

- `PASS` means the selected question passed every generated trial for that seed. `FAIL` includes mismatches, student exceptions, expected exceptions that were not thrown, compile errors, crashes, and timeouts.
- A run evaluates one question. Run separate commands for Q1a, Q1b, Q2a, Q2b, and Q3 if you want a full randomized review of one student.
- Student code runs in a separate JVM with a timeout, but this is not a security sandbox. It has the grading account's filesystem, network, and process permissions. Use trusted submissions or add operating-system/container restrictions.
- The generated practice tester uses a few visible examples. Randomized grading uses additional generated inputs and the instructor answer key kept in the host application.
- The supplied `Project` files are not modified by resource generation or randomized grading.

See [the randomized autograder technical notes](../RANDOMIZED_AUTOGRADER.md) for the design and instructor question-authoring details. See [the repository README](../README.md) for the build steps and existing batch-grading reports.
