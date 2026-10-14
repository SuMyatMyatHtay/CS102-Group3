package autograder.report.feedback;

import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.StudentIdentity;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFeedbackGeneratorTest {
    private static final long CASE_TIMEOUT_MILLIS = 2000;
    private static final int MAX_LINES = 5;

    @TempDir
    Path temporaryDirectory;

    @Test
    void explainsEachCaseOutcomeFromRecordedEvidence() throws Exception {
        StudentGrade grade = new StudentGrade(submission(student("ann.lee.2025"), null, List.of()), List.of(
                result("Q1a-1", "Q1a", TestStatus.PASSED, "", ""),
                result("Q1a-2", "Q1a", TestStatus.FAILED, "printed 7", ""),
                result("Q2a-1", "Q2a", TestStatus.TIMED_OUT, "", "case execution timed out"),
                result("Q2a-2", "Q2a", TestStatus.RUNTIME_ERROR, "",
                        "runner reported an exception\njava.lang.NullPointerException")));

        Path file = generator().generate(grade, temporaryDirectory.resolve("feedback"));
        String markdown = Files.readString(file);

        assertEquals("ann.lee.2025.md", file.getFileName().toString());
        assertTrue(markdown.contains("Student: Ann Lee (ann.lee.2025)"));
        assertTrue(markdown.contains("Total: 1 / 4"));
        assertTrue(markdown.contains("| Q1a | 1 / 2 |"));
        assertTrue(markdown.contains("| Q1a-2 | Wrong result | 0 / 1 |"));
        assertTrue(markdown.contains("did not match the expected result"));
        assertTrue(markdown.contains("printed 7"));
        assertTrue(markdown.contains("2000 ms time limit"));
        assertTrue(markdown.contains("java.lang.NullPointerException"));
        assertFalse(markdown.contains("withheld"));
    }

    @Test
    void distinguishesMissingSourceFromCompilerError() throws Exception {
        Path workingRoot = temporaryDirectory.resolve("working");
        Files.createDirectories(workingRoot.resolve("Q2"));
        Files.writeString(workingRoot.resolve("Q2").resolve("Q2a.java"), "class Q2a {");
        String compilerOutput = "Q2a.java:1: error: reached end of file while parsing";
        String recordedOutput = "/grader/run-1/submission-1/Q2a/src/" + compilerOutput;
        StudentGrade grade = new StudentGrade(submission(student("ann.lee.2025"), workingRoot, List.of()), List.of(
                result("Q1a-1", "Q1a", TestStatus.COMPILE_ERROR, "", "Q1a.java: file not found"),
                result("Q1a-2", "Q1a", TestStatus.COMPILE_ERROR, "", "Q1a.java: file not found"),
                result("Q2a-1", "Q2a", TestStatus.COMPILE_ERROR, "", recordedOutput),
                result("Q2a-2", "Q2a", TestStatus.COMPILE_ERROR, "", recordedOutput)));

        String markdown = generator().render(grade);

        assertTrue(markdown.contains("required files were missing from the Q1 folder: Q1a.java"));
        assertFalse(markdown.contains("file not found"));
        assertTrue(markdown.contains("did not compile"));
        assertEquals(1, occurrences(markdown, compilerOutput));
        assertFalse(markdown.contains("/grader/"));
    }

    @Test
    void withheldSubmissionShowsRecordedReasonAndIsNotFinal() {
        String reason = "duplicate submissions for the same student";
        StudentGrade grade = new StudentGrade(submission(student("ann.lee.2025"), null, List.of()), List.of(
                result("Q1a-1", "Q1a", TestStatus.NOT_RUN, "", reason),
                result("Q1a-2", "Q1a", TestStatus.NOT_RUN, "", reason)));

        String markdown = generator().render(grade);

        assertTrue(markdown.contains("Score withheld for instructor review"));
        assertTrue(markdown.contains("2 of 2 cases were not run"));
        assertEquals(1, occurrences(markdown, "Recorded reason: " + reason + "\n"));
    }

    @Test
    void studentControlledTextCannotBreakTheMarkdownStructure() {
        String hostileOutput = "````\n# fake heading\n<script>alert(1)</script>";
        SubmissionResult submission = submission(student("ann.lee.2025"), null,
                List.of("header name <b>[link](x)</b> | pipe"));
        StudentGrade grade = new StudentGrade(submission, List.of(
                result("Q1a-1", "Q1a", TestStatus.RUNTIME_ERROR, "", hostileOutput)));

        String markdown = generator().render(grade);

        assertTrue(markdown.contains("`````text\n````\n# fake heading"));
        assertTrue(markdown.contains("\n`````\n"));
        assertTrue(markdown.contains("header name \\<b\\>\\[link\\](x)\\</b\\> \\| pipe"));
    }

    @Test
    void longDiagnosticsAreTruncatedWithANote() {
        List<String> lines = new ArrayList<>();
        for (int index = 1; index <= MAX_LINES + 3; index++) {
            lines.add("line " + index);
        }
        StudentGrade grade = new StudentGrade(submission(student("ann.lee.2025"), null, List.of()), List.of(
                result("Q1a-1", "Q1a", TestStatus.RUNTIME_ERROR, "", String.join("\n", lines))));

        String markdown = generator().render(grade);

        assertTrue(markdown.contains("line " + MAX_LINES + "\n"));
        assertFalse(markdown.contains("line " + (MAX_LINES + 1)));
        assertTrue(markdown.contains("3 more lines omitted; see cases.csv"));
    }

    @Test
    void unsafeAndDuplicateNamesStayInsideTheOutputDirectory() throws Exception {
        Path output = temporaryDirectory.resolve("feedback");
        StudentGrade traversal = new StudentGrade(submission(student("../../escape"), null, List.of()),
                List.of(result("Q1a-1", "Q1a", TestStatus.PASSED, "", "")));
        StudentGrade unresolved = new StudentGrade(submission(null, null, List.of()),
                List.of(result("Q1a-1", "Q1a", TestStatus.NOT_RUN, "", "unresolved student identity")));

        Path first = generator().generate(traversal, output);
        Path second = generator().generate(traversal, output);
        Path third = generator().generate(unresolved, output);

        assertEquals(output, first.getParent());
        assertEquals("escape.md", first.getFileName().toString());
        assertEquals("escape-2.md", second.getFileName().toString());
        assertEquals("archive_name.md", third.getFileName().toString());
        assertTrue(Files.readString(third).contains("unresolved identity"));
    }

    @Test
    void listsPreparationProblemsAndCorrections() {
        SubmissionCorrection correction = new SubmissionCorrection("folder", Path.of("Q1"),
                "wrapper", "RenameToYourUsername", "ann.lee.2025", false);
        SubmissionResult submission = new SubmissionResult(Path.of("ann.lee.2025.zip"), null, null, null,
                identity(student("ann.lee.2025")), List.of(), List.of("name header differs from roster"),
                List.of(correction), SubmissionStatus.REVIEW_REQUIRED, true);
        StudentGrade grade = new StudentGrade(submission,
                List.of(result("Q1a-1", "Q1a", TestStatus.PASSED, "", "")));

        String markdown = generator().render(grade);

        assertTrue(markdown.contains("- name header differs from roster"));
        assertTrue(markdown.contains("RenameToYourUsername -\\> ann.lee.2025"));
    }

    @Test
    void rosterMarkersAndJvmLauncherNoticesAreNotShownAsEvidence() {
        StudentRecord student = new StudentRecord("1", "#xing.yan.2023", "XING YAN", "_", "x@example.com");
        StudentGrade grade = new StudentGrade(submission(student, null, List.of()), List.of(
                result("Q1a-1", "Q1a", TestStatus.FAILED, "",
                        "Picked up JAVA_TOOL_OPTIONS: -Dexample=1"),
                result("Q1a-2", "Q1a", TestStatus.RUNTIME_ERROR, "",
                        "Picked up _JAVA_OPTIONS: -Xmx1g\njava.lang.ArithmeticException")));

        String markdown = generator().render(grade);

        assertTrue(markdown.contains("Student: XING YAN (xing.yan.2023)"));
        assertFalse(markdown.contains("Picked up"));
        assertEquals(1, occurrences(markdown, "Error output:"));
        assertTrue(markdown.contains("java.lang.ArithmeticException"));
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new MarkdownFeedbackGenerator(assessment(), 0, MAX_LINES, "#", "_"));
        assertThrows(IllegalArgumentException.class,
                () -> new MarkdownFeedbackGenerator(assessment(), CASE_TIMEOUT_MILLIS, 0, "#", "_"));
    }

    private MarkdownFeedbackGenerator generator() {
        return new MarkdownFeedbackGenerator(assessment(), CASE_TIMEOUT_MILLIS, MAX_LINES, "#", "_");
    }

    private AssessmentDefinition assessment() {
        return new AssessmentDefinition("test", "Test Assessment", List.of(
                new QuestionDefinition("Q1a", "Q1", "Q1a.java", Path.of("Q1aTester.java"), 2, 1,
                        List.of(), List.of(), List.of()),
                new QuestionDefinition("Q2a", "Q2", "Q2a.java", Path.of("Q2aTester.java"), 2, 1,
                        List.of(), List.of(), List.of())));
    }

    private TestCaseResult result(String id, String question, TestStatus status, String stdout, String stderr) {
        TestCaseDefinition test = new TestCaseDefinition(id, question, "Runner", List.of(), 1);
        return new TestCaseResult(test, status, 10, stdout, stderr);
    }

    private StudentRecord student(String username) {
        return new StudentRecord("1", username, "Ann", "Lee", username + "@example.com");
    }

    private StudentIdentity identity(StudentRecord student) {
        return new StudentIdentity("Ann Lee", "", "", List.of(), List.of(), List.of(), student,
                "test fixture", List.of());
    }

    private SubmissionResult submission(StudentRecord student, Path workingRoot, List<String> anomalies) {
        return new SubmissionResult(Path.of("archive name.zip"), null, null, workingRoot, identity(student),
                List.of(), anomalies, List.of(), SubmissionStatus.READY, true);
    }

    private static int occurrences(String text, String fragment) {
        int count = 0;
        int index = text.indexOf(fragment);
        while (index >= 0) {
            count++;
            index = text.indexOf(fragment, index + fragment.length());
        }
        return count;
    }
}
