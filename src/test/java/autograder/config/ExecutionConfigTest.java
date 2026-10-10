package autograder.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesToolAndOutputPathsAgainstConfiguration() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        properties.setProperty("execution.javac", "tools/custom-javac");
        ExecutionConfig config = load(properties, app);
        assertEquals(temporaryDirectory.toRealPath().resolve("grading"), config.directory());
        assertEquals(temporaryDirectory.resolve("tools/custom-javac").toString(), config.javacExecutable());
        assertTrue(Path.of(config.javaExecutable()).isAbsolute());
    }

    @Test
    void rejectsInputOverlapAndInvalidLimitsOrReportNames() throws Exception {
        AppConfig app = AppConfig.load(Path.of("config.properties"));
        Properties properties = settings();
        properties.setProperty("grading.directory", app.getInputDirectory().resolve("output").toString());
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
        properties.setProperty("grading.directory", "grading");
        properties.setProperty("execution.case.timeout.ms", "0");
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
        properties.setProperty("execution.case.timeout.ms", "2000");
        properties.setProperty("grading.report.file", "../source.csv");
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
        properties.setProperty("grading.report.file", "cases.csv");
        assertThrows(IllegalArgumentException.class, () -> load(properties, app));
    }

    private ExecutionConfig load(Properties properties, AppConfig app) throws Exception {
        Path path = temporaryDirectory.resolve("execution.properties");
        try (var output = Files.newOutputStream(path)) {
            properties.store(output, null);
        }
        return ExecutionConfig.load(path, app);
    }

    private Properties settings() {
        Properties properties = new Properties();
        properties.setProperty("grading.directory", "grading");
        properties.setProperty("grading.report.file", "scores.csv");
        properties.setProperty("execution.java", "JDK");
        properties.setProperty("execution.javac", "JDK");
        properties.setProperty("execution.compile.timeout.ms", "15000");
        properties.setProperty("execution.case.timeout.ms", "2000");
        properties.setProperty("execution.output.max.bytes", "65536");
        return properties;
    }
}
