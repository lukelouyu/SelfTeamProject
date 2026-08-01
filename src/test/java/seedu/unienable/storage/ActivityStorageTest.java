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
import seedu.unienable.model.classes.Topic;
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

    @Test
    public void load_zeroId_recordsWarning() throws Exception {
        Path file = writeFile("FIXED|0|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|CG3207|note");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("positive"));
    }

    @Test
    public void load_negativeId_recordsWarning() throws Exception {
        Path file = writeFile("FIXED|-1|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|CG3207|note");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(1, result.getWarnings().size());
    }

    @Test
    public void load_duplicateId_secondLineIsSkippedWithWarning() throws Exception {
        Path file = writeFile(
                "FIXED|12|First|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||",
                "FIXED|12|Second|ACADEMIC|2026-08-16|09:00|11:00|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(1, result.getRecords().size());
        assertEquals("First", result.getRecords().get(0).getDescription());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("duplicate activity id"));
    }

    @Test
    public void load_blankDescription_recordsWarning() throws Exception {
        Path file = writeFile("FIXED|12||ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("description"));
    }

    @Test
    public void load_fixedEndNotAfterStart_recordsWarning() throws Exception {
        Path file = writeFile("FIXED|12|desc|ACADEMIC|2026-08-15|11:00|09:00|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("end time must be later than start time"));
    }

    @Test
    public void load_flexibleLatestNotAfterEarliest_recordsWarning() throws Exception {
        Path file = writeFile("FLEXIBLE|12|desc|ACADEMIC|2026-08-15|18:00|10:00|60|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("latest end time must be after earliest start time"));
    }

    @Test
    public void load_flexibleDurationExceedsWindow_recordsWarning() throws Exception {
        Path file = writeFile("FLEXIBLE|12|desc|ACADEMIC|2026-08-15|10:00|11:00|500|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("60 min available"));
    }

    @Test
    public void load_flexibleNonPositiveDuration_recordsWarning() throws Exception {
        Path file = writeFile("FLEXIBLE|12|desc|ACADEMIC|2026-08-15|10:00|18:00|0|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("positive"));
    }

    @Test
    public void load_exactDuplicateActivity_secondLineIsSkippedWithWarning() throws Exception {
        Path file = writeFile(
                "FIXED|12|Same activity|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||",
                "FIXED|13|Same activity|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(1, result.getRecords().size());
        assertEquals(12, result.getRecords().get(0).getId());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("duplicates activity [12]"));
    }

    @Test
    public void load_overlappingFixedActivities_secondLineIsSkippedWithWarning() throws Exception {
        Path file = writeFile(
                "FIXED|12|Base|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||",
                "FIXED|13|Overlapping|ACADEMIC|2026-08-15|10:00|12:00|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(1, result.getRecords().size());
        assertEquals(12, result.getRecords().get(0).getId());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("overlaps activity [12]"));
    }

    @Test
    public void load_nonOverlappingFixedActivitiesOnSameDate_bothLoad() throws Exception {
        Path file = writeFile(
                "FIXED|12|Base|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||",
                "FIXED|13|Later|ACADEMIC|2026-08-15|11:00|12:00|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void load_fixedAndFlexibleWithSameTimingDoNotCountAsOverlap() throws Exception {
        // Overlap detection only applies between two FixedActivity records - a FlexibleActivity
        // has no confirmed placement yet, so it cannot conflict with a fixed activity's slot.
        Path file = writeFile(
                "FIXED|12|Base|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||",
                "FLEXIBLE|13|Flex|ACADEMIC|2026-08-15|09:00|11:00|60|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getWarnings().size());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void loadWithValidTopics_unknownTopicReference_recordsWarning() throws Exception {
        Path file = writeFile("FIXED|12|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|Ghost topic|");
        List<Topic> validTopics = List.of(new Topic(ActivityCategory.ACADEMIC, "CG3207"));

        LoadResult<Activity> result = new ActivityStorage().load(file, validTopics);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("topic"));
        assertTrue(result.getWarnings().get(0).contains("Ghost topic"));
    }

    @Test
    public void loadWithValidTopics_matchingTopic_loadsNormally() throws Exception {
        Path file = writeFile("FIXED|12|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|CG3207|");
        List<Topic> validTopics = List.of(new Topic(ActivityCategory.ACADEMIC, "CG3207"));

        LoadResult<Activity> result = new ActivityStorage().load(file, validTopics);

        assertEquals(1, result.getRecords().size());
        assertEquals(0, result.getWarnings().size());
    }

    @Test
    public void load_withValidTopics_sameNameUnderDifferentCategoryIsRejected() throws Exception {
        // A topic is scoped to its category, so a topic recorded under CCA does not validate an
        // activity's reference to a same-named topic under ACADEMIC.
        Path file = writeFile("FIXED|12|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|CG3207|");
        List<Topic> validTopics = List.of(new Topic(ActivityCategory.CCA, "CG3207"));

        LoadResult<Activity> result = new ActivityStorage().load(file, validTopics);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    public void loadWithValidTopics_noTopicOnActivity_neverTriggersCheck() throws Exception {
        Path file = writeFile("FIXED|12|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE||");

        LoadResult<Activity> result = new ActivityStorage().load(file, List.of());

        assertEquals(1, result.getRecords().size());
        assertEquals(0, result.getWarnings().size());
    }

    @Test
    public void load_withNullValidTopics_skipsTopicCrossCheckEntirely() throws Exception {
        // Passing null (as the single-argument load() overload does) must behave exactly like
        // the pre-existing single-file validation, with no topic cross-check at all.
        Path file = writeFile("FIXED|12|desc|ACADEMIC|2026-08-15|09:00|11:00|4|3|INCOMPLETE|Ghost topic|");

        LoadResult<Activity> result = new ActivityStorage().load(file, null);

        assertEquals(1, result.getRecords().size());
        assertEquals(0, result.getWarnings().size());
    }

    @Test
    public void load_fixedLineWithExtraColumn_recordsWarningInsteadOfSilentlyDiscardingIt() throws Exception {
        // Regression test for RC04 (v1.0 RC retest, 2026-08-01): a 13th FIXED column was
        // previously parsed successfully and silently discarded.
        Path file = writeFile(
                "FIXED|1|Lecture|ACADEMIC|2026-08-15|09:00|10:00|2|2|INCOMPLETE|Topic|Visible note|extra column");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    public void load_flexibleLineWithExtraColumn_recordsWarning() throws Exception {
        Path file = writeFile("FLEXIBLE|1|Task|ACADEMIC|2026-08-15|09:00|18:00|60|2|2|INCOMPLETE|Topic|Note|extra");

        LoadResult<Activity> result = new ActivityStorage().load(file);

        assertEquals(0, result.getRecords().size());
        assertEquals(1, result.getWarnings().size());
    }
}
