package autograder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void helpDoesNotRequireConfiguration() {
        assertEquals(0, App.run(new String[] {"--help"}));
    }

    @Test
    void missingArgumentsAndMissingConfigFailCleanly() {
        assertEquals(1, App.run(new String[] {}));
        assertEquals(1, App.run(new String[] {
                temporaryDirectory.resolve("missing.properties").toString()}));
    }
}
