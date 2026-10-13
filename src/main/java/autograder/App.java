package autograder;

import autograder.api.PlagiarismDetector;
import autograder.config.AppConfig;
import autograder.config.ExecutionConfig;
import autograder.config.PlagiarismConfig;
import autograder.grading.GradingPipeline;
import autograder.grading.GradingRun;
import autograder.model.StudentGrade;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.plagiarism.NGramPlagiarismDetector;
import autograder.submission.SubmissionProcessor;
import autograder.ui.ConsoleProgressListener;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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
            ExecutionConfig execution = ExecutionConfig.load(Path.of(args[0]), config);
            PlagiarismConfig plagiarism = PlagiarismConfig.load(Path.of(args[0]));
            PlagiarismDetector detector = (submissions, assessment) -> List.of();
            if (plagiarism.enabled()) {
                detector = new NGramPlagiarismDetector(plagiarism);
            }
            GradingPipeline grading = new GradingPipeline(config, execution, detector);
            grading.validateEnvironment();
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
            GradingRun run = grading.grade(results);
            for (StudentGrade grade : run.batch().getGrades()) {
                String reason = run.withheld().get(grade.getSubmission());
                System.out.println(grade.getSubmission().getIdentity().getLabel() + " | "
                        + (reason == null ? "Score: " + grade.getTotalScore()
                        : "WITHHELD: " + reason));
            }
            System.out.println("Score report: " + run.directory().resolve(execution.reportFile()));
            System.out.println("Per-case diagnostics: " + run.directory().resolve("cases.csv"));
            System.out.println("Export decisions and review warnings: "
                    + run.directory().resolve("submissions.csv"));
            System.out.println("Withheld or absent students retain their original score-sheet rows");
            printSimilarity(run, plagiarism);
            return 0;
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("could not complete grading: " + exception.getMessage());
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

    private static void printSimilarity(GradingRun run, PlagiarismConfig plagiarism) {
        if (!plagiarism.enabled()) {
            System.out.println("Similarity check: disabled in configuration");
            return;
        }
        int pairs = run.batch().getSimilarities().size();
        System.out.println("Similarity review: " + pairs + " question pair(s) at or above "
                + String.format(Locale.ROOT, "%.2f", plagiarism.threshold())
                + " similarity, starter code excluded: "
                + run.directory().resolve(GradingPipeline.SIMILARITY_REPORT));
        if (pairs > 0) {
            System.out.println("Similarity needs instructor judgement and is not proof of copying");
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar <application.jar> <configuration.properties>");
        System.out.println("Prepares, compiles and grades submissions using timed instructor cases");
    }
}
