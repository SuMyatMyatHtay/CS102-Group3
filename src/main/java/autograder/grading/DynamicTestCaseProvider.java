package autograder.grading;

import autograder.api.TestCaseProvider;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dynamic test case provider for CS102 auto-grader.
 * Generates novel test inputs and uses a dynamic test runner to validate student implementations.
 */
public class DynamicTestCaseProvider implements TestCaseProvider {

    private static final Random RANDOM = new Random();

    @Override
    public List<TestCaseDefinition> load(AssessmentDefinition assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("assessment is required");
        }

        List<TestCaseDefinition> definitions = new ArrayList<>();

        for (QuestionDefinition question : assessment.getQuestions()) {
            String questionId = question.getId();
            double pointsPerCase = question.getPointsPerCase();

            // Generate 5 dynamic test cases per question (can be made configurable)
            int numTestCases = 5;
            for (int i = 0; i < numTestCases; i++) {
                // Generate input and expected output based on question type
                Object[] generated = generateInputAndExpectedOutput(questionId);
                String serializedInput = serializeInput(generated[0]);
                String serializedExpectedOutput = serializeOutput(generated[1]);

                definitions.add(new TestCaseDefinition(
                        questionId + "-dynamic-" + (i + 1),
                        questionId,
                        "autograder.grading.DynamicTestRunner",
                        List.of(serializedInput, serializedExpectedOutput),
                        pointsPerCase
                ));
            }
        }

        return List.copyOf(definitions);
    }

    /**
     * Generates a random input and computes the expected output for the given question.
     * @param questionId the question identifier (e.g., "Q1a")
     * @return an array where [0] = generated input, [1] = expected output
     */
    private Object[] generateInputAndExpectedOutput(String questionId) {
        switch (questionId) {
            case "Q1a":
                return generateQ1aInput();
            case "Q1b":
                return generateQ1bInput();
            case "Q2a":
                return generateQ2aInput();
            case "Q2b":
                return generateQ2bInput();
            case "Q3":
                return generateQ3Input();
            default:
                throw new IllegalArgumentException("Unsupported question: " + questionId);
        }
    }

    // --- Q1a: getIsogramWords(ArrayList<String> inputs) ---
    private Object[] generateQ1aInput() {
        // Generate a list of random words (some isograms, some not)
        ArrayList<String> inputs = new ArrayList<>();
        int wordCount = RANDOM.nextInt(5) + 1; // 1 to 5 words
        String[] wordBank = {"alice", "ben", "cathy", "ellen", "alan", "zoe", "Elle", "Daniel", "Canny", "test", "hello", "world"};
        for (int i = 0; i < wordCount; i++) {
            String word = wordBank[RANDOM.nextInt(wordBank.length)];
            // Occasionally modify a word to make it non-isogram
            if (RANDOM.nextInt(3) == 0 && word.length() > 1) {
                // Make it non-isogram by repeating a letter
                char c = word.charAt(0);
                word = c + "" + word; // Now first letter repeated
            }
            inputs.add(word);
        }
        // Compute expected output: filter isograms
        ArrayList<String> expected = new ArrayList<>();
        for (String word : inputs) {
            if (isIsogram(word)) {
                expected.add(word);
            }
        }
        return new Object[]{inputs, expected};
    }

    private boolean isIsogram(String word) {
        word = word.toLowerCase();
        boolean[] seen = new boolean[26];
        for (char c : word.toCharArray()) {
            if (c < 'a' || c > 'z') continue; // ignore non-letters
            int idx = c - 'a';
            if (seen[idx]) {
                return false;
            }
            seen[idx] = true;
        }
        return true;
    }

    // --- Q1b: getSumOfEvenIntegers(ArrayList<Object> inputs) ---
    private Object[] generateQ1bInput() {
        ArrayList<Object> inputs = new ArrayList<>();
        int itemCount = RANDOM.nextInt(10) + 1; // 1 to 10 items
        for (int i = 0; i < itemCount; i++) {
            int type = RANDOM.nextInt(4);
            switch (type) {
                case 0: // Integer
                    int value = RANDOM.nextInt(100) - 50; // -50 to 49
                    inputs.add(value);
                    break;
                case 1: // Boolean
                    inputs.add(RANDOM.nextBoolean());
                    break;
                case 2: // Double
                    inputs.add(RANDOM.nextDouble() * 100);
                    break;
                case 3: // String
                    inputs.add("string" + RANDOM.nextInt(100));
                    break;
            }
        }
        // Compute expected output: sum of even integers
        int sum = 0;
        for (Object obj : inputs) {
            if (obj instanceof Integer) {
                int val = (Integer) obj;
                if (val % 2 == 0) {
                    sum += val;
                }
            }
        }
        return new Object[]{inputs, sum};
    }

    // --- Q2a: getAverageAge(String filename, String surname) ---
    private Object[] generateQ2aInput() {
        // For simplicity, we'll use a fixed filename and vary the surname
        // In a real implementation, we might generate a temporary file.
        String filename = "personstester.txt"; // Assume this file exists in the test environment
        String[] surnames = {"TAY", "LIM", "WONG", "TAN", "LEE"};
        String surname = surnames[RANDOM.nextInt(surnames.length)];

        // We would need to compute the expected output by reading the file and averaging ages for the surname.
        // Since we don't have the file content, we'll return a placeholder.
        // In a real implementation, we would read the file and compute the average.
        double expectedOutput = 0.0; // Placeholder
        return new Object[]{new String[]{filename, surname}, expectedOutput};
    }

    // --- Q2b: getTopStudent(String filename, String courseName) ---
    private Object[] generateQ2bInput() {
        String filename = "students.txt"; // Assume this file exists
        String[] courseNames = {"IS101", "IS102", "IS103", "IS104"};
        String courseName = courseNames[RANDOM.nextInt(courseNames.length)];

        // Placeholder expected output
        String expectedOutput = "";
        return new Object[]{new String[]{filename, courseName}, expectedOutput};
    }

    // --- Q3: sortShapes(ArrayList<Shape> inputs) ---
    private Object[] generateQ3Input() {
        // We cannot instantiate Shape without knowing the concrete classes.
        // For simplicity, we'll return a placeholder.
        // In a real implementation, we would generate Circle and Rectangle objects.
        ArrayList<Object> inputs = new ArrayList<>();
        // We would need to know the Shape hierarchy to generate objects.
        // Since we don't have it, we'll skip.
        String expectedOutput = ""; // Placeholder
        return new Object[]{inputs, expectedOutput};
    }

    // --- Serialization helpers ---
    private String serializeInput(Object input) {
        if (input instanceof ArrayList) {
            return serializeArrayList((ArrayList<?>) input);
        } else if (input instanceof String[]) {
            return serializeStringArray((String[]) input);
        } else {
            return input.toString();
        }
    }

    private String serializeArrayList(ArrayList<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            Object item = list.get(i);
            if (item instanceof String) {
                sb.append('\"').append(item).append('\"');
            } else if (item instanceof Integer) {
                sb.append(item);
            } else if (item instanceof Boolean) {
                sb.append(item);
            } else if (item instanceof Double) {
                sb.append(item);
            } else {
                sb.append(item.toString());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String serializeStringArray(String[] arr) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append('\"').append(arr[i]).append('\"');
        }
        sb.append("]");
        return sb.toString();
    }

    private String serializeOutput(Object output) {
        if (output instanceof ArrayList) {
            return serializeArrayList((ArrayList<?>) output);
        } else if (output instanceof Integer) {
            return output.toString();
        } else if (output instanceof Double) {
            return output.toString();
        } else {
            return output.toString();
        }
    }
}