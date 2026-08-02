package seedu.unienable.command.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmation;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.dashboard.DashboardService;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.logic.timetable.TimetableService;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.model.recommend.RecommendedPlacement;

class RecommendCommandTest {
    private static final LocalDate DATE = LocalDate.of(2099, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2099, 1, 1, 8, 0);

    @Test
    public void generate_storesProposalWithoutMutatingOriginalFlexibleActivity() throws Exception {
        FlexibleActivity flexible = flexible(2, "Essay draft", LocalTime.of(9, 0), LocalTime.of(14, 0), 60);
        ActivityManager activityManager = managerWith(List.of(
                fixed(1, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                flexible));
        PreferenceManager preferenceManager = new PreferenceManager();
        RecommendationManager recommendationManager = new RecommendationManager();

        CommandResult result = new RecommendGenerateCommand(activityManager, preferenceManager,
                recommendationManager, NOW, DATE).execute();

        assertTrue(recommendationManager.hasProposal());
        assertFalse(flexible.hasAdoptedPlacement());
        assertTrue(result.getFeedback().contains("Recommended timetable preview"));
        assertTrue(result.getFeedback().contains("Essay draft"));
    }

    @Test
    public void view_withoutProposal_rejected() {
        ActivityManager activityManager = new ActivityManager();
        RecommendationManager recommendationManager = new RecommendationManager();

        InvalidCommandException exception = assertThrows(InvalidCommandException.class,
                () -> new RecommendViewCommand(activityManager, recommendationManager).execute());

        assertEquals("No recommendation proposal is currently active. Generate one with recommend.",
                exception.getMessage());
    }

    @Test
    public void cancel_clearsProposalAndReturnsNoChangeMessage() throws Exception {
        RecommendationManager recommendationManager = new RecommendationManager();
        recommendationManager.setProposal(new RecommendationProposal(
                TimetableService.resolveThisWeek(NOW),
                DashboardService.resolveThisWeek(NOW), List.of(), List.of()));

        CommandResult result = new RecommendCancelCommand(recommendationManager).execute();

        assertEquals("Recommendation proposal discarded. No changes were made.", result.getFeedback());
        assertFalse(recommendationManager.hasProposal());
    }

    @Test
    public void adopt_confirmationAndExecute_applyScheduledPlacement() throws Exception {
        FlexibleActivity flexible = flexible(2, "Essay draft", LocalTime.of(9, 0), LocalTime.of(14, 0), 60);
        ActivityManager activityManager = managerWith(List.of(flexible));
        RecommendationProposal proposal = new RecommendationProposal(
                TimetableService.resolveThisWeek(NOW),
                DashboardService.resolveThisWeek(NOW),
                List.of(new RecommendedPlacement(2, "Essay draft", DATE,
                        LocalTime.of(11, 0), LocalTime.of(12, 0), false)),
                List.of());
        RecommendAdoptCommand command = new RecommendAdoptCommand(activityManager, proposal);

        Confirmation confirmation = command.getConfirmation();
        assertTrue(confirmation.isAsk());
        assertTrue(confirmation.getMessage().contains("Scheduled flexible activities: 1"));

        CommandResult result = command.execute();

        assertEquals(LocalTime.of(11, 0), flexible.getAdoptedStartTime());
        assertEquals("Recommendation adopted.\nScheduled flexible activities: 1\nUnscheduled flexible activities: 0",
                result.getFeedback());
    }

    private static ActivityManager managerWith(List<Activity> activities) {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(activities);
        return manager;
    }

    private static FixedActivity fixed(int id, LocalTime start, LocalTime end) throws Exception {
        return new FixedActivity(id, "Fixed " + id, ActivityCategory.ACADEMIC, DATE, start, end,
                EnergyRating.of(2), SensoryRating.of(2), null, null);
    }

    private static FlexibleActivity flexible(int id, String description, LocalTime earliest, LocalTime latest,
            int durationMinutes) throws Exception {
        return new FlexibleActivity(id, description, ActivityCategory.ACADEMIC, DATE,
                earliest, latest, durationMinutes, EnergyRating.of(4), SensoryRating.of(3), null, null);
    }
}
