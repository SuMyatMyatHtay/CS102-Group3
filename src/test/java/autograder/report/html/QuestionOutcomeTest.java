package autograder.report.html;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionOutcomeTest {
    @Test
    void assignsExactBandsAtAndAroundEveryBoundary() {
        QuestionDefinition fourPointQuestion = question("Q", 4, 1.0);

        assertBand(fourPointQuestion, "0", QuestionOutcome.CreditBand.ZERO);
        assertBand(fourPointQuestion, "0.999999", QuestionOutcome.CreditBand.LOW);
        assertBand(fourPointQuestion, "1", QuestionOutcome.CreditBand.QUARTER);
        assertBand(fourPointQuestion, "1.999999", QuestionOutcome.CreditBand.QUARTER);
        assertBand(fourPointQuestion, "2", QuestionOutcome.CreditBand.HALF);
        assertBand(fourPointQuestion, "2.999999", QuestionOutcome.CreditBand.HALF);
        assertBand(fourPointQuestion, "3", QuestionOutcome.CreditBand.THREE_QUARTER);
        assertBand(fourPointQuestion, "3.999999", QuestionOutcome.CreditBand.THREE_QUARTER);
        assertBand(fourPointQuestion, "4", QuestionOutcome.CreditBand.FULL);

        QuestionDefinition decimalQuestion = question("QD", 3, 0.1);
        assertBand(decimalQuestion, "0.075", QuestionOutcome.CreditBand.QUARTER);
        assertBand(decimalQuestion, "0.15", QuestionOutcome.CreditBand.HALF);
        assertBand(decimalQuestion, "0.225", QuestionOutcome.CreditBand.THREE_QUARTER);
    }

    @Test
    void countsOnlyRecordedStatusesInEnumOrder() {
        QuestionDefinition question = question("Q", 6, 1.0);
        List<TestCaseResult> results = List.of(
                result("Q", "not-run", TestStatus.NOT_RUN),
                result("Q", "runtime", TestStatus.RUNTIME_ERROR),
                result("Q", "compile", TestStatus.COMPILE_ERROR),
                result("Q", "timeout", TestStatus.TIMED_OUT),
                result("Q", "failed", TestStatus.FAILED),
                result("Q", "passed", TestStatus.PASSED),
                result("Q", "passed-again", TestStatus.PASSED));

        QuestionOutcome outcome = outcome(question, "1", results);

        assertEquals(List.of("Passed:2", "Failed:1", "Timed out:1", "Compile error:1",
                        "Runtime error:1", "Not run:1"),
                outcome.statusCounts().stream()
                        .map(count -> count.label() + ":" + count.count()).toList());
    }

    @Test
    void missingExcessAndAbsentRecordsStayNeutralWithTruthfulCounts() {
        QuestionDefinition question = question("Q", 3, 1.0);

        QuestionOutcome missing = outcome(question, "2", List.of(
                result("Q", "one", TestStatus.PASSED), result("Q", "two", TestStatus.FAILED)));
        QuestionOutcome excess = outcome(question, "2", List.of(
                result("Q", "one", TestStatus.PASSED), result("Q", "two", TestStatus.FAILED),
                result("Q", "three", TestStatus.FAILED), result("Q", "four", TestStatus.FAILED)));
        QuestionOutcome absent = outcome(question, "0", List.of());

        assertNull(missing.creditBand());
        assertTrue(missing.evidenceNotes().contains("Recorded 2 of 3 configured cases."));
        assertNull(excess.creditBand());
        assertTrue(excess.evidenceNotes().contains("Recorded 4 of 3 configured cases."));
        assertNull(absent.creditBand());
        assertTrue(absent.evidenceNotes().contains("No case results recorded."));
        assertTrue(absent.evidenceNotes().contains("Recorded 0 of 3 configured cases."));
    }

    @Test
    void allNotRunEvidenceIsNeutral() {
        QuestionDefinition question = question("Q", 2, 1.0);
        QuestionOutcome outcome = outcome(question, "0", List.of(
                result("Q", "one", TestStatus.NOT_RUN), result("Q", "two", TestStatus.NOT_RUN)));

        assertNull(outcome.creditBand());
        assertTrue(outcome.evidenceNotes().contains("All recorded cases are NOT_RUN."));
    }

    @Test
    void nonpositiveAndNonfiniteMaximumsDoNotProduceCreditBands() {
        QuestionOutcome zeroMaximum = outcome(question("Q0", 0, 1.0), "0", List.of());
        QuestionOutcome negativeMaximum = outcome(question("QN", 1, -1.0), "0",
                List.of(result("QN", "one", TestStatus.FAILED)));
        QuestionOutcome nonfiniteMaximum = outcome(question("QX", 2, Double.NaN), "0", List.of());

        assertTrue(zeroMaximum.configuredMaximum().signum() == 0);
        assertNull(zeroMaximum.creditBand());
        assertTrue(zeroMaximum.evidenceNotes().contains(
                "Configured maximum is not positive; credit shading is unavailable."));
        assertEquals(new BigDecimal("-1.0"), negativeMaximum.configuredMaximum());
        assertNull(negativeMaximum.creditBand());
        assertNull(nonfiniteMaximum.configuredMaximum());
        assertNull(nonfiniteMaximum.creditBand());
        assertTrue(nonfiniteMaximum.evidenceNotes().stream()
                .anyMatch(note -> note.contains("non-finite")));
    }

    @Test
    void scoresOutsideConfiguredRangeAreNeutral() {
        QuestionDefinition question = question("Q", 2, 1.0);
        List<TestCaseResult> results = List.of(result("Q", "one", TestStatus.FAILED),
                result("Q", "two", TestStatus.FAILED));

        QuestionOutcome below = outcome(question, "-0.01", results);
        QuestionOutcome above = outcome(question, "2.01", results);

        assertNull(below.creditBand());
        assertNull(above.creditBand());
        assertTrue(below.evidenceNotes().contains("Question score is outside the configured range."));
        assertTrue(above.evidenceNotes().contains("Question score is outside the configured range."));
    }

    private void assertBand(QuestionDefinition question, String score,
                            QuestionOutcome.CreditBand expected) {
        List<TestCaseResult> results = new ArrayList<>();
        for (int index = 0; index < question.getCaseCount(); index++) {
            results.add(result(question.getId(), "case-" + index, TestStatus.FAILED));
        }
        assertEquals(expected, outcome(question, score, results).creditBand(),
                "credit band for score " + score);
    }

    private QuestionOutcome outcome(QuestionDefinition question, String score,
                                    List<TestCaseResult> results) {
        return QuestionOutcome.from(question, new BigDecimal(score), results);
    }

    private QuestionDefinition question(String id, int cases, double pointsPerCase) {
        return new QuestionDefinition(id, id, "Main.java", Path.of("Tester.java"), cases,
                pointsPerCase, List.of(), List.of(), List.of());
    }

    private TestCaseResult result(String questionId, String id, TestStatus status) {
        TestCaseDefinition definition = new TestCaseDefinition(id, questionId, "Runner", List.of(), 1.0);
        return new TestCaseResult(definition, status, 0, "", "");
    }
}
