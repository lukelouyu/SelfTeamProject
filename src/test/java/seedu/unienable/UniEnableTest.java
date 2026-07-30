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
}
