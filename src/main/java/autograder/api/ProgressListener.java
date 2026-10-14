package autograder.api;

public interface ProgressListener {
    void onProgress(String stage, String item, int completed, int total);
}
