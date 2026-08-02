package seedu.unienable.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoggingConfigTest {
    @TempDir
    Path dataDirectory;

    @AfterEach
    public void releaseLogFileHandle() {
        // Otherwise the FileHandler configure() opened stays locked (Windows in particular) and
        // JUnit's @TempDir cleanup for this test fails.
        LoggingConfig.shutdown();
    }

    @Test
    public void configure_calledRepeatedly_neverAccumulatesMoreThanOneHandler() {
        LoggingConfig.configure(dataDirectory);
        LoggingConfig.configure(dataDirectory);
        LoggingConfig.configure(dataDirectory);

        assertEquals(1, Logger.getLogger("").getHandlers().length,
                "repeated configure() calls must never accumulate duplicate handlers");
    }

    @Test
    public void shutdown_releasesTheHandlerConfigureAdded() {
        LoggingConfig.configure(dataDirectory);

        LoggingConfig.shutdown();

        assertEquals(0, Logger.getLogger("").getHandlers().length,
                "shutdown() must detach the handler configure() added");
    }
}
