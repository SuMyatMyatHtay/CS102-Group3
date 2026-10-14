package autograder.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestCaseResultTest {
    @Test
    void onlyPassingCasesEarnTheirConfiguredPoints() {
        TestCaseDefinition test = new TestCaseDefinition("question-case", "question",
                "ExampleRunner", List.of("case"), 2.5);
        for (TestStatus status : TestStatus.values()) {
            TestCaseResult result = new TestCaseResult(test, status, 50, "", "");
            if (status == TestStatus.PASSED) {
                assertEquals(2.5, result.getScore());
            } else {
                assertEquals(0, result.getScore());
            }
        }
    }

    @Test
    void rejectsInvalidPointsAndDuration() {
        for (double invalid : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new TestCaseDefinition(
                    "test", "question", "Runner", List.of(), invalid));
        }
        TestCaseDefinition test = new TestCaseDefinition("test", "question", "Runner",
                List.of(), 1);
        assertThrows(IllegalArgumentException.class,
                () -> new TestCaseResult(test, TestStatus.PASSED, -1, "", ""));
    }
}
