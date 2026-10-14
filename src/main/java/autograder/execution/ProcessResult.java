package autograder.execution;

public record ProcessResult(int exitCode, boolean timedOut, long durationMillis,
                           String stdout, String stderr) {
}
