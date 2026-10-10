package autograder.report.feedback;

import autograder.api.FeedbackGenerator;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;
import autograder.model.SubmissionResult;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MarkdownFeedbackGenerator implements FeedbackGenerator {
    private static final String FILE_EXTENSION = ".md";
    private static final String FALLBACK_FILE_NAME = "submission";
    private static final String ARCHIVE_EXTENSION = ".zip";
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private static final int MAX_FILE_NAME_ATTEMPTS = 1000;
    private static final int MIN_FENCE_LENGTH = 3;
    private static final String MARKDOWN_SPECIAL_CHARACTERS = "\\`*_[]<>#|!~";
    private static final String COMPILE_WORKSPACE_PATH = "\\S*[/\\\\]src[/\\\\](?=[^/\\\\\\s]+\\.java)";
    private static final String JVM_OPTIONS_NOTICE = "Picked up (JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|JDK_JAVA_OPTIONS):.*";

    private final AssessmentDefinition assessment;
    private final long caseTimeoutMillis;
    private final int maxDiagnosticLines;
    private final String rosterPrefix;
    private final String lastNamePlaceholder;

    public MarkdownFeedbackGenerator(AssessmentDefinition assessment, long caseTimeoutMillis,
                                     int maxDiagnosticLines, String rosterPrefix,
                                     String lastNamePlaceholder) {
        if (caseTimeoutMillis <= 0 || maxDiagnosticLines <= 0) {
            throw new IllegalArgumentException("case timeout and diagnostic line limit must be positive");
        }
        this.assessment = Objects.requireNonNull(assessment);
        this.caseTimeoutMillis = caseTimeoutMillis;
        this.maxDiagnosticLines = maxDiagnosticLines;
        this.rosterPrefix = Objects.requireNonNull(rosterPrefix);
        this.lastNamePlaceholder = Objects.requireNonNull(lastNamePlaceholder);
    }

    @Override
    public Path generate(StudentGrade grade, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        String content = render(grade);
        String baseName = baseFileName(grade.getSubmission());
        for (int attempt = 1; attempt <= MAX_FILE_NAME_ATTEMPTS; attempt++) {
            String suffix = attempt == 1 ? "" : "-" + attempt;
            Path file = outputDirectory.resolve(baseName + suffix + FILE_EXTENSION);
            try {
                Files.writeString(file, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return file;
            } catch (FileAlreadyExistsException exception) {
                // another submission already uses this name, so try the next suffix
            }
        }
        throw new IOException("could not choose a unique feedback file name for " + baseName);
    }

    String render(StudentGrade grade) {
        StringBuilder markdown = new StringBuilder();
        appendHeader(markdown, grade);
        appendWithholdingNotice(markdown, grade);
        appendSummaryTable(markdown, grade);
        for (Map.Entry<String, List<TestCaseResult>> entry : resultsByQuestion(grade).entrySet()) {
            appendQuestion(markdown, grade.getSubmission(), entry.getKey(), entry.getValue());
        }
        appendSubmissionChecks(markdown, grade.getSubmission());
        return markdown.toString();
    }

    private void appendHeader(StringBuilder markdown, StudentGrade grade) {
        SubmissionResult submission = grade.getSubmission();
        markdown.append("# Feedback: ").append(escape(assessment.getName())).append("\n\n");
        markdown.append("- Student: ").append(escape(studentLabel(submission))).append('\n');
        markdown.append("- Archive: ").append(escape(archiveName(submission))).append('\n');
        markdown.append("- Total: ").append(format(grade.getTotalScore())).append(" / ")
                .append(format(maximumScore(grade.getTestResults()))).append("\n\n");
    }

    private void appendWithholdingNotice(StringBuilder markdown, StudentGrade grade) {
        Set<String> reasons = new LinkedHashSet<>();
        int notRun = 0;
        for (TestCaseResult result : grade.getTestResults()) {
            if (result.getStatus() == TestStatus.NOT_RUN) {
                notRun++;
                reasons.add(firstLine(result.getStandardError()));
            }
        }
        if (notRun == 0) {
            return;
        }
        markdown.append("> **Score withheld for instructor review.** ").append(notRun).append(" of ")
                .append(grade.getTestResults().size())
                .append(" cases were not run, so this total is not a final grade.\n");
        for (String reason : reasons) {
            markdown.append("> Recorded reason: ").append(escape(reason)).append('\n');
        }
        markdown.append('\n');
    }

    private void appendSummaryTable(StringBuilder markdown, StudentGrade grade) {
        markdown.append("| Question | Marks |\n| --- | ---: |\n");
        for (Map.Entry<String, List<TestCaseResult>> entry : resultsByQuestion(grade).entrySet()) {
            markdown.append("| ").append(escape(entry.getKey())).append(" | ")
                    .append(format(grade.getQuestionScore(entry.getKey()))).append(" / ")
                    .append(format(maximumScore(entry.getValue()))).append(" |\n");
        }
        markdown.append('\n');
    }

    private void appendQuestion(StringBuilder markdown, SubmissionResult submission,
                                String questionId, List<TestCaseResult> results) {
        markdown.append("## ").append(escape(questionId)).append("\n\n");
        markdown.append("| Case | Result | Marks |\n| --- | --- | ---: |\n");
        for (TestCaseResult result : results) {
            markdown.append("| ").append(escape(result.getTestCase().getId())).append(" | ")
                    .append(statusLabel(result.getStatus())).append(" | ")
                    .append(format(result.getScore())).append(" / ")
                    .append(format(result.getTestCase().getMaximumScore())).append(" |\n");
        }
        markdown.append('\n');
        if (allPassed(results)) {
            markdown.append("All cases passed.\n\n");
            return;
        }
        if (allNotRun(results)) {
            markdown.append("No cases in this part were run; see the withholding notice above.\n\n");
            return;
        }
        if (results.get(0).getStatus() == TestStatus.COMPILE_ERROR) {
            appendCompilationExplanation(markdown, submission, questionId, results.get(0));
            return;
        }
        for (TestCaseResult result : results) {
            if (result.getStatus() != TestStatus.PASSED) {
                appendCaseExplanation(markdown, result);
            }
        }
    }

    private void appendCompilationExplanation(StringBuilder markdown, SubmissionResult submission,
                                              String questionId, TestCaseResult result) {
        QuestionDefinition question = findQuestion(questionId);
        List<String> missing = missingSourceFiles(submission, question);
        if (!missing.isEmpty()) {
            markdown.append("This part was not compiled because required files were missing from the ")
                    .append(escape(question.getFolder())).append(" folder: ")
                    .append(escape(String.join(", ", missing)))
                    .append(". Every case in this part scores zero.\n\n");
            return;
        }
        markdown.append("This part did not compile, so every case in it scores zero. Compiler output:\n\n");
        // grading workspace paths mean nothing to the student, so keep only the file name
        appendDiagnostic(markdown, result.getStandardError().replaceAll(COMPILE_WORKSPACE_PATH, ""));
    }

    private void appendCaseExplanation(StringBuilder markdown, TestCaseResult result) {
        String caseId = escape(result.getTestCase().getId());
        switch (result.getStatus()) {
            case FAILED -> markdown.append("- **").append(caseId)
                    .append("**: the program ran but its result did not match the expected result.\n");
            case TIMED_OUT -> markdown.append("- **").append(caseId)
                    .append("**: the program did not finish within the ").append(caseTimeoutMillis)
                    .append(" ms time limit and was stopped. Possible causes include a loop whose exit "
                            + "condition is never reached or a program waiting for input.\n");
            case RUNTIME_ERROR -> markdown.append("- **").append(caseId)
                    .append("**: the program stopped with an error before producing a valid result.\n");
            case COMPILE_ERROR -> markdown.append("- **").append(caseId)
                    .append("**: this part did not compile.\n");
            case NOT_RUN -> markdown.append("- **").append(caseId).append("**: not run. Recorded reason: ")
                    .append(escape(firstLine(result.getStandardError()))).append('\n');
        }
        markdown.append('\n');
        if (result.getStatus() != TestStatus.NOT_RUN) {
            appendCapturedOutput(markdown, result);
        }
    }

    private void appendCapturedOutput(StringBuilder markdown, TestCaseResult result) {
        if (!withoutJvmNotices(result.getStandardOutput()).isBlank()) {
            markdown.append("Program output:\n\n");
            appendDiagnostic(markdown, result.getStandardOutput());
        }
        if (!withoutJvmNotices(result.getStandardError()).isBlank()) {
            markdown.append("Error output:\n\n");
            appendDiagnostic(markdown, result.getStandardError());
        }
    }

    private void appendSubmissionChecks(StringBuilder markdown, SubmissionResult submission) {
        markdown.append("## Submission checks\n\n");
        markdown.append("Problems found:\n\n");
        if (submission.getAnomalies().isEmpty()) {
            markdown.append("- None recorded\n");
        }
        for (String anomaly : submission.getAnomalies()) {
            markdown.append("- ").append(escape(anomaly)).append('\n');
        }
        markdown.append("\nAutomatic corrections:\n\n");
        if (submission.getCorrections().isEmpty()) {
            markdown.append("- None recorded\n");
        }
        for (SubmissionCorrection correction : submission.getCorrections()) {
            markdown.append("- ").append(escape(correction.toString())).append('\n');
        }
    }

    private void appendDiagnostic(StringBuilder markdown, String text) {
        String[] lines = withoutJvmNotices(text).strip().split("\\R", -1);
        int shown = Math.min(lines.length, maxDiagnosticLines);
        List<String> kept = new ArrayList<>();
        for (int index = 0; index < shown; index++) {
            kept.add(lines[index]);
        }
        String body = String.join("\n", kept);
        String fence = "`".repeat(Math.max(MIN_FENCE_LENGTH, longestBacktickRun(body) + 1));
        markdown.append(fence).append("text\n").append(body).append('\n').append(fence).append("\n\n");
        if (lines.length > shown) {
            markdown.append("(").append(lines.length - shown)
                    .append(" more lines omitted; see cases.csv for the full recorded output)\n\n");
        }
    }

    // the jvm prints these launcher notices for every process, so they are not student evidence
    private static String withoutJvmNotices(String text) {
        List<String> kept = new ArrayList<>();
        for (String line : text.split("\\R", -1)) {
            if (!line.matches(JVM_OPTIONS_NOTICE)) {
                kept.add(line);
            }
        }
        return String.join("\n", kept);
    }

    private Map<String, List<TestCaseResult>> resultsByQuestion(StudentGrade grade) {
        Map<String, List<TestCaseResult>> grouped = new LinkedHashMap<>();
        for (TestCaseResult result : grade.getTestResults()) {
            grouped.computeIfAbsent(result.getTestCase().getQuestionId(), key -> new ArrayList<>())
                    .add(result);
        }
        return grouped;
    }

    private List<String> missingSourceFiles(SubmissionResult submission, QuestionDefinition question) {
        List<String> missing = new ArrayList<>();
        if (question == null || submission.getWorkingRoot() == null) {
            return missing;
        }
        Path folder = submission.getWorkingRoot().resolve(question.getFolder());
        List<String> required = new ArrayList<>();
        required.add(question.getSourceFile());
        required.addAll(question.getCompanionSourceFiles());
        for (String fileName : required) {
            if (!Files.isRegularFile(folder.resolve(fileName))) {
                missing.add(fileName);
            }
        }
        return missing;
    }

    private QuestionDefinition findQuestion(String questionId) {
        for (QuestionDefinition question : assessment.getQuestions()) {
            if (question.getId().equals(questionId)) {
                return question;
            }
        }
        return null;
    }

    private static boolean allPassed(List<TestCaseResult> results) {
        for (TestCaseResult result : results) {
            if (result.getStatus() != TestStatus.PASSED) {
                return false;
            }
        }
        return true;
    }

    private static boolean allNotRun(List<TestCaseResult> results) {
        for (TestCaseResult result : results) {
            if (result.getStatus() != TestStatus.NOT_RUN) {
                return false;
            }
        }
        return true;
    }

    private static double maximumScore(List<TestCaseResult> results) {
        BigDecimal total = BigDecimal.ZERO;
        for (TestCaseResult result : results) {
            total = total.add(BigDecimal.valueOf(result.getTestCase().getMaximumScore()));
        }
        return total.doubleValue();
    }

    private static String statusLabel(TestStatus status) {
        return switch (status) {
            case PASSED -> "Passed";
            case FAILED -> "Wrong result";
            case TIMED_OUT -> "Timed out";
            case COMPILE_ERROR -> "Did not compile";
            case RUNTIME_ERROR -> "Runtime error";
            case NOT_RUN -> "Not run";
        };
    }

    private String studentLabel(SubmissionResult submission) {
        StudentRecord student = submission.getIdentity().getStudentRecord();
        if (student == null) {
            return "unresolved identity: " + submission.getIdentity().getLabel();
        }
        String lastName = student.getLastName().equals(lastNamePlaceholder) ? "" : student.getLastName();
        String name = (student.getFirstName() + " " + lastName).strip();
        String username = displayUsername(student);
        return name.isEmpty() ? username : name + " (" + username + ")";
    }

    private String displayUsername(StudentRecord student) {
        String username = student.getUsername();
        if (!rosterPrefix.isEmpty() && username.startsWith(rosterPrefix)) {
            return username.substring(rosterPrefix.length());
        }
        return username;
    }

    private static String archiveName(SubmissionResult submission) {
        Path fileName = submission.getArchivePath().getFileName();
        return fileName == null ? submission.getArchivePath().toString() : fileName.toString();
    }

    static String baseFileName(SubmissionResult submission) {
        StudentRecord student = submission.getIdentity().getStudentRecord();
        String name = student != null ? student.getUsername() : archiveName(submission);
        if (student == null && name.toLowerCase().endsWith(ARCHIVE_EXTENSION)) {
            name = name.substring(0, name.length() - ARCHIVE_EXTENSION.length());
        }
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^[._-]+", "");
        if (safe.length() > MAX_FILE_NAME_LENGTH) {
            safe = safe.substring(0, MAX_FILE_NAME_LENGTH);
        }
        return safe.isEmpty() ? FALLBACK_FILE_NAME : safe;
    }

    static String escape(String text) {
        StringBuilder escaped = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (character == '\r' || character == '\n') {
                escaped.append(' ');
            } else {
                if (MARKDOWN_SPECIAL_CHARACTERS.indexOf(character) >= 0) {
                    escaped.append('\\');
                }
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private static int longestBacktickRun(String text) {
        int longest = 0;
        int current = 0;
        for (char character : text.toCharArray()) {
            current = character == '`' ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }

    private static String firstLine(String text) {
        String stripped = text.strip();
        if (stripped.isEmpty()) {
            return "no reason was recorded";
        }
        return stripped.split("\\R", 2)[0];
    }

    private static String format(double score) {
        return BigDecimal.valueOf(score).stripTrailingZeros().toPlainString();
    }
}
