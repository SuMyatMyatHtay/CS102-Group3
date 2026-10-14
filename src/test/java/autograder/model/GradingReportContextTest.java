package autograder.model;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GradingReportContextTest {
    @Test
    void snapshotsWithholdingAndRejectsUnrelatedOrEmptyDecisions() {
        SubmissionResult submission = submission("student.zip");
        BatchResult batch = batch(submission);
        Map<SubmissionResult, String> withheld = new HashMap<>();
        withheld.put(submission, "infrastructure failure");
        GradingReportContext context = new GradingReportContext(batch, withheld);
        withheld.clear();
        assertEquals("infrastructure failure", context.withheld().get(submission));
        assertThrows(UnsupportedOperationException.class, () -> context.withheld().clear());
        assertThrows(IllegalArgumentException.class,
                () -> new GradingReportContext(batch, Map.of(submission("other.zip"), "duplicate")));
        assertThrows(IllegalArgumentException.class,
                () -> new GradingReportContext(batch, Map.of(submission, " ")));
    }

    private static SubmissionResult submission(String archive) {
        StudentIdentity identity = new StudentIdentity("", "", "", List.of(), List.of(), List.of(),
                null, "unresolved", List.of());
        return new SubmissionResult(Path.of(archive), null, null, null, identity,
                List.of(), List.of(), List.of(), SubmissionStatus.FAILED, false);
    }

    private static BatchResult batch(SubmissionResult submission) {
        QuestionDefinition question = new QuestionDefinition("Q", "Q", "Q.java", Path.of("QTester.java"),
                1, 1, List.of(), List.of(), List.of());
        return new BatchResult(new AssessmentDefinition("exam", "Exam", List.of(question)),
                List.of(new StudentGrade(submission, List.of())), List.of(), List.of());
    }
}
