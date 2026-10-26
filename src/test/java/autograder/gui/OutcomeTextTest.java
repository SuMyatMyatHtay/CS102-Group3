package autograder.gui;

import autograder.RunOutcome;
import autograder.RunOutcome.Stage;
import autograder.RunOutcome.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutcomeTextTest {
    private static RunOutcome outcome(Status status, Stage stage, Exception failure) {
        return new RunOutcome(status, stage, failure, List.of(), null, 0);
    }

    @Test
    void headlinesForNonFailureStatuses() {
        assertEquals("Grading complete", OutcomeText.headline(outcome(Status.COMPLETED, null, null)));
        assertEquals("Grading complete. Some reports could not be written.",
                OutcomeText.headline(outcome(Status.REPORT_FAILURES, null, null)));
        assertEquals("No submissions matched",
                OutcomeText.headline(outcome(Status.NO_SUBMISSIONS, null, null)));
        assertEquals("Grading stopped. No scores were written.",
                OutcomeText.headline(outcome(Status.INTERRUPTED, null, null)));
    }

    @Test
    void failureNamesStageAndMessage() {
        String text = OutcomeText.headline(outcome(Status.FAILED, Stage.PREPARATION,
                new IllegalStateException("bad zip")));
        assertEquals("Grading stopped during preparation: bad zip", text);
    }

    @Test
    void failureWithoutMessageUsesExceptionName() {
        String text = OutcomeText.headline(outcome(Status.FAILED, Stage.REPORTS,
                new IllegalStateException()));
        assertEquals("Grading stopped during reports: IllegalStateException", text);
    }

    @Test
    void environmentFailureAddsHint() {
        String text = OutcomeText.headline(outcome(Status.FAILED, Stage.ENVIRONMENT,
                new IllegalStateException("no compiler")));
        assertTrue(text.startsWith("Grading stopped during environment: no compiler"));
        assertTrue(text.contains("JDK"));
    }

    @Test
    void progressStrings() {
        assertEquals("Preparing 2 of 6: chee.zip", OutcomeText.intake("chee.zip", 2, 6));
        assertEquals("Grading 3 of 6: Chee", OutcomeText.grading("Chee", 3, 6));
        assertEquals("Grading 3 of 6", OutcomeText.grading("", 3, 6));
        assertEquals("Checking setup…", OutcomeText.CHECKING);
        assertEquals("Writing reports…", OutcomeText.WRITING_REPORTS);
    }
}
