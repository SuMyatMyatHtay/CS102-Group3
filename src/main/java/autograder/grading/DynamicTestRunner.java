package autograder.grading;

import autograder.model.TestCaseDefinition;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Dynamic test runner for CS102 auto-grader.
 * Executes student code with dynamically generated inputs and validates outputs.
 */
public class DynamicTestRunner {

    public static void main(String[] args) {
        // Expect at least 4 arguments: [questionId, serializedInput, serializedExpectedOutput, resultFilePath]
        if (args.length < 4) {
            System.err.println("Usage: java DynamicTestRunner <questionId> <serializedInput> <serializedExpectedOutput> <resultFilePath>");
            System.exit(1);
        }

        String questionId = args[0];
        String serializedInput = args[1];
        String serializedExpectedOutput = args[2];
        String resultFilePath = args[3];

        String status = "ERROR";
        try {
            Object actualOutput = null;
            Object expectedOutput = null;

            switch (questionId) {
                case "Q1a":
                    // Input: ArrayList<String>
                    // Expected output: ArrayList<String> (isograms)
                    ArrayList<String> inputList = deserializeStringArrayList(serializedInput);
                    ArrayList<String> expectedList = deserializeStringArrayList(serializedExpectedOutput);
                    actualOutput = invokeQ1aMethod(inputList);
                    expectedOutput = expectedList;
                    break;
                case "Q1b":
                    // Input: ArrayList<Object>
                    // Expected output: Integer (sum of even integers)
                    ArrayList<Object> inputListObj = deserializeObjectArrayList(serializedInput);
                    Integer expectedInt = deserializeInteger(serializedExpectedOutput);
                    actualOutput = invokeQ1bMethod(inputListObj);
                    expectedOutput = expectedInt;
                    break;
                default:
                    // For other questions, we don't implement the dynamic runner yet.
                    // In a real implementation, we would handle Q2a, Q2b, Q3 similarly.
                    status = "ERROR: Unsupported question for dynamic test: " + questionId;
                    break;
            }

            if (actualOutput != null && expectedOutput != null && status.equals("ERROR")) {
                // Compare actual and expected
                boolean match = deepEquals(actualOutput, expectedOutput);
                status = match ? "PASSED" : "FAILED";
            }

        } catch (Exception e) {
            e.printStackTrace();
            status = "ERROR: " + e.getMessage();
        }

        // Write the status to the result file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFilePath))) {
            writer.write(status);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // --- Deserialization methods ---

    private static ArrayList<String> deserializeStringArrayList(String serialized) {
        ArrayList<String> list = new ArrayList<>();
        if (serialized.startsWith("[") && serialized.endsWith("]")) {
            String content = serialized.substring(1, serialized.length() - 1);
            if (!content.isEmpty()) {
                String[] parts = content.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("\"") && part.endsWith("\"")) {
                        list.add(part.substring(1, part.length() - 1));
                    } else {
                        list.add(part);
                    }
                }
            }
        }
        return list;
    }

    private static ArrayList<Object> deserializeObjectArrayList(String serialized) {
        ArrayList<Object> list = new ArrayList<>();
        if (serialized.startsWith("[") && serialized.endsWith("]")) {
            String content = serialized.substring(1, serialized.length() - 1);
            if (!content.isEmpty()) {
                String[] parts = content.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("\"") && part.endsWith("\"")) {
                        list.add(part.substring(1, part.length() - 1));
                    } else if (part.equalsIgnoreCase("true") || part.equalsIgnoreCase("false")) {
                        list.add(Boolean.parseBoolean(part));
                    } else if (part.matches("-?\\d+")) {
                        list.add(Integer.parseInt(part));
                    } else if (part.matches("-?\\d+(\\.\\d+)?")) {
                        list.add(Double.parseDouble(part));
                    } else {
                        list.add(part);
                    }
                }
            }
        }
        return list;
    }

    private static Integer deserializeInteger(String serialized) {
        try {
            return Integer.parseInt(serialized.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // --- Invocation methods ---

    private static Object invokeQ1aMethod(ArrayList<String> inputs) throws Exception {
        // Load the student's Q1a class
        Class<?> studentClass = Class.forName("Q1a");
        Method method = studentClass.getMethod("getIsogramWords", ArrayList.class);
        // Make the method accessible if it's not public
        method.setAccessible(true);
        // Invoke with null as the instance if the method is static, or with a new instance if not.
        // We don't know if the method is static or instance. Looking at the adapter, it calls:
        //   getIsogramWords(inputs)
        // and the adapter extends Q1a, so the method is likely instance.
        // But the adapter is in the same package and extends Q1a, so it can call protected methods.
        // We'll try to create an instance.
        Object instance = studentClass.getDeclaredConstructor().newInstance();
        return method.invoke(instance, inputs);
    }

    private static Object invokeQ1bMethod(ArrayList<Object> inputs) throws Exception {
        // Load the student's Q1b class
        Class<?> studentClass = Class.forName("Q1b");
        Method method = studentClass.getMethod("getSumOfEvenIntegers", ArrayList.class);
        method.setAccessible(true);
        Object instance = studentClass.getDeclaredConstructor().newInstance();
        return method.invoke(instance, inputs);
    }

    // --- Deep equality check ---

    private static boolean deepEquals(Object o1, Object o2) {
        if (o1 == o2) return true;
        if (o1 == null || o2 == null) return false;
        if (o1.getClass() != o2.getClass()) return false;

        if (o1 instanceof ArrayList) {
            ArrayList<?> list1 = (ArrayList<?>) o1;
            ArrayList<?> list2 = (ArrayList<?>) o2;
            if (list1.size() != list2.size()) return false;
            for (int i = 0; i < list1.size(); i++) {
                if (!deepEquals(list1.get(i), list2.get(i))) return false;
            }
            return true;
        } else if (o1 instanceof Integer) {
            return o1.equals(o2);
        } else {
            return o1.equals(o2);
        }
    }
}