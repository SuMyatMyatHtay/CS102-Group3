package autograder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import autograder.config.HtmlReportConfig;
import autograder.grading.GradingRun;
import autograder.model.AssessmentDefinition;
import autograder.model.BatchResult;
import autograder.model.QuestionDefinition;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private GradingRun run() {
        QuestionDefinition question = new QuestionDefinition("Q", "Q", "Q.java", Path.of("QTester.java"),
                1, 1, List.of(), List.of(), List.of());
        BatchResult batch = new BatchResult(new AssessmentDefinition("exam", "Exam", List.of(question)),
                List.of(), List.of(), List.of());
        return new GradingRun(batch, temporaryDirectory, Map.of());
    }
}
