package seedu.unienable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.storage.Storage;

/**
 * End-to-end tests driving UniEnable.run() through a scripted input stream, against a temporary
 * data directory so the real project's data/ folder is never touched.
 */
class UniEnableTest {
    @TempDir
    Path tempDir;

    private String runWithInput(String input) {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));

        try {
            UniEnable.run(tempDir, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        } finally {
            System.setOut(originalOut);
        }
        return capturedOutput.toString();
    }

    private String runWithInputCapturingStderr(String input) {
        ByteArrayOutputStream capturedError = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        System.setErr(new PrintStream(capturedError));

        try {
            UniEnable.run(tempDir, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return capturedError.toString();
    }

    @Test
    public void run_dataDirectoryCannotBeCreated_showsStartupErrorAndNeverStartsCommandLoop() throws Exception {
        // Characterization test: a fatal storage failure during startup (here, the data
        // directory path is blocked by a regular file) must show a framed error and return
        // before the command loop starts at all - "bye" in the input must never be reached.
        Path blockingFile = tempDir.resolve("blocking-file");
        Files.writeString(blockingFile, "not a directory");
        Path invalidDataDirectory = blockingFile.resolve("data");

        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput));
        try {
            UniEnable.run(invalidDataDirectory,
                    new ByteArrayInputStream("list\nbye\n".getBytes(StandardCharsets.UTF_8)));
        } finally {
            System.setOut(originalOut);
        }

        String output = capturedOutput.toString();
        assertTrue(output.contains("[Error]"));
        assertTrue(!output.contains("No activities found."));
        assertTrue(!output.contains("Bye! Take care and see you again."));
    }

    @Test
    public void run_blankLineIsIgnored_doesNotErrorAndContinuesProcessing() {
        String output = runWithInput("\n   \nlist\nbye\n");

        assertTrue(!output.contains("[Error]"));
        assertTrue(output.contains("No activities found."));
        assertTrue(output.contains("Bye! Take care and see you again."));
    }

    @Test
    public void run_streamEndsWithoutBye_stopsCleanlyWithNoFarewellOrError() {
        String output = runWithInput("list\n");

        assertTrue(!output.contains("[Error]"));
        assertTrue(!output.contains("Bye! Take care and see you again."));
    }

    @Test
    public void run_deleteConfirmationInvalidAnswer_treatedAsCancel() throws Exception {
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "delete 1\n"
                        + "maybe\n"
                        + "bye\n");

        assertTrue(output.contains("Cancelled. No changes were made."));

        Storage storage = new Storage(tempDir);
        assertEquals(1, storage.loadActivities().getRecords().size());
    }

    @Test
    public void run_topicDeleteWithConfirmation_deletesOnY() {
        String output = runWithInput(
                "topic add c/ACADEMIC n/CS2113\n"
                        + "topic delete c/ACADEMIC n/CS2113\n"
                        + "y\n"
                        + "bye\n");

        assertTrue(output.contains("Delete topic \"CS2113\" under ACADEMIC? (y/n)"));
        assertTrue(output.contains("Topic CS2113 has been deleted."));
    }

    @Test
    public void run_activitiesPersistAcrossRestart() {
        runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "bye\n");

        String secondRunOutput = runWithInput("list\nbye\n");

        assertTrue(secondRunOutput.contains("CG3207 lecture"));
    }

    @Test
    public void run_topicsPersistAcrossRestart() {
        runWithInput("topic add c/ACADEMIC n/CS2113\nbye\n");

        String secondRunOutput = runWithInput("topic list c/ACADEMIC\nbye\n");

        assertTrue(secondRunOutput.contains("CS2113"));
    }

    @Test
    public void run_readOnlyCommands_neverCreateSettingsFile() {
        // Regression test: saveApplicationState() previously ran after every successfully
        // executed command, including read-only ones, so settings.txt was created the first
        // time the user ran *any* command rather than only when mutating state.
        runWithInput("list\nnext\nfacility list\nbye\n");

        assertTrue(!Files.exists(tempDir.resolve("settings.txt")));
    }

    @Test
    public void run_addSaveFailure_showsStorageErrorInsteadOfFalseSuccess() throws Exception {
        // Regression test: the success feedback ("Got it. Activity [n] has been added:...")
        // was previously shown before the save was attempted, so a save failure left a false
        // success message on screen with nothing actually persisted.
        Path activitiesFile = tempDir.resolve("activities.txt");
        Files.writeString(activitiesFile, "");
        assertTrue(activitiesFile.toFile().setWritable(false), "test setup: could not make file read-only");

        try {
            String output = runWithInput(
                    "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                            + "energy/4 sensory/3\n"
                            + "bye\n");

            assertTrue(!output.contains("Got it. Activity"));
            assertTrue(output.contains("[Error] Storage error"));
            assertTrue(!output.contains("Your data has been saved."));
            assertTrue(output.contains("Your latest changes could not be saved."));
            assertTrue(output.contains("Bye! Take care and see you again."));
        } finally {
            activitiesFile.toFile().setWritable(true);
        }
    }

    @Test
    public void run_byeStillTerminatesLoop_evenWhenFinalSaveFails() throws Exception {
        // Regression test: when the exit command's own save attempt failed, the exception
        // previously propagated out of the try block before "return !result.isShouldExit()"
        // was reached, so processCommand() fell into the catch clause and returned true
        // (keep looping) instead of ending the session - "list" below used to still execute
        // after "bye".
        Path activitiesFile = tempDir.resolve("activities.txt");
        Files.writeString(activitiesFile, "");
        assertTrue(activitiesFile.toFile().setWritable(false), "test setup: could not make file read-only");

        try {
            String output = runWithInput(
                    "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                            + "energy/4 sensory/3\n"
                            + "bye\n"
                            + "list\n");

            assertTrue(!output.contains("matching activit"));
            assertTrue(!output.contains("No activities found."));
        } finally {
            activitiesFile.toFile().setWritable(true);
        }
    }

    @Test
    public void run_addListViewDeleteWithConfirmation_worksEndToEndAndPersistsToDisk() throws Exception {
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "list\n"
                        + "view 1\n"
                        + "delete 1\n"
                        + "y\n"
                        + "bye\n");

        assertTrue(output.contains("Welcome to UniEnable"));
        assertTrue(output.contains("Got it. Activity [1] has been added:"));
        assertTrue(output.contains("CG3207 lecture"));
        assertTrue(output.contains("You selected activity [1]:"));
        assertTrue(output.contains("Delete this activity? (y/n)"));
        assertTrue(output.contains("Activity [1] has been deleted."));
        assertTrue(output.contains("Bye! Take care and see you again."));

        Storage storage = new Storage(tempDir);
        assertTrue(storage.loadActivities().getRecords().isEmpty());
    }

    @Test
    public void run_deleteCancelledWithN_keepsActivityOnDisk() throws Exception {
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "delete 1\n"
                        + "n\n"
                        + "bye\n");

        assertTrue(output.contains("Cancelled. No changes were made."));

        Storage storage = new Storage(tempDir);
        List<Activity> records = storage.loadActivities().getRecords();
        assertEquals(1, records.size());
        assertEquals("CG3207 lecture", records.get(0).getDescription());
    }

    @Test
    public void run_deleteAtEndOfInput_cancelsAndKeepsActivityOnDisk() throws Exception {
        // Regression coverage: HANDOVER.md claimed this EOF-during-confirmation path already had
        // a test, but no such test existed anywhere in the suite for any of the four
        // confirmation-requiring commands. The input stream ends right after "delete 1" with no
        // y/n line and no "bye"; confirm() must default to "n" via scanner.hasNextLine() rather
        // than throwing NoSuchElementException, and the run loop must then exit cleanly.
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "delete 1\n");

        assertTrue(output.contains("Delete this activity? (y/n)"));
        assertTrue(output.contains("Cancelled. No changes were made."));

        Storage storage = new Storage(tempDir);
        assertEquals(1, storage.loadActivities().getRecords().size());
    }

    @Test
    public void run_editAtEndOfInput_cancelsAndLeavesActivityUnchangedOnDisk() throws Exception {
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "edit 1 energy/5\n");

        assertTrue(output.contains("Save changes? (y/n)"));
        assertTrue(output.contains("Cancelled. No changes were made."));

        Storage storage = new Storage(tempDir);
        assertEquals(4, storage.loadActivities().getRecords().get(0).getEnergyRating().getValue());
    }

    @Test
    public void run_topicRenameAtEndOfInput_cancelsAndLeavesTopicUnchanged() {
        String output = runWithInput(
                "topic add c/ACADEMIC n/CS2113\n"
                        + "topic rename c/ACADEMIC old/CS2113 new/CS3207\n");

        assertTrue(output.contains("Save changes? (y/n)"));
        assertTrue(output.contains("Cancelled. No changes were made."));
        assertTrue(!output.contains("Topic renamed from CS2113 to CS3207."));
    }

    @Test
    public void run_topicDeleteAtEndOfInput_cancelsAndLeavesTopicInPlace() {
        String output = runWithInput(
                "topic add c/ACADEMIC n/CS2113\n"
                        + "topic delete c/ACADEMIC n/CS2113\n");

        assertTrue(output.contains("Delete topic \"CS2113\" under ACADEMIC? (y/n)"));
        assertTrue(output.contains("Cancelled. No changes were made."));
        assertTrue(!output.contains("Topic CS2113 has been deleted."));
    }

    @Test
    public void run_malformedActivitiesFile_surfacesPartialLoadWarningAndKeepsValidRecords() throws Exception {
        // End-to-end coverage: the "[Warning] Partial data loaded" path was previously only
        // tested at the *Storage.load() unit level, never verified to actually reach the user
        // through the real startup sequence in UniEnable.run().
        Files.writeString(tempDir.resolve("activities.txt"),
                "FIXED|1|Valid lecture|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||\n"
                        + "FIXED|2|Bad energy|ACADEMIC|2026-08-15|09:00|11:00|99|3|INCOMPLETE||\n");

        String output = runWithInput("list\nbye\n");

        assertTrue(output.contains("[Warning] Partial data loaded: activities.txt"));
        assertTrue(output.contains("Line 2 was skipped"));
        assertTrue(output.contains("Valid lecture"));
        assertTrue(!output.contains("Bad energy"));
    }

    @Test
    public void run_malformedFacilitiesFile_surfacesPartialLoadWarningAndKeepsValidRecords() throws Exception {
        Files.writeString(tempDir.resolve("facilities.txt"),
                "FACILITY|F01|COM3|Engineering building\n"
                        + "FEATURE|F01|LIFT|YES|Level 1 lobby\n"
                        + "FEATURE|F01|BOGUS_TYPE|YES|Bad feature\n");
        Files.writeString(tempDir.resolve("connections.txt"), "");

        String output = runWithInput("facility view COM3\nbye\n");

        assertTrue(output.contains("[Warning] Partial data loaded: facilities.txt"));
        assertTrue(output.contains("Line 3 was skipped"));
        assertTrue(output.contains("LIFT"));
    }

    @Test
    public void run_orderSetPersistsAcrossRestart() {
        // Regression test: ActivityManager.defaultOrder was memory-only and always reset to
        // CHRONOLOGICAL on restart, even though OrderSetCommand's feedback and the User Guide
        // both call it the "saved default". Two separate UniEnable.run() calls against the same
        // tempDir simulate a restart.
        runWithInput(
                "add n/LaterDateActivity c/ACADEMIC date/2026-08-20 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2\n"
                        + "add n/EarlierDateActivity c/ACADEMIC date/2026-08-19 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2\n"
                        + "order set input\n"
                        + "bye\n");

        String secondRunOutput = runWithInput("order view\nlist\nbye\n");

        assertTrue(secondRunOutput.contains("Saved default activity order: input"));
        int indexOfLater = secondRunOutput.indexOf("LaterDateActivity");
        int indexOfEarlier = secondRunOutput.indexOf("EarlierDateActivity");
        assertTrue(indexOfLater >= 0 && indexOfEarlier >= 0 && indexOfLater < indexOfEarlier);
    }

    @Test
    public void run_noSettingsFile_defaultsToChronologicalOrder() {
        String output = runWithInput("order view\nbye\n");

        assertTrue(output.contains("Saved default activity order: chronological"));
    }

    @Test
    public void run_malformedSettingsFile_surfacesWarningAndFallsBackToDefault() throws Exception {
        Files.writeString(tempDir.resolve("settings.txt"), "ORDER|BOGUS\n");

        String output = runWithInput("order view\nbye\n");

        assertTrue(output.contains("[Warning] Partial data loaded: settings.txt"));
        assertTrue(output.contains("Line 1 was skipped"));
        assertTrue(output.contains("Saved default activity order: chronological"));
    }

    @Test
    public void run_orderOverride_doesNotChangePersistedDefault() {
        // A one-shot "order/" override on list/find must not change the saved default.
        runWithInput(
                "add n/LaterDateActivity c/ACADEMIC date/2026-08-20 type/FIXED from/09:00 to/10:00 "
                        + "energy/2 sensory/2\n"
                        + "list order/input\n"
                        + "bye\n");

        String secondRunOutput = runWithInput("order view\nbye\n");

        assertTrue(secondRunOutput.contains("Saved default activity order: chronological"));
    }

    @Test
    public void run_unknownCommand_showsErrorAndKeepsProcessingSubsequentCommands() {
        String output = runWithInput("banana\nbye\n");

        assertTrue(output.contains("[Error] Invalid input: Unknown command \"banana\""));
        assertTrue(output.contains("Bye! Take care and see you again."));
    }

    @Test
    public void run_freshDataDirectory_loadsBundledFacilityDefaults() {
        String output = runWithInput("facility list\nbye\n");

        assertTrue(output.contains("Known facilities in the local reference:"));
        assertTrue(output.contains("AS1"));
    }

    @Test
    public void run_editWithConfirmation_showsBeforeAfterDiffAndSavesOnY() throws Exception {
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "edit 1 energy/5\n"
                        + "y\n"
                        + "bye\n");

        assertTrue(output.contains("Before: energy = 4/5"));
        assertTrue(output.contains("After : energy = 5/5"));
        assertTrue(output.contains("Save changes? (y/n)"));
        assertTrue(output.contains("Activity [1] has been updated."));

        Storage storage = new Storage(tempDir);
        Activity activity = storage.loadActivities().getRecords().get(0);
        assertEquals(5, activity.getEnergyRating().getValue());
    }

    @Test
    public void run_editCancelledWithN_leavesActivityUnchangedOnDisk() throws Exception {
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "edit 1 energy/5\n"
                        + "n\n"
                        + "bye\n");

        assertTrue(output.contains("Cancelled. No changes were made."));

        Storage storage = new Storage(tempDir);
        Activity activity = storage.loadActivities().getRecords().get(0);
        assertEquals(4, activity.getEnergyRating().getValue());
    }

    @Test
    public void run_editWithNoActualChange_skipsConfirmationAndReportsNoChanges() throws Exception {
        String output = runWithInput(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "edit 1 energy/4\n"
                        + "bye\n");

        assertTrue(output.contains("No changes to activity [1]."));
        assertTrue(!output.contains("Save changes? (y/n)"));
    }

    @Test
    public void run_editWouldOverlapAnotherActivity_rejectsBeforePromptAndKeepsSubsequentLineAsCommand()
            throws Exception {
        // Regression test: overlap validation previously only ran inside
        // ActivityManager.replace(), called from EditCommand.execute() - after the confirmation
        // prompt was already shown and answered. The next line of scripted input ("y", meant to
        // answer that prompt) was silently consumed as the confirmation answer even though the
        // edit was about to fail anyway. Now the conflict is caught during parsing, before
        // dispatch() ever returns a command to confirm, so "y" falls through as its own (unknown)
        // top-level command instead of being consumed.
        String output = runWithInput(
                "add n/Base c/ACADEMIC date/2026-08-20 type/FIXED from/10:00 to/12:00 energy/2 sensory/2\n"
                        + "add n/Later c/ACADEMIC date/2026-08-20 type/FIXED from/13:00 to/14:00 "
                        + "energy/2 sensory/2\n"
                        + "edit 2 from/10:30 to/11:30\n"
                        + "y\n"
                        + "bye\n");

        assertTrue(!output.contains("Save changes? (y/n)"));
        assertTrue(output.contains("[Error] Conflict: This timing overlaps activity [1], Base (10:00 -> 12:00)."));
        assertTrue(output.contains("[Error] Invalid input: Unknown command \"y\""));

        Storage storage = new Storage(tempDir);
        Activity unchanged = storage.loadActivities().getRecords().get(1);
        assertEquals(LocalTime.of(13, 0), ((FixedActivity) unchanged).getStartTime());
    }

    @Test
    public void run_topicRenameWithConfirmation_showsBeforeAfterDiffAndSavesOnY() {
        String output = runWithInput(
                "topic add c/ACADEMIC n/CS2113\n"
                        + "topic rename c/ACADEMIC old/CS2113 new/CS3207\n"
                        + "y\n"
                        + "bye\n");

        assertTrue(output.contains("Before: topic = CS2113"));
        assertTrue(output.contains("After : topic = CS3207"));
        assertTrue(output.contains("Save changes? (y/n)"));
        assertTrue(output.contains("Topic renamed from CS2113 to CS3207."));
    }

    @Test
    public void run_topicRenameAndActivityEdits_produceNoStrayLoggingOnStderr() {
        // Regression test: Topic.setName()/Activity.setTopic() etc. log at INFO for internal
        // diagnostics; the JVM's default console handler used to print those records straight to
        // stderr, interleaving a timestamped "INFO: Renaming topic..." line into the middle of
        // the framed CLI output during real interactive use.
        String stderr = runWithInputCapturingStderr(
                "add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED from/09:00 to/11:00 "
                        + "energy/4 sensory/3\n"
                        + "topic add c/ACADEMIC n/CS2113\n"
                        + "edit 1 topic/CS2113\n"
                        + "y\n"
                        + "topic rename c/ACADEMIC old/CS2113 new/CS3207\n"
                        + "y\n"
                        + "bye\n");

        assertTrue(stderr.isEmpty());
    }
}
