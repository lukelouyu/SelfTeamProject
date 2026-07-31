package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.model.enums.ActivityOrder;

class SettingsStorageTest {
    @TempDir
    Path tempDir;

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("settings.txt");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    public void saveThenLoad_roundTripsOrder() throws Exception {
        Path file = tempDir.resolve("settings.txt");

        new SettingsStorage().saveDefaultOrder(file, ActivityOrder.INPUT);
        LoadResult<ActivityOrder> result = new SettingsStorage().loadDefaultOrder(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(ActivityOrder.INPUT, result.getRecords().get(0));
    }

    @Test
    public void load_missingFile_returnsChronologicalDefaultWithNoWarning() throws Exception {
        Path missing = tempDir.resolve("does-not-exist.txt");

        LoadResult<ActivityOrder> result = new SettingsStorage().loadDefaultOrder(missing);

        assertEquals(0, result.getWarnings().size());
        assertEquals(ActivityOrder.CHRONOLOGICAL, result.getRecords().get(0));
    }

    @Test
    public void load_invalidOrderValue_recordsWarningAndFallsBackToDefault() throws Exception {
        Path file = writeFile("ORDER|BOGUS");

        LoadResult<ActivityOrder> result = new SettingsStorage().loadDefaultOrder(file);

        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Line 1"));
        assertEquals(ActivityOrder.CHRONOLOGICAL, result.getRecords().get(0));
    }

    @Test
    public void load_unknownRecordTag_recordsWarningAndFallsBackToDefault() throws Exception {
        Path file = writeFile("BOGUS|INPUT");

        LoadResult<ActivityOrder> result = new SettingsStorage().loadDefaultOrder(file);

        assertEquals(1, result.getWarnings().size());
        assertEquals(ActivityOrder.CHRONOLOGICAL, result.getRecords().get(0));
    }

    @Test
    public void load_blankFile_returnsDefaultWithNoWarning() throws Exception {
        Path file = writeFile("");

        LoadResult<ActivityOrder> result = new SettingsStorage().loadDefaultOrder(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(ActivityOrder.CHRONOLOGICAL, result.getRecords().get(0));
    }
}
