package seedu.unienable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.model.classes.Activity;
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
