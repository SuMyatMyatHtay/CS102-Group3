package autograder.report.html;

import autograder.model.AnalysisFinding;
import autograder.model.AssessmentDefinition;
import autograder.model.BatchResult;
import autograder.model.GradingReportContext;
import autograder.model.QuestionDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.StudentGrade;
import autograder.model.StudentIdentity;
import autograder.model.StudentRecord;
import autograder.model.SubmissionCorrection;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineHtmlReportGeneratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesOneSelfContainedOfflineReportWithInlineResources() throws Exception {
        BatchResult batch = batch(List.of(), List.of(), List.of());
        Path outputDirectory = temporaryDirectory.resolve("nested").resolve("report");

        Path output = new OfflineHtmlReportGenerator("graded-results.html")
                .generate(new GradingReportContext(batch, Map.of()), outputDirectory);

        assertEquals(outputDirectory.resolve("graded-results.html"), output);
        String html = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(html.contains("<style>"));
        assertTrue(html.contains("<script>"));
        assertTrue(html.contains("No submissions were received in this run."));
        assertTrue(html.contains("No analysis data supplied for this run."));
        assertFalse(html.contains("href=\"http"));
        assertFalse(html.contains("src=\"http"));
        assertFalse(html.contains("<link "));
        assertFalse(html.contains("fetch("));
        assertFalse(html.contains("@import"));
    }

    @Test
    void rendersAllStatusesAndUsesExportedOnlyDecimalStatistics() throws Exception {
        AssessmentDefinition assessment = assessment();
        SubmissionResult review = submission("A. Review", "areview", SubmissionStatus.REVIEW_REQUIRED,
                List.of("Name differs from roster"), Path.of("review.zip"));
        SubmissionResult high = submission("B. High", "bhigh", SubmissionStatus.READY,
                List.of(), Path.of("high.zip"));
        SubmissionResult middle = submission("C. Middle", "cmid", SubmissionStatus.READY,
                List.of(), Path.of("middle.zip"));
        SubmissionResult withheld = submission("D. Partial", "dpartial", SubmissionStatus.FAILED,
                List.of(), Path.of("partial.zip"));

        StudentGrade reviewGrade = grade(review, new TestStatus[]{TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.TIMED_OUT, TestStatus.COMPILE_ERROR,
                TestStatus.RUNTIME_ERROR, TestStatus.NOT_RUN});
        StudentGrade highGrade = grade(high, new TestStatus[]{TestStatus.PASSED,
                TestStatus.PASSED, TestStatus.FAILED, TestStatus.PASSED,
                TestStatus.PASSED, TestStatus.FAILED});
        StudentGrade middleGrade = grade(middle, new TestStatus[]{TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.TIMED_OUT, TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.NOT_RUN});
        StudentGrade withheldGrade = grade(withheld, new TestStatus[]{TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.NOT_RUN, TestStatus.PASSED,
                TestStatus.RUNTIME_ERROR, TestStatus.NOT_RUN});
        GradingReportContext context = new GradingReportContext(
                new BatchResult(assessment,
                        List.of(reviewGrade, highGrade, middleGrade, withheldGrade), List.of(), List.of()),
                Map.of(withheld, "duplicate submission; score held for review"));

        String html = render(context);

        assertTrue(html.contains("Received submissions</h3><p class=\"metric-value\">4"));
        assertTrue(html.contains("Exported</h3><p class=\"metric-value\">3"));
        assertTrue(html.contains("Withheld</h3><p class=\"metric-value\">1"));
        assertTrue(html.contains("Submissions with warnings</h3><p class=\"metric-value\">1"));
        assertTrue(html.contains("0.3333 points"));
        assertTrue(html.contains("0.3 points"));
        assertTrue(html.contains("REVIEW_REQUIRED"));
        assertTrue(html.contains("TIMED_OUT"));
        assertTrue(html.contains("COMPILE_ERROR"));
        assertTrue(html.contains("RUNTIME_ERROR"));
        assertTrue(html.contains("NOT_RUN"));
        assertTrue(html.contains("PASSED"));
        assertTrue(html.contains("FAILED"));
        assertTrue(html.contains("EXPORTED"));
        assertTrue(html.contains("WITHHELD"));
        assertTrue(html.contains("duplicate submission; score held for review"));
        assertTrue(html.contains("Question QB<span class=\"th-sub\"> / 0.3 max"));
        assertTrue(html.contains("Question QA<span class=\"th-sub\"> / 0.6 max"));
        assertTrue(html.contains("Total points<span class=\"th-sub\"> / 0.9 max"));
        assertTrue(html.contains("Scores for WITHHELD submissions are diagnostic only."));
        assertTrue(html.contains("No analysis data supplied for this run."));
        assertTrue(html.contains("mean rounded to 4 decimal places"));
    }

    @Test
    void escapesUntrustedTextInNamesPathsCorrectionsOutputsAndAnalysisEvidence() throws Exception {
        String payload = "<script>alert(\"x\")</script> & 'quoted' > <\nline two";
        StudentIdentity identity = identity(payload, "user&<name>");
        Path archive = Path.of("archive & file.zip");
        SubmissionCorrection correction = new SubmissionCorrection("header", Path.of("src/Main.java"),
                "name & id", payload, "fixed <value> & \"quoted\"", false);
        SubmissionResult submission = new SubmissionResult(archive, Path.of("extract/x"),
                Path.of("student & copy"), Path.of("working copy"), identity,
                List.of(), List.of("warning: " + payload), List.of(correction),
                SubmissionStatus.REVIEW_REQUIRED, true);
        List<TestCaseResult> cases = cases(new TestStatus[]{TestStatus.RUNTIME_ERROR},
                new String[]{"first line </script><script>alert(1)</script>\n<&\"'>"},
                new String[]{"stderr & <error>\nnext line"});
        StudentGrade grade = new StudentGrade(submission, cases);
        AnalysisFinding finding = new AnalysisFinding("finding <code>", Path.of("student/source.java"),
                "analysis message " + payload);
        SimilarityFinding similarity = new SimilarityFinding(archive, Path.of("second&archive.zip"),
                0.875, List.of("matching token evidence: " + payload));
        GradingReportContext context = new GradingReportContext(
                batch(List.of(grade), List.of(finding), List.of(similarity)), Map.of());

        String html = render(context);

        assertTrue(html.contains("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;"));
        assertTrue(html.contains("&amp; &#39;quoted&#39; &gt; &lt;"));
        assertTrue(html.contains("archive &amp; file.zip"));
        assertTrue(html.contains("first line &lt;/script&gt;&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(html.contains("stderr &amp; &lt;error&gt;"));
        assertTrue(html.contains("matching token evidence:"));
        assertFalse(html.contains("alert(\"x\")</script>"));
        assertFalse(html.contains("</script><script>alert(1)</script>"));
        assertTrue(html.contains("line two"));
        assertTrue(html.contains("second&amp;archive.zip"));
    }

    @Test
    void usesMatchedRosterIdentityInRowsAndPreservesRawHeaderValuesInDetails() throws Exception {
        StudentIdentity identity = new StudentIdentity("Header Alias", "alias@example.edu", "alias",
                List.of("Header Alias"), List.of("alias@example.edu"), List.of("alias"),
                new StudentRecord("student-42", "david.tan", "David", "Tan", "david@example.edu"),
                "validated roster match", List.of("matched username"));
        SubmissionResult submission = submission(identity, SubmissionStatus.REVIEW_REQUIRED,
                Path.of("david-assignment.zip"), List.of("Header name differs from roster"));
        StudentGrade grade = grade(submission, new TestStatus[]{TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED});

        String html = render(new GradingReportContext(batch(List.of(grade), List.of(), List.of()), Map.of()));

        assertTrue(html.contains("<strong>David Tan</strong>"));
        assertTrue(html.contains("Username: david.tan"));
        assertTrue(html.contains("Archive: david-assignment.zip"));
        assertTrue(html.contains("data-sort-name=\"david tan\""));
        assertTrue(html.contains("Raw header name</dt><dd>Header Alias"));
        assertTrue(html.contains("Raw header username</dt><dd>alias"));
        assertTrue(html.contains("Roster name</dt><dd>David Tan"));
        assertTrue(html.contains("Roster username</dt><dd>david.tan"));
    }

    @Test
    void linksQuestionScoresToGroupedEvidenceAndKeepsUnmatchedCasesVisible() throws Exception {
        String configuredId = "Q<&script>";
        String unmatchedId = "<script>unexpected</script>";
        QuestionDefinition scoredQuestion = question(configuredId, 2, 0.5);
        QuestionDefinition emptyQuestion = question("Empty", 1, 1.0);
        AssessmentDefinition assessment = new AssessmentDefinition("A-2", "Grouped report",
                List.of(scoredQuestion, emptyQuestion));
        SubmissionResult submission = submission("Student One", "student1", SubmissionStatus.READY,
                List.of(), Path.of("one.zip"));
        List<TestCaseResult> results = List.of(
                new TestCaseResult(testCase("case<&one", configuredId, 0.5), TestStatus.RUNTIME_ERROR,
                        17, "stdout & <value>", "stderr </script><script>alert(1)</script>"),
                new TestCaseResult(testCase("case-two", configuredId, 0.5), TestStatus.PASSED,
                        23, "passed output", ""),
                new TestCaseResult(testCase("orphan", unmatchedId, 0.75), TestStatus.COMPILE_ERROR,
                        31, "compile stdout", "compile stderr"));
        StudentGrade grade = new StudentGrade(submission, results);
        GradingReportContext context = new GradingReportContext(
                new BatchResult(assessment, List.of(grade), List.of(), List.of()),
                Map.of(submission, "duplicate identity; hold for review"));

        String html = render(context);

        assertTrue(html.contains("class=\"credit-legend\""));
        assertTrue(html.contains("&gt;0–&lt;25%"));
        assertTrue(html.contains("25–&lt;50%"));
        assertTrue(html.contains("href=\"#submission-1-question-1\""));
        assertTrue(html.contains("id=\"submission-1-question-1\" tabindex=\"-1\">Question Q&lt;&amp;script&gt;"));
        assertTrue(html.contains("href=\"#submission-1-question-2\""));
        assertTrue(html.contains("id=\"submission-1-question-2\" tabindex=\"-1\">Question Empty"));
        assertTrue(html.contains("credit-band-half"));
        assertTrue(html.contains("Diagnostic</span>"));
        assertFalse(html.contains("Runtime error: 1 · Passed: 1"),
                "status summaries must follow enum order, with Passed before Runtime error");
        assertTrue(html.contains("Passed: 1 · Runtime error: 1"));
        assertTrue(html.contains("Recorded 0 of 1 configured cases."));
        assertTrue(html.contains("No case results recorded for this question."));
        assertTrue(html.contains("Other recorded cases"));
        assertTrue(html.contains("&lt;script&gt;unexpected&lt;/script&gt;"));
        assertTrue(html.contains("case&lt;&amp;one"));
        assertTrue(html.contains("stdout &amp; &lt;value&gt;"));
        assertTrue(html.contains("stderr &lt;/script&gt;&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(html.contains("31 ms"));
        assertTrue(html.contains("COMPILE_ERROR durations refer to the compilation attempt"));
        assertFalse(html.contains("</script><script>alert(1)</script>"));
        assertEquals(1, occurrences(html, "case&lt;&amp;one"));
        assertEquals(1, occurrences(html, "orphan"));
    }

    @Test
    void rendersEmptyGroupsAndZeroMaximumAsNeutralEvidence() throws Exception {
        QuestionDefinition zeroMaximum = question("Zero", 0, 1.0);
        QuestionDefinition validQuestion = question("Valid", 1, 1.0);
        AssessmentDefinition assessment = new AssessmentDefinition("A-3", "Zero maximum",
                List.of(zeroMaximum, validQuestion));
        SubmissionResult submission = submission("Student", "student", SubmissionStatus.READY,
                List.of(), Path.of("empty.zip"));
        StudentGrade grade = new StudentGrade(submission, List.of());

        String html = render(new GradingReportContext(
                new BatchResult(assessment, List.of(grade), List.of(), List.of()), Map.of()));

        assertTrue(html.contains("Question Zero<span class=\"th-sub\"> / 0 max"));
        assertTrue(html.contains("class=\"numeric question-score-cell credit-band-neutral\""));
        assertTrue(html.contains("0 / 0"));
        assertTrue(html.contains("Configured maximum is not positive; credit shading is unavailable."));
        assertTrue(html.contains("No case results recorded for this question."));
        assertFalse(html.contains("100% of maximum"));
    }

    @Test
    void handlesEmptyAndAllWithheldRunsDuplicateNamesAndMissingIdentity() throws Exception {
        String empty = render(new GradingReportContext(batch(List.of(), List.of(), List.of()), Map.of()));
        assertTrue(empty.contains("Received submissions</h3><p class=\"metric-value\">0"));
        assertTrue(empty.contains("Average exported score</h3><p class=\"metric-value\">N/A"));
        assertTrue(empty.contains("No submissions were received in this run."));

        StudentIdentity duplicateOne = identity("Same Name", "same");
        StudentIdentity duplicateTwo = identity("Same Name", "same");
        SubmissionResult first = submission(duplicateOne, SubmissionStatus.READY, Path.of("one.zip"));
        SubmissionResult second = submission(duplicateTwo, SubmissionStatus.READY, Path.of("two.zip"));
        SubmissionResult unidentified = submission(null, SubmissionStatus.FAILED, Path.of("mystery.zip"));
        StudentGrade firstGrade = grade(first, new TestStatus[]{TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED});
        StudentGrade secondGrade = grade(second, new TestStatus[]{TestStatus.FAILED,
                TestStatus.PASSED, TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED, TestStatus.FAILED});
        StudentGrade unidentifiedGrade = grade(unidentified, new TestStatus[]{TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.FAILED, TestStatus.PASSED, TestStatus.FAILED, TestStatus.FAILED});
        GradingReportContext allWithheld = new GradingReportContext(batch(
                List.of(firstGrade, secondGrade, unidentifiedGrade), List.of(), List.of()),
                Map.of(first, "duplicate identity", second, "duplicate identity",
                        unidentified, "identity unresolved"));

        String html = render(allWithheld);

        assertTrue(html.contains("Average exported score</h3><p class=\"metric-value\">N/A"));
        assertTrue(html.contains("Median exported score</h3><p class=\"metric-value\">N/A"));
        assertTrue(html.contains("Unidentified submission #3"));
        assertTrue(html.contains("id=\"submission-1-details\""));
        assertTrue(html.contains("id=\"submission-2-details\""));
        assertTrue(html.contains("id=\"submission-3-details\""));
        assertTrue(html.contains("identity unresolved"));
        assertTrue(html.contains("data-export=\"withheld\""));
    }

    @Test
    void reportsOutputDirectoryWriteFailures() throws Exception {
        Path notDirectory = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(notDirectory, "file", StandardCharsets.UTF_8);
        GradingReportContext context = new GradingReportContext(
                batch(List.of(), List.of(), List.of()), Map.of());

        assertThrows(IOException.class, () -> new OfflineHtmlReportGenerator("report.html")
                .generate(context, notDirectory));
    }

    private String render(GradingReportContext context) throws IOException {
        Path output = new OfflineHtmlReportGenerator("dashboard.html")
                .generate(context, temporaryDirectory.resolve("output-" + outputCounter++));
        return Files.readString(output, StandardCharsets.UTF_8);
    }

    private BatchResult batch(List<StudentGrade> grades, List<AnalysisFinding> findings,
                              List<SimilarityFinding> similarities) {
        return new BatchResult(assessment(), grades, findings, similarities);
    }

    private AssessmentDefinition assessment() {
        return new AssessmentDefinition("A-1", "Midterm assessment", List.of(
                question("QB", 3, 0.1), question("QA", 3, 0.2)));
    }

    private QuestionDefinition question(String id, int cases, double pointsPerCase) {
        return new QuestionDefinition(id, id, "Main.java", temporaryDirectory.resolve(id + "Tester.java"),
                cases, pointsPerCase, List.of(), List.of(), List.of());
    }

    private StudentGrade grade(SubmissionResult submission, TestStatus[] statuses) {
        return new StudentGrade(submission, cases(statuses, new String[statuses.length],
                new String[statuses.length]));
    }

    private List<TestCaseResult> cases(TestStatus[] statuses, String[] stdout, String[] stderr) {
        List<TestCaseDefinition> definitions = List.of(
                testCase("QB-1", "QB", 0.1), testCase("QB-2", "QB", 0.1),
                testCase("QB-3", "QB", 0.1), testCase("QA-1", "QA", 0.2),
                testCase("QA-2", "QA", 0.2), testCase("QA-3", "QA", 0.2));
        List<TestCaseResult> results = new ArrayList<>();
        for (int index = 0; index < statuses.length; index++) {
            results.add(new TestCaseResult(definitions.get(index), statuses[index], index * 10L,
                    stdout[index] == null ? "" : stdout[index], stderr[index] == null ? "" : stderr[index]));
        }
        return results;
    }

    private int occurrences(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private TestCaseDefinition testCase(String id, String question, double maximum) {
        return new TestCaseDefinition(id, question, "Runner", List.of(), maximum);
    }

    private SubmissionResult submission(String name, String username, SubmissionStatus status,
                                        List<String> warnings, Path archive) {
        return submission(identity(name, username), status, archive, warnings);
    }

    private SubmissionResult submission(StudentIdentity identity, SubmissionStatus status, Path archive) {
        return submission(identity, status, archive, List.of());
    }

    private SubmissionResult submission(StudentIdentity identity, SubmissionStatus status,
                                        Path archive, List<String> warnings) {
        return new SubmissionResult(archive, temporaryDirectory.resolve("extract"),
                temporaryDirectory.resolve("student"), temporaryDirectory.resolve("working"),
                identity, List.of(), warnings, List.of(), status, true);
    }

    private StudentIdentity identity(String name, String username) {
        String safeName = name == null ? "" : name.trim();
        String[] nameParts = safeName.isEmpty() ? new String[]{"", ""} : safeName.split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        StudentRecord roster = new StudentRecord("test-id", username, firstName, lastName,
                "student@example.edu");
        return new StudentIdentity(name, "student@example.edu", username, List.of(), List.of(),
                List.of(), roster, "test fixture", List.of());
    }

    private int outputCounter;
}
