package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.enums.ActivityCategory;

class TopicStorageTest {
    @TempDir
    Path tempDir;

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("topics.txt");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    public void saveThenLoad_roundTripsTopicRecords() throws Exception {
        Path file = tempDir.resolve("topics.txt");
        List<TopicStorage.TopicRecord> topics = List.of(
                new TopicStorage.TopicRecord(ActivityCategory.ACADEMIC, "CG3207"),
                new TopicStorage.TopicRecord(ActivityCategory.CCA, "Computing Club"));

        new TopicStorage().save(file, topics);
        LoadResult<TopicStorage.TopicRecord> result = new TopicStorage().load(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(2, result.getRecords().size());
        assertEquals(ActivityCategory.ACADEMIC, result.getRecords().get(0).getCategory());
        assertEquals("CG3207", result.getRecords().get(0).getName());
        assertEquals(ActivityCategory.CCA, result.getRecords().get(1).getCategory());
        assertEquals("Computing Club", result.getRecords().get(1).getName());
    }

    @Test
    public void load_unknownRecordTag_recordsWarning() throws Exception {
        Path file = writeFile("BOGUS|ACADEMIC|CG3207");

        LoadResult<TopicStorage.TopicRecord> result = new TopicStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Line 1"));
    }

    @Test
    public void load_invalidCategory_recordsWarningAndKeepsOtherLines() throws Exception {
        Path file = writeFile(
                "TOPIC|NOT_A_CATEGORY|CG3207",
                "TOPIC|ACADEMIC|CS2113");

        LoadResult<TopicStorage.TopicRecord> result = new TopicStorage().load(file);

        assertEquals(1, result.getWarnings().size());
        assertEquals(1, result.getRecords().size());
        assertEquals("CS2113", result.getRecords().get(0).getName());
    }

    @Test
    public void save_nameContainingDelimiter_throwsStorageException() {
        List<TopicStorage.TopicRecord> topics = List.of(
                new TopicStorage.TopicRecord(ActivityCategory.ACADEMIC, "Bad | name"));
        Path file = tempDir.resolve("topics.txt");

        assertThrows(StorageException.class, () -> new TopicStorage().save(file, topics));
    }

    @Test
    public void load_missingFile_throwsStorageException() {
        Path missing = tempDir.resolve("does-not-exist.txt");

        assertThrows(StorageException.class, () -> new TopicStorage().load(missing));
    }
}
