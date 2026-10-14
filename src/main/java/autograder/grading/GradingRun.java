package autograder.grading;

import autograder.model.BatchResult;
import autograder.model.SubmissionResult;
import java.nio.file.Path;
import java.util.Map;

public record GradingRun(BatchResult batch, Path directory,
                         Map<SubmissionResult, String> withheld) {
    public GradingRun {
        withheld = Map.copyOf(withheld);
    }
}
