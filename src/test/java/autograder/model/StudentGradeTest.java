package autograder.model;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentGradeTest {
    @Test
    void timeoutResultDoesNotEraseOtherRecordedPassingCases() {
        StudentGrade grade = new StudentGrade(submission(), List.of(
                result("first", "one", TestStatus.PASSED),
                result("second", "one", TestStatus.TIMED_OUT),
                result("third", "two", TestStatus.PASSED)));

        assertEquals(2, grade.getTotalScore());
        assertEquals(1, grade.getQuestionScore("one"));
        assertEquals(1, grade.getQuestionScore("two"));
    }

    @Test
    void duplicateResultsCannotInflateTheScore() {
        TestCaseResult pass = result("same-case", "question", TestStatus.PASSED);
        assertThrows(IllegalArgumentException.class,
                () -> new StudentGrade(submission(), List.of(pass, pass)));
    }

    @Test
    void fractionalMarksDoNotExceedTheDecimalTotalThroughRounding() {
        TestCaseDefinition first = new TestCaseDefinition("a", "question", "Runner", List.of(), 0.1);
        TestCaseDefinition second = new TestCaseDefinition("b", "question", "Runner", List.of(), 0.1);
        TestCaseDefinition third = new TestCaseDefinition("c", "question", "Runner", List.of(), 0.1);
        StudentGrade grade = new StudentGrade(submission(), List.of(
                new TestCaseResult(first, TestStatus.PASSED, 0, "", ""),
                new TestCaseResult(second, TestStatus.PASSED, 0, "", ""),
                new TestCaseResult(third, TestStatus.PASSED, 0, "", "")));
        assertEquals(0.3, grade.getTotalScore());
        assertEquals(0.3, grade.getQuestionScore("question"));
    }

    private TestCaseResult result(String id, String question, TestStatus status) {
        TestCaseDefinition test = new TestCaseDefinition(id, question, "Runner", List.of(), 1);
        return new TestCaseResult(test, status, 10, "", "");
    }

    private SubmissionResult submission() {
        StudentIdentity identity = new StudentIdentity("Example", "", "", List.of("Example"),
                List.of(), List.of(), null, "test fixture", List.of());
        return new SubmissionResult(Path.of("example.zip"), null, null, null, identity,
                List.of(), List.of(), List.of(), SubmissionStatus.REVIEW_REQUIRED, false);
    }
}
