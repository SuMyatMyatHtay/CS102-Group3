package autograder.api;

import autograder.model.AnalysisFinding;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface BoilerplateDetector {
    List<AnalysisFinding> analyze(SubmissionResult submission, Path starterDirectory)
            throws IOException;
}
