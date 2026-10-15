package autograder.grading;

import autograder.api.PlagiarismDetector;
import autograder.config.AppConfig;
import autograder.config.ExecutionConfig;
import autograder.csv.CsvCodec;
import autograder.execution.CaseExecutor;
import autograder.execution.ProcessResult;
import autograder.execution.ProcessRunner;
import autograder.execution.StudentCompiler;
import autograder.model.*;
import autograder.report.CsvScoreReportWriter;
import autograder.roster.ScoreSheetRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GradingPipeline {
    public static final String SIMILARITY_REPORT = "similarity.csv";

    private final AppConfig app;
    private final ExecutionConfig execution;
    private final List<TestCaseDefinition> cases;
    private final PlagiarismDetector plagiarismDetector;

    public GradingPipeline(AppConfig app, ExecutionConfig execution) throws IOException {
        this(app, execution, (submissions, assessment) -> List.of());
    }

    public GradingPipeline(AppConfig app, ExecutionConfig execution,
                           PlagiarismDetector plagiarismDetector) throws IOException {
        this.app = app;
        this.execution = execution;
        this.plagiarismDetector = plagiarismDetector;
        this.cases = new InstructorTestCaseProvider().load(app.getAssessment());
    }

    public void validateEnvironment() throws IOException, InterruptedException {
        for (String executable : List.of(execution.javaExecutable(), execution.javacExecutable())) {
            ProcessResult result = ProcessRunner.run(List.of(executable, "-version"),
                    Path.of(".").toAbsolutePath(), execution.compileTimeoutMillis(),
                    execution.maxOutputBytes());
            if (result.timedOut() || result.exitCode() != 0) {
                throw new IOException("could not start configured Java tool: " + executable
                        + " " + result.stderr());
            }
        }
        for (QuestionDefinition question : app.getAssessment().getQuestions()) {
            List<Path> required = new ArrayList<>(question.getSupportFiles());
            required.addAll(question.getFixtures());
            required.add(question.getTesterFile());
            for (Path path : required) {
                if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                    throw new IOException("missing/unreadable instructor resource: " + path);
                }
            }
        }
    }

    public GradingRun grade(List<SubmissionResult> submissions) throws IOException, InterruptedException {
        Files.createDirectories(execution.directory());
        Path run = Files.createTempDirectory(execution.directory(), "run-");
        Map<String, Integer> identities = new HashMap<>();
        for (SubmissionResult submission : submissions) {
            StudentRecord student = submission.getIdentity().getStudentRecord();
            if (student != null) {
                identities.merge(student.getId(), 1, Integer::sum);
            }
        }
        Map<SubmissionResult, String> withheld = new LinkedHashMap<>();
        List<StudentGrade> grades = new ArrayList<>();
        int index = 0;
        for (SubmissionResult submission : submissions) {
            index++;
            String blocker = blocker(submission, identities);
            List<TestCaseResult> results = new ArrayList<>();
            System.out.println("Grading " + index + "/" + submissions.size() + ": "
                    + submission.getArchivePath().getFileName());
            if (blocker != null) {
                withheld.put(submission, blocker);
                for (TestCaseDefinition test : cases) {
                    results.add(new TestCaseResult(test, TestStatus.NOT_RUN, 0, "", blocker));
                }
            } else {
                Path studentDirectory = Files.createDirectory(run.resolve("submission-" + index));
                for (QuestionDefinition question : app.getAssessment().getQuestions()) {
                    List<TestCaseDefinition> questionCases = casesFor(question.getId());
                    try {
                        Path part = studentDirectory.resolve(question.getId());
                        ProcessResult compilation = new StudentCompiler().compile(question,
                                submission.getWorkingRoot(), part, execution.javacExecutable(),
                                execution.compileTimeoutMillis(), execution.maxOutputBytes());
                        if (compilation.timedOut() || compilation.exitCode() != 0) {
                            String diagnostic = compilation.stderr();
                            if (compilation.timedOut()) {
                                diagnostic = "compilation deadline exceeded\n" + diagnostic;
                            }
                            for (TestCaseDefinition test : questionCases) {
                                results.add(new TestCaseResult(test, TestStatus.COMPILE_ERROR,
                                        compilation.durationMillis(), compilation.stdout(), diagnostic));
                            }
                        } else {
                            for (TestCaseDefinition test : questionCases) {
                                results.add(new CaseExecutor().execute(test, question, part,
                                        execution.javaExecutable(), execution.caseTimeoutMillis(),
                                        execution.maxOutputBytes()));
                            }
                        }
                    } catch (IOException exception) {
                        String reason = "grading infrastructure error: " + exception.getMessage();
                        withheld.put(submission, reason);
                        for (TestCaseDefinition test : questionCases) {
                            if (!hasResult(results, test.getId())) {
                                results.add(new TestCaseResult(test, TestStatus.NOT_RUN, 0, "", reason));
                            }
                        }
                    }
                }
            }
            if (results.size() != cases.size()) {
                throw new IOException("incomplete grading result; score export stopped");
            }
            grades.add(new StudentGrade(submission, results));
        }
        BatchResult batch = new BatchResult(app.getAssessment(), grades, List.of(),
                compareSimilarity(submissions));
        GradingRun result = new GradingRun(batch, run, withheld);
        writeReports(result);
        return result;
    }

    private String blocker(SubmissionResult submission, Map<String, Integer> identities) {
        StudentRecord student = submission.getIdentity().getStudentRecord();
        if (student == null) {
            return "unresolved student identity";
        }
        if (identities.get(student.getId()) > 1) {
            return "duplicate submissions for the same student";
        }
        if (submission.getStatus() == SubmissionStatus.FAILED || submission.getWorkingRoot() == null) {
            return "submission preparation did not produce a usable working copy";
        }
        return null;
    }

    private List<TestCaseDefinition> casesFor(String questionId) {
        List<TestCaseDefinition> selected = new ArrayList<>();
        for (TestCaseDefinition test : cases) {
            if (test.getQuestionId().equals(questionId)) {
                selected.add(test);
            }
        }
        return selected;
    }

    private boolean hasResult(List<TestCaseResult> results, String id) {
        for (TestCaseResult result : results) {
            if (result.getTestCase().getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // similarity is advisory, so a detector failure is reported without blocking scores
    private List<SimilarityFinding> compareSimilarity(List<SubmissionResult> submissions) {
        try {
            return plagiarismDetector.compare(submissions, app.getAssessment());
        } catch (IOException | RuntimeException exception) {
            System.err.println("similarity check skipped: " + exception.getMessage());
            return List.of();
        }
    }

    private void writeReports(GradingRun run) throws IOException {
        List<List<String>> caseRows = new ArrayList<>();
        caseRows.add(List.of("Archive", "Username", "Case", "Status", "Score", "Maximum",
                "DurationMillis", "Stdout", "Stderr"));
        List<List<String>> submissionRows = new ArrayList<>();
        submissionRows.add(List.of("Archive", "Username", "Export", "Reason", "ComputedScore",
                "PreparationWarnings"));
        Map<String, Double> scores = new LinkedHashMap<>();
        for (StudentGrade grade : run.batch().getGrades()) {
            SubmissionResult submission = grade.getSubmission();
            StudentRecord student = submission.getIdentity().getStudentRecord();
            String username = student == null ? "" : student.getUsername();
            String archive = submission.getArchivePath().getFileName().toString();
            String reason = run.withheld().get(submission);
            if (reason == null) {
                scores.put(student.getId(), grade.getTotalScore());
            }
            submissionRows.add(List.of(archive, username, reason == null ? "EXPORTED" : "WITHHELD",
                    reason == null ? "" : reason, Double.toString(grade.getTotalScore()),
                    String.join("\n", submission.getAnomalies())));
            for (TestCaseResult result : grade.getTestResults()) {
                caseRows.add(List.of(archive, username, result.getTestCase().getId(),
                        result.getStatus().name(), Double.toString(result.getScore()),
                        Double.toString(result.getTestCase().getMaximumScore()),
                        Long.toString(result.getDurationMillis()), result.getStandardOutput(),
                        result.getStandardError()));
            }
        }
        Files.writeString(run.directory().resolve("cases.csv"), CsvCodec.write(caseRows));
        Files.writeString(run.directory().resolve("submissions.csv"), CsvCodec.write(submissionRows));
        Files.writeString(run.directory().resolve(SIMILARITY_REPORT), CsvCodec.write(similarityRows(run)));
        ScoreSheet sheet = new ScoreSheetRepository(app.getScoreSheetPath(),
                app.getScoreSheetColumns()).load();
        new CsvScoreReportWriter().write(sheet, scores, run.directory().resolve(execution.reportFile()));
    }

    private List<List<String>> similarityRows(GradingRun run) {
        Map<Path, String> usernames = new HashMap<>();
        for (StudentGrade grade : run.batch().getGrades()) {
            StudentRecord student = grade.getSubmission().getIdentity().getStudentRecord();
            usernames.put(grade.getSubmission().getArchivePath(), student == null ? "" : student.getUsername());
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("FirstArchive", "FirstUsername", "SecondArchive", "SecondUsername",
                "Similarity", "Evidence"));
        for (SimilarityFinding finding : run.batch().getSimilarities()) {
            rows.add(List.of(finding.getFirstArchive().getFileName().toString(),
                    usernames.getOrDefault(finding.getFirstArchive(), ""),
                    finding.getSecondArchive().getFileName().toString(),
                    usernames.getOrDefault(finding.getSecondArchive(), ""),
                    String.format(Locale.ROOT, "%.3f", finding.getSimilarity()),
                    String.join("\n", finding.getEvidence())));
        }
        return rows;
    }
}
