package autograder.grading;

import autograder.config.AppConfig;
import autograder.config.ExecutionConfig;
import autograder.execution.CaseExecutor;
import autograder.execution.ProcessResult;
import autograder.execution.ProcessRunner;
import autograder.execution.StudentCompiler;
import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterEquivalenceTest {
    private static final Pattern SCORE_LINE = Pattern.compile("(?m)^\\s*(\\d+(?:\\.\\d+)?)\\s*$");
    private static final String ARCHIVE = "Project/student-submission/2023-2024-ping.lee.2023.zip";
    private static Path testRoot;
    private static AppConfig app;
    private static ExecutionConfig execution;
    private static List<TestCaseDefinition> cases;

    @BeforeAll
    static void prepareTestRuntime() throws IOException {
        Path targetDirectory = Path.of("target").toAbsolutePath().normalize();
        Files.createDirectories(targetDirectory);
        testRoot = Files.createTempDirectory(targetDirectory, "adapter-equivalence-");
        app = AppConfig.load(Path.of("config.properties"));
        execution = new ExecutionConfig(testRoot.resolve("runs"), "scores.csv",
                javaTool("java"), javaTool("javac"), 30000, 5000, 1024 * 1024);
        cases = new InstructorTestCaseProvider().load(app.getAssessment());
    }

    @Test
    void adaptersMatchEveryOriginalTesterTotalForPingSubmission() throws Exception {
        Path workingRoot = extractSources("ping.lee.2023");
        int totalAdapters = 0;
        int totalOriginal = 0;
        for (QuestionDefinition question : app.getAssessment().getQuestions()) {
            List<TestCaseResult> results = runAdapters(question, workingRoot, "ping-" + question.getId());
            int adapterScore = score(results);
            int originalScore = runOriginalTester(question, workingRoot, "ping-original-" + question.getId());
            assertEquals(originalScore, adapterScore,
                    question.getId() + " adapter score must match the original tester total");
            assertEquals(question.getCaseCount(), adapterScore,
                    question.getId() + " known-good reference should pass every supplied case");
            totalAdapters += adapterScore;
            totalOriginal += originalScore;
        }
        assertEquals(20, totalOriginal);
        assertEquals(totalOriginal, totalAdapters);
    }

    @Test
    void wrongResultsFailAndQ2bExpectedExceptionsKeepTheirOriginalMarks() throws Exception {
        Path wrongRoot = extractSources("ping-wrong");
        QuestionDefinition q1a = question("Q1a");
        Files.writeString(wrongRoot.resolve(q1a.getFolder()).resolve(q1a.getSourceFile()),
                "import java.util.ArrayList; public class Q1a { public static ArrayList<String> "
                        + "getIsogramWords(ArrayList<String> inputs) { return new ArrayList<>(); } }");
        List<TestCaseResult> wrongResults = runAdapters(q1a, wrongRoot, "wrong-q1a");
        assertEquals(0, score(wrongResults));
        assertTrue(wrongResults.stream().allMatch(result -> result.getStatus() == TestStatus.FAILED));
        assertEquals(0, runOriginalTester(q1a, wrongRoot, "wrong-original-q1a"));

        Path exceptionRoot = extractSources("ping-q2b-exception");
        QuestionDefinition q2b = question("Q2b");
        Files.writeString(exceptionRoot.resolve(q2b.getFolder()).resolve(q2b.getSourceFile()),
                "public class Q2b { public static String getTopStudent(String filename, "
                        + "String courseName) { throw new DataException(); } }");
        List<TestCaseResult> exceptionResults = runAdapters(q2b, exceptionRoot, "q2b-exception");
        assertEquals(2, score(exceptionResults));
        assertEquals(TestStatus.PASSED, exceptionResults.get(0).getStatus());
        assertEquals(TestStatus.PASSED, exceptionResults.get(1).getStatus());
        for (int index = 2; index < exceptionResults.size(); index++) {
            assertEquals(TestStatus.RUNTIME_ERROR, exceptionResults.get(index).getStatus());
            assertTrue(exceptionResults.get(index).getStandardError().contains("DataException"));
        }
        assertEquals(2, runOriginalTester(q2b, exceptionRoot, "q2b-exception-original"));
    }

    private static List<TestCaseResult> runAdapters(QuestionDefinition question, Path workingRoot,
                                                     String label)
            throws IOException, InterruptedException {
        Path partWorkspace = Files.createDirectories(testRoot.resolve(label));
        ProcessResult compilation = new StudentCompiler().compile(question, workingRoot,
                partWorkspace, execution.javacExecutable(), execution.compileTimeoutMillis(),
                execution.maxOutputBytes());
        assertFalse(compilation.timedOut(), question.getId() + " adapter compilation timed out");
        assertEquals(0, compilation.exitCode(), compilation.stderr());

        List<TestCaseResult> results = new ArrayList<>();
        for (TestCaseDefinition testCase : casesFor(question.getId())) {
            results.add(new CaseExecutor().execute(testCase, question, partWorkspace,
                    execution.javaExecutable(), execution.caseTimeoutMillis(),
                    execution.maxOutputBytes()));
        }
        return results;
    }

    private static int runOriginalTester(QuestionDefinition question, Path workingRoot,
                                         String label)
            throws IOException, InterruptedException {
        Path partWorkspace = Files.createDirectories(testRoot.resolve(label));
        Path classesDirectory = Files.createDirectories(partWorkspace.resolve("classes"));
        copyStudentSources(question, workingRoot, partWorkspace.resolve("src"));
        for (Path support : question.getSupportFiles()) {
            Files.copy(support, classesDirectory.resolve(support.getFileName()));
        }
        List<String> compileCommand = new ArrayList<>(List.of(execution.javacExecutable(),
                "-proc:none", "-encoding", "UTF-8", "-d", classesDirectory.toString(),
                "-classpath", classesDirectory.toString()));
        Path sourceDirectory = partWorkspace.resolve("src");
        try (var sourceFiles = Files.list(sourceDirectory)) {
            sourceFiles.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> compileCommand.add(path.toString()));
        }
        compileCommand.add(question.getTesterFile().toAbsolutePath().toString());
        ProcessResult compilation = ProcessRunner.run(compileCommand, partWorkspace,
                execution.compileTimeoutMillis(), execution.maxOutputBytes());
        assertFalse(compilation.timedOut(), question.getId() + " original tester compilation timed out");
        assertEquals(0, compilation.exitCode(), compilation.stderr());

        Path originalDirectory = Files.createDirectories(partWorkspace.resolve("original-run"));
        for (Path fixture : question.getFixtures()) {
            Files.copy(fixture, originalDirectory.resolve(fixture.getFileName()));
        }
        ProcessResult original = ProcessRunner.run(List.of(execution.javaExecutable(), "-classpath",
                classesDirectory.toString(), testerClass(question.getId())), originalDirectory,
                execution.caseTimeoutMillis() * question.getCaseCount(), execution.maxOutputBytes());
        assertFalse(original.timedOut(), question.getId() + " original tester timed out");
        assertEquals(0, original.exitCode(), original.stderr());
        // reads trusted original tester output only as this test's reference score
        Matcher matcher = SCORE_LINE.matcher(original.stdout());
        String finalScore = null;
        while (matcher.find()) {
            finalScore = matcher.group(1);
        }
        assertTrue(finalScore != null, "original tester output must end with a numeric score");
        return (int) Double.parseDouble(finalScore);
    }

    private static void copyStudentSources(QuestionDefinition question, Path workingRoot,
                                           Path sourceDirectory) throws IOException {
        Files.createDirectories(sourceDirectory);
        List<String> sourceNames = new ArrayList<>();
        sourceNames.add(question.getSourceFile());
        sourceNames.addAll(question.getCompanionSourceFiles());
        for (String sourceName : sourceNames) {
            Files.copy(workingRoot.resolve(question.getFolder()).resolve(sourceName),
                    sourceDirectory.resolve(sourceName));
        }
    }

    private static Path extractSources(String workingName) throws IOException {
        Path workingRoot = Files.createDirectories(testRoot.resolve("working-" + workingName));
        try (ZipFile zip = new ZipFile(ARCHIVE)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".java")) continue;
                Path entryPath = Path.of(entry.getName()).normalize();
                if (entryPath.isAbsolute() || entryPath.startsWith("..")
                        || entryPath.getNameCount() < 3
                        || !entryPath.getName(0).toString().equals("ping.lee.2023")) {
                    throw new IOException("unexpected source path in reference archive: " + entry.getName());
                }
                Path relative = entryPath.subpath(1, entryPath.getNameCount());
                Path target = workingRoot.resolve(relative).normalize();
                if (!target.startsWith(workingRoot)) {
                    throw new IOException("unsafe source path in reference archive: " + entry.getName());
                }
                Files.createDirectories(target.getParent());
                try (var input = zip.getInputStream(entry)) {
                    Files.copy(input, target);
                }
            }
        }
        return workingRoot;
    }

    private static List<TestCaseDefinition> casesFor(String questionId) {
        List<TestCaseDefinition> selected = new ArrayList<>();
        for (TestCaseDefinition testCase : cases) {
            if (testCase.getQuestionId().equals(questionId)) selected.add(testCase);
        }
        return selected;
    }

    private static QuestionDefinition question(String questionId) {
        return app.getAssessment().getQuestions().stream()
                .filter(candidate -> candidate.getId().equals(questionId))
                .findFirst().orElseThrow();
    }

    private static int score(List<TestCaseResult> results) {
        return (int) results.stream().mapToDouble(TestCaseResult::getScore).sum();
    }

    private static String testerClass(String questionId) {
        return questionId + "Tester";
    }

    private static String javaTool(String tool) {
        String executable = tool;
        if (System.getProperty("os.name").startsWith("Windows")) executable += ".exe";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }
}
