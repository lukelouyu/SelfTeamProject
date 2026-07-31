package seedu.unienable;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import seedu.unienable.app.ApplicationRunner;

/** Main entry-point for the UniEnable application: loads saved data, then runs the command loop. */
public class UniEnable {
    private static final Path DATA_DIRECTORY = Paths.get("data");

    /**
     * Application entry point. Runs against the real "data" folder and standard input.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        run(DATA_DIRECTORY, System.in);
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
}
