package autograder.api;

import autograder.model.GradingReportContext;
import java.io.IOException;
import java.nio.file.Path;

public interface HtmlReportGenerator {
    Path generate(GradingReportContext context, Path outputDirectory) throws IOException;
}
