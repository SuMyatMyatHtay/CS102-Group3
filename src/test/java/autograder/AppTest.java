package autograder;

import autograder.config.FeedbackConfig;
import autograder.config.HtmlReportConfig;
import autograder.grading.GradingRun;
import autograder.model.AssessmentDefinition;
import autograder.model.BatchResult;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.StudentIdentity;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void helpDoesNotRequireConfiguration() {
        assertEquals(0, App.run(new String[] {"--help"}));
    }

    @Test
    void missingArgumentsAndMissingConfigFailCleanly() {
        assertEquals(1, App.run(new String[] {}));
        assertEquals(1, App.run(new String[] {
                temporaryDirectory.resolve("missing.properties").toString()}));
    }

    @Test
    void reportsHtmlFailureAsNonzeroAndPreservesCompletedCsvEvidence() throws Exception {
        Files.writeString(temporaryDirectory.resolve("scorereport.csv"), "existing scores\n");
        Files.writeString(temporaryDirectory.resolve("cases.csv"), "existing cases\n");
        Files.writeString(temporaryDirectory.resolve("submissions.csv"), "existing decisions\n");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream original = System.err;
        try (PrintStream capture = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            int exit = App.generateHtmlReport(run(), new HtmlReportConfig(true, "dashboard.html"),
                    (context, directory) -> { throw new IOException("disk unavailable"); });
            assertEquals(1, exit);
        } finally {
            System.setErr(original);
        }
        String error = errors.toString(StandardCharsets.UTF_8);
        assertTrue(error.contains("disk unavailable"));
        assertTrue(error.contains(temporaryDirectory.resolve("dashboard.html").toString()));
        assertTrue(error.contains("Grading and CSV exports are complete"));
        assertEquals("existing scores\n", Files.readString(temporaryDirectory.resolve("scorereport.csv")));
        assertEquals("existing cases\n", Files.readString(temporaryDirectory.resolve("cases.csv")));
        assertEquals("existing decisions\n", Files.readString(temporaryDirectory.resolve("submissions.csv")));
    }

    @Test
    void disabledHtmlDoesNotInvokeGenerator() {
        assertEquals(0, App.generateHtmlReport(run(), new HtmlReportConfig(false, "dashboard.html"),
                (context, directory) -> { throw new AssertionError("disabled generator was invoked"); }));
    }

    @Test
    void oneFeedbackFailureDoesNotStopTheRemainingStudents() {
        GradingRun run = run(List.of(grade("first.zip"), grade("second.zip")));
        List<String> written = new ArrayList<>();

        int exitCode = App.generateFeedback(run, new FeedbackConfig(true, "feedback", 10),
                (grade, directory) -> {
                    String archive = grade.getSubmission().getArchivePath().toString();
                    if (archive.equals("first.zip")) {
                        throw new IOException("disk full");
                    }
                    written.add(archive);
                    return directory.resolve(archive + ".md");
                });

        assertEquals(1, exitCode);
        assertEquals(List.of("second.zip"), written);
    }

    @Test
    void disabledFeedbackWritesNothing() {
        GradingRun run = run(List.of(grade("first.zip")));

        int exitCode = App.generateFeedback(run, new FeedbackConfig(false, "feedback", 10),
                (grade, directory) -> {
                    throw new IOException("should not be called");
                });

        assertEquals(0, exitCode);
        assertFalse(Files.exists(temporaryDirectory.resolve("feedback")));
    }

    private GradingRun run() {
        return run(List.of());
    }

    private GradingRun run(List<StudentGrade> grades) {
        QuestionDefinition question = new QuestionDefinition("Q1a", "Q1", "Q1a.java", Path.of("Q1aTester.java"),
                1, 1, List.of(), List.of(), List.of());
        BatchResult batch = new BatchResult(new AssessmentDefinition("exam", "Exam", List.of(question)),
                grades, List.of(), List.of());
        return new GradingRun(batch, temporaryDirectory, Map.of());
    }

    private StudentGrade grade(String archive) {
        StudentIdentity identity = new StudentIdentity("", "", "", List.of(), List.of(), List.of(),
                null, "test fixture", List.of());
        SubmissionResult submission = new SubmissionResult(Path.of(archive), null, null, null, identity,
                List.of(), List.of(), List.of(), SubmissionStatus.READY, true);
        TestCaseDefinition test = new TestCaseDefinition("Q1a-1", "Q1a", "Runner", List.of(), 1);
        return new StudentGrade(submission, List.of(new TestCaseResult(test, TestStatus.PASSED, 1, "", "")));
    }
}
