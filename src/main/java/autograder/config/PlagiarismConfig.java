package autograder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record PlagiarismConfig(boolean enabled, int ngramSize, double threshold, int minimumNgrams,
                               Path starterDirectory) {
    public PlagiarismConfig {
        if (ngramSize < 1) {
            throw new IllegalArgumentException("plagiarism.ngram.size must be at least 1");
        }
        if (!Double.isFinite(threshold) || threshold <= 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("plagiarism.threshold must be above 0 and at most 1");
        }
        if (minimumNgrams < 1) {
            throw new IllegalArgumentException("plagiarism.min.ngrams must be at least 1");
        }
    }

    public static PlagiarismConfig load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        Path base = file.toAbsolutePath().normalize().getParent();
        String enabled = required(properties, "plagiarism.enabled");
        if (!enabled.equals("true") && !enabled.equals("false")) {
            throw new IllegalArgumentException("plagiarism.enabled must be true or false");
        }
        Path starterDirectory = AppConfig.canonicalize(
                base.resolve(required(properties, "plagiarism.starter.directory")));
        if (!Files.isDirectory(starterDirectory)) {
            throw new IllegalArgumentException("plagiarism.starter.directory is not a directory: "
                    + starterDirectory);
        }
        return new PlagiarismConfig(Boolean.parseBoolean(enabled),
                integer(properties, "plagiarism.ngram.size"),
                decimal(properties, "plagiarism.threshold"),
                integer(properties, "plagiarism.min.ngrams"), starterDirectory);
    }

    private static int integer(Properties properties, String key) {
        try {
            return Integer.parseInt(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid whole number: " + key, exception);
        }
    }

    private static double decimal(Properties properties, String key) {
        try {
            return Double.parseDouble(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid number: " + key, exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required configuration value: " + key);
        }
        return value.trim();
    }
}
