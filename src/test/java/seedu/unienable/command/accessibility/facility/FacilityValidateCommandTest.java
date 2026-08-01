package seedu.unienable.command.accessibility.facility;

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

class FacilityValidateCommandTest {
    @TempDir
    Path tempDir;

    private void write(String fileName, String... lines) throws IOException {
        Files.write(tempDir.resolve(fileName), List.of(lines));
    }

    @Test
    public void execute_wellFormedFile_reportsNoIssues() throws Exception {
        write("facilities.txt", "FACILITY|F01|AS1|Faculty block");
        Storage storage = new Storage(tempDir);

        CommandResult result = new FacilityValidateCommand(storage).execute();

        assertEquals("facilities.txt: no issues found.", result.getFeedback());
    }

    @Test
    public void execute_malformedLine_reportsLineNumberAndReason() throws Exception {
        write("facilities.txt",
                "FACILITY|F01|AS1|Faculty block",
                "FACILITY||AS2|Missing id");
        Storage storage = new Storage(tempDir);

        CommandResult result = new FacilityValidateCommand(storage).execute();

        String feedback = result.getFeedback();
        assertTrue(feedback.startsWith("facilities.txt: 1 issue found:"));
        assertTrue(feedback.contains("Line 2 was skipped"));
    }

    @Test
    public void execute_multipleMalformedLines_countsEachIssue() throws Exception {
        write("facilities.txt",
                "FACILITY||AS1|Missing id",
                "FACILITY|F02||Missing name");
        Storage storage = new Storage(tempDir);

        CommandResult result = new FacilityValidateCommand(storage).execute();

        assertTrue(result.getFeedback().startsWith("facilities.txt: 2 issues found:"));
    }

    @Test
    public void execute_doesNotModifyTheFileOnDisk() throws Exception {
        write("facilities.txt", "FACILITY||AS1|Missing id");
        Storage storage = new Storage(tempDir);
        String before = Files.readString(tempDir.resolve("facilities.txt"));

        new FacilityValidateCommand(storage).execute();

        assertEquals(before, Files.readString(tempDir.resolve("facilities.txt")));
    }
}
