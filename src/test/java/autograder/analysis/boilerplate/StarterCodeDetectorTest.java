package autograder.analysis.boilerplate;

import autograder.config.BoilerplateConfig;
import autograder.model.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StarterCodeDetectorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void detectsHeaderAndFormattingOnlyEditsWithoutChangingEitherFile() throws Exception {
        String starter = "// Name:\nclass Answer { int value() { return 0; } }";
        String answer = "/* Name: Sera */\r\nclass Answer{int value(){ /* work */ return 0;}}";
        var findings = analyze(starter, answer, 0.98);
        assertEquals("UNCHANGED_STARTER", findings.get(0).getCode());
        assertTrue(findings.get(0).getMessage().contains("0 token edits"));
        assertEquals(starter, Files.readString(temporaryDirectory.resolve("starter/Q1/Answer.java")));
        assertEquals(answer, Files.readString(temporaryDirectory.resolve("working/Q1/Answer.java")));
    }

    @Test
    void distinguishesSmallEditsFromGenuineImplementationAndHonoursThreshold() throws Exception {
        String starter = "class Answer { int value() { return 0; } }";
        assertEquals("MINIMAL_STARTER_CHANGES", analyze(starter,
                "class Answer { int value() { return 1; } }", 0.9).get(0).getCode());
        assertTrue(analyze(starter, "class Answer { int value() { return 1; } }", 1).isEmpty());
        assertTrue(analyze(starter, "class Answer { int value() { int sum = 0; "
                + "for (int i = 0; i < 10; i++) { sum += i; } return sum; } }", 0.98).isEmpty());
    }

    @Test
    void preservesStringsCharactersTextBlocksAndOperatorBoundaries() throws Exception {
        assertNotEquals(JavaSourceTokens.read("String s = \"a b // /*\";", 100),
                JavaSourceTokens.read("String s = \"ab // /*\";", 100));
        assertEquals(List.of("' '", "\"a\\\"b\""), JavaSourceTokens.read("' ' \"a\\\"b\"", 100));
        assertNotEquals(JavaSourceTokens.read("a ++ b", 100), JavaSourceTokens.read("a + + b", 100));
        assertNotEquals(JavaSourceTokens.read("a b", 100), JavaSourceTokens.read("ab", 100));
        assertEquals(List.of("\"\"\"\n// literal text\n\"\"\""),
                JavaSourceTokens.read("\"\"\"\n// literal text\n\"\"\"", 100));
    }

    @Test
    void reportsMissingFilesAndKeepsComparingOtherConfiguredAnswers() throws Exception {
        prepare("class Answer {}", "class Answer {}");
        Files.delete(temporaryDirectory.resolve("working/Q1/Answer.java"));
        Files.writeString(temporaryDirectory.resolve("starter/Q1/Companion.java"), "class Companion {}");
        Files.writeString(temporaryDirectory.resolve("working/Q1/Companion.java"), "class Companion {}");
        QuestionDefinition question = new QuestionDefinition("Q", "Q1", "Answer.java", Path.of("tester"),
                1, 1, List.of(), List.of(), List.of("Companion.java"));
        var detector = new StarterCodeDetector(new AssessmentDefinition("exam", "Exam", List.of(question)),
                config(0.98, 1024, 100));
        var findings = detector.analyze(submission(), temporaryDirectory.resolve("starter"));
        assertEquals(2, findings.size());
        assertEquals("STARTER_COMPARISON_UNAVAILABLE", findings.get(0).getCode());
        assertEquals("UNCHANGED_STARTER", findings.get(1).getCode());
        assertTrue(findings.get(0).getMessage().contains("Answer.java"));
        Files.delete(temporaryDirectory.resolve("starter/Q1/Answer.java"));
        assertEquals("STARTER_COMPARISON_UNAVAILABLE", detector(0.98, 1024, 100)
                .analyze(submission(), temporaryDirectory.resolve("starter")).get(0).getCode());
    }

    @Test
    void boundsSourceSizeAndTokensAndReportsUnsupportedOrMalformedSyntax() throws Exception {
        prepare("class Answer {}", "class Answer {}");
        assertEquals("STARTER_COMPARISON_UNAVAILABLE", detector(0.98, 5, 100)
                .analyze(submission(), temporaryDirectory.resolve("starter")).get(0).getCode());
        assertEquals("STARTER_COMPARISON_UNAVAILABLE", detector(0.98, 1024, 2)
                .analyze(submission(), temporaryDirectory.resolve("starter")).get(0).getCode());
        assertThrows(java.io.IOException.class, () -> JavaSourceTokens.read("/* unfinished", 100));
        assertThrows(java.io.IOException.class, () -> JavaSourceTokens.read("\"unfinished", 100));
        assertThrows(java.io.IOException.class, () -> JavaSourceTokens.read("\\" + "u0061", 100));
    }

    @Test
    void includesStudentCompanionsButExcludesUnconfiguredSupportSources() throws Exception {
        prepare("class Answer {}", "class Answer {}");
        Files.writeString(temporaryDirectory.resolve("starter/Q1/Companion.java"), "class Companion {}");
        Files.writeString(temporaryDirectory.resolve("working/Q1/Companion.java"), "class Companion {}");
        Files.writeString(temporaryDirectory.resolve("starter/Q1/Support.java"), "class Support {}");
        Files.writeString(temporaryDirectory.resolve("working/Q1/Support.java"), "class Support {}");
        QuestionDefinition question = new QuestionDefinition("Q", "Q1", "Answer.java", Path.of("tester"),
                1, 1, List.of(), List.of(), List.of("Companion.java"));
        var detector = new StarterCodeDetector(new AssessmentDefinition("exam", "Exam", List.of(question)),
                config(0.98, 1024, 100));
        assertEquals(2, detector.analyze(submission(), temporaryDirectory.resolve("starter")).size());
    }

    private List<AnalysisFinding> analyze(String starter, String answer, double threshold) throws Exception {
        prepare(starter, answer);
        return detector(threshold, 1024, 100).analyze(submission(), temporaryDirectory.resolve("starter"));
    }

    @Test
    void reportsAbsentWorkingCopyAndInvalidUtf8RatherThanInventingAMatch() throws Exception {
        SubmissionResult missing = new SubmissionResult(Path.of("missing.zip"), null, null, null, null,
                List.of(), List.of(), List.of(), SubmissionStatus.FAILED, false);
        assertEquals("STARTER_COMPARISON_UNAVAILABLE", detector(0.98, 1024, 100)
                .analyze(missing, temporaryDirectory.resolve("starter")).get(0).getCode());
        prepare("class Answer {}", "class Answer {}");
        Files.write(temporaryDirectory.resolve("working/Q1/Answer.java"), new byte[] {(byte) 0xc3, 0x28});
        assertEquals("STARTER_COMPARISON_UNAVAILABLE", detector(0.98, 1024, 100)
                .analyze(submission(), temporaryDirectory.resolve("starter")).get(0).getCode());
    }

    private void prepare(String starter, String answer) throws Exception {
        Files.createDirectories(temporaryDirectory.resolve("starter/Q1"));
        Files.createDirectories(temporaryDirectory.resolve("working/Q1"));
        Files.writeString(temporaryDirectory.resolve("starter/Q1/Answer.java"), starter);
        Files.writeString(temporaryDirectory.resolve("working/Q1/Answer.java"), answer);
    }

    private BoilerplateConfig config(double threshold, int bytes, int tokens) {
        return new BoilerplateConfig(true, temporaryDirectory.resolve("starter"), threshold, bytes, tokens);
    }

    private StarterCodeDetector detector(double threshold, int bytes, int tokens) {
        QuestionDefinition question = new QuestionDefinition("Q", "Q1", "Answer.java", Path.of("tester"),
                1, 1, List.of(), List.of(), List.of());
        return new StarterCodeDetector(new AssessmentDefinition("exam", "Exam", List.of(question)),
                config(threshold, bytes, tokens));
    }

    private SubmissionResult submission() {
        Path root = temporaryDirectory.resolve("working");
        return new SubmissionResult(Path.of("sera.zip"), root, root, root, null,
                List.of("Q1"), List.of(), List.of(), SubmissionStatus.READY, true);
    }
}
