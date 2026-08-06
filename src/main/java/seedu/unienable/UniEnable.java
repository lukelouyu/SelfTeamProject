package seedu.unienable;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

import seedu.unienable.app.ApplicationRunner;

/** Main entry-point for the UniEnable application: loads saved data, then runs the command loop. */
public class UniEnable {
    private static final Path DATA_DIRECTORY = Paths.get("data");
    private static final String FIXED_NOW_PROPERTY = "unienable.fixedNow";

    /**
     * Runs the application against the real "data" folder and standard input.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        run(DATA_DIRECTORY, System.in, resolveNowSupplier());
    }

    /**
     * Runs the full application: loads saved data from the given directory, then processes
     * commands from the given input stream until "bye" or the stream ends. Separated from
     * main() so tests can point it at a temporary directory instead of the real "data" folder.
     *
     * @param dataDirectory the directory containing (or to create) the five data files
     * @param input the source of command-line input, e.g. System.in
     */
    public static void run(Path dataDirectory, InputStream input) {
        new ApplicationRunner(dataDirectory, input).run();
    }

    /**
     * Runs the full application with an injectable current-time source for deterministic scripted
     * testing.
     *
     * @param dataDirectory the directory containing (or to create) the five data files
     * @param input the source of command-line input, e.g. System.in
     * @param nowSupplier the current-time source to use for command dispatch
     */
    public static void run(Path dataDirectory, InputStream input, Supplier<LocalDateTime> nowSupplier) {
        new ApplicationRunner(dataDirectory, input, nowSupplier).run();
    }

    private static Supplier<LocalDateTime> resolveNowSupplier() {
        String fixedNowText = System.getProperty(FIXED_NOW_PROPERTY);
        if (fixedNowText == null || fixedNowText.isBlank()) {
            return LocalDateTime::now;
        }
        try {
            LocalDateTime fixedNow = LocalDateTime.parse(fixedNowText.trim());
            return () -> fixedNow;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("System property " + FIXED_NOW_PROPERTY
                    + " must be an ISO local date-time such as 2026-08-19T16:00.", e);
        }
    }
}
