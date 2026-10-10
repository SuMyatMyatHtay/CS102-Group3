package autograder.grading;

import autograder.config.AppConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructorTestCaseProviderTest {
    private final InstructorTestCaseProvider provider = new InstructorTestCaseProvider();

    @Test
    void providesAllTwentyCasesWithPerCaseRunnerArgumentsAndMarks() throws IOException {
        AssessmentDefinition assessment = AppConfig.load(Path.of("config.properties")).getAssessment();

        List<TestCaseDefinition> cases = provider.load(assessment);

        assertEquals(20, cases.size());
        assertEquals("Q1a-1", cases.get(0).getId());
        assertEquals("Q1aAdapter", cases.get(0).getRunnerClass());
        assertEquals(List.of("1"), cases.get(0).getArguments());
        assertEquals("Q2b-5", cases.get(15).getId());
        assertEquals("Q3-4", cases.get(19).getId());
        assertTrue(cases.stream().allMatch(testCase -> testCase.getMaximumScore() == 1.0));
    }

    @Test
    void rejectsUnknownQuestionsAndUnexpectedCaseCounts() throws IOException {
        AssessmentDefinition assessment = AppConfig.load(Path.of("config.properties")).getAssessment();
        List<QuestionDefinition> unknown = new ArrayList<>(assessment.getQuestions());
        unknown.set(0, question("Q9", 3, 1));
        assertThrows(IllegalArgumentException.class,
                () -> provider.load(new AssessmentDefinition("x", "x", unknown)));

        List<QuestionDefinition> wrongCount = new ArrayList<>(assessment.getQuestions());
        wrongCount.set(0, question("Q1a", 2, 1));
        assertThrows(IllegalArgumentException.class,
                () -> provider.load(new AssessmentDefinition("x", "x", wrongCount)));
    }

    @Test
    void rejectsMissingOrDuplicateQuestionDefinitions() throws IOException {
        AssessmentDefinition assessment = AppConfig.load(Path.of("config.properties")).getAssessment();
        List<QuestionDefinition> missing = new ArrayList<>(assessment.getQuestions());
        missing.remove(4);
        assertThrows(IllegalArgumentException.class,
                () -> provider.load(new AssessmentDefinition("x", "x", missing)));

        List<QuestionDefinition> duplicate = new ArrayList<>(assessment.getQuestions());
        duplicate.set(4, duplicate.get(0));
        assertThrows(IllegalArgumentException.class,
                () -> provider.load(new AssessmentDefinition("x", "x", duplicate)));
    }

    @Test
    void rejectsChangedInstructorTesterEvenWhenQuestionIdIsUnchanged() throws IOException {
        AssessmentDefinition assessment = AppConfig.load(Path.of("config.properties")).getAssessment();
        List<QuestionDefinition> changedTester = new ArrayList<>(assessment.getQuestions());
        Path modified = Files.createTempFile("changed-Q1a-tester", ".java");
        try {
            String source = Files.readString(changedTester.get(0).getTesterFile());
            Files.writeString(modified, source.replace("Arrays.asList(\"alice\", \"ben\", \"cathy\")",
                    "Arrays.asList(\"alice\", \"ben\", \"changed\")"));
            QuestionDefinition original = changedTester.get(0);
            changedTester.set(0, new QuestionDefinition(original.getId(), original.getFolder(),
                    original.getSourceFile(), modified, original.getCaseCount(),
                    original.getPointsPerCase(), original.getSupportFiles(), original.getFixtures(),
                    original.getCompanionSourceFiles()));
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> provider.load(new AssessmentDefinition("x", "x", changedTester)));
            assertTrue(exception.getMessage().contains("unsupported instructor tester for Q1a"));
        } finally {
            Files.deleteIfExists(modified);
        }
    }

    @Test
    void acceptsInstructorTestersWithWindowsLineEndings() throws IOException {
        AssessmentDefinition assessment = AppConfig.load(Path.of("config.properties")).getAssessment();
        List<QuestionDefinition> windowsLineEndings = new ArrayList<>(assessment.getQuestions());
        QuestionDefinition original = windowsLineEndings.get(0);
        Path converted = Files.createTempFile("Q1aTester-crlf", ".java");
        try {
            String source = Files.readString(original.getTesterFile());
            Files.writeString(converted, source.replace("\n", "\r\n"));
            windowsLineEndings.set(0, new QuestionDefinition(original.getId(), original.getFolder(),
                    original.getSourceFile(), converted, original.getCaseCount(),
                    original.getPointsPerCase(), original.getSupportFiles(), original.getFixtures(),
                    original.getCompanionSourceFiles()));
            assertEquals(20, provider.load(new AssessmentDefinition("x", "x", windowsLineEndings)).size());
        } finally {
            Files.deleteIfExists(converted);
        }
    }

    private static QuestionDefinition question(String id, int count, double points) {
        return new QuestionDefinition(id, "Q1", id + ".java",
                Path.of("Project/Tester-Files", id + "Tester.java"),
                count, points, List.of(), List.of(), List.of());
    }
}
