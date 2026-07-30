package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.accessibility.classes.Connection;
import seedu.unienable.accessibility.classes.Facility;
import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.accessibility.enums.ShelterStatus;
import seedu.unienable.accessibility.enums.TraversalType;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class StorageTest {
    @TempDir
    Path tempDir;

    private void write(String fileName, String... lines) throws IOException {
        Files.write(tempDir.resolve(fileName), List.of(lines));
    }

    @Test
    public void saveThenLoadActivities_delegatesToActivitiesFile() throws Exception {
        write("activities.txt");
        Storage storage = new Storage(tempDir);
        FixedActivity fixed = new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);

        storage.saveActivities(List.of(fixed));
        LoadResult<Activity> result = storage.loadActivities();

        assertEquals(1, result.getRecords().size());
        assertEquals(12, result.getRecords().get(0).getId());
    }

    @Test
    public void saveThenLoadTopics_delegatesToTopicsFile() throws Exception {
        write("topics.txt");
        Storage storage = new Storage(tempDir);
        List<TopicStorage.TopicRecord> topics = List.of(
                new TopicStorage.TopicRecord(ActivityCategory.ACADEMIC, "CG3207"));

        storage.saveTopics(topics);
        LoadResult<TopicStorage.TopicRecord> result = storage.loadTopics();

        assertEquals(1, result.getRecords().size());
        assertEquals("CG3207", result.getRecords().get(0).getName());
    }

    @Test
    public void loadFacilities_delegatesToFacilitiesFile() throws Exception {
        write("facilities.txt", "FACILITY|F01|COM3|Engineering building");
        Storage storage = new Storage(tempDir);

        LoadResult<Facility> result = storage.loadFacilities();

        assertEquals(1, result.getRecords().size());
        assertEquals("COM3", result.getRecords().get(0).getName());
    }

    @Test
    public void loadConnections_delegatesToConnectionsFile() throws Exception {
        write("connections.txt", "CONNECTION|12|COM3|COM1|80|YES|SHELTERED_RAMP|YES");
        Storage storage = new Storage(tempDir);

        LoadResult<Connection> result = storage.loadConnections();

        assertEquals(1, result.getRecords().size());
        Connection connection = result.getRecords().get(0);
        assertEquals(AccessibilityStatus.YES, connection.getAccessibility());
        assertEquals(TraversalType.SHELTERED_RAMP, connection.getType());
        assertEquals(ShelterStatus.YES, connection.getShelter());
    }

    @Test
    public void copyDefaultAccessibilityDataIfMissing_createsBothFilesFromBundledDefaults() throws Exception {
        Storage storage = new Storage(tempDir);

        storage.copyDefaultAccessibilityDataIfMissing();

        assertTrue(Files.exists(tempDir.resolve("facilities.txt")));
        assertTrue(Files.exists(tempDir.resolve("connections.txt")));
        assertFalse(storage.loadFacilities().getRecords().isEmpty());
        assertFalse(storage.loadConnections().getRecords().isEmpty());
    }

    @Test
    public void copyDefaultAccessibilityDataIfMissing_existingFacilitiesFile_isNotOverwritten() throws Exception {
        write("facilities.txt", "FACILITY|F99|CUSTOM|A manually edited facility");
        Storage storage = new Storage(tempDir);

        storage.copyDefaultAccessibilityDataIfMissing();

        LoadResult<Facility> result = storage.loadFacilities();
        assertEquals(1, result.getRecords().size());
        assertEquals("CUSTOM", result.getRecords().get(0).getName());
    }
}
