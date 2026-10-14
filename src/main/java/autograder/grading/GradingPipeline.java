package autograder.grading;

import autograder.config.AppConfig;
import autograder.config.ExecutionConfig;
import autograder.execution.CaseExecutor;
import autograder.execution.ProcessResult;
import autograder.execution.ProcessRunner;
import autograder.execution.StudentCompiler;
import autograder.model.BatchResult;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.StudentRecord;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GradingPipeline {
    private final AppConfig app;
    private final ExecutionConfig execution;
    private final List<TestCaseDefinition> cases;

    public GradingPipeline(AppConfig app, ExecutionConfig execution) throws IOException {
        this.app = app;
        this.execution = execution;
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
        Map<String, Integer> identities = identityCounts(submissions);
        Map<SubmissionResult, String> withheld = new LinkedHashMap<>();
        List<StudentGrade> grades = new ArrayList<>();
        int index = 0;
        for (SubmissionResult submission : submissions) {
            index++;
            grades.add(gradeSubmission(submission, identities, run, index, submissions.size(), withheld));
        }
        BatchResult batch = new BatchResult(app.getAssessment(), grades, List.of(), List.of());
        GradingRun result = new GradingRun(batch, run, withheld);
        new GradingDiagnosticsWriter(app, execution).write(result);
        return result;
    }

    private Map<String, Integer> identityCounts(List<SubmissionResult> submissions) {
        Map<String, Integer> identities = new HashMap<>();
        for (SubmissionResult submission : submissions) {
            StudentRecord student = submission.getIdentity().getStudentRecord();
            if (student != null) {
                identities.merge(student.getId(), 1, Integer::sum);
            }
        }
        return identities;
    }

    private StudentGrade gradeSubmission(SubmissionResult submission, Map<String, Integer> identities,
                                         Path run, int index, int total,
                                         Map<SubmissionResult, String> withheld)
            throws IOException, InterruptedException {
        String blocker = blocker(submission, identities);
        List<TestCaseResult> results = new ArrayList<>();
        System.out.println("Grading " + index + "/" + total + ": "
                + submission.getArchivePath().getFileName());
        if (blocker != null) {
            withheld.put(submission, blocker);
            for (TestCaseDefinition test : cases) {
                results.add(new TestCaseResult(test, TestStatus.NOT_RUN, 0, "", blocker));
            }
        } else {
            Path studentDirectory = Files.createDirectory(run.resolve("submission-" + index));
            for (QuestionDefinition question : app.getAssessment().getQuestions()) {
                gradeQuestion(submission, question, studentDirectory, results, withheld);
            }
        }
        if (results.size() != cases.size()) {
            throw new IOException("incomplete grading result; score export stopped");
        }
        return new StudentGrade(submission, results);
    }

    private void gradeQuestion(SubmissionResult submission, QuestionDefinition question,
                               Path studentDirectory, List<TestCaseResult> results,
                               Map<SubmissionResult, String> withheld) throws InterruptedException {
        List<TestCaseDefinition> questionCases = casesFor(question.getId());
        try {
            Path part = studentDirectory.resolve(question.getId());
            ProcessResult compilation = new StudentCompiler().compile(question,
                    submission.getWorkingRoot(), part, execution.javacExecutable(),
                    execution.compileTimeoutMillis(), execution.maxOutputBytes());
            if (compilation.timedOut() || compilation.exitCode() != 0) {
                recordCompilationFailure(results, questionCases, compilation);
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

    private void recordCompilationFailure(List<TestCaseResult> results,
                                          List<TestCaseDefinition> questionCases,
                                          ProcessResult compilation) {
        String diagnostic = compilation.stderr();
        if (compilation.timedOut()) {
            diagnostic = "compilation deadline exceeded\n" + diagnostic;
        }
        for (TestCaseDefinition test : questionCases) {
            results.add(new TestCaseResult(test, TestStatus.COMPILE_ERROR,
                    compilation.durationMillis(), compilation.stdout(), diagnostic));
        }
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
}
