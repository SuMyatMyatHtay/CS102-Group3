package autograder;

import autograder.config.AppConfig;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.submission.SubmissionProcessor;
import autograder.ui.ConsoleProgressListener;

import java.nio.file.Path;
import java.util.List;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args) {
        if (args.length == 1 && args[0].equals("--help")) {
            printUsage();
            return 0;
        }
        if (args.length != 1) {
            printUsage();
            return 1;
        }

        try {
            AppConfig config = AppConfig.load(Path.of(args[0]));
            System.out.println("Preparing submissions for " + config.getAssessment().getName());
            SubmissionProcessor processor = new SubmissionProcessor(config,
                    new ConsoleProgressListener());
            List<SubmissionResult> results = processor.processBatch();
            if (results.isEmpty()) {
                System.err.println("No submission archives matched the configured input directory and pattern");
                return 1;
            }
            printResults(results);
            System.out.println("Diagnostics: " + config.getAnomalyLog());
            System.out.println("Corrections: " + config.getCorrectionLog());
            System.out.println("Preparation only: student code has not been run and no scores were exported");
            return 0;
        } catch (Exception exception) {
            System.err.println("could not run submission ingestion: " + exception.getMessage());
            return 1;
        }
    }

    private static void printResults(List<SubmissionResult> results) {
        int ready = 0;
        int reviewRequired = 0;
        int failed = 0;
        System.out.println("Prepared " + results.size() + " submission archives");
        for (SubmissionResult result : results) {
            if (result.getStatus() == SubmissionStatus.READY) {
                ready++;
            } else if (result.getStatus() == SubmissionStatus.REVIEW_REQUIRED) {
                reviewRequired++;
            } else {
                failed++;
            }
            String identityLabel = result.getIdentity().getLabel();
            if (result.getIdentity().getStudentRecord() != null) {
                identityLabel = result.getIdentity().getStudentRecord().getUsername();
            }
            System.out.println(result.getArchivePath().getFileName() + " | "
                    + identityLabel + " | " + result.getStatus()
                    + " | issues=" + result.getAnomalies().size()
                    + " | corrections=" + result.getCorrections().size());
            if (result.getWorkingRoot() != null) {
                System.out.println("  Working copy: " + result.getWorkingRoot());
            }
        }
        System.out.println("Ready: " + ready + "; needs review: " + reviewRequired
                + "; failed: " + failed);
        System.out.println("READY means ready for the next stage, not a passing grade");
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar <application.jar> <configuration.properties>");
        System.out.println("Checks and prepares submission copies without executing student code");
    }
}
