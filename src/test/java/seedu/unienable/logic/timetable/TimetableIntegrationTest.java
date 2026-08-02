package seedu.unienable.logic.timetable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.app.ApplicationRunner;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.timetable.TimetableView;
import seedu.unienable.storage.LoadResult;
import seedu.unienable.storage.Storage;

class TimetableIntegrationTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

    @TempDir
    Path dataDirectory;

    @Test
    public void storageManagerService_fixedAndFlexibleRemainSeparated() throws Exception {
        Storage storage = new Storage(dataDirectory);
        storage.prepareDataFiles();
        storage.saveActivities(List.of(fixed(), flexible()));
        LoadResult<Activity> loaded = storage.loadActivities();
        ActivityManager manager = new ActivityManager();
        manager.loadAll(loaded.getRecords());

        TimetableView view = TimetableService.build(manager, TimetableService.resolveWeek(MONDAY));

        assertEquals(1, view.getFixedEntries().size());
        assertEquals(1, view.getUnscheduledFlexibleEntries().size());
    }

    @Test
    public void applicationRunner_timetableCommandsNeverChangeDataFiles() throws Exception {
        Storage storage = new Storage(dataDirectory);
        storage.prepareDataFiles();
        storage.saveActivities(List.of(fixed(), flexible()));
        Path activitiesFile = dataDirectory.resolve("activities.txt");
        Path topicsFile = dataDirectory.resolve("topics.txt");
        byte[] activitiesBefore = Files.readAllBytes(activitiesFile);
        byte[] topicsBefore = Files.readAllBytes(topicsFile);
        String input = "timetable week/2026-08-17\n"
                + "timetable week/2026-08-17 compact\n"
                + "timetable day/2026-08-17 detail\nbye\n";

        String output = runApplication(input);

        assertFalse(output.contains("[Error]"));
        assertTrue(output.contains("Weekly Timetable"));
        assertTrue(output.contains("Daily Timetable"));
        assertArrayEquals(activitiesBefore, Files.readAllBytes(activitiesFile));
        assertArrayEquals(topicsBefore, Files.readAllBytes(topicsFile));
    }

    @Test
    public void restart_sameStoredActivities_producesSameExplicitDateOutput() throws Exception {
        Storage storage = new Storage(dataDirectory);
        storage.prepareDataFiles();
        storage.saveActivities(List.of(fixed(), flexible()));

        String first = runApplication("timetable week/2026-08-17 detail\nbye\n");
        String second = runApplication("timetable week/2026-08-17 detail\nbye\n");

        assertEquals(first, second);
    }

    private String runApplication(String input) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new ApplicationRunner(dataDirectory,
                    new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))).run();
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private FixedActivity fixed() throws Exception {
        return new FixedActivity(1, "Lecture", ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), EnergyRating.of(2),
                SensoryRating.of(3), null, null);
    }

    private FlexibleActivity flexible() throws Exception {
        return new FlexibleActivity(2, "Study", ActivityCategory.OTHERS, MONDAY,
                LocalTime.of(13, 0), LocalTime.of(17, 0), 90, EnergyRating.of(3),
                SensoryRating.of(4), null, null);
    }
}
