package autograder;

import autograder.analysis.boilerplate.StarterCodeDetector;
import autograder.api.PlagiarismDetector;
import autograder.api.ProgressListener;
import autograder.config.AppConfig;
import autograder.config.RunSettings;
import autograder.grading.GradingPipeline;
import autograder.grading.GradingRun;
import autograder.model.SubmissionResult;
import autograder.plagiarism.NGramPlagiarismDetector;
import autograder.report.feedback.MarkdownFeedbackGenerator;
import autograder.report.html.OfflineHtmlReportGenerator;
import autograder.submission.SubmissionProcessor;

import java.util.List;

/**
 * Runs preparation, grading and report generation for one configuration.
 * Contains no user interface code and never exits the JVM.
 */
public final class GradingService {
    private static final String GRADING_STAGE = "grading";

    public RunOutcome run(RunSettings settings, RunListener listener) {
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.ENVIRONMENT, new InterruptedException("grading stopped"));
        }
        GradingPipeline pipeline;
        try {
            pipeline = new GradingPipeline(settings.app(), settings.execution(), detector(settings),
                    gradingAdapter(listener));
            pipeline.validateEnvironment();
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.ENVIRONMENT, exception, List.of(), null);
        }
        return prepareAndGrade(settings, listener, pipeline);
    }

    private RunOutcome prepareAndGrade(RunSettings settings, RunListener listener, GradingPipeline pipeline) {
        AppConfig app = settings.app();
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.PREPARATION, new InterruptedException("grading stopped"));
        }
        List<SubmissionResult> results;
        try {
            listener.preparing(app.getAssessment().getName());
            results = new SubmissionProcessor(app, listener).processBatch();
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.PREPARATION, exception, List.of(), null);
        }
        if (results.isEmpty()) {
            return new RunOutcome(RunOutcome.Status.NO_SUBMISSIONS, null, null, results, null, 1);
        }
        listener.prepared(results, app);
        if (Thread.currentThread().isInterrupted()) {
            return interrupted(RunOutcome.Stage.GRADING, new InterruptedException("grading stopped"));
        }
        return gradeAndReport(settings, listener, pipeline, results);
    }

    private RunOutcome gradeAndReport(RunSettings settings, RunListener listener, GradingPipeline pipeline,
                                      List<SubmissionResult> results) {
        GradingRun run;
        try {
            run = pipeline.grade(results);
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.GRADING, exception, results, null);
        }
        listener.graded(run, settings);
        int reportExit;
        try {
            reportExit = reports(settings, run);
        } catch (Exception exception) {
            return failed(RunOutcome.Stage.REPORTS, exception, results, run);
        }
        if (reportExit == 0) {
            return new RunOutcome(RunOutcome.Status.COMPLETED, null, null, results, run, 0);
        }
        return new RunOutcome(RunOutcome.Status.REPORT_FAILURES, null, null, results, run, reportExit);
    }

    private static int reports(RunSettings settings, GradingRun run) {
        AppConfig app = settings.app();
        return App.generateReports(run, settings.boilerplate(),
                new StarterCodeDetector(app.getAssessment(), settings.boilerplate()),
                settings.sanity(), settings.html(), new OfflineHtmlReportGenerator(settings.html().file()),
                settings.feedback(), new MarkdownFeedbackGenerator(
                app.getAssessment(), settings.execution().caseTimeoutMillis(),
                settings.feedback().maxDiagnosticLines(), app.getRosterPrefix(),
                app.getRosterLastNamePlaceholder()));
    }

    private static PlagiarismDetector detector(RunSettings settings) {
        if (settings.plagiarism().enabled()) {
            return new NGramPlagiarismDetector(settings.plagiarism());
        }
        return (submissions, assessment) -> List.of();
    }

    private static ProgressListener gradingAdapter(RunListener listener) {
        return (stage, item, completed, total) -> {
            if (GRADING_STAGE.equals(stage)) {
                listener.gradingProgress(item, completed, total);
            }
        };
    }

    private static RunOutcome failed(RunOutcome.Stage stage, Exception exception,
                                     List<SubmissionResult> preparation, GradingRun run) {
        if (exception instanceof InterruptedException) {
            return interrupted(stage, exception, preparation, run);
        }
        return new RunOutcome(RunOutcome.Status.FAILED, stage, exception, preparation, run, 1);
    }

    private static RunOutcome interrupted(RunOutcome.Stage stage, Exception exception) {
        return interrupted(stage, exception, List.of(), null);
    }

    private static RunOutcome interrupted(RunOutcome.Stage stage, Exception exception,
                                          List<SubmissionResult> preparation, GradingRun run) {
        Thread.currentThread().interrupt();
        return new RunOutcome(RunOutcome.Status.INTERRUPTED, stage, exception, preparation, run, 1);
    }
}
