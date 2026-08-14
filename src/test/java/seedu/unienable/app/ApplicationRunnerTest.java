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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.storage.Storage;
import seedu.unienable.testutil.recur.RecurrenceTestData;

/**
 * Verifies that a save failure occurring right after any mutating command rolls back that
 * command's complete pre-command state instead of just being reported -
 * {@code CommandTransactionExecutor}'s job for every mutating command, not just recur/reset. Uses
 * the package-private constructor that injects a {@link Storage} whose
 * {@code saveAll} fails in a controlled, deterministic way, so none of this depends on real
 * filesystem permission quirks.
 */
class ApplicationRunnerTest {
    @TempDir
    Path dataDirectory;

    private String run(String input, Storage storage) {
        return run(input, storage, LocalDateTime::now);
    }

    private String run(String input, Storage storage, java.util.function.Supplier<LocalDateTime> nowSupplier) {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
        try {
            new ApplicationRunner(dataDirectory,
                    new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), storage, nowSupplier).run();
        } finally {
            System.setOut(originalOut);
        }
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    private static FailAfterStorage alwaysFailing(Path dataDirectory) {
        return new FailAfterStorage(dataDirectory, 0);
    }

    private static RuntimeExceptionAfterStorage alwaysFailingWithRuntimeException(Path dataDirectory) {
        return new RuntimeExceptionAfterStorage(dataDirectory, 0);
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
    public void fixedNowSupplier_makesRelativeCommandsDeterministic() throws Exception {
        Storage storage = new Storage(dataDirectory);
        storage.prepareDataFiles();
        storage.saveActivities(List.of(new FixedActivity(1, "Deterministic lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 19), LocalTime.of(14, 0), LocalTime.of(15, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null)));

        String output = run("list today\nbye\n", storage, () -> LocalDateTime.of(2026, 8, 19, 16, 0));

        assertTrue(output.contains("Deterministic lecture"));
        assertTrue(output.contains("2026-08-19"));
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
    public void find_duplicateKeywordMarker_rejectedEndToEnd() throws Exception {
        // Bug F regression, application level: "find k/Study k/Assignment" must be rejected as a
        // duplicate marker, not silently mis-parsed into a garbled two-word keyword search.
        String input = String.join("\n", "find k/Study k/Assignment", "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));

        assertTrue(output.contains("Duplicate option \"k/\"."));
    }

    @Test
    public void topicRename_duplicateNewMarker_rejectedEndToEnd() throws Exception {
        String input = String.join("\n",
                "topic add c/ACADEMIC n/Foo",
                "topic rename c/ACADEMIC old/Foo new/Baz new/Qux",
                "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));

        assertTrue(output.contains("Duplicate option \"new/\"."));
    }

    @Test
    public void recur_pastOccurrence_rejectedEndToEndWithNothingCreated() throws Exception {
        // Bug D regression, end to end: Week 2's target date (2026-08-21) has already passed
        // relative to "now" (2026-08-25); the whole plan - including Week 3's genuinely future
        // date - must be rejected atomically before the confirmation prompt, with nothing created.
        RecurrenceTestData.writeCalendar(dataDirectory.resolve("academic-calendar.txt"));
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("activities.txt"),
                "FIXED|1|CS2113 LEC|ACADEMIC|2026-08-14|16:00|18:00|2|2|INCOMPLETE||\n");
        String input = String.join("\n", "recur 1 week 1;2;3", "list", "bye") + "\n";

        String output = run(input, new Storage(dataDirectory), () -> LocalDateTime.of(2026, 8, 25, 9, 0));

        assertTrue(output.contains("Week 2"));
        assertTrue(output.contains("2026-08-21"));
        assertTrue(output.contains("already passed"));
        assertTrue(output.contains("Here are 1 matching activity:"),
                "no recurring occurrence - past or future - may be created once any one is rejected");
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
    public void add_saveFailsWithRuntimeException_rollsBackAndReportsNoFalseSuccess() throws Exception {
        String input = String.join("\n",
                "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "list",
                "bye") + "\n";

        // An unchecked exception from saveAll() (e.g. an unanticipated bug in the storage layer)
        // must roll back exactly like a documented StorageException, not just be logged and
        // reported while the mutation stays applied in memory.
        String output = run(input, alwaysFailingWithRuntimeException(dataDirectory));

        assertFalse(output.contains("Got it. Activity [1] has been added"),
                "a failed save must never show add's success message");
        assertTrue(output.contains("[Error] An unexpected internal error occurred while saving"));
        assertTrue(output.contains("No activities found."),
                "the added activity must not remain in memory after the rollback");
    }

    @Test
    public void add_saveFailsWithRuntimeExceptionThenRetrySucceeds_doesNotConsumeIdOnFailedAttempt()
            throws Exception {
        String addCommand = "add n/Task c/OTHERS date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/1 sensory/1";
        String input = String.join("\n", addCommand, addCommand, "list", "bye") + "\n";

        // First add's save throws a RuntimeException and must be rolled back (including the ID
        // it consumed); the identical second add's save succeeds and must receive ID [1], not [2].
        String output = run(input, new RuntimeExceptionOnceThenSucceedStorage(dataDirectory));

        assertTrue(output.contains("[Error] An unexpected internal error occurred while saving"));
        assertTrue(output.contains("Got it. Activity [1] has been added"),
                "the retry must receive ID [1] - the failed attempt's ID must not have been consumed");
        assertTrue(output.contains("Here are 1 matching activity:"));
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
    public void recommendAdopt_persistsPlacementAndReloadsOnRestart() throws Exception {
        String adoptInput = String.join("\n",
                "add n/Essay draft c/ACADEMIC date/2099-01-01 type/FLEXIBLE earliest/09:00 latest/14:00 "
                        + "dur/60 energy/4 sensory/3",
                "recommend date/2099-01-01",
                "recommend adopt",
                "y",
                "bye") + "\n";

        String adoptOutput = run(adoptInput, new Storage(dataDirectory));
        String restartOutput = run("view 1\nbye\n", new Storage(dataDirectory));

        assertTrue(adoptOutput.contains("Recommendation adopted."));
        assertEquals(List.of("FLEXIBLE|1|Essay draft|ACADEMIC|2099-01-01|09:00|14:00|60|4|3|INCOMPLETE|||09:00"),
                Files.readAllLines(dataDirectory.resolve("activities.txt")));
        assertTrue(restartOutput.contains("Adopted     : 09:00 -> 10:00"));
    }

    @Test
    public void recommendAdopt_saveFails_rollsBackAdoptedPlacement() throws Exception {
        String input = String.join("\n",
                "add n/Essay draft c/ACADEMIC date/2099-01-01 type/FLEXIBLE earliest/09:00 latest/14:00 "
                        + "dur/60 energy/4 sensory/3",
                "recommend date/2099-01-01",
                "recommend adopt",
                "y",
                "view 1",
                "bye") + "\n";

        String output = run(input, new FailAfterStorage(dataDirectory, 1));

        assertFalse(output.contains("Recommendation adopted."),
                "a failed save must never show recommend adopt's success message");
        assertTrue(output.contains("[Error] Storage error:"));
        String viewSection = output.substring(output.lastIndexOf("Activity [1]"));
        assertTrue(viewSection.contains("Duration    : 60 min"));
        assertFalse(viewSection.contains("Adopted"),
                "the rolled-back flexible activity must remain unscheduled in memory");
    }

    @Test
    public void recommendAdopt_thenNonSchedulingEdit_preservesAdoptedPlacementAcrossRestart() throws Exception {
        // Bug A regression, end to end: add -> recommend -> adopt -> edit a non-scheduling field
        // -> save -> restart -> the adopted placement must still be there, both in memory
        // immediately after the edit and after a full reload from disk.
        String input = String.join("\n",
                "add n/Essay draft c/ACADEMIC date/2099-01-01 type/FLEXIBLE earliest/09:00 latest/14:00 "
                        + "dur/60 energy/4 sensory/3",
                "recommend date/2099-01-01",
                "recommend adopt",
                "y",
                "edit 1 note/Bring notes",
                "y",
                "view 1",
                "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));
        String restartOutput = run("view 1\nbye\n", new Storage(dataDirectory));

        String viewSection = output.substring(output.lastIndexOf("Activity [1]"));
        assertTrue(viewSection.contains("Adopted     : 09:00 -> 10:00"),
                "adopted placement must survive a non-scheduling edit in memory");
        assertTrue(viewSection.contains("Bring notes"));
        assertEquals(List.of("FLEXIBLE|1|Essay draft|ACADEMIC|2099-01-01|09:00|14:00|60|4|3|INCOMPLETE||"
                        + "Bring notes|09:00"),
                Files.readAllLines(dataDirectory.resolve("activities.txt")));
        assertTrue(restartOutput.contains("Adopted     : 09:00 -> 10:00"),
                "adopted placement must survive a save + restart reload");
    }

    @Test
    public void addFixed_overlappingAdoptedFlexible_isRejectedEndToEnd() throws Exception {
        // Bug C regression, end to end: once a flexible activity is adopted, a new fixed activity
        // request that overlaps its adopted slot must be rejected at add-time, not silently
        // accepted and only later flagged with [OVERLAP] in the timetable.
        String input = String.join("\n",
                "add n/Study c/ACADEMIC date/2099-01-01 type/FLEXIBLE earliest/09:00 latest/14:00 "
                        + "dur/60 energy/4 sensory/3",
                "recommend date/2099-01-01",
                "recommend adopt",
                "y",
                "add n/Meeting c/ACADEMIC date/2099-01-01 type/FIXED from/09:00 to/10:00 energy/2 sensory/2",
                "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));

        assertTrue(output.contains("This timing overlaps activity [1], Study (09:00 -> 10:00)."));
        assertEquals(List.of("FLEXIBLE|1|Study|ACADEMIC|2099-01-01|09:00|14:00|60|4|3|INCOMPLETE|||09:00"),
                Files.readAllLines(dataDirectory.resolve("activities.txt")));
    }

    @Test
    public void recommend_thenPreferenceChangeInvalidatesProposal_adoptRejectedNotLost() throws Exception {
        // Bug E regression: preference set/reset must not indiscriminately clear an active
        // recommendation proposal (the documented contract - User Guide S11/S12.4, guide
        // preference/recommend). The proposal must survive the preference change and be
        // re-validated at adopt time - previously ApplicationRunner cleared every proposal after
        // any successful mutation, including preference set, so "recommend adopt" always failed
        // with "No recommendation proposal is currently active" instead of ever reaching the
        // "no longer fits your current preferred daily start/end" revalidation message.
        String input = String.join("\n",
                "add n/Essay draft c/ACADEMIC date/2099-01-01 type/FLEXIBLE earliest/09:00 latest/14:00 "
                        + "dur/60 energy/4 sensory/3",
                "recommend date/2099-01-01",
                "preference set start/10:30",
                "y",
                "recommend adopt",
                "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));

        assertTrue(output.contains("Preference profile updated."));
        assertTrue(output.contains("no longer fits your current preferred daily start/end"),
                "the proposal must survive the preference change and be rejected by revalidation, "
                        + "not report \"no active proposal\"");
        assertFalse(output.contains("No recommendation proposal is currently active."));
        assertFalse(output.contains("Recommendation adopted."));
    }

    @Test
    public void recommend_thenPreferenceChangeStillFits_adoptStillSucceeds() throws Exception {
        // Companion case: a preference change that does NOT invalidate the proposal's placements
        // must still let adoption succeed normally afterward.
        String input = String.join("\n",
                "add n/Essay draft c/ACADEMIC date/2099-01-01 type/FLEXIBLE earliest/09:00 latest/14:00 "
                        + "dur/60 energy/4 sensory/3",
                "recommend date/2099-01-01",
                "preference set start/08:30",
                "y",
                "recommend adopt",
                "y",
                "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));

        assertTrue(output.contains("Preference profile updated."));
        assertTrue(output.contains("Recommendation adopted."));
    }

    @Test
    public void recommend_thenUnrelatedMutation_stillClearsProposal() throws Exception {
        // Regression guard: the fix is scoped to preference set/reset only - every other mutating
        // command must still clear an active proposal on success, exactly as before.
        String input = String.join("\n",
                "add n/Essay draft c/ACADEMIC date/2099-01-01 type/FLEXIBLE earliest/09:00 latest/14:00 "
                        + "dur/60 energy/4 sensory/3",
                "recommend date/2099-01-01",
                "add n/Other c/ACADEMIC date/2099-01-02 type/FIXED from/09:00 to/10:00 energy/1 sensory/1",
                "recommend adopt",
                "bye") + "\n";

        String output = run(input, new Storage(dataDirectory));

        assertTrue(output.contains("No recommendation proposal is currently active."));
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

    @Test
    public void mark_afterIdSpaceExhausted_stillSucceeds() throws Exception {
        // Regression test: CommandTransactionExecutor.Snapshot used to capture the pre-command
        // next-ID via the throwing getNextId(), so once the ID space was exhausted, every
        // mutating command's snapshot capture failed before execute() even started - including
        // ones, like mark, that never need to allocate a new ID at all.
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("activities.txt"),
                "FIXED|2147483647|Old task|OTHERS|2099-01-01|09:00|10:00|1|1|INCOMPLETE||\n");

        String output = run("mark 2147483647\nlist\nbye\n", new Storage(dataDirectory));

        assertFalse(output.contains("[Error]"), "an unrelated mutating command must not be "
                + "blocked by an exhausted activity-ID space");
        assertTrue(output.contains("Nice! Activity [2147483647] is now complete"));
        assertTrue(output.contains("[2147483647][X]"));
    }

    @Test
    public void deleteThenResetAll_afterIdSpaceExhaustedAndNoActivitiesRemain_stillOffersMenu() throws Exception {
        // Regression test for the same root cause as above, exercised through a second call site:
        // ResetCommand.hasAnythingToReset() used to call the throwing getNextId() too, so once
        // exhausted and every activity deleted (activity count back to 0, everything else at
        // defaults), the reset menu itself could never be shown - the one command that should be
        // able to recover the application could not run either.
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("activities.txt"),
                "FIXED|2147483647|Old task|OTHERS|2099-01-01|09:00|10:00|1|1|INCOMPLETE||\n");

        String output = run("delete 2147483647\ny\nreset all\n1\nbye\n", new Storage(dataDirectory));

        assertFalse(output.contains("[Error]"), "an unrelated mutating command must not be "
                + "blocked by an exhausted activity-ID space");
        assertTrue(output.contains("has been deleted"));
        assertTrue(output.contains("All user data has been reset."),
                "the reset menu must still be offered and actionable once the ID space is "
                        + "exhausted, even with zero activities currently stored");
    }

    /**
     * Returns a Supplier that returns {@code early} for the first {@code callsBeforeAdvance}
     * calls, then {@code late} forever after - simulating real time advancing partway through a
     * scripted, non-interactive run, e.g. between a command's initial dispatch-time validation
     * and a later, post-confirmation re-validation, without depending on actual wall-clock delay.
     */
    private static Supplier<LocalDateTime> advancingAfterCalls(int callsBeforeAdvance, LocalDateTime early,
            LocalDateTime late) {
        AtomicInteger callCount = new AtomicInteger(0);
        return () -> callCount.getAndIncrement() < callsBeforeAdvance ? early : late;
    }

    @Test
    public void recommendAdopt_placementElapsesDuringConfirmationWait_rejectsInsteadOfAdopting() throws Exception {
        // Regression test: RecommendCommandParser only checked staleness once, at dispatch time,
        // before the confirmation prompt was even shown. A user can take a real amount of time to
        // answer "Proceed with adoption? (y/n)"; if the proposed start time elapses during that
        // wait, adoption used to still silently proceed against the now-past placement.
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("activities.txt"),
                "FLEXIBLE|1|Essay draft|OTHERS|2026-08-10|12:00|12:10|5|1|1|INCOMPLETE|||\n");
        // now stays 10:00 for "recommend today"'s generation and "recommend adopt"'s own
        // dispatch-time check (both still before the 12:00 placement, so neither rejects it);
        // it then advances to 12:01 - after the placement's 12:00 start - for the fresh
        // post-confirmation check this fix adds.
        Supplier<LocalDateTime> nowSupplier = advancingAfterCalls(2,
                LocalDateTime.of(2026, 8, 10, 10, 0), LocalDateTime.of(2026, 8, 10, 12, 1));

        String output = run("recommend today\nrecommend adopt\ny\nview 1\nbye\n", new Storage(dataDirectory),
                nowSupplier);

        assertFalse(output.contains("Recommendation adopted."),
                "adoption must not succeed once the proposed placement has elapsed during confirmation");
        assertTrue(output.contains("[Error] Invalid input:") && output.contains("stale"));
        String viewSection = output.substring(output.lastIndexOf("Activity [1]"));
        assertFalse(viewSection.contains("Adopted"), "the activity must remain unscheduled");
    }

    @Test
    public void edit_startTimeElapsesDuringConfirmationWait_rejectsInsteadOfSaving() throws Exception {
        // Same root cause as above, for edit: EditCommandParser only checked "not in the past"
        // once, at dispatch time, before the "Save changes? (y/n)" prompt was shown.
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("activities.txt"),
                "FIXED|1|Old task|OTHERS|2026-08-10|09:00|10:00|1|1|INCOMPLETE||\n");
        // now stays 10:00 for edit's own dispatch-time check (before the newly-requested 12:01
        // start, so it is not rejected yet); it then advances to 12:02 - after that newly
        // requested start - for the fresh post-confirmation check this fix adds.
        Supplier<LocalDateTime> nowSupplier = advancingAfterCalls(1,
                LocalDateTime.of(2026, 8, 10, 10, 0), LocalDateTime.of(2026, 8, 10, 12, 2));

        String output = run("edit 1 from/12:01 to/12:30\ny\nview 1\nbye\n", new Storage(dataDirectory), nowSupplier);

        assertFalse(output.contains("Activity [1] has been updated."),
                "the edit must not succeed once its newly-requested start time has elapsed during confirmation");
        assertTrue(output.contains("[Error] Invalid input:") && output.contains("has passed"));
        String viewSection = output.substring(output.lastIndexOf("Activity [1]"));
        assertTrue(viewSection.contains("09:00"), "the original, unedited start time must remain in effect");
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

    /**
     * A real, filesystem-backed Storage whose saveAll() succeeds for the first
     * {@code successesAllowed} calls, then throws an unchecked {@link IllegalStateException} on
     * every call after that - simulating an unanticipated bug in the storage layer, as opposed to
     * {@link FailAfterStorage}'s documented checked {@link StorageException} failure mode.
     */
    private static final class RuntimeExceptionAfterStorage extends Storage {
        private int remainingSuccesses;

        private RuntimeExceptionAfterStorage(Path dataDirectory, int successesAllowed) {
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
            throw new IllegalStateException("simulated unexpected storage bug");
        }
    }

    /** A real, filesystem-backed Storage whose saveAll() throws an unchecked exception once, then succeeds. */
    private static final class RuntimeExceptionOnceThenSucceedStorage extends Storage {
        private boolean hasFailedOnce;

        private RuntimeExceptionOnceThenSucceedStorage(Path dataDirectory) {
            super(dataDirectory);
        }

        @Override
        public void saveAll(List<Activity> activities, List<Topic> topics, ActivityOrder order,
                PreferenceProfile preferences) throws StorageException {
            if (!hasFailedOnce) {
                hasFailedOnce = true;
                throw new IllegalStateException("simulated unexpected storage bug");
            }
            super.saveAll(activities, topics, order, preferences);
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
