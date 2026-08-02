package seedu.unienable.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures {@code java.util.logging} for the whole application: one {@link FileHandler} under
 * the data directory capturing INFO and above (with full stack traces for logged exceptions), and
 * no console handler at all - the CLI's own framed output is the user-facing channel, so console
 * logging would just interleave with or duplicate it.
 *
 * <p>Tracks only the single {@code FileHandler} it adds, never touching a handler added by
 * anyone else (e.g. a test capturing records with its own {@link Handler}): {@link #configure}
 * replaces just that tracked handler on each call, so repeated initialisation (once per
 * {@code ApplicationRunner} instance, as tests do) never accumulates duplicates; {@link #shutdown}
 * closes and detaches it, releasing the file so a caller (or a test's {@code @TempDir} cleanup)
 * can delete or replace the underlying file immediately after a run ends.
 */
final class LoggingConfig {
    private static final String LOG_FILE_NAME = "unienable.log";

    private static boolean defaultHandlersRemoved;
    private static Handler activeFileHandler;

    private LoggingConfig() {
    }

    /**
     * Points application-wide logging at a file under the given data directory, replacing the
     * file handler (if any) a previous call left active. If the log file cannot be created (e.g.
     * a read-only data directory), logging is disabled for this run rather than preventing the
     * application from starting.
     *
     * @param dataDirectory the directory to create {@code unienable.log} under
     */
    static synchronized void configure(Path dataDirectory) {
        Logger rootLogger = Logger.getLogger("");
        removeDefaultHandlersOnce(rootLogger);
        detachActiveFileHandler(rootLogger);
        rootLogger.setLevel(Level.INFO);
        try {
            Files.createDirectories(dataDirectory);
            FileHandler fileHandler = new FileHandler(
                    dataDirectory.resolve(LOG_FILE_NAME).toAbsolutePath().toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);
            rootLogger.addHandler(fileHandler);
            activeFileHandler = fileHandler;
        } catch (IOException e) {
            // Logging is a diagnostic aid, not a startup requirement - if the log file can't be
            // created, the application must still start; it just runs without a file log for
            // this session rather than failing or falling back to console output.
            rootLogger.setLevel(Level.OFF);
        }
    }

    /**
     * Closes and detaches the active file handler, if any, releasing its lock on the log file.
     * Callers should invoke this once they are done logging (e.g. when a run of the application
     * ends), so the file is free to be deleted or reopened elsewhere immediately afterward.
     */
    static synchronized void shutdown() {
        detachActiveFileHandler(Logger.getLogger(""));
    }

    private static void removeDefaultHandlersOnce(Logger rootLogger) {
        if (defaultHandlersRemoved) {
            return;
        }
        for (Handler handler : rootLogger.getHandlers()) {
            handler.close();
            rootLogger.removeHandler(handler);
        }
        defaultHandlersRemoved = true;
    }

    private static void detachActiveFileHandler(Logger rootLogger) {
        if (activeFileHandler == null) {
            return;
        }
        activeFileHandler.close();
        rootLogger.removeHandler(activeFileHandler);
        activeFileHandler = null;
    }
}
