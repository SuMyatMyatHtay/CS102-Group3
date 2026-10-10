package autograder.config;

import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class AppConfig {
    private static final List<String> SCORE_COLUMN_KEYS = List.of(
            "id", "username", "lastName", "firstName", "email",
            "numerator", "denominator", "end");

    private final Path inputDirectory;
    private final Path extractionDirectory;
    private final Path workingDirectory;
    private final String runDirectoryPrefix;
    private final String workingRunDirectoryPrefix;
    private final String workingSubmissionDirectoryPrefix;
    private final Path anomalyLog;
    private final Path correctionLog;
    private final Pattern archiveFilePattern;
    private final Pattern javaSourcePattern;
    private final Pattern questionDirectoryPattern;
    private final List<String> requiredQuestionDirectories;
    private final Pattern metadataDirectoryPattern;
    private final Pattern identityNamePattern;
    private final Pattern identityEmailPattern;
    private final Pattern identityUsernamePattern;
    private final String identityNameLabel;
    private final String identityEmailLabel;
    private final String identityUsernameLabel;
    private final Pattern archiveUsernamePattern;
    private final Pattern wrapperUsernamePattern;
    private final Pattern wrapperStudentIdPattern;
    private final Pattern wrapperPlaceholderPattern;
    private final String rosterPrefix;
    private final String rosterLastNamePlaceholder;
    private final int identityHeaderMaxLines;
    private final int ioBufferBytes;
    private final int maxArchiveEntries;
    private final long maxEntryBytes;
    private final long maxArchiveBytes;
    private final Path scoreSheetPath;
    private final Map<String, String> scoreSheetColumns;
    private final AssessmentDefinition assessment;

    private AppConfig(Path inputDirectory, Path extractionDirectory, Path workingDirectory,
                      String runDirectoryPrefix, String workingRunDirectoryPrefix,
                      String workingSubmissionDirectoryPrefix, Path anomalyLog,
                      Path correctionLog,
                      Pattern archiveFilePattern, Pattern javaSourcePattern,
                      Pattern questionDirectoryPattern, List<String> requiredQuestionDirectories,
                      Pattern metadataDirectoryPattern, Pattern identityNamePattern,
                      Pattern identityEmailPattern, Pattern identityUsernamePattern,
                      String identityNameLabel, String identityEmailLabel,
                      String identityUsernameLabel,
                      Pattern archiveUsernamePattern, Pattern wrapperUsernamePattern,
                      Pattern wrapperStudentIdPattern, Pattern wrapperPlaceholderPattern,
                      String rosterPrefix, String rosterLastNamePlaceholder,
                      int identityHeaderMaxLines, int ioBufferBytes, int maxArchiveEntries,
                      long maxEntryBytes, long maxArchiveBytes, Path scoreSheetPath,
                      Map<String, String> scoreSheetColumns, AssessmentDefinition assessment) {
        this.inputDirectory = inputDirectory;
        this.extractionDirectory = extractionDirectory;
        this.workingDirectory = workingDirectory;
        this.runDirectoryPrefix = runDirectoryPrefix;
        this.workingRunDirectoryPrefix = workingRunDirectoryPrefix;
        this.workingSubmissionDirectoryPrefix = workingSubmissionDirectoryPrefix;
        this.anomalyLog = anomalyLog;
        this.correctionLog = correctionLog;
        this.archiveFilePattern = archiveFilePattern;
        this.javaSourcePattern = javaSourcePattern;
        this.questionDirectoryPattern = questionDirectoryPattern;
        this.requiredQuestionDirectories = Collections.unmodifiableList(
                new ArrayList<>(requiredQuestionDirectories));
        this.metadataDirectoryPattern = metadataDirectoryPattern;
        this.identityNamePattern = identityNamePattern;
        this.identityEmailPattern = identityEmailPattern;
        this.identityUsernamePattern = identityUsernamePattern;
        this.identityNameLabel = identityNameLabel;
        this.identityEmailLabel = identityEmailLabel;
        this.identityUsernameLabel = identityUsernameLabel;
        this.archiveUsernamePattern = archiveUsernamePattern;
        this.wrapperUsernamePattern = wrapperUsernamePattern;
        this.wrapperStudentIdPattern = wrapperStudentIdPattern;
        this.wrapperPlaceholderPattern = wrapperPlaceholderPattern;
        this.rosterPrefix = rosterPrefix;
        this.rosterLastNamePlaceholder = rosterLastNamePlaceholder;
        this.identityHeaderMaxLines = identityHeaderMaxLines;
        this.ioBufferBytes = ioBufferBytes;
        this.maxArchiveEntries = maxArchiveEntries;
        this.maxEntryBytes = maxEntryBytes;
        this.maxArchiveBytes = maxArchiveBytes;
        this.scoreSheetPath = scoreSheetPath;
        this.scoreSheetColumns = Collections.unmodifiableMap(
                new LinkedHashMap<>(scoreSheetColumns));
        this.assessment = assessment;
    }

    public static AppConfig load(Path configPath) throws IOException {
        Path absoluteConfigPath = configPath.toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (InputStream input = java.nio.file.Files.newInputStream(absoluteConfigPath)) {
            properties.load(input);
        }
        Path baseDirectory = absoluteConfigPath.getParent();
        if (baseDirectory == null) {
            throw new IllegalArgumentException("configuration path must have a parent directory");
        }

        Pattern questionDirectoryPattern = compile(properties, "question.directory.regex", false);
        List<String> requiredQuestionDirectories = parseNames(
                required(properties, "question.directories.required"), "question.directories.required");
        for (String directory : requiredQuestionDirectories) {
            if (!questionDirectoryPattern.matcher(directory).matches()) {
                throw new IllegalArgumentException("required question directory does not match its pattern: "
                        + directory);
            }
        }

        Path scoreSheetPath = canonicalize(resolvePath(baseDirectory,
                required(properties, "score.sheet.path")));
        Map<String, String> scoreSheetColumns = loadScoreSheetColumns(properties);
        Pattern javaSourcePattern = compile(properties, "java.source.regex", false);
        AssessmentDefinition assessment = loadAssessment(properties, baseDirectory,
                questionDirectoryPattern, javaSourcePattern);
        assessment = canonicalizeAssessment(assessment);
        for (QuestionDefinition question : assessment.getQuestions()) {
            if (!requiredQuestionDirectories.contains(question.getFolder())) {
                throw new IllegalArgumentException("assessment question uses an undeclared question folder: "
                        + question.getFolder());
            }
        }

        Path inputDirectory = canonicalize(resolvePath(baseDirectory,
                required(properties, "input.directory")));
        Path extractionDirectory = canonicalize(resolvePath(baseDirectory,
                required(properties, "extraction.directory")));
        Path workingDirectory = canonicalize(resolvePath(baseDirectory,
                required(properties, "working.directory")));
        Path anomalyLog = canonicalize(resolvePath(baseDirectory, required(properties, "anomaly.log")));
        Path correctionLog = canonicalize(resolvePath(baseDirectory,
                required(properties, "correction.log")));
        validateOutputPaths(inputDirectory, extractionDirectory, workingDirectory,
                anomalyLog, correctionLog, scoreSheetPath, assessment);

        return new AppConfig(
                inputDirectory,
                extractionDirectory,
                workingDirectory,
                pathPrefix(properties, "run.directory.prefix"),
                pathPrefix(properties, "working.run.directory.prefix"),
                pathPrefix(properties, "working.submission.directory.prefix"),
                anomalyLog,
                correctionLog,
                compile(properties, "archive.file.regex", false),
                javaSourcePattern,
                questionDirectoryPattern,
                requiredQuestionDirectories,
                compile(properties, "metadata.directory.regex", false),
                compile(properties, "identity.name.regex", true),
                compile(properties, "identity.email.regex", true),
                compile(properties, "identity.username.regex", true),
                required(properties, "identity.name.label"),
                required(properties, "identity.email.label"),
                required(properties, "identity.username.label"),
                compile(properties, "identity.archive.username.regex", true),
                compile(properties, "identity.wrapper.username.regex", false),
                compile(properties, "identity.wrapper.student.id.regex", false),
                compile(properties, "identity.wrapper.placeholder.regex", false),
                required(properties, "identity.roster.prefix"),
                required(properties, "identity.roster.last-name.placeholder"),
                positiveInt(properties, "identity.header.max.lines"),
                positiveInt(properties, "io.buffer.bytes"),
                positiveInt(properties, "limits.archive.entries"),
                positiveLong(properties, "limits.entry.bytes"),
                positiveLong(properties, "limits.archive.bytes"),
                scoreSheetPath, scoreSheetColumns, assessment);
    }

    private static Map<String, String> loadScoreSheetColumns(Properties properties) {
        Map<String, String> columns = new LinkedHashMap<>();
        for (String key : SCORE_COLUMN_KEYS) {
            columns.put(key, required(properties, "score.sheet.columns." + key));
        }
        return columns;
    }

    private static AssessmentDefinition loadAssessment(Properties properties, Path baseDirectory,
                                                       Pattern questionDirectoryPattern,
                                                       Pattern javaSourcePattern) {
        String assessmentId = required(properties, "assessment.id");
        String assessmentName = required(properties, "assessment.name");
        List<String> questionIds = parseNames(required(properties, "assessment.question.ids"),
                "assessment.question.ids");
        List<QuestionDefinition> questions = new ArrayList<>();

        for (String questionId : questionIds) {
            String propertyPrefix = "assessment.question." + questionId + ".";
            String folder = required(properties, propertyPrefix + "folder");
            String sourceFile = required(properties, propertyPrefix + "source.file");
            if (!questionDirectoryPattern.matcher(folder).matches()) {
                throw new IllegalArgumentException("assessment folder does not match its pattern: " + folder);
            }
            Path sourcePath = Path.of(sourceFile);
            if (sourcePath.isAbsolute() || sourcePath.getNameCount() != 1) {
                throw new IllegalArgumentException("assessment source file must be a file name: " + sourceFile);
            }
            if (!javaSourcePattern.matcher(sourceFile).matches()) {
                throw new IllegalArgumentException("assessment source file does not match its pattern: "
                        + sourceFile);
            }
            Path testerFile = resolvePath(baseDirectory, required(properties, propertyPrefix + "tester.file"));
            int caseCount = positiveInt(properties, propertyPrefix + "case.count");
            double pointsPerCase = positiveDouble(properties, propertyPrefix + "points.per.case");
            List<Path> supportFiles = pathList(baseDirectory,
                    properties.getProperty(propertyPrefix + "support.files"));
            List<Path> fixtures = pathList(baseDirectory,
                    properties.getProperty(propertyPrefix + "fixtures"));
            List<String> companionSourceFiles = parseOptionalNames(
                    properties.getProperty(propertyPrefix + "companion.source.files"));
            for (String companionSourceFile : companionSourceFiles) {
                Path companionPath = Path.of(companionSourceFile);
                if (companionPath.isAbsolute() || companionPath.getNameCount() != 1
                        || !javaSourcePattern.matcher(companionSourceFile).matches()) {
                    throw new IllegalArgumentException("invalid companion source file: "
                            + companionSourceFile);
                }
            }
            questions.add(new QuestionDefinition(questionId, folder, sourceFile, testerFile,
                    caseCount, pointsPerCase, supportFiles, fixtures, companionSourceFiles));
        }
        return new AssessmentDefinition(assessmentId, assessmentName, questions);
    }

    private static AssessmentDefinition canonicalizeAssessment(AssessmentDefinition assessment)
            throws IOException {
        List<QuestionDefinition> questions = new ArrayList<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            List<Path> supportFiles = new ArrayList<>();
            for (Path path : question.getSupportFiles()) {
                supportFiles.add(canonicalize(path));
            }
            List<Path> fixtures = new ArrayList<>();
            for (Path path : question.getFixtures()) {
                fixtures.add(canonicalize(path));
            }
            questions.add(new QuestionDefinition(question.getId(), question.getFolder(),
                    question.getSourceFile(), canonicalize(question.getTesterFile()),
                    question.getCaseCount(), question.getPointsPerCase(), supportFiles,
                    fixtures, question.getCompanionSourceFiles()));
        }
        return new AssessmentDefinition(assessment.getId(), assessment.getName(), questions);
    }

    static Path canonicalize(Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath().normalize();
        Path existingPath = absolutePath;
        List<Path> missingNames = new ArrayList<>();
        while (existingPath != null && !Files.exists(existingPath, LinkOption.NOFOLLOW_LINKS)) {
            Path name = existingPath.getFileName();
            if (name != null) {
                missingNames.add(name);
            }
            existingPath = existingPath.getParent();
        }
        if (existingPath == null) {
            return absolutePath;
        }
        Path canonicalPath = existingPath.toRealPath();
        for (int index = missingNames.size() - 1; index >= 0; index--) {
            canonicalPath = canonicalPath.resolve(missingNames.get(index));
        }
        return canonicalPath.normalize();
    }

    private static void validateOutputPaths(Path inputDirectory, Path extractionDirectory,
                                            Path workingDirectory, Path anomalyLog,
                                            Path correctionLog, Path scoreSheetPath,
                                            AssessmentDefinition assessment) {
        requireDisjoint(extractionDirectory, workingDirectory,
                "extraction and working directories must be separate");
        requireDisjoint(extractionDirectory, inputDirectory,
                "extraction directory cannot overlap submission input");
        requireDisjoint(workingDirectory, inputDirectory,
                "working directory cannot overlap submission input");

        List<Path> protectedFiles = new ArrayList<>();
        protectedFiles.add(scoreSheetPath);
        for (QuestionDefinition question : assessment.getQuestions()) {
            protectedFiles.add(question.getTesterFile());
            for (Path path : question.getSupportFiles()) {
                protectedFiles.add(path);
            }
            for (Path path : question.getFixtures()) {
                protectedFiles.add(path);
            }
        }

        List<Path> outputDirectories = List.of(extractionDirectory, workingDirectory);
        for (Path outputDirectory : outputDirectories) {
            for (Path protectedFile : protectedFiles) {
                if (protectedFile.startsWith(outputDirectory)
                        || outputDirectory.equals(protectedFile)
                        || outputDirectory.startsWith(protectedFile)) {
                    throw new IllegalArgumentException("output directory overlaps an input resource: "
                            + protectedFile);
                }
            }
        }

        List<Path> logPaths = List.of(anomalyLog, correctionLog);
        requireDisjoint(anomalyLog, correctionLog,
                "anomaly and correction logs must be separate paths");
        for (Path logPath : logPaths) {
            if (logPath.startsWith(inputDirectory) || inputDirectory.startsWith(logPath)) {
                throw new IllegalArgumentException("log path cannot overlap the submission directory");
            }
            for (Path outputDirectory : outputDirectories) {
                if (logPath.startsWith(outputDirectory) || outputDirectory.startsWith(logPath)) {
                    throw new IllegalArgumentException("log path overlaps an output directory");
                }
            }
            for (Path protectedFile : protectedFiles) {
                if (logPath.equals(protectedFile) || logPath.startsWith(protectedFile)
                        || protectedFile.startsWith(logPath)) {
                    throw new IllegalArgumentException("log path overlaps an input resource: "
                            + protectedFile);
                }
            }
        }
    }

    private static void requireDisjoint(Path first, Path second, String message) {
        if (first.startsWith(second) || second.startsWith(first)) {
            throw new IllegalArgumentException(message + ": " + first + " and " + second);
        }
    }

    private static List<Path> pathList(Path baseDirectory, String value) {
        List<Path> paths = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return paths;
        }
        for (String item : value.split(",", -1)) {
            String path = item.trim();
            if (!path.isEmpty()) {
                paths.add(resolvePath(baseDirectory, path));
            }
        }
        return paths;
    }

    private static List<String> parseNames(String value, String key) {
        List<String> names = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            String name = item.trim();
            if (name.isEmpty() || names.contains(name)) {
                throw new IllegalArgumentException("invalid or duplicate value in configuration: " + key);
            }
            names.add(name);
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("configuration list cannot be empty: " + key);
        }
        return names;
    }

    private static List<String> parseOptionalNames(String value) {
        List<String> names = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return names;
        }
        for (String item : value.split(",", -1)) {
            String name = item.trim();
            if (name.isEmpty() || names.contains(name)) {
                throw new IllegalArgumentException("invalid or duplicate companion source file");
            }
            names.add(name);
        }
        return names;
    }

    private static Path resolvePath(Path baseDirectory, String value) {
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return baseDirectory.resolve(path).normalize();
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required configuration value: " + key);
        }
        return value.trim();
    }

    private static String pathPrefix(Properties properties, String key) {
        String value = required(properties, key);
        Path path;
        try {
            path = Path.of(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("invalid output directory prefix: " + key, exception);
        }
        if (value.contains("/") || value.contains("\\") || path.isAbsolute()
                || path.getNameCount() != 1) {
            throw new IllegalArgumentException("output directory prefix must be a single path segment: "
                    + key);
        }
        return value;
    }

    private static Pattern compile(Properties properties, String key, boolean needsCaptureGroup) {
        try {
            Pattern pattern = Pattern.compile(required(properties, key));
            if (needsCaptureGroup && pattern.matcher("").groupCount() == 0) {
                throw new IllegalArgumentException("configuration pattern needs a capture group: " + key);
            }
            return pattern;
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("invalid regular expression in configuration: " + key,
                    exception);
        }
    }

    private static int positiveInt(Properties properties, String key) {
        try {
            int value = Integer.parseInt(required(properties, key));
            if (value <= 0) {
                throw new IllegalArgumentException("configuration value must be positive: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("configuration value must be an integer: " + key,
                    exception);
        }
    }

    private static long positiveLong(Properties properties, String key) {
        try {
            long value = Long.parseLong(required(properties, key));
            if (value <= 0) {
                throw new IllegalArgumentException("configuration value must be positive: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("configuration value must be an integer: " + key,
                    exception);
        }
    }

    private static double positiveDouble(Properties properties, String key) {
        try {
            double value = Double.parseDouble(required(properties, key));
            if (!Double.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException("configuration value must be positive: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("configuration value must be numeric: " + key,
                    exception);
        }
    }

    public Path getInputDirectory() {
        return inputDirectory;
    }

    public Path getExtractionDirectory() {
        return extractionDirectory;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public String getRunDirectoryPrefix() {
        return runDirectoryPrefix;
    }

    public String getWorkingRunDirectoryPrefix() {
        return workingRunDirectoryPrefix;
    }

    public String getWorkingSubmissionDirectoryPrefix() {
        return workingSubmissionDirectoryPrefix;
    }

    public Path getAnomalyLog() {
        return anomalyLog;
    }

    public Path getCorrectionLog() {
        return correctionLog;
    }

    public Pattern getArchiveFilePattern() {
        return archiveFilePattern;
    }

    public Pattern getJavaSourcePattern() {
        return javaSourcePattern;
    }

    public Pattern getQuestionDirectoryPattern() {
        return questionDirectoryPattern;
    }

    public List<String> getRequiredQuestionDirectories() {
        return requiredQuestionDirectories;
    }

    public Pattern getMetadataDirectoryPattern() {
        return metadataDirectoryPattern;
    }

    public Pattern getIdentityNamePattern() {
        return identityNamePattern;
    }

    public Pattern getIdentityEmailPattern() {
        return identityEmailPattern;
    }

    public Pattern getIdentityUsernamePattern() {
        return identityUsernamePattern;
    }

    public String getIdentityNameLabel() {
        return identityNameLabel;
    }

    public String getIdentityEmailLabel() {
        return identityEmailLabel;
    }

    public String getIdentityUsernameLabel() {
        return identityUsernameLabel;
    }

    public Pattern getArchiveUsernamePattern() {
        return archiveUsernamePattern;
    }

    public Pattern getWrapperUsernamePattern() {
        return wrapperUsernamePattern;
    }

    public Pattern getWrapperStudentIdPattern() {
        return wrapperStudentIdPattern;
    }

    public Pattern getWrapperPlaceholderPattern() {
        return wrapperPlaceholderPattern;
    }

    public String getRosterPrefix() {
        return rosterPrefix;
    }

    public String getRosterLastNamePlaceholder() {
        return rosterLastNamePlaceholder;
    }

    public int getIdentityHeaderMaxLines() {
        return identityHeaderMaxLines;
    }

    public int getIoBufferBytes() {
        return ioBufferBytes;
    }

    public int getMaxArchiveEntries() {
        return maxArchiveEntries;
    }

    public long getMaxEntryBytes() {
        return maxEntryBytes;
    }

    public long getMaxArchiveBytes() {
        return maxArchiveBytes;
    }

    public Path getScoreSheetPath() {
        return scoreSheetPath;
    }

    public Map<String, String> getScoreSheetColumns() {
        return scoreSheetColumns;
    }

    public AssessmentDefinition getAssessment() {
        return assessment;
    }
}
