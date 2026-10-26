package autograder;

import autograder.analysis.sanity.SanityChecker;
import autograder.analysis.sanity.SanityCsvWriter;
import autograder.analysis.sanity.SanityFinding;
import autograder.config.SanityConfig;
import autograder.grading.GradingRun;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

final class SanityAnalysis {
    static final String REPORT = "sanity-checks.csv";

    private SanityAnalysis() {
    }

    record Result(List<SanityFinding> findings, int exitCode) {
    }

    static Result analyze(GradingRun run, SanityConfig config) {
        if (!config.enabled()) {
            return new Result(List.of(), 0);
        }
        int exitCode = 0;
        List<SanityFinding> findings = List.of();
        try {
            findings = new SanityChecker().check(run, config);
        } catch (RuntimeException exception) {
            System.err.println("could not run sanity checks: " + exception.getMessage());
            exitCode = 1;
        }
        return new Result(findings, Math.max(exitCode, write(run.directory(), findings)));
    }

    private static int write(Path directory, List<SanityFinding> findings) {
        Path report = directory.resolve(REPORT);
        try {
            Files.writeString(report, SanityCsvWriter.format(findings),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException | RuntimeException exception) {
            System.err.println("could not write sanity checks " + report + ": " + exception.getMessage());
            return 1;
        }
        print(findings, report);
        return 0;
    }

    private static void print(List<SanityFinding> findings, Path report) {
        if (findings.isEmpty()) {
            System.out.println("Sanity checks: none");
            return;
        }
        System.out.println("Sanity checks: " + findings.size() + " possible assessment problems (" + report + ")");
        for (SanityFinding finding : findings) {
            String where = finding.getCaseId().isEmpty() ? finding.getQuestionId() : finding.getCaseId();
            System.out.println("  " + finding.getCheck() + " | " + where + " | " + finding.getMessage());
        }
    }
}
