package autograder.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlagiarismConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsSettingsAndResolvesStarterDirectoryRelativeToTheFile() throws Exception {
        Files.createDirectories(temporaryDirectory.resolve("starter"));

        PlagiarismConfig config = PlagiarismConfig.load(write("true", "5", "0.8", "20", "starter"));

        assertTrue(config.enabled());
        assertEquals(5, config.ngramSize());
        assertEquals(0.8, config.threshold());
        assertEquals(20, config.minimumNgrams());
        assertEquals(temporaryDirectory.resolve("starter").toRealPath(), config.starterDirectory());
    }

    @Test
    void rejectsInvalidValuesWithTheSettingName() throws Exception {
        Files.createDirectories(temporaryDirectory.resolve("starter"));

        assertMessage("plagiarism.ngram.size", write("true", "0", "0.8", "20", "starter"));
        assertMessage("plagiarism.threshold", write("true", "5", "1.5", "20", "starter"));
        assertMessage("plagiarism.threshold", write("true", "5", "0", "20", "starter"));
        assertMessage("plagiarism.min.ngrams", write("true", "5", "0.8", "abc", "starter"));
        assertMessage("plagiarism.enabled", write("yes", "5", "0.8", "20", "starter"));
        assertMessage("plagiarism.starter.directory", write("true", "5", "0.8", "20", "missing"));
    }

    @Test
    void reportsMissingSettings() throws Exception {
        Path file = temporaryDirectory.resolve("empty.properties");
        Files.writeString(file, "");

        assertMessage("plagiarism.enabled", file);
    }

    private void assertMessage(String expected, Path file) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> PlagiarismConfig.load(file));
        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
    }

    private Path write(String enabled, String size, String threshold, String minimum, String starter)
            throws Exception {
        Path file = Files.createTempFile(temporaryDirectory, "plagiarism", ".properties");
        Files.writeString(file, "plagiarism.enabled=" + enabled + "\n"
                + "plagiarism.ngram.size=" + size + "\n"
                + "plagiarism.threshold=" + threshold + "\n"
                + "plagiarism.min.ngrams=" + minimum + "\n"
                + "plagiarism.starter.directory=" + starter + "\n");
        return file;
    }
}
