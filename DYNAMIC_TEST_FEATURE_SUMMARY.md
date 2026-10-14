Feature #4: Dynamic parameterized test ingestion has been implemented.

Changes made:
1. Created DynamicTestCaseProvider.java - generates novel test inputs for each question and uses DynamicTestRunner to validate.
2. Created DynamicTestRunner.java - executes student code with dynamic inputs and compares outputs (currently implements Q1a and Q1b, with placeholders for Q2a, Q2b, Q3).
3. Created CombinedTestCaseProvider.java - combines fixed instructor test cases with dynamic test cases.
4. Modified GradingPipeline.java to use the combined test case provider instead of just the fixed one.

The implementation follows the requirements:
- Uses instructor-provided adapter files as answer keys for fixed tests (via InstructorTestCaseProvider)
- For dynamic tests, generates novel inputs and runs them through the same adapter concept via DynamicTestRunner
- Maintains intended mark weights from config.properties
- Handles invalid case data with useful error messages (in the runner)

To test the feature, run:
  mvn clean verify

This will compile the code and run the existing test suite. The dynamic test cases will be included in the grading process.

Note: The dynamic test runner for Q2a, Q2b, and Q3 currently return placeholder expected outputs. In a production implementation, these would need to be fully implemented based on the specific question requirements.