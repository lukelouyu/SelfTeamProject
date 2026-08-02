package seedu.unienable.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.storage.Storage;
import seedu.unienable.testutil.recur.RecurrenceTestData;

/**
 * Verifies that a save failure occurring right after any mutating command rolls back that
 * command's complete pre-command state instead of just being reported -
 * {@code ApplicationRunner.ApplicationStateSnapshot}'s job for every mutating command, not just
 * recur/reset. Uses the package-private constructor that injects a {@link Storage} whose
 * {@code saveAll} fails in a controlled, deterministic way, so none of this depends on real
 * filesystem permission quirks.
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

    private static FailAfterStorage alwaysFailing(Path dataDirectory) {
        return new FailAfterStorage(dataDirectory, 0);
    }

    @Test
    public void mutatingCommandSaveFailure_logsRecordWithThrowable() throws Exception {
        RecordingHandler handler = new RecordingHandler();
        Logger.getLogger("").addHandler(handler);
        try {
            String input = String.join("\n",
                    "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                    "bye") + "\n";

            run(input, alwaysFailing(dataDirectory));

            boolean loggedWithThrowable = handler.records.stream()
                    .anyMatch(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                            && record.getThrown() != null);
            assertTrue(loggedWithThrowable,
                    "a forced save failure must log a record at WARNING or above with the exception attached");
        } finally {
            Logger.getLogger("").removeHandler(handler);
        }
    }

    @Test
    public void normalSuccessfulCommands_produceNoWarningOrSevereLogRecords() throws Exception {
        RecordingHandler handler = new RecordingHandler();
        Logger.getLogger("").addHandler(handler);
        try {
            String input = String.join("\n",
                    "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                    "mark 1",
                    "list",
                    "bye") + "\n";

            String output = run(input, new Storage(dataDirectory));

            assertTrue(output.contains("Got it. Activity [1] has been added"),
                    "test setup: the add must actually have succeeded");
            boolean anyNoisyRecord = handler.records.stream()
                    .anyMatch(record -> record.getLevel().intValue() >= Level.WARNING.intValue());
            assertFalse(anyNoisyRecord,
                    "normal successful commands must not produce WARNING/SEVERE log records");
        } finally {
            Logger.getLogger("").removeHandler(handler);
        }
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

        // 1 success allowed: the seeding "add" must persist normally so activity [1] exists for
        // recur to act on; recur's own save (the 2nd call) is the one under test and must fail.
        String output = run(input, new FailAfterStorage(dataDirectory, 1));

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

        // 2 successes allowed: both seeding "add"s must persist so reset has something real to
        // act on; reset's own save (the 3rd call) is the one under test and must fail.
        String output = run(input, new FailAfterStorage(dataDirectory, 2));

        assertFalse(output.contains("All user data has been reset."),
                "a failed save must never show reset's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Here are 2 matching activities:"),
                "both activities must still be in memory after the rollback");
    }

    @Test
    public void add_saveFails_rollsBackAndReportsNoFalseSuccess() throws Exception {
        String input = String.join("\n",
                "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "list",
                "bye") + "\n";

        String output = run(input, alwaysFailing(dataDirectory));

        assertFalse(output.contains("Got it. Activity [1] has been added"),
                "a failed save must never show add's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("No activities found."),
                "the added activity must not remain in memory after the rollback");
    }

    @Test
    public void add_saveFailsThenRetrySucceeds_doesNotConsumeIdOnFailedAttempt() throws Exception {
        String addCommand = "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1";
        String input = String.join("\n", addCommand, addCommand, "list", "bye") + "\n";

        // First add's save fails and must be rolled back (including the ID it consumed); the
        // identical second add's save succeeds and must receive ID [1], not [2].
        String output = run(input, new FailOnceThenSucceedStorage(dataDirectory));

        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Got it. Activity [1] has been added"),
                "the retry must receive ID [1] - the failed attempt's ID must not have been consumed");
        assertTrue(output.contains("Here are 1 matching activity:"));
    }

    @Test
    public void edit_saveFails_rollsBackToOriginalActivity() throws Exception {
        String input = String.join("\n",
                "add n/Original c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "edit 1 n/Changed",
                "y",
                "list",
                "bye") + "\n";

        // 1 success allowed: the seeding add.
        String output = run(input, new FailAfterStorage(dataDirectory, 1));

        assertFalse(output.contains("Activity [1] has been updated"),
                "a failed save must never show edit's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Original"), "the original description must still be in memory");
        assertFalse(output.substring(output.lastIndexOf("matching activit")).contains("Changed"),
                "the edited description must not appear in the post-rollback list");
    }

    @Test
    public void delete_saveFails_rollsBackDeletedActivity() throws Exception {
        String input = String.join("\n",
                "add n/Keep me c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "delete 1",
                "y",
                "list",
                "bye") + "\n";

        String output = run(input, new FailAfterStorage(dataDirectory, 1));

        assertFalse(output.contains("Activity [1] has been deleted"),
                "a failed save must never show delete's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Here are 1 matching activity:"),
                "the deleted activity must still be in memory after the rollback");
    }

    @Test
    public void mark_saveFails_rollsBackCompletionStatus() throws Exception {
        String input = String.join("\n",
                "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "mark 1",
                "list",
                "bye") + "\n";

        String output = run(input, new FailAfterStorage(dataDirectory, 1));

        assertFalse(output.contains("Nice! Activity [1] is now complete"),
                "a failed save must never show mark's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("[1][ ]"),
                "activity [1] must still show as incomplete after the rollback (proves the snapshot "
                        + "captured an independent copy, not a reference to the same mutated object)");
        assertFalse(output.contains("[1][X]"), "activity [1] must not show as complete after the rollback");
    }

    @Test
    public void unmark_saveFails_rollsBackCompletionStatus() throws Exception {
        String input = String.join("\n",
                "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "mark 1",
                "unmark 1",
                "list",
                "bye") + "\n";

        // 2 successes allowed: the seeding add, then the mark that must actually take effect.
        String output = run(input, new FailAfterStorage(dataDirectory, 2));

        assertFalse(output.contains("Activity [1] is now incomplete"),
                "a failed save must never show unmark's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("[1][X]"), "activity [1] must still show as complete after the rollback");
        assertFalse(output.contains("[1][ ]"), "activity [1] must not show as incomplete after the rollback");
    }

    @Test
    public void orderSet_saveFails_rollsBackToPreviousOrder() throws Exception {
        String input = String.join("\n", "order set time", "order view", "bye") + "\n";

        String output = run(input, alwaysFailing(dataDirectory));

        assertFalse(output.contains("Default activity order updated"),
                "a failed save must never show order set's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Saved default activity order: chronological"),
                "the default (chronological) order must still be in effect after the rollback");
    }

    @Test
    public void topicAdd_saveFails_rollsBackAddedTopic() throws Exception {
        String input = String.join("\n", "topic add c/ACADEMIC n/CG3207", "topic list c/ACADEMIC", "bye") + "\n";

        String output = run(input, alwaysFailing(dataDirectory));

        assertFalse(output.contains("Topic created"), "a failed save must never show topic add's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("No topics"), "the added topic must not remain in memory after the rollback");
    }

    @Test
    public void topicAdd_saveFailsThenRetrySucceeds_doesNotFalselyRejectAsDuplicate() throws Exception {
        String topicAddCommand = "topic add c/ACADEMIC n/CG3207";
        String input = String.join("\n", topicAddCommand, topicAddCommand, "bye") + "\n";

        String output = run(input, new FailOnceThenSucceedStorage(dataDirectory));

        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Topic created"),
                "the retry must succeed - the failed attempt's topic must not have been left in memory");
        assertFalse(output.contains("already exists"),
                "the retry must not be rejected as a duplicate of the rolled-back failed attempt");
    }

    @Test
    public void topicRename_saveFails_rollsBackNameAndEveryCascadedActivity() throws Exception {
        String input = String.join("\n",
                "topic add c/ACADEMIC n/OldName",
                "add n/Lecture one c/ACADEMIC date/2099-01-01 type/FIXED from/09:00 to/10:00 "
                        + "energy/1 sensory/1 topic/OldName",
                "add n/Lecture two c/ACADEMIC date/2099-01-02 type/FIXED from/09:00 to/10:00 "
                        + "energy/1 sensory/1 topic/OldName",
                "topic rename c/ACADEMIC old/OldName new/NewName",
                "y",
                "list",
                "topic list c/ACADEMIC",
                "bye") + "\n";

        // 3 successes allowed: topic add, then the two seeding adds; the rename's own save (the
        // 4th call) is the one under test and must fail.
        String output = run(input, new FailAfterStorage(dataDirectory, 3));

        assertFalse(output.contains("Topic renamed from OldName to NewName"),
                "a failed save must never show topic rename's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        String listSection = output.substring(output.indexOf("Here are"), output.indexOf("ACADEMIC topics:"));
        assertFalse(listSection.contains("NewName"),
                "neither cascaded activity may show the new topic name after the rollback");
        assertEquals(2, countOccurrences(listSection, "OldName"),
                "both activities must show the original topic name after the rollback");
        String topicSection = output.substring(output.indexOf("ACADEMIC topics:"));
        assertTrue(topicSection.contains("OldName"), "the topic itself must still show its original name");
        assertFalse(topicSection.contains("NewName"),
                "the topic registry must not show the new name after the rollback");
    }

    @Test
    public void topicDelete_saveFails_rollsBackDeletedTopic() throws Exception {
        String input = String.join("\n",
                "topic add c/ACADEMIC n/CG3207",
                "topic delete c/ACADEMIC n/CG3207",
                "y",
                "topic list c/ACADEMIC",
                "bye") + "\n";

        String output = run(input, new FailAfterStorage(dataDirectory, 1));

        assertFalse(output.contains("has been deleted"),
                "a failed save must never show topic delete's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.substring(output.lastIndexOf("ACADEMIC")).contains("CG3207"),
                "the deleted topic must still be in memory after the rollback");
    }

    @Test
    public void preferenceView_isReadOnlyAndDoesNotCreatePreferenceFile() throws Exception {
        SaveCallCountingStorage storage = new SaveCallCountingStorage(dataDirectory);

        String output = run("preference view\nbye\n", storage);

        assertEquals(0, storage.saveCallCount);
        assertFalse(Files.exists(dataDirectory.resolve("preferences.txt")));
        assertTrue(output.contains("Preferred daily start: 08:00"));
        assertTrue(output.contains("Tomato suggestion: OFF"));
    }

    @Test
    public void preferenceSet_confirmedPersistsAndReloadsOnRestart() throws Exception {
        String setInput = String.join("\n",
                "preference set tomato/on buffer/30 end/21:00 start/07:30",
                "y",
                "bye") + "\n";

        String setOutput = run(setInput, new Storage(dataDirectory));
        String restartOutput = run("preference view\nbye\n", new Storage(dataDirectory));

        assertTrue(setOutput.contains("Preference profile updated."));
        assertEquals(List.of(
                "PREFERRED_START|07:30",
                "PREFERRED_END|21:00",
                "MINIMUM_BUFFER|30",
                "TOMATO_SUGGESTION|ON"),
                Files.readAllLines(dataDirectory.resolve("preferences.txt")));
        assertTrue(restartOutput.contains("Preferred daily start: 07:30"));
        assertTrue(restartOutput.contains("Preferred daily end: 21:00"));
        assertTrue(restartOutput.contains("Minimum buffer: 30 minutes"));
        assertTrue(restartOutput.contains("Tomato suggestion: ON"));
    }

    @Test
    public void preferenceSet_cancelledMakesNoChangeAndDoesNotSave() throws Exception {
        SaveCallCountingStorage storage = new SaveCallCountingStorage(dataDirectory);
        String input = String.join("\n",
                "preference set tomato/on",
                "n",
                "preference view",
                "bye") + "\n";

        String output = run(input, storage);

        assertEquals(0, storage.saveCallCount);
        assertFalse(Files.exists(dataDirectory.resolve("preferences.txt")));
        assertTrue(output.contains("Cancelled. No changes were made."));
        assertTrue(output.contains("Tomato suggestion: OFF"));
    }

    @Test
    public void malformedPreferenceFile_warnsAndLoadsWholeDefaultProfile() throws Exception {
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("preferences.txt"), String.join("\n",
                "PREFERRED_START|07:00",
                "PREFERRED_END|21:00",
                "MINIMUM_BUFFER|30",
                "TOMATO_SUGGESTION|MAYBE"));

        String output = run("preference view\nbye\n", new Storage(dataDirectory));

        assertTrue(output.contains("preferences.txt"));
        assertTrue(output.contains("all defaults were loaded"));
        assertTrue(output.contains("Preferred daily start: 08:00"),
                "one malformed field must discard the otherwise valid custom fields");
        assertTrue(output.contains("Preferred daily end: 20:00"));
        assertTrue(output.contains("Minimum buffer: 15 minutes"));
        assertTrue(output.contains("Tomato suggestion: OFF"));
    }

    @Test
    public void preferenceSet_saveFails_rollsBackProfileAndShowsNoFalseSuccess() throws Exception {
        String input = String.join("\n",
                "preference set tomato/on start/07:00",
                "y",
                "preference view",
                "bye") + "\n";

        String output = run(input, alwaysFailing(dataDirectory));

        assertFalse(output.contains("Preference profile updated."));
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Preferred daily start: 08:00"));
        assertTrue(output.contains("Tomato suggestion: OFF"));
    }

    @Test
    public void resetAll_optionsApplyApprovedPreferenceRules() throws Exception {
        String input = String.join("\n",
                "preference set tomato/on",
                "y",
                "reset all",
                "2",
                "preference view",
                "reset all",
                "1",
                "preference view",
                "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));

        int retainedView = output.indexOf("Tomato suggestion: ON", output.indexOf("Reset complete."));
        int resetView = output.indexOf("Tomato suggestion: OFF", retainedView + 1);
        assertTrue(retainedView > 0, "reset option 2 must retain custom preferences");
        assertTrue(resetView > retainedView, "reset option 1 must restore default preferences");
    }

    @Test
    public void resetAllOptionOne_saveFails_restoresCompleteCustomPreferenceProfile() throws Exception {
        String input = String.join("\n",
                "preference set start/09:00 end/18:00 buffer/20 tomato/on",
                "y",
                "reset all",
                "1",
                "preference view",
                "bye") + "\n";

        String output = run(input, new FailAfterStorage(dataDirectory, 1));

        assertFalse(output.contains("All user data has been reset."));
        assertTrue(output.contains("[Error] Storage error:"));
        assertTrue(output.contains("Preferred daily start: 09:00"));
        assertTrue(output.contains("Preferred daily end: 18:00"));
        assertTrue(output.contains("Minimum buffer: 20 minutes"));
        assertTrue(output.contains("Tomato suggestion: ON"));
    }

    @Test
    public void resetAll_nothingToReset_doesNotInvokeSave() throws Exception {
        String input = String.join("\n", "reset all", "bye") + "\n";
        SaveCallCountingStorage storage = new SaveCallCountingStorage(dataDirectory);

        String output = run(input, storage);

        assertEquals(0, storage.saveCallCount, "resetting already-empty state must never call Storage.saveAll()");
        assertTrue(output.contains("All user data has been reset."),
                "the existing no-op message must still be shown even though nothing was actually saved");
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }

    /**
     * A real, filesystem-backed Storage whose saveAll() succeeds for the first
     * {@code successesAllowed} calls, then fails on every call after that. Lets a test's seeding
     * commands (e.g. an "add" that creates the activity the command actually under test will act
     * on) persist normally, while the command under test's own save still fails deterministically.
     */
    private static final class FailAfterStorage extends Storage {
        private int remainingSuccesses;

        private FailAfterStorage(Path dataDirectory, int successesAllowed) {
            super(dataDirectory);
            this.remainingSuccesses = successesAllowed;
        }

        @Override
        public void saveAll(List<Activity> activities, List<Topic> topics, ActivityOrder order,
                PreferenceProfile preferences) throws StorageException {
            if (remainingSuccesses > 0) {
                remainingSuccesses--;
                super.saveAll(activities, topics, order, preferences);
                return;
            }
            throw new StorageException("simulated disk failure");
        }
    }

    /** A real, filesystem-backed Storage whose saveAll() fails once, then succeeds on every later call. */
    private static final class FailOnceThenSucceedStorage extends Storage {
        private boolean hasFailedOnce;

        private FailOnceThenSucceedStorage(Path dataDirectory) {
            super(dataDirectory);
        }

        @Override
        public void saveAll(List<Activity> activities, List<Topic> topics, ActivityOrder order,
                PreferenceProfile preferences) throws StorageException {
            if (!hasFailedOnce) {
                hasFailedOnce = true;
                throw new StorageException("simulated disk failure");
            }
            super.saveAll(activities, topics, order, preferences);
        }
    }

    /** A real, filesystem-backed Storage that counts every saveAll() call while behaving normally. */
    private static final class SaveCallCountingStorage extends Storage {
        private int saveCallCount;

        private SaveCallCountingStorage(Path dataDirectory) {
            super(dataDirectory);
        }

        @Override
        public void saveAll(List<Activity> activities, List<Topic> topics, ActivityOrder order,
                PreferenceProfile preferences) throws StorageException {
            saveCallCount++;
            super.saveAll(activities, topics, order, preferences);
        }
    }

    /** Captures every LogRecord published to it, for tests to inspect after a run completes. */
    private static final class RecordingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
