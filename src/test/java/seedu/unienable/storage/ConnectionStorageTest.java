package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.exception.StorageException;

class ConnectionStorageTest {
    @TempDir
    Path tempDir;

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("connections.txt");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    public void load_validFile_parsesConnection() throws Exception {
        Path file = writeFile("CONNECTION|12|COM3|COM1|80|YES|SHELTERED_RAMP|YES||Gentle slope");

        LoadResult<Connection> result = new ConnectionStorage().load(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(1, result.getRecords().size());
        Connection connection = result.getRecords().get(0);
        assertEquals(12, connection.getId());
        assertEquals("COM3", connection.getFrom());
        assertEquals("COM1", connection.getTo());
        assertEquals(80, connection.getDistanceInMetres());
        assertEquals(AccessibilityStatus.YES, connection.getAccessibility());
        assertEquals(TraversalType.SHELTERED_RAMP, connection.getType());
        assertEquals(ShelterStatus.YES, connection.getShelter());
        assertNull(connection.getKnownBarrier());
        assertEquals("Gentle slope", connection.getNotes());
    }

    @Test
    public void load_withoutOptionalFields_treatsAsNull() throws Exception {
        Path file = writeFile("CONNECTION|18|AS2|AS3|60|NO|PATH|NO");

        LoadResult<Connection> result = new ConnectionStorage().load(file);

        Connection connection = result.getRecords().get(0);
        assertNull(connection.getKnownBarrier());
        assertNull(connection.getNotes());
    }

    @Test
    public void load_unknownRecordTag_recordsWarning() throws Exception {
        Path file = writeFile("BOGUS|12|COM3|COM1|80|YES|RAMP|YES");

        LoadResult<Connection> result = new ConnectionStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Line 1"));
    }

    @Test
    public void load_nonNumericDistance_recordsWarning() throws Exception {
        Path file = writeFile("CONNECTION|12|COM3|COM1|far|YES|RAMP|YES");

        LoadResult<Connection> result = new ConnectionStorage().load(file);

        assertEquals(1, result.getWarnings().size());
    }

    @Test
    public void load_invalidEnumValue_recordsWarningAndKeepsOtherLines() throws Exception {
        Path file = writeFile(
                "CONNECTION|12|COM3|COM1|80|MAYBE|RAMP|YES",
                "CONNECTION|13|COM1|AS6|100|YES|RAMP|YES");

        LoadResult<Connection> result = new ConnectionStorage().load(file);

        assertEquals(1, result.getWarnings().size());
        assertEquals(1, result.getRecords().size());
        assertEquals(13, result.getRecords().get(0).getId());
    }

    @Test
    public void load_missingFile_throwsStorageException() {
        Path missing = tempDir.resolve("does-not-exist.txt");

        assertThrows(StorageException.class, () -> new ConnectionStorage().load(missing));
    }
}
