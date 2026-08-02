package seedu.unienable.logic.dashboard;

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
import java.time.LocalDateTime;
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
import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.dashboard.DashboardSummary;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.storage.LoadResult;
import seedu.unienable.storage.Storage;
import seedu.unienable.ui.dashboard.DashboardFormatter;

/**
 * Proves {@code dashboard} reads through the real storage/manager path correctly, produces
 * identical output across a simulated restart, safely skips a malformed persisted line, and never
 * writes any file - all against a temp data directory, never the repository's own {@code data/}.
 */
class DashboardIntegrationTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

    @TempDir
    Path dataDirectory;

    private static FixedActivity fixed(int id, LocalDate date, LocalTime start, LocalTime end) throws Exception {
        return new FixedActivity(id, "Lecture " + id, ActivityCategory.ACADEMIC, date, start, end,
                EnergyRating.of(2), SensoryRating.of(2), null, null);
    }

    private static FlexibleActivity flexible(int id, LocalDate date, LocalTime earliest, LocalTime latest,
            int durationMinutes) throws Exception {
        return new FlexibleActivity(id, "Task " + id, ActivityCategory.ACADEMIC, date, earliest, latest,
                durationMinutes, EnergyRating.of(3), SensoryRating.of(3), null, null);
    }

    @Test
    public void dashboard_readsThroughStorageAndActivityManager_fixedAndFlexibleCombineCorrectly() throws Exception {
        Storage storage = new Storage(dataDirectory);
        storage.prepareDataFiles();
        storage.saveActivities(List.of(
                fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                flexible(2, MONDAY, LocalTime.of(12, 0), LocalTime.of(18, 0), 90)));

        LoadResult<Activity> loaded = storage.loadActivities();
        ActivityManager activityManager = new ActivityManager();
        activityManager.loadAll(loaded.getRecords());
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(activityManager, period,
                LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(2, summary.getTotalActivityCount());
        assertEquals(120 + 90, summary.getPlannedWorkloadMinutes());
    }

    @Test
    public void restartConsistency_sameStorageAndFixedClock_producesIdenticalDashboardOutput() throws Exception {
        Storage storage = new Storage(dataDirectory);
        storage.prepareDataFiles();
        storage.saveActivities(List.of(fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 12, 0);

        // First "run": fresh manager loaded from disk.
        ActivityManager firstRun = new ActivityManager();
        firstRun.loadAll(storage.loadActivities().getRecords());
        String firstOutput = DashboardFormatter.format(DashboardService.summarize(firstRun, period, now), true);

        // Simulated restart: a completely separate manager, reloaded from the same files.
        ActivityManager secondRun = new ActivityManager();
        secondRun.loadAll(storage.loadActivities().getRecords());
        String secondOutput = DashboardFormatter.format(DashboardService.summarize(secondRun, period, now), true);

        assertEquals(firstOutput, secondOutput);
    }

    @Test
    public void malformedStorageLine_skippedWithWarning_validActivitiesStillContributeCorrectly() throws Exception {
        Path activitiesFile = dataDirectory.resolve("activities.txt");
        Files.createDirectories(dataDirectory);
        Files.writeString(activitiesFile,
                "FIXED|1|Lecture|ACADEMIC|2026-08-17|09:00|11:00|2|2|INCOMPLETE||\n"
                        + "this is not a valid activity line\n"
                        + "FIXED|2|Tutorial|ACADEMIC|2026-08-17|12:00|13:00|3|3|INCOMPLETE||\n");
        Storage storage = new Storage(dataDirectory);

        LoadResult<Activity> result = storage.loadActivities();

        assertEquals(1, result.getWarnings().size());
        assertEquals(2, result.getRecords().size());

        ActivityManager activityManager = new ActivityManager();
        activityManager.loadAll(result.getRecords());
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        DashboardSummary summary = DashboardService.summarize(activityManager, period,
                LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(2, summary.getTotalActivityCount());
        assertEquals(120 + 60, summary.getPlannedWorkloadMinutes());
    }

    @Test
    public void dashboardCommands_neverChangeAnyDataFile() throws Exception {
        Storage storage = new Storage(dataDirectory);
        storage.prepareDataFiles();
        storage.saveActivities(List.of(fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))));

        Path activitiesFile = dataDirectory.resolve("activities.txt");
        Path topicsFile = dataDirectory.resolve("topics.txt");
        Path settingsFile = dataDirectory.resolve("settings.txt");
        byte[] activitiesBefore = Files.readAllBytes(activitiesFile);
        byte[] topicsBefore = Files.readAllBytes(topicsFile);
        byte[] settingsBefore = Files.exists(settingsFile) ? Files.readAllBytes(settingsFile) : new byte[0];

        String input = "dashboard date/2026-08-17\ndashboard date/2026-08-17 detail\ndashboard this week\nbye\n";
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
        try {
            new ApplicationRunner(dataDirectory, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
                    .run();
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(Files.exists(activitiesFile));
        assertEquals(new String(activitiesBefore, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(activitiesFile), StandardCharsets.UTF_8));
        assertEquals(new String(topicsBefore, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(topicsFile), StandardCharsets.UTF_8));
        if (Files.exists(settingsFile)) {
            assertEquals(new String(settingsBefore, StandardCharsets.UTF_8),
                    new String(Files.readAllBytes(settingsFile), StandardCharsets.UTF_8));
        }
        assertFalse(capturedOutput.toString(StandardCharsets.UTF_8).contains("[Error]"));
    }
}
