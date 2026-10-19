package autograder.grading;

import autograder.api.TestCaseProvider;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic test case provider for the CS102 auto-grader (Feature 4).
 *
 * It emits a fixed number of dynamic cases per question. Each case is executed by
 * {@code DynamicRunner} (a default-package class compiled alongside the student code, see
 * {@code StudentCompiler}) which generates random inputs and checks a property/invariant of the
 * question -- it never relies on a pre-computed expected answer, so the team does not have to act
 * as an oracle. The seed makes every run reproducible.
 */
public class DynamicTestCaseProvider implements TestCaseProvider {

    private static final int CASES_PER_QUESTION = 5;
    private static final long SEED_BASE = 0x9E3779B1L;

    @Override
    public List<TestCaseDefinition> load(AssessmentDefinition assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("assessment is required");
        }

        List<TestCaseDefinition> definitions = new ArrayList<>();
        int questionIndex = 0;
        for (QuestionDefinition question : assessment.getQuestions()) {
            String questionId = question.getId();
            double pointsPerCase = question.getPointsPerCase();
            for (int caseNumber = 0; caseNumber < CASES_PER_QUESTION; caseNumber++) {
                long seed = SEED_BASE + questionIndex * 1_000_003L + caseNumber * 7_919L;
                definitions.add(new TestCaseDefinition(
                        questionId + "-dynamic-" + (caseNumber + 1),
                        questionId,
                        "DynamicRunner",
                        List.of(questionId, Long.toString(seed)),
                        pointsPerCase));
            }
            questionIndex++;
        }
        return List.copyOf(definitions);
    }
}
