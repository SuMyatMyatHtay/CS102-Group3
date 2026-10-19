package autograder.grading;

import autograder.api.TestCaseProvider;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Combines fixed and dynamic test case providers.
 */
public class CombinedTestCaseProvider implements TestCaseProvider {

    private final TestCaseProvider fixedProvider;
    private final TestCaseProvider dynamicProvider;

    public CombinedTestCaseProvider(TestCaseProvider fixedProvider, TestCaseProvider dynamicProvider) {
        this.fixedProvider = fixedProvider;
        this.dynamicProvider = dynamicProvider;
    }

    @Override
    public List<TestCaseDefinition> load(AssessmentDefinition assessment) throws IOException {
        List<TestCaseDefinition> fixed = fixedProvider.load(assessment);
        List<TestCaseDefinition> dynamic = dynamicProvider.load(assessment);
        List<TestCaseDefinition> combined = new ArrayList<>(fixed.size() + dynamic.size());
        combined.addAll(fixed);
        combined.addAll(dynamic);
        return List.copyOf(combined);
    }
}