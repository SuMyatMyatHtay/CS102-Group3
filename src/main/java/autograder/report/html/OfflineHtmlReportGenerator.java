package autograder.report.html;

import autograder.api.HtmlReportGenerator;
import autograder.config.HtmlReportConfig;
import autograder.model.AnalysisFinding;
import autograder.model.BatchResult;
import autograder.model.GradingReportContext;
import autograder.model.QuestionDefinition;
import autograder.model.SimilarityFinding;
import autograder.model.StudentGrade;
import autograder.model.StudentIdentity;
import autograder.model.SubmissionCorrection;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestCaseResult;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class OfflineHtmlReportGenerator implements HtmlReportGenerator {
    private static final String STYLE_RESOURCE = "/autograder/html/dashboard.css";
    private static final String SCRIPT_RESOURCE = "/autograder/html/dashboard.js";
    private static final int AVERAGE_DECIMAL_PLACES = 4;

    private final HtmlReportConfig config;

    public OfflineHtmlReportGenerator(String filename) {
        this.config = new HtmlReportConfig(true, filename);
    }

    @Override
    public Path generate(GradingReportContext context, Path outputDirectory) throws IOException {
        Objects.requireNonNull(context, "grading report context is required");
        Objects.requireNonNull(outputDirectory, "output directory is required");
        Files.createDirectories(outputDirectory);
        Path output = outputDirectory.resolve(config.file());
        Files.writeString(output, render(context), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return output;
    }

    private String render(GradingReportContext context) throws IOException {
        BatchResult batch = context.batch();
        String css = readResource(STYLE_RESOURCE);
        String script = readResource(SCRIPT_RESOURCE);
        List<StudentGrade> grades = batch.getGrades();
        List<QuestionDefinition> questions = batch.getAssessment().getQuestions();
        BigDecimal assessmentMaximum = questions.stream()
                .map(QuestionDefinition::getMaxScoreDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int received = grades.size();
        int withheld = 0;
        int warningSubmissions = 0;
        for (StudentGrade grade : grades) {
            if (context.withheld().containsKey(grade.getSubmission())) {
                withheld++;
            }
            if (!grade.getSubmission().getAnomalies().isEmpty()) {
                warningSubmissions++;
            }
        }
        int exported = received - withheld;

        StringBuilder html = new StringBuilder(24_000);
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(escape(batch.getAssessment().getName()))
                .append(" — Grading report</title>\n<style>\n")
                .append(css).append("\n</style>\n</head>\n<body>\n<main class=\"page\">\n")
                .append("<header class=\"report-header\">\n<p class=\"eyebrow\">Offline grading report</p>\n<h1>")
                .append(escape(batch.getAssessment().getName())).append("</h1>\n")
                .append("<p class=\"assessment-meta\"><span>Assessment ID: <strong>")
                .append(escape(batch.getAssessment().getId())).append("</strong></span>")
                .append("<span>Configured maximum: <strong>")
                .append(format(assessmentMaximum))
                .append(" points</strong></span></p>\n</header>\n");

        appendSummary(html, grades, context.withheld(), received, exported, withheld,
                warningSubmissions, assessmentMaximum);
        appendSubmissionSection(html, grades, context.withheld(), questions,
                assessmentMaximum, received);
        appendAnalysisSection(html, batch.getFindings(), batch.getSimilarities());
        html.append("</main>\n<noscript><p class=\"noscript-note\">Filtering and sorting need JavaScript. ")
                .append("All submission details are still present in this report.</p></noscript>\n")
                .append("<script>\n").append(script).append("\n</script>\n</body>\n</html>\n");
        return html.toString();
    }

    private void appendSummary(StringBuilder html, List<StudentGrade> grades,
                               Map<SubmissionResult, String> withheld, int received,
                               int exported, int withheldCount, int warningSubmissions,
                               BigDecimal maximumScore) {
        List<BigDecimal> exportedScores = new ArrayList<>();
        BigDecimal exportedTotal = BigDecimal.ZERO;
        for (StudentGrade grade : grades) {
            if (!withheld.containsKey(grade.getSubmission())) {
                BigDecimal score = totalScore(grade);
                exportedScores.add(score);
                exportedTotal = exportedTotal.add(score);
            }
        }

        html.append("<section class=\"summary-section\" aria-labelledby=\"summary-title\">\n<h2 id=\"summary-title\">Run summary</h2>\n")
                .append("<div class=\"summary-grid\">\n");
        metric(html, "Received submissions", Integer.toString(received), "Submissions present in this grading run");
        metric(html, "Exported", Integer.toString(exported), "Included in the score CSV");
        metric(html, "Withheld", Integer.toString(withheldCount), "Excluded from the score CSV");
        metric(html, "Submissions with warnings", Integer.toString(warningSubmissions), "At least one preparation warning was recorded");
        if (exported == 0) {
            metric(html, "Average exported score", "N/A", "No exported submissions");
            metric(html, "Median exported score", "N/A", "No exported submissions");
        } else {
            BigDecimal average = exportedTotal.divide(BigDecimal.valueOf(exported), AVERAGE_DECIMAL_PLACES,
                    RoundingMode.HALF_UP);
            BigDecimal median = median(exportedScores);
            metric(html, "Average exported score", format(average) + " points",
                    "Exported-only mean rounded to " + AVERAGE_DECIMAL_PLACES + " decimal places; maximum "
                            + format(maximumScore) + " points");
            metric(html, "Median exported score", format(median) + " points",
                    "Exported submissions only; maximum " + format(maximumScore) + " points");
        }
        html.append("</div>\n<p class=\"scope-note\">These results cover received submissions only. ")
                .append("Students without a received submission are not listed here; their template values are retained in the CSV.</p>\n")
                .append("</section>\n");
    }

    private void metric(StringBuilder html, String label, String value, String description) {
        html.append("<article class=\"metric-card\"><h3>").append(escape(label))
                .append("</h3><p class=\"metric-value\">").append(escape(value))
                .append("</p><p class=\"metric-description\">").append(escape(description))
                .append("</p></article>\n");
    }

    private void appendSubmissionSection(StringBuilder html, List<StudentGrade> grades,
                                        Map<SubmissionResult, String> withheld,
                                        List<QuestionDefinition> questions,
                                        BigDecimal assessmentMaximum, int received) {
        int columnCount = questions.size() + 5;
        html.append("<section class=\"submissions-section\" aria-labelledby=\"submissions-title\">\n")
                .append("<div class=\"section-heading\"><div><h2 id=\"submissions-title\">Submissions</h2>")
                .append("<p>Each row represents one received archive, including duplicate identities.</p></div></div>\n");
        if (!withheld.isEmpty()) {
            html.append("<p class=\"scope-note\">Scores for WITHHELD submissions are diagnostic only. ")
                    .append("They were excluded from the CSV and exported-only summary statistics.</p>\n");
        }
        html.append("<div class=\"controls\">\n")
                .append("<div class=\"control\"><label for=\"submission-search\">Search name, username, or archive</label>")
                .append("<input id=\"submission-search\" type=\"search\" autocomplete=\"off\" placeholder=\"Search submissions\"></div>\n")
                .append("<div class=\"control\"><label for=\"export-filter\">Export decision</label><select id=\"export-filter\">")
                .append("<option value=\"all\">All submissions</option><option value=\"exported\">Exported</option>")
                .append("<option value=\"withheld\">Withheld</option></select></div>\n")
                .append("<div class=\"control\"><label for=\"warning-filter\">Preparation warnings</label><select id=\"warning-filter\">")
                .append("<option value=\"all\">All submissions</option><option value=\"warning\">With warnings</option>")
                .append("<option value=\"no-warning\">No warnings</option></select></div>\n")
                .append("<div class=\"control\"><label for=\"sort-order\">Sort submissions</label><select id=\"sort-order\">")
                .append("<option value=\"name-asc\">Name, A to Z</option><option value=\"name-desc\">Name, Z to A</option>")
                .append("<option value=\"score-asc\">Diagnostic total, low to high</option>")
                .append("<option value=\"score-desc\">Diagnostic total, high to low</option></select></div>\n</div>\n")
                .append("<p class=\"result-count\" id=\"result-count\" role=\"status\" aria-live=\"polite\">Showing ")
                .append(received).append(" of ").append(received).append(" submissions.</p>\n")
                .append("<div class=\"table-scroll\" role=\"region\" aria-label=\"Submission results table\" tabindex=\"0\">\n")
                .append("<table><thead><tr><th scope=\"col\">Submission</th><th scope=\"col\">Preparation</th>")
                .append("<th scope=\"col\">Export decision</th>");
        for (QuestionDefinition question : questions) {
            html.append("<th scope=\"col\">Question ").append(escape(question.getId()))
                    .append("<span class=\"th-sub\"> / ").append(format(question.getMaxScoreDecimal()))
                    .append(" max</span></th>");
        }
        html.append("<th scope=\"col\">Total points<span class=\"th-sub\"> / ")
                .append(format(assessmentMaximum)).append(" max</span></th>")
                .append("<th scope=\"col\">Details</th></tr></thead><tbody id=\"submission-rows\">\n");

        if (grades.isEmpty()) {
            html.append("<tr><td colspan=\"").append(columnCount)
                    .append("\">No submissions were received in this run.</td></tr>\n");
        }

        int ordinal = 0;
        for (StudentGrade grade : grades) {
            ordinal++;
            appendStudentRow(html, grade, ordinal, withheld.get(grade.getSubmission()),
                    questions, assessmentMaximum);
        }
        html.append("<tr id=\"no-matches\" hidden><td colspan=\"").append(columnCount)
                .append("\">No submissions match these filters.</td></tr>\n")
                .append("</tbody></table></div>\n</section>\n");
    }

    private void appendStudentRow(StringBuilder html, StudentGrade grade, int ordinal,
                                  String withheldReason, List<QuestionDefinition> questions,
                                  BigDecimal assessmentMaximum) {
        SubmissionResult submission = grade.getSubmission();
        StudentIdentity identity = submission.getIdentity();
        String displayName = displayName(identity, ordinal);
        String username = preferredUsername(identity);
        String archive = pathText(submission.getArchivePath());
        String archiveName = archiveName(submission.getArchivePath());
        String rawName = identity == null ? "" : nullToEmpty(identity.getName());
        String rawUsername = identity == null ? "" : nullToEmpty(identity.getUsername());
        boolean isWithheld = withheldReason != null;
        boolean hasWarnings = !submission.getAnomalies().isEmpty();
        BigDecimal score = totalScore(grade);
        String searchText = (displayName + " " + username + " " + rawName + " " + rawUsername
                + " " + archive).toLowerCase(Locale.ROOT);
        String rowId = "submission-" + ordinal;
        html.append("<tr class=\"submission-row\" id=\"").append(rowId)
                .append("\" data-search=\"").append(escape(searchText))
                .append("\" data-sort-name=\"").append(escape(displayName.toLowerCase(Locale.ROOT)))
                .append("\" data-sort-score=\"").append(format(score))
                .append("\" data-export=\"").append(isWithheld ? "withheld" : "exported")
                .append("\" data-warning=\"").append(hasWarnings).append("\">\n<td><strong>")
                .append(escape(displayName)).append("</strong><span class=\"row-ordinal\">Submission #")
                .append(ordinal).append("</span>");
        if (!username.isBlank()) {
            html.append("<span class=\"row-secondary\">Username: ").append(escape(username)).append("</span>");
        }
        html.append("<span class=\"row-secondary\">Archive: ").append(escape(archiveName)).append("</span>");
        html.append("</td><td>").append(statusLabel(submission.getStatus())).append("</td><td>")
                .append(isWithheld ? "<span class=\"decision withheld\">WITHHELD</span>"
                        : "<span class=\"decision exported\">EXPORTED</span>");
        if (isWithheld) {
            html.append("<span class=\"row-secondary\">").append(escape(withheldReason)).append("</span>");
        }
        html.append("</td>");
        for (QuestionDefinition question : questions) {
            BigDecimal questionScore = questionScore(grade, question.getId());
            html.append("<td class=\"numeric");
            if (isWithheld) {
                html.append(" diagnostic-score\" title=\"Diagnostic score for a withheld submission\">");
            } else {
                html.append("\">");
            }
            html.append(format(questionScore)).append(" / ").append(format(question.getMaxScoreDecimal()))
                    .append("</td>");
        }
        html.append("<td class=\"numeric total-score");
        if (isWithheld) {
            html.append(" diagnostic-score\">");
            html.append("<span class=\"diagnostic-label\">Diagnostic: </span>");
        } else {
            html.append("\">");
        }
        html.append(format(score)).append(" / ").append(format(assessmentMaximum))
                .append("</td><td><a class=\"details-link\" href=\"#submission-")
                .append(ordinal).append("-details\">Open details</a></td></tr>\n");
        appendDetailRow(html, grade, ordinal, displayName, withheldReason, questions.size() + 5);
    }

    private void appendDetailRow(StringBuilder html, StudentGrade grade, int ordinal,
                                 String displayName, String withheldReason, int columnCount) {
        SubmissionResult submission = grade.getSubmission();
        StudentIdentity identity = submission.getIdentity();
        html.append("<tr class=\"detail-row\" id=\"submission-").append(ordinal)
                .append("-details\"><td colspan=\"").append(columnCount)
                .append("\"><details><summary>Details for ").append(escape(displayName))
                .append(" (submission #").append(ordinal).append(")</summary><div class=\"detail-content\">\n");
        html.append("<section class=\"detail-block\"><h3>Submission record</h3><dl class=\"path-list\">");
        detailItem(html, "Archive", pathText(submission.getArchivePath()));
        detailItem(html, "Extracted", submission.isExtracted() ? "Yes" : "No");
        detailItem(html, "Extraction path", pathText(submission.getExtractionPath()));
        detailItem(html, "Student path", pathText(submission.getStudentRoot()));
        detailItem(html, "Working path", pathText(submission.getWorkingRoot()));
        if (identity == null) {
            detailItem(html, "Identity", "Not available");
        } else {
            detailItem(html, "Raw header name", identity.getName());
            detailItem(html, "Raw header username", identity.getUsername());
            detailItem(html, "Raw header email", identity.getEmailId());
            detailItem(html, "Identity source", identity.getSource());
            if (identity.getStudentRecord() == null) {
                detailItem(html, "Roster identity", "Not matched");
            } else {
                detailItem(html, "Roster name", rosterName(identity));
                detailItem(html, "Roster username", identity.getStudentRecord().getUsername());
                detailItem(html, "Roster email", identity.getStudentRecord().getEmail());
                detailItem(html, "Roster ID", identity.getStudentRecord().getId());
            }
        }
        if (withheldReason != null) {
            detailItem(html, "Export decision", "WITHHELD — " + withheldReason);
        } else {
            detailItem(html, "Export decision", "EXPORTED");
        }
        html.append("</dl></section>\n");

        html.append("<section class=\"detail-block\"><h3>Preparation warnings</h3>");
        if (submission.getAnomalies().isEmpty()) {
            html.append("<p>No preparation warnings recorded.</p>");
        } else {
            html.append("<ul class=\"text-list\">");
            for (String warning : submission.getAnomalies()) {
                html.append("<li>").append(escape(warning)).append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</section>\n");

        appendCorrections(html, submission.getCorrections());
        appendCases(html, grade.getTestResults());
        html.append("</div></details></td></tr>\n");
    }

    private void appendCorrections(StringBuilder html, List<SubmissionCorrection> corrections) {
        html.append("<section class=\"detail-block\"><h3>Recorded corrections</h3>");
        if (corrections.isEmpty()) {
            html.append("<p>No corrections recorded.</p>");
        } else {
            html.append("<div class=\"table-scroll compact-table\"><table><thead><tr><th scope=\"col\">Type</th>")
                    .append("<th scope=\"col\">Path</th><th scope=\"col\">Field</th><th scope=\"col\">Original value</th>")
                    .append("<th scope=\"col\">Corrected value</th><th scope=\"col\">Header added</th></tr></thead><tbody>");
            for (SubmissionCorrection correction : corrections) {
                html.append("<tr><td>").append(escape(correction.getType())).append("</td><td>")
                        .append(escape(pathText(correction.getRelativePath()))).append("</td><td>")
                        .append(escape(correction.getField())).append("</td><td><pre class=\"value-block\">")
                        .append(escape(correction.getOriginalValue())).append("</pre></td><td><pre class=\"value-block\">")
                        .append(escape(correction.getCorrectedValue())).append("</pre></td><td>")
                        .append(correction.isHeaderAdded() ? "Yes" : "No").append("</td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        html.append("</section>\n");
    }

    private void appendCases(StringBuilder html, List<TestCaseResult> results) {
        html.append("<section class=\"detail-block\"><h3>Question and case results</h3>");
        if (results.isEmpty()) {
            html.append("<p>No case results recorded.</p></section>\n");
            return;
        }
        html.append("<div class=\"table-scroll compact-table\"><table><thead><tr><th scope=\"col\">Question</th>")
                .append("<th scope=\"col\">Case</th><th scope=\"col\">Status</th><th scope=\"col\">Points</th>")
                .append("<th scope=\"col\">Recorded duration</th><th scope=\"col\">Recorded output</th></tr></thead><tbody>");
        for (TestCaseResult result : results) {
            html.append("<tr><td>").append(escape(result.getTestCase().getQuestionId()))
                    .append("</td><td>").append(escape(result.getTestCase().getId()))
                    .append("</td><td><span class=\"case-status\">").append(escape(result.getStatus().name()))
                    .append("</span></td><td class=\"numeric\">")
                    .append(format(caseScore(result))).append(" / ")
                    .append(format(BigDecimal.valueOf(result.getTestCase().getMaximumScore())))
                    .append("</td><td class=\"numeric\">").append(result.getDurationMillis())
                    .append(" ms</td><td>");
            appendOutputDetails(html, "stdout", result.getStandardOutput());
            appendOutputDetails(html, "stderr", result.getStandardError());
            html.append("</td></tr>");
        }
        html.append("</tbody></table></div><p class=\"metric-description\">COMPILE_ERROR durations refer to ")
                .append("the compilation attempt and can repeat across its cases.</p></section>\n");
    }

    private void appendOutputDetails(StringBuilder html, String label, String output) {
        html.append("<details class=\"output-details\"><summary>").append(label).append("</summary>");
        if (output.isEmpty()) {
            html.append("<p>No output recorded.</p>");
        } else {
            html.append("<pre class=\"output-block\">").append(escape(output)).append("</pre>");
        }
        html.append("</details>");
    }

    private void appendAnalysisSection(StringBuilder html, List<AnalysisFinding> findings,
                                       List<SimilarityFinding> similarities) {
        html.append("<section class=\"analysis-section\" aria-labelledby=\"analysis-title\"><h2 id=\"analysis-title\">Analysis findings</h2>\n");
        if (findings.isEmpty() && similarities.isEmpty()) {
            html.append("<p>No analysis data supplied for this run.</p>\n</section>\n");
            return;
        }
        if (findings.isEmpty()) {
            html.append("<h3>Code analysis findings</h3><p>No code analysis findings supplied.</p>\n");
        } else {
            html.append("<h3>Code analysis findings</h3><ul class=\"analysis-list\">");
            for (AnalysisFinding finding : findings) {
                html.append("<li><strong>").append(escape(finding.getCode())).append("</strong><span>")
                        .append(escape(pathText(finding.getSourceFile()))).append("</span><p>")
                        .append(escape(finding.getMessage())).append("</p></li>");
            }
            html.append("</ul>\n");
        }
        if (similarities.isEmpty()) {
            html.append("<h3>Similarity findings</h3><p>No similarity findings supplied.</p>\n");
        } else {
            html.append("<h3>Similarity findings</h3><ul class=\"analysis-list\">");
            for (SimilarityFinding finding : similarities) {
                BigDecimal percent = BigDecimal.valueOf(finding.getSimilarity())
                        .multiply(BigDecimal.valueOf(100));
                html.append("<li><strong>Similarity: ").append(format(percent)).append("%</strong><span>")
                        .append(escape(pathText(finding.getFirstArchive()))).append(" ↔ ")
                        .append(escape(pathText(finding.getSecondArchive()))).append("</span>");
                if (finding.getEvidence().isEmpty()) {
                    html.append("<p>No evidence details recorded.</p>");
                } else {
                    html.append("<ul class=\"text-list\">");
                    for (String evidence : finding.getEvidence()) {
                        html.append("<li>").append(escape(evidence)).append("</li>");
                    }
                    html.append("</ul>");
                }
                html.append("</li>");
            }
            html.append("</ul>\n");
        }
        html.append("</section>\n");
    }

    private void detailItem(StringBuilder html, String label, String value) {
        html.append("<dt>").append(escape(label)).append("</dt><dd>").append(escape(value))
                .append("</dd>");
    }

    private String statusLabel(SubmissionStatus status) {
        return "<span class=\"prep-status\">" + escape(status == null ? "UNKNOWN" : status.name()) + "</span>";
    }

    private String displayName(StudentIdentity identity, int ordinal) {
        if (identity != null) {
            String matchedName = rosterName(identity);
            if (hasText(matchedName)) {
                return matchedName;
            }
            if (hasText(identity.getName())) {
                return identity.getName();
            }
            String username = preferredUsername(identity);
            if (hasText(username)) {
                return username;
            }
        }
        return "Unidentified submission #" + ordinal;
    }

    private BigDecimal totalScore(StudentGrade grade) {
        return BigDecimal.valueOf(grade.getTotalScore());
    }

    private BigDecimal questionScore(StudentGrade grade, String questionId) {
        return BigDecimal.valueOf(grade.getQuestionScore(questionId));
    }

    private BigDecimal caseScore(TestCaseResult result) {
        return BigDecimal.valueOf(result.getScore());
    }

    private BigDecimal median(List<BigDecimal> scores) {
        List<BigDecimal> ordered = new ArrayList<>(scores);
        ordered.sort(Comparator.naturalOrder());
        int middle = ordered.size() / 2;
        if (ordered.size() % 2 == 1) {
            return ordered.get(middle);
        }
        return ordered.get(middle - 1).add(ordered.get(middle)).divide(BigDecimal.valueOf(2));
    }

    private String format(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.signum() == 0 ? "0" : normalized.toPlainString();
    }

    private String pathText(Path path) {
        return path == null ? "Not recorded" : path.toString();
    }

    private String archiveName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "Not recorded";
        }
        return path.getFileName().toString();
    }

    private String preferredUsername(StudentIdentity identity) {
        if (identity == null) {
            return "";
        }
        if (identity.getStudentRecord() != null
                && hasText(identity.getStudentRecord().getUsername())) {
            return identity.getStudentRecord().getUsername();
        }
        return nullToEmpty(identity.getUsername());
    }

    private String rosterName(StudentIdentity identity) {
        if (identity == null || identity.getStudentRecord() == null) {
            return "";
        }
        String firstName = nullToEmpty(identity.getStudentRecord().getFirstName()).trim();
        String lastName = nullToEmpty(identity.getStudentRecord().getLastName()).trim();
        return (firstName + " " + lastName).trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "Not recorded";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream input = OfflineHtmlReportGenerator.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("required HTML report resource is missing: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
