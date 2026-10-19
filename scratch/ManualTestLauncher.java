package autograder.grading;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minimal launcher so we can run the JUnit test methods without Maven installed.
 * It sets the @TempDir field manually and invokes each @Test method as a plain Java method.
 */
public final class ManualTestLauncher {
    public static void main(String[] args) throws Exception {
        GradingPipelineTest test = new GradingPipelineTest();
        Field tempDir = GradingPipelineTest.class.getDeclaredField("temporaryDirectory");
        tempDir.setAccessible(true);
        Path tmp = Files.createTempDirectory("gradman-");
        tempDir.set(test, tmp);

        run(test, "keepsPassLoopPassMarksAndIsolatesCompilationFailureToItsQuestion");
        run(test, "withholdsDuplicateAndUnresolvedIdentitiesAndRecordsMissingSourceAndRuntimeErrors");
    }

    private static void run(GradingPipelineTest test, String methodName) {
        try {
            GradingPipelineTest.class.getDeclaredMethod(methodName).invoke(test);
            System.out.println("PASS: " + methodName);
        } catch (Throwable thrown) {
            Throwable cause = thrown;
            if (thrown instanceof java.lang.reflect.InvocationTargetException) {
                cause = thrown.getCause();
            }
            System.out.println("FAIL: " + methodName + " -> " + cause);
            cause.printStackTrace(System.out);
        }
    }
}
