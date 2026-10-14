package autograder.api;

import autograder.model.AssessmentDefinition;
import autograder.model.TestCaseDefinition;
import java.io.IOException;
import java.util.List;

public interface TestCaseProvider {
    List<TestCaseDefinition> load(AssessmentDefinition assessment) throws IOException;
}
