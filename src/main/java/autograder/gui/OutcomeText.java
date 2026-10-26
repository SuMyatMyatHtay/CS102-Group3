package autograder.gui;

import autograder.RunOutcome;

import java.util.Locale;

/** Wording for run progress and results, kept free of Swing so it can be tested headless. */
public final class OutcomeText {
    public static final String CHECKING = "Checking setup…";
    public static final String WRITING_REPORTS = "Writing reports…";
    public static final String STOPPING = "Stopping…";
    public static final String FIX_FIELDS = "Fix the highlighted fields";
    private static final String ENVIRONMENT_HINT =
            " Check that a JDK is installed and the tester files are present.";

    private OutcomeText() {
    }

    public static String intake(String item, int completed, int total) {
        return step("Preparing", item, completed, total);
    }

    public static String grading(String item, int completed, int total) {
        return step("Grading", item, completed, total);
    }

    public static String headline(RunOutcome outcome) {
        switch (outcome.status()) {
            case COMPLETED:
                return "Grading complete";
            case REPORT_FAILURES:
                return "Grading complete. Some reports could not be written.";
            case NO_SUBMISSIONS:
                return "No submissions matched";
            case INTERRUPTED:
                return "Grading stopped. No scores were written.";
            default:
                return failed(outcome);
        }
    }

    private static String failed(RunOutcome outcome) {
        String stage = outcome.failedStage() == null
                ? "the run" : outcome.failedStage().name().toLowerCase(Locale.ROOT);
        String text = "Grading stopped during " + stage + ": " + reason(outcome.failure());
        if (outcome.failedStage() == RunOutcome.Stage.ENVIRONMENT) {
            return text + ENVIRONMENT_HINT;
        }
        return text;
    }

    private static String reason(Exception failure) {
        if (failure == null) {
            return "unknown error";
        }
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static String step(String verb, String item, int completed, int total) {
        String text = verb + " " + completed + " of " + total;
        return item == null || item.isBlank() ? text : text + ": " + item;
    }
}
