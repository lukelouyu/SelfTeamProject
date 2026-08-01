package seedu.unienable.command.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.command.CommandResult;
import seedu.unienable.storage.Storage;

class ConnectionValidateCommandTest {
    @TempDir
    Path tempDir;

    private void write(String fileName, String... lines) throws IOException {
        Files.write(tempDir.resolve(fileName), List.of(lines));
    }

    @Test
    public void execute_wellFormedFile_reportsNoIssues() throws Exception {
        write("facilities.txt",
                "FACILITY|F01|AS1|Block 1",
                "FACILITY|F02|AS2|Block 2");
        write("connections.txt", "CONNECTION|1|AS1|AS2|50|YES|PATH|YES");
        Storage storage = new Storage(tempDir);

        CommandResult result = new ConnectionValidateCommand(storage).execute();

        assertEquals("connections.txt: no issues found.", result.getFeedback());
    }

    @Test
    public void execute_endpointNotInFacilitiesFile_reportsLineNumberAndReason() throws Exception {
        write("facilities.txt", "FACILITY|F01|AS1|Block 1");
        write("connections.txt", "CONNECTION|1|AS1|UNKNOWN_BLOCK|50|YES|PATH|YES");
        Storage storage = new Storage(tempDir);

        CommandResult result = new ConnectionValidateCommand(storage).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.startsWith("connections.txt: 1 issue found:"));
        assertTrue(feedback.contains("Line 1 was skipped"));
        assertTrue(feedback.contains("not a known facility"));
    }

    @Test
    public void execute_doesNotModifyEitherFileOnDisk() throws Exception {
        write("facilities.txt", "FACILITY|F01|AS1|Block 1");
        write("connections.txt", "CONNECTION|1|AS1|UNKNOWN_BLOCK|50|YES|PATH|YES");
        Storage storage = new Storage(tempDir);
        String facilitiesBefore = Files.readString(tempDir.resolve("facilities.txt"));
        String connectionsBefore = Files.readString(tempDir.resolve("connections.txt"));

        new ConnectionValidateCommand(storage).execute();

        assertEquals(facilitiesBefore, Files.readString(tempDir.resolve("facilities.txt")));
        assertEquals(connectionsBefore, Files.readString(tempDir.resolve("connections.txt")));
    }
}
