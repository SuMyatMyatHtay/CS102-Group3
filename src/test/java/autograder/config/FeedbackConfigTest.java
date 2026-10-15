package autograder.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void missingSettingsDisableFeedbackWithSafeDefaults() throws Exception {
        FeedbackConfig config = FeedbackConfig.load(write(""));

        assertFalse(config.enabled());
        assertEquals("feedback", config.directory());
        assertEquals(40, config.maxDiagnosticLines());
    }

    @Test
    void readsConfiguredSettings() throws Exception {
        FeedbackConfig config = FeedbackConfig.load(write("grading.feedback.enabled=TRUE\n"
                + "grading.feedback.directory=student-feedback\n"
                + "grading.feedback.max.diagnostic.lines=12\n"));

        assertTrue(config.enabled());
        assertEquals("student-feedback", config.directory());
        assertEquals(12, config.maxDiagnosticLines());
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class,
                () -> FeedbackConfig.load(write("grading.feedback.enabled=yes\n")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedbackConfig.load(write("grading.feedback.directory=../outside\n")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedbackConfig.load(write("grading.feedback.directory=submission-1\n")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedbackConfig.load(write("grading.feedback.max.diagnostic.lines=0\n")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedbackConfig.load(write("grading.feedback.max.diagnostic.lines=many\n")));
    }

    private Path write(String content) throws Exception {
        Path file = Files.createTempFile(temporaryDirectory, "feedback", ".properties");
        Files.writeString(file, content);
        return file;
    }
}
