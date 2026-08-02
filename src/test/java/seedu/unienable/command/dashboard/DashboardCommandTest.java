package seedu.unienable.command.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.dashboard.DashboardService;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

class DashboardCommandTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 8, 0);

    @Test
    public void execute_emptyPeriod_showsNoActivitiesFoundMessage() {
        ActivityManager activityManager = new ActivityManager();
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        DashboardCommand command = new DashboardCommand(activityManager, period, NOW, false);

        CommandResult result = command.execute();

        assertTrue(result.getFeedback().endsWith("No activities found for the selected period."));
    }

    @Test
    public void execute_nonEmptyPeriodNothingEligible_showsNoActivitiesDueYetMessage() throws Exception {
        FixedActivity future = new FixedActivity(1, "Future", ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(20, 0), LocalTime.of(21, 0), EnergyRating.of(2), SensoryRating.of(2), null, null);
        ActivityManager activityManager = new ActivityManager();
        activityManager.loadAll(List.of(future));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        DashboardCommand command = new DashboardCommand(activityManager, period, NOW, false);

        CommandResult result = command.execute();

        assertTrue(result.getFeedback().contains("Completion: No activities are due yet."));
    }

    @Test
    public void execute_neverMutatesActivityManagerState() throws Exception {
        FixedActivity activity = new FixedActivity(1, "Lecture", ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), EnergyRating.of(2), SensoryRating.of(2), null, null);
        ActivityManager activityManager = new ActivityManager();
        activityManager.loadAll(List.of(activity));
        int sizeBefore = activityManager.size();
        boolean completeBefore = activity.isComplete();
        int nextIdBefore = activityManager.getNextId();
        ActivityOrder orderBefore = activityManager.getDefaultOrder();
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        DashboardCommand command = new DashboardCommand(activityManager, period, NOW, true);

        command.execute();

        assertEquals(sizeBefore, activityManager.size());
        assertEquals(completeBefore, activity.isComplete());
        assertEquals(nextIdBefore, activityManager.getNextId());
        assertEquals(orderBefore, activityManager.getDefaultOrder());
    }

    @Test
    public void execute_detailFlagTrue_includesDetailSectionInOutput() throws Exception {
        FixedActivity activity = new FixedActivity(1, "Lecture", ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), EnergyRating.of(2), SensoryRating.of(2), null, null);
        ActivityManager activityManager = new ActivityManager();
        activityManager.loadAll(List.of(activity));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        String withDetail = new DashboardCommand(activityManager, period, NOW, true).execute().getFeedback();
        String withoutDetail = new DashboardCommand(activityManager, period, NOW, false).execute().getFeedback();

        assertTrue(withDetail.contains("Category breakdown"));
        assertTrue(withoutDetail.length() < withDetail.length());
    }
}
