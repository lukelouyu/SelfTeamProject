package seedu.unienable.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.storage.Storage;
import seedu.unienable.testutil.recur.RecurrenceTestData;

/**
 * Verifies that a save failure occurring right after {@code recur}/{@code reset all} rolls back
 * the complete in-memory batch instead of just being reported - the behaviour
 * {@code ApplicationRunner.ApplicationStateSnapshot} exists for. Every other mutating command
 * changes at most one activity/topic, so leaving that one change in memory after a failed save is
 * accepted, existing behaviour; recur/reset can change many at once, so a failed save must not
 * leave a partially-applied batch visible only in memory. Uses the package-private constructor
 * that injects a {@link Storage} whose {@code saveAll} always fails, so this never depends on
 * real filesystem permission quirks.
 */
class ApplicationRunnerTest {
    @TempDir
    Path dataDirectory;

    private String run(String input, Storage storage) {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
        try {
            new ApplicationRunner(dataDirectory,
                    new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), storage).run();
        } finally {
            System.setOut(originalOut);
        }
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void recur_saveFails_rollsBackWholeBatchAndReportsNoFalseSuccess() throws Exception {
        RecurrenceTestData.writeCalendar(dataDirectory.resolve("academic-calendar.txt"));
        String input = String.join("\n",
                "add n/CG3201 LAB [01] c/ACADEMIC date/2027-01-26 type/FIXED from/14:00 to/17:00 "
                        + "energy/3 sensory/3",
                "recur 1 week 3;7;9;11",
                "y",
                "list",
                "bye") + "\n";

        String output = run(input, new FailingSaveAllStorage(dataDirectory));

        assertFalse(output.contains("Created 3 recurring sessions"),
                "a failed save must never show recur's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Here are 1 matching activity:"),
                "the three planned occurrences must not remain in memory after the rollback");
    }

    @Test
    public void resetAllDeleteAll_saveFails_rollsBackAndReportsNoFalseSuccess() throws Exception {
        String input = String.join("\n",
                "add n/Task one c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "add n/Task two c/OTHERS date/2099-01-02 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "reset all",
                "1",
                "list",
                "bye") + "\n";

        String output = run(input, new FailingSaveAllStorage(dataDirectory));

        assertFalse(output.contains("All user data has been reset."),
                "a failed save must never show reset's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Here are 2 matching activities:"),
                "both activities must still be in memory after the rollback");
    }

    /** A real, filesystem-backed Storage whose saveAll() always fails without touching disk. */
    private static final class FailingSaveAllStorage extends Storage {
        private FailingSaveAllStorage(Path dataDirectory) {
            super(dataDirectory);
        }

        @Override
        public void saveAll(List<Activity> activities, List<Topic> topics, ActivityOrder order)
                throws StorageException {
            throw new StorageException("simulated disk failure");
        }
    }
}
