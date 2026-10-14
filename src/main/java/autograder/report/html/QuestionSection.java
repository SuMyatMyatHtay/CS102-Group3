package autograder.report.html;

import autograder.model.QuestionDefinition;
import autograder.model.TestCaseResult;

import java.util.List;

record QuestionSection(QuestionDefinition question, int questionIndex,
                       List<TestCaseResult> results, int submissionOrdinal,
                       QuestionOutcome outcome) {
    QuestionSection {
        results = List.copyOf(results);
    }
}
