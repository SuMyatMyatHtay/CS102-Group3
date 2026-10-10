package autograder.api;

import autograder.model.AssessmentDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.util.List;

public interface PlagiarismDetector {
    List<SimilarityFinding> compare(List<SubmissionResult> submissions,
                                    AssessmentDefinition assessment) throws IOException;
}
