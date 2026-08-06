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
import seedu.unienable.model.recommend.RecommendedPlacement;

class RecommendationServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);
    private static final LocalDateTime MIDNIGHT = MONDAY.atStartOfDay();

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
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

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
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertTrue(proposal.getPlacements().isEmpty());
        assertEquals(List.of(3), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendDate_previewDoesNotMutateOriginalFlexibleActivity() throws Exception {
        FlexibleActivity activity = flexible(3, "Study block", 10, 0, 12, 0, 60);
        ActivityManager manager = managerWith(List.of(activity));

        RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertFalse(activity.hasAdoptedPlacement());
    }

    @Test
    public void recommendDate_tomatoOn_marksSuitableStudyPlacement() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 12, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.ON), MONDAY,
                MIDNIGHT);

        assertEquals(1, proposal.getPlacements().size());
        assertTrue(proposal.getPlacements().get(0).tomatoSuggested());
    }

    @Test
    public void applyPreview_appliesPlacementOnlyToCopiedActivity() throws Exception {
        FlexibleActivity activity = flexible(3, "Study block", 10, 0, 12, 0, 60);
        ActivityManager manager = managerWith(List.of(activity));
        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

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

    @Test
    public void recommendNextWeek_considersOnlyNextWeeksActivitiesAndIsNeverClampedByNow() throws Exception {
        LocalDate nextWeekMonday = MONDAY.plusDays(7);
        FlexibleActivity thisWeek = new FlexibleActivity(3, "This week block", ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(10, 0), LocalTime.of(12, 0), 60, EnergyRating.of(4), SensoryRating.of(3), null, null);
        FlexibleActivity nextWeek = new FlexibleActivity(4, "Next week block", ActivityCategory.ACADEMIC,
                nextWeekMonday, LocalTime.of(9, 0), LocalTime.of(11, 0), 60, EnergyRating.of(4),
                SensoryRating.of(3), null, null);
        ActivityManager manager = managerWith(List.of(thisWeek, nextWeek));

        RecommendationProposal proposal = RecommendationService.recommendNextWeek(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF),
                MONDAY.atTime(23, 59));

        assertEquals("Next week", proposal.getTimetablePeriod().getLabel());
        assertEquals(1, proposal.getPlacements().size());
        assertEquals(4, proposal.getPlacements().get(0).activityId());
        assertEquals(LocalTime.of(9, 0), proposal.getPlacements().get(0).startTime());
        assertTrue(proposal.getUnscheduledActivityIds().isEmpty());
    }

    @Test
    public void recommendDate_wholeWindowElapsedByNow_leavesActivityUnscheduled() throws Exception {
        // Window 09:00-11:00, duration 60 -> latest possible start is 10:00. "now" is 12:00,
        // which is after every candidate start, so the activity must remain unscheduled and
        // never be proposed at an already-passed 09:00.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 9, 0, 11, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MONDAY.atTime(12, 0));

        assertTrue(proposal.getPlacements().isEmpty());
        assertEquals(List.of(3), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendDate_partiallyElapsedWindow_startsAtNextWholeMinuteNotBeforeNow() throws Exception {
        // Window 09:00-14:00, duration 60. "now" is 12:30:20 (with seconds). The earliest legal
        // start must be 12:31 - never 12:30 or earlier, which would already be in the past by the
        // time the seconds are accounted for.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 9, 0, 14, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MONDAY.atTime(12, 30, 20));

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(LocalTime.of(12, 31), proposal.getPlacements().get(0).startTime());
    }

    @Test
    public void recommendDate_exactWholeMinuteNow_isItselfAValidStart() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 9, 0, 14, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MONDAY.atTime(12, 30, 0));

        assertEquals(LocalTime.of(12, 30), proposal.getPlacements().get(0).startTime());
    }

    @Test
    public void recommendDate_futureDate_isNotClippedByTodaysNow() throws Exception {
        LocalDate tuesday = MONDAY.plusDays(1);
        FlexibleActivity onTuesday = new FlexibleActivity(3, "Study block", ActivityCategory.ACADEMIC, tuesday,
                LocalTime.of(9, 0), LocalTime.of(11, 0), 60, EnergyRating.of(4), SensoryRating.of(3), null, null);
        ActivityManager manager = managerWith(List.of(onTuesday));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), tuesday,
                MONDAY.atTime(23, 59));

        assertEquals(LocalTime.of(9, 0), proposal.getPlacements().get(0).startTime());
    }

    @Test
    public void recommendThisWeek_todayPartiallyElapsed_futureDayInWeekUnaffected() throws Exception {
        LocalDate wednesday = MONDAY.plusDays(2);
        FlexibleActivity today = new FlexibleActivity(3, "Today block", ActivityCategory.ACADEMIC, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), 60, EnergyRating.of(2), SensoryRating.of(2), null, null);
        FlexibleActivity future = new FlexibleActivity(4, "Later block", ActivityCategory.ACADEMIC, wednesday,
                LocalTime.of(9, 0), LocalTime.of(11, 0), 60, EnergyRating.of(2), SensoryRating.of(2), null, null);
        ActivityManager manager = managerWith(List.of(today, future));

        RecommendationProposal proposal = RecommendationService.recommendThisWeek(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF),
                MONDAY.atTime(12, 0));

        assertEquals(List.of(3), proposal.getUnscheduledActivityIds());
        assertEquals(1, proposal.getPlacements().size());
        assertEquals(4, proposal.getPlacements().get(0).activityId());
        assertEquals(LocalTime.of(9, 0), proposal.getPlacements().get(0).startTime());
    }

    @Test
    public void hasElapsedPlacement_futureStart_isFalse() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 11, 0, 30)));
        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertFalse(RecommendationService.hasElapsedPlacement(proposal, MONDAY.atTime(9, 0)));
    }

    @Test
    public void hasElapsedPlacement_nowPastProposedStart_isTrue() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 11, 0, 30)));
        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertTrue(RecommendationService.hasElapsedPlacement(proposal, MONDAY.atTime(10, 1)));
    }

    @Test
    public void hasElapsedPlacement_nowExactlyAtProposedStart_isFalse() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 11, 0, 30)));
        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertFalse(RecommendationService.hasElapsedPlacement(proposal, MONDAY.atTime(10, 0)));
    }

    // ---- Preferred daily start/end as a hard boundary (regression for the past-time-review defect) ----

    @Test
    public void recommendDate_windowStartsBeforePreferredStart_placementClampedToPreferredStart() throws Exception {
        // Activity window 06:30-09:30, duration 45, preferred 07:30-21:00: 07:30 is reachable and
        // compliant, so it must be chosen over the non-compliant 06:30 the activity's own window
        // alone would otherwise allow.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Jogging", 6, 30, 9, 30, 45)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(LocalTime.of(7, 30), proposal.getPlacements().get(0).startTime());
    }

    @Test
    public void recommendDate_windowEntirelyAfterPreferredEnd_leavesActivityUnscheduled() throws Exception {
        // Activity window 21:15-22:30, duration 45, preferred end 21:00: no compliant slot exists
        // at all, so the activity must be reported unscheduled rather than placed past 21:00.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Late admin", 21, 15, 22, 30, 45)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertTrue(proposal.getPlacements().isEmpty());
        assertEquals(List.of(3), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendDate_narrowerPreferredWindow_placementClampedToPreferredStart() throws Exception {
        // Same shape with a different, narrower profile (09:30-18:30): the activity's own window
        // (08:00-12:00) starts before preferred start, so the earliest compliant start is 09:30.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Deep work", 8, 0, 12, 0, 90)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(9, 30), LocalTime.of(18, 30), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(LocalTime.of(9, 30), proposal.getPlacements().get(0).startTime());
    }

    @Test
    public void recommendDate_narrowerPreferredWindow_windowAfterPreferredEndLeavesUnscheduled() throws Exception {
        // Profile 09:30-18:30; activity window 18:00-21:00, duration 60: only 18:00-18:30 of the
        // activity's own window is inside the preferred range, too short for a 60-minute duration.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Evening wrap-up", 18, 0, 21, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(9, 30), LocalTime.of(18, 30), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertTrue(proposal.getPlacements().isEmpty());
        assertEquals(List.of(3), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendDate_todayNowLaterThanPreferredStart_clampsToNowNotPreferredStart() throws Exception {
        // Preferred start is 07:30, but now is already 08:15 on the activity's own date: now must
        // win over the preferred start, since scheduling before now is never allowed regardless of
        // preference (the two lower bounds combine as max(activityEarliest, preferredStart, now)).
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Jogging", 6, 30, 12, 0, 30)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MONDAY.atTime(8, 15));

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(LocalTime.of(8, 15), proposal.getPlacements().get(0).startTime());
    }

    @Test
    public void recommendDate_preferredWindowNarrowerThanDuration_leavesUnscheduledWithoutCrashing() throws Exception {
        // Preferred range is only 10 minutes (00:00-00:10); activity's own window (06:00-22:00,
        // duration 60) would ordinarily leave plenty of room, but the intersection is far too
        // narrow for a 60-minute duration. This must not wrap LocalTime arithmetic into a bogus
        // "valid" range - it must cleanly report the activity as unscheduled.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Long block", 6, 0, 22, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(0, 0), LocalTime.of(0, 10), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertTrue(proposal.getPlacements().isEmpty());
        assertEquals(List.of(3), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendDate_windowFullyInsidePreferredRange_isUnaffectedByPreference() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 11, 0, 30)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 0, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(LocalTime.of(10, 0), proposal.getPlacements().get(0).startTime());
    }

    // ---- Whole-day optimization (regression for the greedy-order defect) ----

    private static LocalTime startTimeOf(RecommendationProposal proposal, int activityId) {
        for (var placement : proposal.getPlacements()) {
            if (placement.activityId() == activityId) {
                return placement.startTime();
            }
        }
        throw new AssertionError("Activity " + activityId + " was not scheduled");
    }

    @Test
    public void recommendDate_threeActivitiesFitTogether_allAreScheduled() throws Exception {
        // Bug-report Example A: a greedy, activity-by-activity search let the short "Company ABC
        // application" and "Enablers check-in" each independently claim a slot, leaving no room for
        // the longer "CS2113 coding" even though a whole-day arrangement fits all three (e.g.
        // 14:00-16:00, 16:15-17:15, 18:00-19:00 with the default 15-minute buffer).
        ActivityManager manager = managerWith(List.of(
                flexible(3, "CS2113 coding", 14, 0, 18, 0, 120),
                flexible(4, "Company ABC application", 16, 0, 20, 0, 60),
                flexible(5, "Enablers check-in", 18, 0, 21, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 15, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertEquals(3, proposal.getPlacements().size());
        assertTrue(proposal.getUnscheduledActivityIds().isEmpty());
    }

    @Test
    public void recommendDate_restrictiveProfileFourActivitiesFitTogether_allAreScheduled() throws Exception {
        // Bug-report Example B (restrictive profile, 60-minute buffer): the greedy search left the
        // "Company ABC interview" unscheduled even though CDE2001 project teaming, the interview,
        // CS2113 project coding, and the Enablers meeting can all coexist in one arrangement.
        // CDE2001 (09:30-11:00, 90 min) and Enablers (17:00-18:00, 60 min) each have a window
        // exactly as wide as their own duration, so every valid arrangement places them at those
        // exact times regardless of search order - that makes them a reliable anchor to assert on.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "CDE2001 project teaming", 9, 30, 11, 0, 90),
                flexible(4, "Company ABC interview", 11, 0, 15, 0, 60),
                flexible(5, "CS2113 project coding", 12, 0, 16, 0, 120),
                flexible(6, "Enablers meeting", 17, 0, 18, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(9, 30), LocalTime.of(18, 30), 60, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertEquals(4, proposal.getPlacements().size());
        assertTrue(proposal.getUnscheduledActivityIds().isEmpty());
        assertEquals(LocalTime.of(9, 30), startTimeOf(proposal, 3));
        assertEquals(LocalTime.of(17, 0), startTimeOf(proposal, 6));
    }

    @Test
    public void recommendDate_restrictiveProfileTwoActivitiesFitTogether_bothAreScheduled() throws Exception {
        // Bug-report Thursday scenario (restrictive profile): the interview and the PL2131 study
        // task could both fit (14:00-15:00 and 16:00-18:00), but the greedy search selected only
        // the study task. With this shape, the 60-minute buffer forces both activities' start times
        // to a single feasible combination, so exact times can be asserted directly.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Interview", 14, 0, 16, 0, 60),
                flexible(4, "PL2131 study task", 14, 0, 18, 0, 120)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(9, 0), LocalTime.of(20, 0), 60, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertEquals(2, proposal.getPlacements().size());
        assertTrue(proposal.getUnscheduledActivityIds().isEmpty());
        assertEquals(LocalTime.of(14, 0), startTimeOf(proposal, 3));
        assertEquals(LocalTime.of(16, 0), startTimeOf(proposal, 4));
    }

    // ---- Boundary enforcement end-to-end (defense-in-depth verification) ----

    @Test
    public void recommendDate_earlyWindowClampsToPreferredStart_lateWindowLeftUnscheduled() throws Exception {
        // Exact reported repro: preferred 07:30-21:00, buffer 30. An activity windowed
        // 06:00-09:00 must clamp to 07:30; one windowed 20:30-23:00 (60 min) only has 30
        // preferred-range minutes to work with (20:30-21:00), so it must stay unscheduled rather
        // than ever being proposed past 21:00.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Test Early", 6, 0, 9, 0, 60),
                flexible(4, "Test Late", 20, 30, 23, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 30, TomatoSuggestion.OFF), MONDAY,
                MIDNIGHT);

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(LocalTime.of(7, 30), startTimeOf(proposal, 3));
        assertEquals(List.of(4), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendThisWeek_everyPlacementStaysWithinPreferredRange() throws Exception {
        LocalDate tuesday = MONDAY.plusDays(1);
        LocalDate wednesday = MONDAY.plusDays(2);
        ActivityManager manager = managerWith(List.of(
                new FlexibleActivity(3, "Monday early", ActivityCategory.ACADEMIC, MONDAY,
                        LocalTime.of(6, 0), LocalTime.of(9, 0), 60, EnergyRating.of(2), SensoryRating.of(2),
                        null, null),
                new FlexibleActivity(4, "Tuesday late", ActivityCategory.ACADEMIC, tuesday,
                        LocalTime.of(20, 30), LocalTime.of(23, 0), 60, EnergyRating.of(2), SensoryRating.of(2),
                        null, null),
                new FlexibleActivity(5, "Wednesday mid", ActivityCategory.ACADEMIC, wednesday,
                        LocalTime.of(10, 0), LocalTime.of(12, 0), 60, EnergyRating.of(2), SensoryRating.of(2),
                        null, null)));
        PreferenceProfile preferences = PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 30,
                TomatoSuggestion.OFF);

        RecommendationProposal proposal = RecommendationService.recommendThisWeek(manager, preferences, MIDNIGHT);

        for (RecommendedPlacement placement : proposal.getPlacements()) {
            assertFalse(placement.startTime().isBefore(preferences.getPreferredStart()));
            assertFalse(placement.endTime().isAfter(preferences.getPreferredEnd()));
        }
        assertEquals(List.of(4), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendNextWeek_everyPlacementStaysWithinPreferredRange() throws Exception {
        LocalDate nextWeekMonday = MONDAY.plusDays(7);
        LocalDate nextWeekTuesday = nextWeekMonday.plusDays(1);
        ActivityManager manager = managerWith(List.of(
                new FlexibleActivity(3, "Next Monday early", ActivityCategory.ACADEMIC, nextWeekMonday,
                        LocalTime.of(6, 0), LocalTime.of(9, 0), 60, EnergyRating.of(2), SensoryRating.of(2),
                        null, null),
                new FlexibleActivity(4, "Next Tuesday late", ActivityCategory.ACADEMIC, nextWeekTuesday,
                        LocalTime.of(20, 30), LocalTime.of(23, 0), 60, EnergyRating.of(2), SensoryRating.of(2),
                        null, null)));
        PreferenceProfile preferences = PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 30,
                TomatoSuggestion.OFF);

        RecommendationProposal proposal = RecommendationService.recommendNextWeek(manager, preferences,
                MONDAY.atTime(23, 59));

        for (RecommendedPlacement placement : proposal.getPlacements()) {
            assertFalse(placement.startTime().isBefore(preferences.getPreferredStart()));
            assertFalse(placement.endTime().isAfter(preferences.getPreferredEnd()));
        }
        assertEquals(List.of(4), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void recommendDate_todayClampAndPreferredEndBothApply() throws Exception {
        // now (08:15) is later than preferred start (07:30), so the today-clamp wins for the
        // lower bound; the upper bound (preferred end 21:00) still applies independently and
        // still leaves the 20:30-23:00 activity unscheduled.
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Morning task", 6, 0, 10, 0, 60),
                flexible(4, "Late task", 20, 30, 23, 0, 60)));

        RecommendationProposal proposal = RecommendationService.recommendDate(manager,
                PreferenceProfile.of(LocalTime.of(7, 30), LocalTime.of(21, 0), 30, TomatoSuggestion.OFF), MONDAY,
                MONDAY.atTime(8, 15));

        assertEquals(1, proposal.getPlacements().size());
        assertEquals(LocalTime.of(8, 15), startTimeOf(proposal, 3));
        assertEquals(List.of(4), proposal.getUnscheduledActivityIds());
    }

    @Test
    public void hasOutOfPreferredRangePlacement_everyPlacementWithinRange_isFalse() throws Exception {
        ActivityManager manager = managerWith(List.of(
                flexible(3, "Study block", 10, 0, 12, 0, 60)));
        PreferenceProfile preferences = PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(20, 0), 0,
                TomatoSuggestion.OFF);
        RecommendationProposal proposal = RecommendationService.recommendDate(manager, preferences, MONDAY,
                MIDNIGHT);

        assertFalse(RecommendationService.hasOutOfPreferredRangePlacement(proposal, preferences));
    }

    @Test
    public void hasOutOfPreferredRangePlacement_placementStartsBeforeNewPreferredStart_isTrue() {
        RecommendedPlacement placement = new RecommendedPlacement(1, "Early block", MONDAY,
                LocalTime.of(7, 0), LocalTime.of(8, 0), false);
        RecommendationProposal proposal = new RecommendationProposal(
                seedu.unienable.logic.timetable.TimetableService.resolveDay(MONDAY),
                seedu.unienable.logic.dashboard.DashboardService.resolveDate(MONDAY),
                List.of(placement), List.of());
        PreferenceProfile narrowedProfile = PreferenceProfile.of(LocalTime.of(9, 0), LocalTime.of(20, 0), 0,
                TomatoSuggestion.OFF);

        assertTrue(RecommendationService.hasOutOfPreferredRangePlacement(proposal, narrowedProfile));
    }

    @Test
    public void hasOutOfPreferredRangePlacement_placementEndsAfterNewPreferredEnd_isTrue() {
        RecommendedPlacement placement = new RecommendedPlacement(1, "Late block", MONDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0), false);
        RecommendationProposal proposal = new RecommendationProposal(
                seedu.unienable.logic.timetable.TimetableService.resolveDay(MONDAY),
                seedu.unienable.logic.dashboard.DashboardService.resolveDate(MONDAY),
                List.of(placement), List.of());
        PreferenceProfile narrowedProfile = PreferenceProfile.of(LocalTime.of(8, 0), LocalTime.of(19, 30), 0,
                TomatoSuggestion.OFF);

        assertTrue(RecommendationService.hasOutOfPreferredRangePlacement(proposal, narrowedProfile));
    }
}
