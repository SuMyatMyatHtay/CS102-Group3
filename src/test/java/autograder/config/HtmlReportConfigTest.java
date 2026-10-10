package autograder.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class HtmlReportConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesOlderConfigurationsAndLoadsExplicitSettings() throws Exception {
        assertEquals(new HtmlReportConfig(false, "dashboard.html"), load("grading.directory=grading\n"));
        assertEquals(new HtmlReportConfig(true, "exam-results.html"), load(
                "grading.html.enabled=TRUE\ngrading.html.file=exam-results.html\n"));
        assertEquals(new HtmlReportConfig(false, "exam-results.html"), load(
                "grading.html.enabled=false\ngrading.html.file=exam-results.html\n"));
    }

    @Test
    void rejectsInvalidBooleanAndUnsafeOrConflictingNames() throws Exception {
        for (String value : new String[] {"yes", "1", ""}) {
            assertThrows(IllegalArgumentException.class, () -> load("grading.html.enabled=" + value));
        }
        for (String name : new String[] {"../dashboard.html", "/tmp/report.html", "..\\report.html",
                "C:report.html", "cases.csv", "submissions.csv", "scores.csv", "", ".html",
                "report.html?x", "<report>.html"}) {
            assertThrows(IllegalArgumentException.class, () -> new HtmlReportConfig(true, name), name);
        }
    }

    private HtmlReportConfig load(String settings) throws Exception {
        Path file = temporaryDirectory.resolve("config.properties");
        Files.writeString(file, settings);
        return HtmlReportConfig.load(file);
    }
}
