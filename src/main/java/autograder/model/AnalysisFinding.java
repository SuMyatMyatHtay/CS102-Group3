package autograder.model;

import java.nio.file.Path;
import java.util.Objects;

public final class AnalysisFinding {
    private final String code;
    private final Path sourceFile;
    private final String message;

    public AnalysisFinding(String code, Path sourceFile, String message) {
        this.code = Objects.requireNonNull(code);
        this.sourceFile = Objects.requireNonNull(sourceFile);
        this.message = Objects.requireNonNull(message);
    }

    public String getCode() {
        return code;
    }
    public Path getSourceFile() {
        return sourceFile;
    }
    public String getMessage() {
        return message;
    }
}
