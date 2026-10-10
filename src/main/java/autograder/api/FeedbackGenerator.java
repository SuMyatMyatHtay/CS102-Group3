package autograder.api;

import autograder.model.StudentGrade;
import java.io.IOException;
import java.nio.file.Path;

public interface FeedbackGenerator {
    Path generate(StudentGrade grade, Path outputDirectory) throws IOException;
}
