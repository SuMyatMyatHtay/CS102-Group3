package autograder.submission;

import autograder.config.AppConfig;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.StudentIdentity;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;

import java.io.IOException;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IdentityResolver {
    private final AppConfig config;

    public IdentityResolver(AppConfig config) {
        this.config = config;
    }

    public Resolution resolve(Path archivePath, Path extractionPath, Path studentRoot,
                              AssessmentDefinition assessment, List<StudentRecord> roster)
            throws IOException {
        List<String> anomalies = new ArrayList<>();
        List<ParsedSource> parsedSources = new ArrayList<>();
        List<StrongEvidence> strongEvidence = new ArrayList<>();
        Set<String> names = new TreeSet<>();
        Set<String> emailIds = new TreeSet<>();
        Set<String> usernames = new TreeSet<>();

        addArchiveEvidence(archivePath, strongEvidence, anomalies);
        addWrapperEvidence(extractionPath, studentRoot, strongEvidence);
        validateAndReadSources(studentRoot, assessment, parsedSources, strongEvidence,
                names, emailIds, usernames, anomalies);

        StudentRecord studentRecord = resolveRosterRecord(strongEvidence, roster, anomalies);
        List<SubmissionCorrection> corrections = new ArrayList<>();
        if (studentRecord == null) {
            addUnresolvedHeaderAnomalies(parsedSources, anomalies);
        } else {
            prepareHeaderCorrections(parsedSources, studentRecord, corrections, anomalies);
        }

        String name = onlyValue(names);
        String emailId = onlyValue(emailIds);
        String username = onlyValue(usernames);
        if (studentRecord == null && name.isEmpty() && emailId.isEmpty() && username.isEmpty()) {
            name = "archive-derived fallback: " + archiveStem(archivePath);
        }
        Set<String> evidenceSources = new LinkedHashSet<>();
        List<String> evidenceLabels = new ArrayList<>();
        for (StrongEvidence evidence : strongEvidence) {
            evidenceSources.add(evidence.source);
            evidenceLabels.add(evidence.source + ": " + evidence.value);
        }
        String source;
        if (studentRecord == null) {
            source = "unresolved strong evidence";
        } else {
            source = String.join("+", evidenceSources);
        }
        StudentIdentity identity = new StudentIdentity(name, emailId, username,
                new ArrayList<>(names), new ArrayList<>(emailIds), new ArrayList<>(usernames),
                studentRecord, source, evidenceLabels);
        return new Resolution(identity, anomalies, corrections, parsedSources.size());
    }

    public List<SubmissionCorrection> applyCorrections(Path workingRoot,
                                                        List<SubmissionCorrection> corrections)
            throws IOException {
        Map<Path, List<SubmissionCorrection>> correctionsByFile = new LinkedHashMap<>();
        for (SubmissionCorrection correction : corrections) {
            if (correction.getType().equals("header")) {
                correctionsByFile.computeIfAbsent(correction.getRelativePath(), key -> new ArrayList<>())
                        .add(correction);
            }
        }

        List<SubmissionCorrection> applied = new ArrayList<>();
        for (Map.Entry<Path, List<SubmissionCorrection>> entry : correctionsByFile.entrySet()) {
            Path sourcePath = workingRoot.resolve(entry.getKey()).normalize();
            if (!sourcePath.startsWith(workingRoot)) {
                throw new IOException("header correction path escapes the working directory");
            }
            List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
            List<String> addedLines = new ArrayList<>();
            for (SubmissionCorrection correction : entry.getValue()) {
                Pattern pattern = patternForField(correction.getField());
                boolean found = false;
                boolean filled = false;
                int lineLimit = Math.min(lines.size(), config.getIdentityHeaderMaxLines());
                for (int lineIndex = 0; lineIndex < lineLimit; lineIndex++) {
                    Matcher matcher = pattern.matcher(lines.get(lineIndex));
                    if (matcher.matches()) {
                        found = true;
                        if (matcher.group(1).trim().isEmpty()) {
                            String line = lines.get(lineIndex);
                            String beforeValue = line.substring(0, matcher.start(1));
                            if (!beforeValue.endsWith(" ")) {
                                beforeValue = beforeValue + " ";
                            }
                            lines.set(lineIndex, beforeValue + correction.getCorrectedValue()
                                    + line.substring(matcher.end(1)));
                            filled = true;
                        }
                    }
                }
                if (!found && correction.isHeaderAdded()) {
                    addedLines.add(" * " + headerLabelForField(correction.getField()) + ": "
                            + correction.getCorrectedValue());
                    applied.add(correction);
                } else if (filled) {
                    applied.add(correction);
                }
            }

            if (!addedLines.isEmpty()) {
                List<String> headerBlock = new ArrayList<>();
                headerBlock.add("/*");
                headerBlock.addAll(addedLines);
                headerBlock.add(" */");
                lines.addAll(0, headerBlock);
            }
            Files.write(sourcePath, lines, StandardCharsets.UTF_8);
        }
        return applied;
    }

    private void addArchiveEvidence(Path archivePath, List<StrongEvidence> evidence,
                                    List<String> anomalies) {
        String fileName = archivePath.getFileName().toString();
        Matcher matcher = config.getArchiveUsernamePattern().matcher(fileName);
        if (!matcher.matches()) {
            anomalies.add("archive name does not match the configured student username pattern");
            return;
        }
        String username = matcher.group(1).trim();
        if (username.isEmpty()) {
            anomalies.add("archive name contains an empty student username");
            return;
        }
        evidence.add(new StrongEvidence(EvidenceType.USERNAME, username, "archive"));
    }

    private void addWrapperEvidence(Path extractionPath, Path studentRoot,
                                   List<StrongEvidence> evidence) {
        if (studentRoot == null || extractionPath.equals(studentRoot)) {
            return;
        }
        Path relativePath = extractionPath.relativize(studentRoot);
        for (Path segment : relativePath) {
            String value = segment.toString();
            if (config.getWrapperPlaceholderPattern().matcher(value).matches()) {
                continue;
            }
            if (config.getWrapperUsernamePattern().matcher(value).matches()) {
                evidence.add(new StrongEvidence(EvidenceType.USERNAME, value, "wrapper username"));
            } else if (config.getWrapperStudentIdPattern().matcher(value).matches()) {
                evidence.add(new StrongEvidence(EvidenceType.STUDENT_ID, value, "wrapper student id"));
            }
        }
    }

    private void validateAndReadSources(Path studentRoot, AssessmentDefinition assessment,
                                        List<ParsedSource> parsedSources,
                                        List<StrongEvidence> evidence, Set<String> names,
                                        Set<String> emailIds, Set<String> usernames,
                                        List<String> anomalies) throws IOException {
        for (QuestionDefinition question : assessment.getQuestions()) {
            if (studentRoot == null) {
                anomalies.add("required source cannot be checked because the student root is unresolved: "
                        + question.getId());
                continue;
            }
            Path questionDirectory = studentRoot.resolve(question.getFolder()).normalize();
            Path sourcePath = questionDirectory.resolve(question.getSourceFile()).normalize();
            if (!sourcePath.startsWith(studentRoot)
                    || !hasExactDirectory(studentRoot, question.getFolder())
                    || !hasExactFile(questionDirectory, question.getSourceFile())) {
                anomalies.add("required source is missing: " + question.getId() + " ("
                        + question.getFolder() + "/" + question.getSourceFile() + ")");
            } else {
                readSource(question.getId(), sourcePath, studentRoot, parsedSources,
                        evidence, names, emailIds, usernames);
            }
            for (String companion : question.getCompanionSourceFiles()) {
                Path companionPath = questionDirectory.resolve(companion).normalize();
                if (!companionPath.startsWith(studentRoot)
                        || !hasExactDirectory(studentRoot, question.getFolder())
                        || !hasExactFile(questionDirectory, companion)) {
                    anomalies.add("required companion source is missing: " + question.getId()
                            + " (" + question.getFolder() + "/" + companion + ")");
                } else {
                    readSource(question.getId(), companionPath, studentRoot, parsedSources,
                            evidence, names, emailIds, usernames);
                }
            }
        }
    }

    private void readSource(String questionId, Path sourcePath, Path studentRoot,
                            List<ParsedSource> parsedSources, List<StrongEvidence> evidence,
                            Set<String> names, Set<String> emailIds, Set<String> usernames)
            throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            String line;
            while (lines.size() < config.getIdentityHeaderMaxLines()
                    && (line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        HeaderField name = readField(lines, config.getIdentityNamePattern());
        HeaderField emailId = readField(lines, config.getIdentityEmailPattern());
        HeaderField username = readField(lines, config.getIdentityUsernamePattern());
        ParsedSource parsedSource = new ParsedSource(questionId, sourcePath, studentRoot.relativize(sourcePath),
                name, emailId, username);
        parsedSources.add(parsedSource);

        for (String value : nonemptyValues(name)) {
            names.add(value);
        }
        for (String value : nonemptyValues(emailId)) {
            emailIds.add(value);
            evidence.add(new StrongEvidence(EvidenceType.EMAIL_OR_USERNAME, value,
                    "header email id in " + sourcePath.getFileName()));
        }
        for (String value : nonemptyValues(username)) {
            usernames.add(value);
            evidence.add(new StrongEvidence(EvidenceType.USERNAME, value,
                    "header username in " + sourcePath.getFileName()));
        }
    }

    private boolean hasExactFile(Path directory, String expectedName) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (entry.getFileName().toString().equals(expectedName)
                        && Files.isRegularFile(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasExactDirectory(Path parent, String expectedName) throws IOException {
        if (!Files.isDirectory(parent)) {
            return false;
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(parent)) {
            for (Path entry : entries) {
                if (entry.getFileName().toString().equals(expectedName)
                        && Files.isDirectory(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

    private HeaderField readField(List<String> lines, Pattern pattern) {
        HeaderField field = new HeaderField();
        int lineLimit = Math.min(lines.size(), config.getIdentityHeaderMaxLines());
        for (int lineIndex = 0; lineIndex < lineLimit; lineIndex++) {
            Matcher matcher = pattern.matcher(lines.get(lineIndex));
            if (matcher.matches()) {
                field.lines.add(new HeaderLine(matcher.group(1).trim()));
            }
        }
        return field;
    }

    private List<String> nonemptyValues(HeaderField field) {
        List<String> values = new ArrayList<>();
        for (HeaderLine line : field.lines) {
            if (!line.value.isBlank()) {
                values.add(line.value);
            }
        }
        return values;
    }

    private StudentRecord resolveRosterRecord(List<StrongEvidence> evidence,
                                              List<StudentRecord> roster,
                                              List<String> anomalies) {
        Map<String, StudentRecord> matchedStudents = new LinkedHashMap<>();
        boolean unresolvedEvidence = false;
        for (StrongEvidence item : evidence) {
            List<StudentRecord> matches = findMatches(item, roster);
            if (matches.isEmpty()) {
                anomalies.add(item.source + " does not match a score sheet student");
                unresolvedEvidence = true;
            } else if (matches.size() > 1) {
                anomalies.add(item.source + " matches multiple score sheet students");
                unresolvedEvidence = true;
            } else {
                StudentRecord student = matches.get(0);
                matchedStudents.put(student.getId(), student);
            }
        }
        if (matchedStudents.size() > 1) {
            anomalies.add("conflicting strong identity evidence points to different score sheet students");
            unresolvedEvidence = true;
        }
        if (matchedStudents.isEmpty()) {
            anomalies.add("no strong identity evidence matched the score sheet");
            return null;
        }
        if (unresolvedEvidence) {
            return null;
        }
        return matchedStudents.values().iterator().next();
    }

    private List<StudentRecord> findMatches(StrongEvidence evidence, List<StudentRecord> roster) {
        List<StudentRecord> matches = new ArrayList<>();
        for (StudentRecord student : roster) {
            boolean matchesStudent = false;
            if (evidence.type == EvidenceType.STUDENT_ID) {
                matchesStudent = stripRosterPrefix(student.getId()).equalsIgnoreCase(evidence.value);
            } else if (evidence.type == EvidenceType.EMAIL_OR_USERNAME
                    && evidence.value.contains("@")) {
                matchesStudent = student.getEmail().trim().equalsIgnoreCase(evidence.value);
            } else {
                matchesStudent = stripRosterPrefix(student.getUsername())
                        .equalsIgnoreCase(evidence.value);
            }
            if (matchesStudent) {
                matches.add(student);
            }
        }
        return matches;
    }

    private String stripRosterPrefix(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith(config.getRosterPrefix())) {
            return trimmed.substring(config.getRosterPrefix().length());
        }
        return trimmed;
    }

    private void prepareHeaderCorrections(List<ParsedSource> sources,
                                          StudentRecord student,
                                          List<SubmissionCorrection> corrections,
                                          List<String> anomalies) {
        String expectedName = rosterName(student);
        String expectedEmail = student.getEmail().trim();
        String expectedUsername = stripRosterPrefix(student.getUsername());
        for (ParsedSource source : sources) {
            if (expectedName.isEmpty()) {
                anomalies.add(source.questionId + ": roster name is empty");
            } else {
                prepareFieldCorrection(source, "name", source.name, expectedName,
                        true, anomalies, corrections);
            }
            prepareFieldCorrection(source, "email id", source.emailId, expectedEmail,
                    true, anomalies, corrections);
            prepareFieldCorrection(source, "username", source.username, expectedUsername,
                    false, anomalies, corrections);
        }
    }

    private void prepareFieldCorrection(ParsedSource source, String fieldName,
                                        HeaderField field, String expectedValue,
                                        boolean addMissingHeader, List<String> anomalies,
                                        List<SubmissionCorrection> corrections) {
        boolean hasNonemptyValue = false;
        for (HeaderLine line : field.lines) {
            if (!line.value.isBlank()) {
                hasNonemptyValue = true;
                if (fieldName.equals("name")
                        && !canonicalName(line.value).equals(canonicalName(expectedValue))) {
                    anomalies.add(source.questionId + ": name header does not corroborate the roster");
                }
            }
        }

        if (field.lines.isEmpty()) {
            if (addMissingHeader) {
                corrections.add(new SubmissionCorrection("header",
                        source.relativePath, fieldName, "", expectedValue, true));
            }
            return;
        }
        if (hasNonemptyValue) {
            for (HeaderLine line : field.lines) {
                if (line.value.isBlank()) {
                    corrections.add(new SubmissionCorrection("header",
                            source.relativePath, fieldName, "",
                            expectedValue, false));
                    break;
                }
            }
            return;
        }
        corrections.add(new SubmissionCorrection("header",
                source.relativePath, fieldName, "", expectedValue, false));
    }

    private void addUnresolvedHeaderAnomalies(List<ParsedSource> sources, List<String> anomalies) {
        for (ParsedSource source : sources) {
            if (source.name.lines.isEmpty()) {
                anomalies.add(source.questionId + ": name header is missing");
            } else if (nonemptyValues(source.name).isEmpty()) {
                anomalies.add(source.questionId + ": name header is blank");
            }
            if (source.emailId.lines.isEmpty()) {
                anomalies.add(source.questionId + ": email id header is missing");
            } else if (nonemptyValues(source.emailId).isEmpty()) {
                anomalies.add(source.questionId + ": email id header is blank");
            }
        }
    }

    private Pattern patternForField(String field) throws IOException {
        if (field.equals("name")) {
            return config.getIdentityNamePattern();
        }
        if (field.equals("email id")) {
            return config.getIdentityEmailPattern();
        }
        if (field.equals("username")) {
            return config.getIdentityUsernamePattern();
        }
        throw new IOException("unknown header correction field: " + field);
    }

    private String headerLabelForField(String field) throws IOException {
        if (field.equals("name")) {
            return config.getIdentityNameLabel();
        }
        if (field.equals("email id")) {
            return config.getIdentityEmailLabel();
        }
        if (field.equals("username")) {
            return config.getIdentityUsernameLabel();
        }
        throw new IOException("unknown header correction field: " + field);
    }

    private String rosterName(StudentRecord student) {
        String firstName = student.getFirstName().trim();
        String lastName = student.getLastName().trim();
        if (lastName.isEmpty() || lastName.equals(config.getRosterLastNamePlaceholder())) {
            return firstName;
        }
        if (firstName.isEmpty()) {
            return lastName;
        }
        return firstName + " " + lastName;
    }

    private String canonicalName(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder();
        for (int index = 0; index < lower.length(); index++) {
            char character = lower.charAt(index);
            if (Character.isLetterOrDigit(character)) {
                normalized.append(character);
            }
        }
        return normalized.toString();
    }

    private String onlyValue(Set<String> values) {
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return "";
    }

    private String archiveStem(Path archivePath) {
        String fileName = archivePath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            return fileName.substring(0, extensionIndex);
        }
        return fileName;
    }

    public static final class Resolution {
        private final StudentIdentity identity;
        private final List<String> anomalies;
        private final List<SubmissionCorrection> corrections;
        private final int validatedSourceCount;

        private Resolution(StudentIdentity identity, List<String> anomalies,
                           List<SubmissionCorrection> corrections, int validatedSourceCount) {
            this.identity = identity;
            this.anomalies = List.copyOf(anomalies);
            this.corrections = List.copyOf(corrections);
            this.validatedSourceCount = validatedSourceCount;
        }

        public StudentIdentity getIdentity() {
            return identity;
        }

        public List<String> getAnomalies() {
            return anomalies;
        }

        public List<SubmissionCorrection> getCorrections() {
            return corrections;
        }

        public int getValidatedSourceCount() {
            return validatedSourceCount;
        }
    }

    private enum EvidenceType {
        USERNAME,
        STUDENT_ID,
        EMAIL_OR_USERNAME
    }

    private static final class StrongEvidence {
        private final EvidenceType type;
        private final String value;
        private final String source;

        private StrongEvidence(EvidenceType type, String value, String source) {
            this.type = type;
            this.value = value.trim();
            this.source = source;
        }
    }

    private static final class HeaderLine {
        private final String value;

        private HeaderLine(String value) {
            this.value = value;
        }
    }

    private static final class HeaderField {
        private final List<HeaderLine> lines = new ArrayList<>();
    }

    private static final class ParsedSource {
        private final String questionId;
        private final Path sourcePath;
        private final Path relativePath;
        private final HeaderField name;
        private final HeaderField emailId;
        private final HeaderField username;

        private ParsedSource(String questionId, Path sourcePath, Path relativePath,
                             HeaderField name, HeaderField emailId, HeaderField username) {
            this.questionId = questionId;
            this.sourcePath = sourcePath;
            this.relativePath = relativePath;
            this.name = name;
            this.emailId = emailId;
            this.username = username;
        }
    }
}
