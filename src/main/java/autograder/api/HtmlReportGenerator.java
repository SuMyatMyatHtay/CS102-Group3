package autograder.api;

import autograder.model.BatchResult;
import java.io.IOException;
import java.nio.file.Path;

public interface HtmlReportGenerator {
    Path generate(BatchResult batch, Path outputDirectory) throws IOException;
}
