package seedu.unienable.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.CompletionStatus;
import seedu.unienable.model.enums.ScheduleType;

class ActivityStorageTest {
    @TempDir
    Path tempDir;

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("activities.txt");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    public void saveThenLoad_roundTripsFixedAndFlexibleActivities() throws Exception {
        Path file = tempDir.resolve("activities.txt");
        FixedActivity fixed = new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", "Bring laptop");
        fixed.mark();
        FlexibleActivity flexible = new FlexibleActivity(13, "Finish assignment 1", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), LocalTime.of(18, 0), 90,
                EnergyRating.of(5), SensoryRating.of(2), "CG3207", null);

        new ActivityStorage().save(file, List.of(fixed, flexible));
        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(2, result.getRecords().size());

        Activity loadedFixed = result.getRecords().get(0);
        assertEquals(ScheduleType.FIXED, loadedFixed.getScheduleType());
        assertEquals(12, loadedFixed.getId());
        assertEquals("CG3207 lecture", loadedFixed.getDescription());
        assertEquals(ActivityCategory.ACADEMIC, loadedFixed.getCategory());
        assertEquals(CompletionStatus.COMPLETE, loadedFixed.getStatus());
        assertEquals("Bring laptop", loadedFixed.getNote());
        assertEquals(LocalTime.of(9, 0), ((FixedActivity) loadedFixed).getStartTime());
        assertEquals(LocalTime.of(11, 0), ((FixedActivity) loadedFixed).getEndTime());

        Activity loadedFlexible = result.getRecords().get(1);
        assertEquals(ScheduleType.FLEXIBLE, loadedFlexible.getScheduleType());
        assertEquals(CompletionStatus.INCOMPLETE, loadedFlexible.getStatus());
        assertNull(loadedFlexible.getNote());
        assertEquals(90, ((FlexibleActivity) loadedFlexible).getDurationMinutes());
    }

    @Test
    public void load_unknownRecordTag_recordsWarning() throws Exception {
        Path file = writeFile("BOGUS|12|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|CG3207|note");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Line 1"));
    }

    @Test
    public void load_invalidEnergyRating_recordsWarningAndKeepsOtherLines() throws Exception {
        Path file = writeFile(
                "FIXED|12|desc|ACADEMIC|2026-08-15|09:00|11:00|9|3|INCOMPLETE|CG3207|note",
                "FIXED|13|desc2|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|CG3207|note");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(1, result.getWarnings().size());
        assertEquals(1, result.getRecords().size());
        assertEquals(13, result.getRecords().get(0).getId());
    }

    @Test
    public void load_invalidDate_recordsWarning() throws Exception {
        Path file = writeFile("FIXED|12|desc|ACADEMIC|15-08-2026|09:00|11:00|4|3|INCOMPLETE|CG3207|note");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(1, result.getWarnings().size());
    }

    @Test
    public void load_missingOptionalTopicAndNotes_treatsAsNull() throws Exception {
        Path file = writeFile("FIXED|12|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        Activity activity = result.getRecords().get(0);
        assertNull(activity.getTopic());
        assertNull(activity.getNote());
    }

    @Test
    public void save_fieldContainingDelimiter_throwsStorageException() throws Exception {
        FixedActivity fixed = new FixedActivity(12, "Bad | description", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);
        Path file = tempDir.resolve("activities.txt");

        assertThrows(StorageException.class, () -> new ActivityStorage().save(file, List.of(fixed)));
    }

    @Test
    public void load_missingFile_throwsStorageException() {
        Path missing = tempDir.resolve("does-not-exist.txt");

        assertThrows(StorageException.class, () -> new ActivityStorage().load(missing));
    }
}
