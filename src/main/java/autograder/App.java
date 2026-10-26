package autograder;

import autograder.api.BoilerplateDetector;
import autograder.api.FeedbackGenerator;
import autograder.api.HtmlReportGenerator;
import autograder.config.BoilerplateConfig;
import autograder.config.FeedbackConfig;
import autograder.config.HtmlReportConfig;
import autograder.config.RunSettings;
import autograder.config.RuntimePathOverrides;
import autograder.grading.GradingRun;
import autograder.model.GradingReportContext;
import autograder.model.StudentGrade;

import java.io.IOException;
import java.nio.file.Path;

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
            RunSettings settings = RunSettings.load(Path.of(args[0]), RuntimePathOverrides.none());
            RunOutcome outcome = new GradingService().run(settings, new ConsoleRunListener());
            return report(outcome);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("could not complete grading: " + exception.getMessage());
            return 1;
        }
    }

    private static int report(RunOutcome outcome) {
        if (outcome.status() == RunOutcome.Status.NO_SUBMISSIONS) {
            System.err.println("No submission archives matched the configured input directory and pattern");
        } else if (outcome.failure() != null) {
            System.err.println("could not complete grading: " + outcome.failure().getMessage());
        }
        return outcome.exitCode();
    }

    static int generateReports(GradingRun run, BoilerplateConfig boilerplate, BoilerplateDetector detector,
                               HtmlReportConfig html, HtmlReportGenerator htmlGenerator,
                               FeedbackConfig feedback, FeedbackGenerator feedbackGenerator) {
        StarterAnalysis.Result analysis = StarterAnalysis.analyze(run, boilerplate, detector);
        int htmlExit = generateHtmlReport(analysis.run(), html, htmlGenerator);
        int feedbackExit = generateFeedback(analysis.run(), feedback, feedbackGenerator);
        return Math.max(analysis.exitCode(), Math.max(htmlExit, feedbackExit));
    }

    static int generateFeedback(GradingRun run, FeedbackConfig config, FeedbackGenerator generator) {
        if (!config.enabled()) {
            return 0;
        }
        Path directory = run.directory().resolve(config.directory());
        int written = 0;
        int failed = 0;
        for (StudentGrade grade : run.batch().getGrades()) {
            try {
                generator.generate(grade, directory);
                written++;
            } catch (IOException | RuntimeException exception) {
                failed++;
                System.err.println("could not write feedback for "
                        + grade.getSubmission().getArchivePath().getFileName() + ": " + exception.getMessage());
            }
        }
        System.out.println("Student feedback: " + written + " Markdown files in " + directory);
        if (failed > 0) {
            System.err.println(failed + " feedback files could not be written; grading and CSV exports "
                    + "are complete in " + run.directory());
            return 1;
        }
        return 0;
    }

    static int generateHtmlReport(GradingRun run, HtmlReportConfig config,
                                 HtmlReportGenerator generator) {
        if (!config.enabled()) {
            return 0;
        }
        try {
            Path report = generator.generate(new GradingReportContext(run.batch(), run.withheld()),
                    run.directory());
            System.out.println("HTML dashboard: " + report);
            return 0;
        } catch (IOException exception) {
            System.err.println("could not write HTML dashboard " + run.directory().resolve(config.file())
                    + ": " + exception.getMessage());
            System.err.println("Grading and CSV exports are complete; reports remain in " + run.directory());
            return 1;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar <application.jar> <configuration.properties>");
        System.out.println("Prepares, compiles and grades submissions using timed instructor cases");
    }
}
