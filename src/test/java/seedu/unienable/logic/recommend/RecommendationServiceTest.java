package seedu.unienable.logic.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;
import seedu.unienable.model.recommend.RecommendationProposal;

class RecommendationServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

    private static FixedActivity fixed(int id, int startHour, int endHour) throws Exception {
        return new FixedActivity(id, "Fixed " + id, ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(startHour, 0), LocalTime.of(endHour, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null);
    }

    private static FlexibleActivity flexible(int id, String description, int earliestHour, int earliestMinute,
            int latestHour, int latestMinute, int durationMinutes) throws Exception {
        return new FlexibleActivity(id, description, ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(earliestHour, earliestMinute), LocalTime.of(latestHour, latestMinute), durationMinutes,
                EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    private static ActivityManager managerWith(List<Activity> activities) {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(activities);
        return manager;
    }

    @Test
    public void recommendDate_exactFitSlot_isAccepted() throws Exception {
        ActivityManager manager = managerWith(List.of(
                fixed(1, 9, 10),
                fixed(2, 11, 12),
                flexible(3, "Study block", 10, 0, 11, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY);

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(3, proposal.getPlacements().get(0).activityId());
        assertEquals(LocalTime.of(10, 0), proposal.getPlacements().get(0).startTime());
        assertTrue(proposal.getUnscheduledActivityIds().isEmpty());
    }

    @Test
    public void recommendDate_oneMinuteTooShort_isRejected() throws Exception {
        ActivityManager manager = managerWith(List.of(
                new FixedActivity(1, "Short blocker", ActivityCategory.ACADEMIC, MONDAY,
                        LocalTime.of(10, 59), LocalTime.of(11, 0), EnergyRating.of(2), SensoryRating.of(2),
                        null, null),
                flexible(3, "Study block", 10, 0, 11, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY);

        assertTrue(proposal.getPlacements().isEmpty());
        assertEquals(List.of(3), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendDate_previewDoesNotMutateOriginalFlexibleActivity() throws Exception {
        FlexibleActivity activity = flexible(3, "Study block", 10, 0, 12, 0, 60);
        ActivityManager manager = managerWith(List.of(activity));

        RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY);

        assertFalse(activity.hasAdoptedPlacement());
    }

    @Test
    public void recommendDate_tomatoOn_marksSuitableStudyPlacement() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 12, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.ON), MONDAY);

        assertEquals(1, proposal.getPlacements().size());
        assertTrue(proposal.getPlacements().get(0).tomatoSuggested());
    }

    @Test
    public void applyPreview_appliesPlacementOnlyToCopiedActivity() throws Exception {
        FlexibleActivity activity = flexible(3, "Study block", 10, 0, 12, 0, 60);
        ActivityManager manager = managerWith(List.of(activity));
        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY);

        List<Activity> preview = RecommendationService.applyPreview(manager.getAll(), proposal);

        FlexibleActivity copied = (FlexibleActivity) preview.get(0);
        assertTrue(copied.hasAdoptedPlacement());
        assertFalse(activity.hasAdoptedPlacement());
    }

    @Test
    public void recommendThisWeek_usesInjectedCurrentWeek() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 12, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendThisWeek(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF),
                LocalDateTime.of(2026, 8, 19, 9, 0));

        assertEquals("This week", proposal.getTimetablePeriod().getLabel());
    }
}
