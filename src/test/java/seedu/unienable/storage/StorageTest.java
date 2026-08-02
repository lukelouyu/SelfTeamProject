package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;

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
        // Storage's public API works with the Topic domain class directly; the TopicRecord
        // conversion is an internal detail (see TopicStorageTest for record-level coverage).
        write("topics.txt");
        Storage storage = new Storage(tempDir);
        List<Topic> topics = List.of(new Topic(ActivityCategory.ACADEMIC, "CG3207"));

        storage.saveTopics(topics);
        LoadResult<Topic> result = storage.loadTopics();

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

    @Test
    public void prepareDataFiles_freshDataDirectory_createsAllFourFilesAndAllLoadSuccessfully() throws Exception {
        Storage storage = new Storage(tempDir);

        storage.prepareDataFiles();

        assertTrue(Files.exists(tempDir.resolve("activities.txt")));
        assertTrue(Files.exists(tempDir.resolve("topics.txt")));
        assertTrue(storage.loadActivities().getRecords().isEmpty());
        assertTrue(storage.loadTopics().getRecords().isEmpty());
        assertFalse(storage.loadFacilities().getRecords().isEmpty());
        assertFalse(storage.loadConnections().getRecords().isEmpty());
    }

    @Test
    public void prepareDataFiles_existingActivitiesFile_isNotOverwritten() throws Exception {
        Storage storage = new Storage(tempDir);
        FixedActivity fixed = new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);
        storage.saveActivities(List.of(fixed));

        storage.prepareDataFiles();

        assertEquals(1, storage.loadActivities().getRecords().size());
    }

    @Test
    public void saveAll_normalCase_savesAllFourUserStateFilesTogether() throws Exception {
        Storage storage = new Storage(tempDir);
        FixedActivity fixed = new FixedActivity(1, "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);
        List<Topic> topics = List.of(new Topic(ActivityCategory.ACADEMIC, "CG3207"));

        PreferenceProfile preferences = PreferenceProfile.of(LocalTime.of(7, 30),
                LocalTime.of(21, 0), 25, TomatoSuggestion.ON);

        storage.saveAll(List.of(fixed), topics, ActivityOrder.INPUT, preferences);

        assertEquals(1, storage.loadActivities().getRecords().size());
        assertEquals(1, storage.loadTopics().getRecords().size());
        assertEquals(ActivityOrder.INPUT, storage.loadSettings().getRecords().get(0));
        assertEquals(preferences, storage.loadPreferences().getRecords().get(0));
    }

    @Test
    public void saveAll_preferencesCommitFails_earlierFilesAreRolledBack() throws Exception {
        write("activities.txt", "FIXED|1|Old activity|ACADEMIC|2026-08-15|09:00|10:00|2|2|INCOMPLETE||");
        write("topics.txt");
        write("settings.txt", "DEFAULT_ORDER|CHRONOLOGICAL");
        Path preferencesPath = tempDir.resolve("preferences.txt");
        Files.createDirectory(preferencesPath);
        Files.writeString(preferencesPath.resolve("blocker.txt"), "x");
        Storage storage = new Storage(tempDir);
        FixedActivity newActivity = new FixedActivity(2, "New activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);
        PreferenceProfile proposed = PreferenceProfile.of(LocalTime.of(7, 0),
                LocalTime.of(19, 0), 30, TomatoSuggestion.ON);

        assertThrows(StorageException.class,
                () -> storage.saveAll(List.of(newActivity), List.of(), ActivityOrder.INPUT, proposed));

        List<Activity> activitiesOnDisk = storage.loadActivities().getRecords();
        assertEquals(1, activitiesOnDisk.size());
        assertEquals("Old activity", activitiesOnDisk.get(0).getDescription());
        assertEquals(ActivityOrder.CHRONOLOGICAL, storage.loadSettings().getRecords().get(0));
        assertTrue(Files.isDirectory(preferencesPath));
        assertTrue(Files.exists(preferencesPath.resolve("blocker.txt")));
        assertFalse(Files.exists(tempDir.resolve("activities.txt.tmp")));
        assertFalse(Files.exists(tempDir.resolve("activities.txt.bak")));
        assertFalse(Files.exists(tempDir.resolve("preferences.txt.tmp")));
        assertFalse(Files.exists(tempDir.resolve("preferences.txt.bak")));
    }

    @Test
    public void saveAll_laterFileCommitFails_earlierCommittedFileIsRolledBack() throws Exception {
        // Regression test for RC01 (v1.0 RC retest, 2026-08-01): saveAll() previously moved each
        // temporary file into place sequentially with no way to undo an earlier successful move
        // if a later one failed. Reproducing the exact repro from that report: topics.txt is not
        // a plain file (here, a directory) at commit time, so Files.isWritable() passes the
        // upfront check but the actual move still fails - after activities.txt has already been
        // replaced.
        write("activities.txt", "FIXED|1|Old activity|ACADEMIC|2026-08-15|09:00|10:00|2|2|INCOMPLETE||");
        Files.createDirectory(tempDir.resolve("topics.txt"));
        Files.writeString(tempDir.resolve("topics.txt").resolve("blocker.txt"), "x");
        Storage storage = new Storage(tempDir);
        FixedActivity newActivity = new FixedActivity(2, "New activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);

        assertThrows(StorageException.class,
                () -> storage.saveAll(List.of(newActivity), List.of(), ActivityOrder.INPUT));

        List<Activity> activitiesOnDisk = storage.loadActivities().getRecords();
        assertEquals(1, activitiesOnDisk.size());
        assertEquals("Old activity", activitiesOnDisk.get(0).getDescription());
        assertFalse(Files.exists(tempDir.resolve("settings.txt")));
        assertFalse(Files.exists(tempDir.resolve("activities.txt.tmp")));
        assertFalse(Files.exists(tempDir.resolve("activities.txt.bak")));
    }

    @Test
    public void saveAll_failureOnFreshDirectory_leavesNoNewFilesBehind() throws Exception {
        // Same failure as above, but activities.txt did not exist before this call - the rollback
        // must delete the file it created, not just restore stale content, so the directory ends
        // up exactly as it started.
        Files.createDirectory(tempDir.resolve("topics.txt"));
        Files.writeString(tempDir.resolve("topics.txt").resolve("blocker.txt"), "x");
        Storage storage = new Storage(tempDir);
        FixedActivity newActivity = new FixedActivity(1, "New activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);

        assertThrows(StorageException.class,
                () -> storage.saveAll(List.of(newActivity), List.of(), ActivityOrder.INPUT));

        assertFalse(Files.exists(tempDir.resolve("activities.txt")));
        assertFalse(Files.exists(tempDir.resolve("settings.txt")));
        assertFalse(Files.exists(tempDir.resolve("preferences.txt")));
        assertFalse(Files.exists(tempDir.resolve("preferences.txt.tmp")));
    }

    @Test
    public void saveAll_readOnlyDestination_rejectedBeforeAnyFileIsTouched() throws Exception {
        write("activities.txt", "FIXED|1|Old activity|ACADEMIC|2026-08-15|09:00|10:00|2|2|INCOMPLETE||");
        write("topics.txt");
        Path topicsFile = tempDir.resolve("topics.txt");
        assertTrue(topicsFile.toFile().setWritable(false), "test setup: could not make file read-only");
        Storage storage = new Storage(tempDir);
        FixedActivity newActivity = new FixedActivity(2, "New activity", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);

        try {
            assertThrows(StorageException.class,
                    () -> storage.saveAll(List.of(newActivity), List.of(), ActivityOrder.INPUT));

            List<Activity> activitiesOnDisk = storage.loadActivities().getRecords();
            assertEquals(1, activitiesOnDisk.size());
            assertEquals("Old activity", activitiesOnDisk.get(0).getDescription());
            assertFalse(Files.exists(tempDir.resolve("settings.txt")));
        } finally {
            topicsFile.toFile().setWritable(true);
        }
    }
}
