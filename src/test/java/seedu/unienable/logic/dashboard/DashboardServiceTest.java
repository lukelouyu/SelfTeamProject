package seedu.unienable.logic.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.dashboard.DashboardSummary;
import seedu.unienable.model.dashboard.RatingSummary;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;

/** Covers {@link DashboardService} with synthetic fixtures and an injected fixed clock only. */
class DashboardServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);

    private static FixedActivity fixed(int id, LocalDate date, LocalTime start, LocalTime end, int energy,
            int sensory) throws Exception {
        return new FixedActivity(id, "Fixed " + id, ActivityCategory.ACADEMIC, date, start, end,
                EnergyRating.of(energy), SensoryRating.of(sensory), null, null);
    }

    private static FixedActivity fixedInCategory(int id, LocalDate date, LocalTime start, LocalTime end,
            ActivityCategory category) throws Exception {
        return new FixedActivity(id, "Fixed " + id, category, date, start, end, EnergyRating.of(2),
                SensoryRating.of(2), null, null);
    }

    private static FlexibleActivity flexible(int id, LocalDate date, LocalTime earliest, LocalTime latest,
            int durationMinutes, int energy, int sensory) throws Exception {
        return new FlexibleActivity(id, "Flexible " + id, ActivityCategory.ACADEMIC, date, earliest, latest,
                durationMinutes, EnergyRating.of(energy), SensoryRating.of(sensory), null, null);
    }

    private static ActivityManager managerWith(List<Activity> activities) {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(activities);
        return manager;
    }

    // ---- Period resolution (AC-DASH-PERIOD) ----

    @Test
    public void resolveToday_resolveTomorrow_matchExpectedHalfOpenBounds() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 14, 30);

        DashboardPeriod today = DashboardService.resolveToday(now);
        DashboardPeriod tomorrow = DashboardService.resolveTomorrow(now);

        assertEquals("Today", today.getLabel());
        assertEquals(LocalDateTime.of(2026, 8, 17, 0, 0), today.getStart());
        assertEquals(LocalDateTime.of(2026, 8, 18, 0, 0), today.getEnd());
        assertEquals("Tomorrow", tomorrow.getLabel());
        assertEquals(LocalDateTime.of(2026, 8, 18, 0, 0), tomorrow.getStart());
        assertEquals(LocalDateTime.of(2026, 8, 19, 0, 0), tomorrow.getEnd());
    }

    @Test
    public void resolveDate_matchesHalfOpenBounds() {
        DashboardPeriod period = DashboardService.resolveDate(LocalDate.of(2026, 8, 20));

        assertEquals("2026-08-20", period.getLabel());
        assertEquals(LocalDateTime.of(2026, 8, 20, 0, 0), period.getStart());
        assertEquals(LocalDateTime.of(2026, 8, 21, 0, 0), period.getEnd());
    }

    @Test
    public void resolveThisWeek_matchesListThisWeekMondayToSundayBoundary() {
        // now falls on a Wednesday; the week must still resolve to that week's Monday-Sunday,
        // identical to ListCommandParser's own previousOrSame(MONDAY)/nextOrSame(SUNDAY) logic.
        LocalDateTime wednesday = LocalDateTime.of(2026, 8, 19, 9, 0);

        DashboardPeriod week = DashboardService.resolveThisWeek(wednesday);

        assertEquals("This week", week.getLabel());
        assertEquals(LocalDateTime.of(2026, 8, 17, 0, 0), week.getStart());
        assertEquals(LocalDateTime.of(2026, 8, 24, 0, 0), week.getEnd());
    }

    @Test
    public void resolveNextWeek_isExactlyOneWeekAfterResolveThisWeek() {
        LocalDateTime wednesday = LocalDateTime.of(2026, 8, 19, 9, 0);

        DashboardPeriod nextWeek = DashboardService.resolveNextWeek(wednesday);

        assertEquals("Next week", nextWeek.getLabel());
        assertEquals(LocalDateTime.of(2026, 8, 24, 0, 0), nextWeek.getStart());
        assertEquals(LocalDateTime.of(2026, 8, 31, 0, 0), nextWeek.getEnd());
    }

    @Test
    public void getCapacityMinutes_dayAndWeekPeriods_returnExpectedMinutes() {
        DashboardPeriod day = DashboardService.resolveToday(LocalDateTime.of(2026, 8, 17, 0, 0));
        DashboardPeriod week = DashboardService.resolveThisWeek(LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(1440, day.getCapacityMinutes());
        assertEquals(10080, week.getCapacityMinutes());
    }

    @Test
    public void resolveDate_monthYearAndLeapDayTransitions_boundariesCorrect() {
        DashboardPeriod monthEnd = DashboardService.resolveDate(LocalDate.of(2026, 1, 31));
        DashboardPeriod yearEnd = DashboardService.resolveDate(LocalDate.of(2026, 12, 31));
        DashboardPeriod leapDay = DashboardService.resolveDate(LocalDate.of(2028, 2, 29));

        assertEquals(LocalDateTime.of(2026, 2, 1, 0, 0), monthEnd.getEnd());
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), yearEnd.getEnd());
        assertEquals(LocalDateTime.of(2028, 3, 1, 0, 0), leapDay.getEnd());
    }

    // ---- Interval clipping (AC-DASH-INTERVAL) - package-private clip(), tested directly ----

    @Test
    public void clip_fullyInsidePeriod_contributesFullDuration() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 8, 17, 0, 0);
        LocalDateTime periodEnd = LocalDateTime.of(2026, 8, 18, 0, 0);

        long minutes = DashboardService.clip(LocalDateTime.of(2026, 8, 17, 10, 0),
                LocalDateTime.of(2026, 8, 17, 12, 0), periodStart, periodEnd);

        assertEquals(120, minutes);
    }

    @Test
    public void clip_fullyOutsidePeriod_contributesZero() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 8, 17, 0, 0);
        LocalDateTime periodEnd = LocalDateTime.of(2026, 8, 18, 0, 0);

        long minutes = DashboardService.clip(LocalDateTime.of(2026, 8, 20, 10, 0),
                LocalDateTime.of(2026, 8, 20, 12, 0), periodStart, periodEnd);

        assertEquals(0, minutes);
    }

    @Test
    public void clip_activityFullyCoveringPeriod_contributesExactPeriodDuration() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 8, 17, 9, 0);
        LocalDateTime periodEnd = LocalDateTime.of(2026, 8, 17, 11, 0);

        long minutes = DashboardService.clip(LocalDateTime.of(2026, 8, 17, 0, 0),
                LocalDateTime.of(2026, 8, 18, 0, 0), periodStart, periodEnd);

        assertEquals(120, minutes);
    }

    @Test
    public void clip_endsExactlyAtPeriodStart_contributesZero() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 8, 17, 9, 0);
        LocalDateTime periodEnd = LocalDateTime.of(2026, 8, 17, 17, 0);

        long minutes = DashboardService.clip(LocalDateTime.of(2026, 8, 17, 7, 0), periodStart, periodStart,
                periodEnd);

        assertEquals(0, minutes);
    }

    @Test
    public void clip_startsExactlyAtPeriodEnd_contributesZero() {
        LocalDateTime periodStart = LocalDateTime.of(2026, 8, 17, 9, 0);
        LocalDateTime periodEnd = LocalDateTime.of(2026, 8, 17, 17, 0);

        long minutes = DashboardService.clip(periodEnd, LocalDateTime.of(2026, 8, 17, 19, 0), periodStart,
                periodEnd);

        assertEquals(0, minutes);
    }

    @Test
    public void clip_syntheticCrossMidnightInterval_matchesDayAndWeekContributions() {
        // Simulates a hypothetical "Monday 23:00 -> Tuesday 01:00" interval directly as
        // LocalDateTime boundaries - the current activity model cannot construct such an
        // activity (see docs/tasks/v2/dashboard/IMPLEMENTATION_NOTES.md), so the clipping
        // calculation itself is exercised instead, per explicit instruction.
        LocalDateTime intervalStart = LocalDateTime.of(2026, 8, 17, 23, 0);
        LocalDateTime intervalEnd = LocalDateTime.of(2026, 8, 18, 1, 0);

        long mondayOnly = DashboardService.clip(intervalStart, intervalEnd,
                LocalDateTime.of(2026, 8, 17, 0, 0), LocalDateTime.of(2026, 8, 18, 0, 0));
        long tuesdayOnly = DashboardService.clip(intervalStart, intervalEnd,
                LocalDateTime.of(2026, 8, 18, 0, 0), LocalDateTime.of(2026, 8, 19, 0, 0));
        long wholeWeek = DashboardService.clip(intervalStart, intervalEnd,
                LocalDateTime.of(2026, 8, 17, 0, 0), LocalDateTime.of(2026, 8, 24, 0, 0));

        assertEquals(60, mondayOnly);
        assertEquals(60, tuesdayOnly);
        assertEquals(120, wholeWeek);
    }

    // ---- Inclusion + workload + buffer (AC-DASH-WORKLOAD, AC-DASH-BUFFER) ----

    @Test
    public void summarize_fixedActivityStartingExactlyAtPeriodEnd_excludedFromEarlierPeriod() throws Exception {
        // An activity dated Tuesday starting at 00:00 begins exactly when Monday's period ends;
        // it must belong to Tuesday's dashboard, not Monday's.
        FixedActivity nextDayActivity = fixed(1, TUESDAY, LocalTime.of(0, 0), LocalTime.of(1, 0), 2, 2);
        ActivityManager manager = managerWith(List.of(nextDayActivity));
        DashboardPeriod monday = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, monday, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(0, summary.getTotalActivityCount());
    }

    @Test
    public void summarize_flexibleWindowStartingExactlyAtPeriodEnd_excludedFromEarlierPeriod() throws Exception {
        FlexibleActivity nextDayActivity = flexible(1, TUESDAY, LocalTime.of(0, 0), LocalTime.of(1, 0), 30, 2, 2);
        ActivityManager manager = managerWith(List.of(nextDayActivity));
        DashboardPeriod monday = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, monday, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(0, summary.getTotalActivityCount());
    }

    @Test
    public void summarize_mixedFixedAndFlexible_workloadIsSumOfBothContributions() throws Exception {
        FixedActivity fixedActivity = fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 2, 2);
        FlexibleActivity flexibleActivity = flexible(2, MONDAY, LocalTime.of(12, 0), LocalTime.of(18, 0), 90, 3, 3);
        ActivityManager manager = managerWith(List.of(fixedActivity, flexibleActivity));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(2, summary.getTotalActivityCount());
        assertEquals(1, summary.getFixedCount());
        assertEquals(1, summary.getFlexibleCount());
        assertEquals(120 + 90, summary.getPlannedWorkloadMinutes());
    }

    @Test
    public void summarize_overlappingFixedActivities_countedIndividuallyNotMerged() throws Exception {
        FixedActivity activityA = fixed(1, MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), 2, 2);
        FixedActivity activityB = fixed(2, MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0), 2, 2);
        ActivityManager manager = managerWith(List.of(activityA, activityB));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(240, summary.getPlannedWorkloadMinutes());
    }

    @Test
    public void summarize_emptyPeriod_workloadIsZeroAndBufferIsFullCapacity() throws Exception {
        ActivityManager manager = managerWith(List.of());
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(0, summary.getTotalActivityCount());
        assertEquals(0, summary.getPlannedWorkloadMinutes());
        assertEquals(1440, summary.getNominalBufferMinutes());
        assertEquals(0, summary.getOverloadMinutes());
    }

    @Test
    public void summarize_flexibleWorkload_usesFullRequestedDurationNotWindowSpan() throws Exception {
        // The window itself (06:00-10:00, 4 hours = 240 minutes) is wider than the requested
        // duration (90 minutes); the contribution must be the requested duration, not the window
        // span - proving flexible workload comes from durationMinutes, never derived from the
        // window's own length.
        FlexibleActivity activity = flexible(1, MONDAY, LocalTime.of(6, 0), LocalTime.of(10, 0), 90, 4, 4);
        ActivityManager manager = managerWith(List.of(activity));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(90, summary.getPlannedWorkloadMinutes());
    }

    // A single same-day activity can contribute at most 1439 minutes (LocalTime's day boundary
    // is 23:59:59.999999999, truncating to 1439 whole minutes - never a clean 1440), so exact- and
    // over-capacity scenarios use two flexible activities together rather than one.

    @Test
    public void summarize_exactCapacity_bufferAndOverloadAreBothZero() throws Exception {
        FlexibleActivity almostFullDay = flexible(1, MONDAY, LocalTime.of(0, 0), LocalTime.of(23, 59), 1439, 2, 2);
        FlexibleActivity oneMoreMinute = flexible(2, MONDAY, LocalTime.of(0, 0), LocalTime.of(0, 1), 1, 2, 2);
        ActivityManager manager = managerWith(List.of(almostFullDay, oneMoreMinute));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(1440, summary.getPlannedWorkloadMinutes());
        assertEquals(0, summary.getNominalBufferMinutes());
        assertEquals(0, summary.getOverloadMinutes());
        assertFalse(summary.isOverloaded());
    }

    @Test
    public void summarize_oneMinuteUnderCapacity_bufferIsOneMinute() throws Exception {
        FlexibleActivity almostFullDay = flexible(1, MONDAY, LocalTime.of(0, 0), LocalTime.of(23, 59), 1439, 2, 2);
        ActivityManager manager = managerWith(List.of(almostFullDay));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(1439, summary.getPlannedWorkloadMinutes());
        assertEquals(1, summary.getNominalBufferMinutes());
        assertEquals(0, summary.getOverloadMinutes());
        assertFalse(summary.isOverloaded());
    }

    @Test
    public void summarize_oneMinuteOverCapacity_overloadIsOneMinute() throws Exception {
        FlexibleActivity almostFullDay = flexible(1, MONDAY, LocalTime.of(0, 0), LocalTime.of(23, 59), 1439, 2, 2);
        FlexibleActivity twoMoreMinutes = flexible(2, MONDAY, LocalTime.of(0, 0), LocalTime.of(0, 2), 2, 2, 2);
        ActivityManager manager = managerWith(List.of(almostFullDay, twoMoreMinutes));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(1441, summary.getPlannedWorkloadMinutes());
        assertEquals(0, summary.getNominalBufferMinutes());
        assertEquals(1, summary.getOverloadMinutes());
        assertTrue(summary.isOverloaded());
    }

    // ---- Energy / sensory ratings (AC-DASH-RATING) ----

    @Test
    public void summarize_totalsAndHighCounts_matchIndependentThresholdCheck() throws Exception {
        FixedActivity low = fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 2, 5);
        FixedActivity high = fixed(2, MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), 5, 2);
        ActivityManager manager = managerWith(List.of(low, high));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(7, summary.getEnergy().getTotal());
        assertEquals(7, summary.getSensory().getTotal());
        assertEquals(1, summary.getEnergy().getHighCount());
        assertEquals(1, summary.getSensory().getHighCount());
    }

    @Test
    public void highRatingThreshold_ratingThreeNotHigh_ratingFourAndFiveAreHigh() throws Exception {
        FixedActivity ratingThree = fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 3, 3);
        FixedActivity ratingFour = fixed(2, MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), 4, 4);
        FixedActivity ratingFive = fixed(3, MONDAY, LocalTime.of(13, 0), LocalTime.of(14, 0), 5, 5);
        ActivityManager manager = managerWith(List.of(ratingThree, ratingFour, ratingFive));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(2, summary.getEnergy().getHighCount());
        assertEquals(2, summary.getSensory().getHighCount());
    }

    @Test
    public void averageRating_halfUpRoundingToOneDecimal_deterministic() throws Exception {
        // Energy ratings 3, 3, 4 average to 3.333... -> rounds to 3.3 (half-up).
        FixedActivity a = fixed(1, MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), 3, 1);
        FixedActivity b = fixed(2, MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 3, 1);
        FixedActivity c = fixed(3, MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), 4, 1);
        ActivityManager manager = managerWith(List.of(a, b, c));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(3.3, summary.getEnergy().getAverage(), 0.0001);
    }

    @Test
    public void averageAndHighest_emptyIncludedSet_reportedAsHasDataFalse() {
        ActivityManager manager = managerWith(List.of());
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertFalse(summary.getEnergy().hasData());
        assertFalse(summary.getSensory().hasData());
    }

    // ---- Completion eligibility (AC-DASH-COMPLETION) ----

    @Test
    public void eligibility_fixedEndExactlyNow_includedAsEligible() throws Exception {
        FixedActivity activity = fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 2, 2);
        ActivityManager manager = managerWith(List.of(activity));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 11, 0);

        DashboardSummary summary = DashboardService.summarize(manager, period, now);

        assertEquals(1, summary.getEligibleCount());
    }

    @Test
    public void eligibility_flexibleLatestEndExactlyNow_includedAsEligible() throws Exception {
        FlexibleActivity activity = flexible(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 60, 2, 2);
        ActivityManager manager = managerWith(List.of(activity));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 11, 0);

        DashboardSummary summary = DashboardService.summarize(manager, period, now);

        assertEquals(1, summary.getEligibleCount());
    }

    @Test
    public void eligibility_adoptedFlexibleUsesAdoptedEndInsteadOfWindowEnd() throws Exception {
        FlexibleActivity activity = flexible(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 2, 2);
        activity.setAdoptedStartTime(LocalTime.of(10, 0));
        ActivityManager manager = managerWith(List.of(activity));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 11, 0);

        DashboardSummary summary = DashboardService.summarize(manager, period, now);

        assertEquals(1, summary.getEligibleCount());
    }

    @Test
    public void eligibility_futureAndOngoingFixed_excludedFromEligibility() throws Exception {
        FixedActivity future = fixed(1, MONDAY, LocalTime.of(20, 0), LocalTime.of(21, 0), 2, 2);
        FixedActivity ongoing = fixed(2, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 2, 2);
        ActivityManager manager = managerWith(List.of(future, ongoing));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 10, 0);

        DashboardSummary summary = DashboardService.summarize(manager, period, now);

        assertEquals(0, summary.getEligibleCount());
        assertEquals(2, summary.getTotalActivityCount());
    }

    @Test
    public void eligibility_futureAndOngoingFlexible_excludedFromEligibility() throws Exception {
        FlexibleActivity future = flexible(1, MONDAY, LocalTime.of(20, 0), LocalTime.of(22, 0), 60, 2, 2);
        FlexibleActivity ongoing = flexible(2, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 60, 2, 2);
        ActivityManager manager = managerWith(List.of(future, ongoing));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 10, 0);

        DashboardSummary summary = DashboardService.summarize(manager, period, now);

        assertEquals(0, summary.getEligibleCount());
    }

    @Test
    public void eligibleCount_equalsCompletedPlusIncompleteEligible_futureExcludedEntirely() throws Exception {
        FixedActivity completedEligible = fixed(1, MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), 2, 2);
        completedEligible.mark();
        FixedActivity incompleteEligible = fixed(2, MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 2, 2);
        FixedActivity future = fixed(3, MONDAY, LocalTime.of(20, 0), LocalTime.of(21, 0), 2, 2);
        ActivityManager manager = managerWith(List.of(completedEligible, incompleteEligible, future));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 12, 0);

        DashboardSummary summary = DashboardService.summarize(manager, period, now);

        assertEquals(2, summary.getEligibleCount());
        assertEquals(1, summary.getCompletedEligibleCount());
        assertEquals(3, summary.getTotalActivityCount());
    }

    // ---- Completion percentage (AC-DASH-PERCENT) ----

    @Test
    public void completionPercentage_zeroPercent() throws Exception {
        assertPercentage(0, 10, 0);
    }

    @Test
    public void completionPercentage_sixtyPercent() throws Exception {
        assertPercentage(6, 10, 60);
    }

    @Test
    public void completionPercentage_hundredPercent() throws Exception {
        assertPercentage(10, 10, 100);
    }

    @Test
    public void completionPercentage_twoThirds_roundsToSixtySeven() throws Exception {
        assertPercentage(2, 3, 67);
    }

    @Test
    public void completionPercentage_noEligibleActivities_isEmpty() throws Exception {
        FixedActivity future = fixed(1, MONDAY, LocalTime.of(20, 0), LocalTime.of(21, 0), 2, 2);
        ActivityManager manager = managerWith(List.of(future));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(OptionalInt.empty(), summary.getCompletionPercentage());
    }

    private void assertPercentage(int completed, int eligible, int expectedPercentage) throws Exception {
        LocalTime start = LocalTime.of(0, 0);
        List<Activity> activities = new ArrayList<>();
        int id = 1;
        for (int i = 0; i < eligible; i++) {
            LocalTime activityStart = start.plusMinutes((long) i * 10);
            LocalTime activityEnd = activityStart.plusMinutes(5);
            FixedActivity activity = fixed(id++, MONDAY, activityStart, activityEnd, 2, 2);
            if (i < completed) {
                activity.mark();
            }
            activities.add(activity);
        }
        ActivityManager manager = managerWith(activities);
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 23, 0);

        DashboardSummary summary = DashboardService.summarize(manager, period, now);

        assertEquals(eligible, summary.getEligibleCount());
        assertEquals(completed, summary.getCompletedEligibleCount());
        assertEquals(OptionalInt.of(expectedPercentage), summary.getCompletionPercentage());
    }

    // ---- Detail-mode metrics (AC-DASH-DETAIL) ----

    @Test
    public void summarize_fixedAndFlexibleCounts_matchIncludedActivityTypes() throws Exception {
        FixedActivity fixedOne = fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 2, 2);
        FixedActivity fixedTwo = fixed(2, MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), 2, 2);
        FlexibleActivity flexibleOne = flexible(3, MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0), 30, 2, 2);
        ActivityManager manager = managerWith(List.of(fixedOne, fixedTwo, flexibleOne));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(2, summary.getFixedCount());
        assertEquals(1, summary.getFlexibleCount());
    }

    @Test
    public void categoryCounts_allFourCategoriesPresent_inEnumDeclarationOrder() throws Exception {
        FixedActivity academic = fixedInCategory(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0),
                ActivityCategory.ACADEMIC);
        FixedActivity cca = fixedInCategory(2, MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0),
                ActivityCategory.CCA);
        ActivityManager manager = managerWith(List.of(academic, cca));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        Map<ActivityCategory, Integer> counts = summary.getCategoryCounts();
        assertEquals(4, counts.size());
        assertEquals(1, counts.get(ActivityCategory.ACADEMIC));
        assertEquals(1, counts.get(ActivityCategory.CCA));
        assertEquals(0, counts.get(ActivityCategory.WORK_INTERNSHIP));
        assertEquals(0, counts.get(ActivityCategory.OTHERS));
    }

    @Test
    public void distribution_ascendingOrderWithZeroCounts() throws Exception {
        FixedActivity ratingOne = fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 1, 1);
        FixedActivity ratingOneAgain = fixed(2, MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), 1, 1);
        FixedActivity ratingFive = fixed(3, MONDAY, LocalTime.of(13, 0), LocalTime.of(14, 0), 5, 5);
        ActivityManager manager = managerWith(List.of(ratingOne, ratingOneAgain, ratingFive));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);

        DashboardSummary summary = DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        int[] distribution = summary.getEnergy().getDistribution();
        assertEquals(2, distribution[0]);
        assertEquals(0, distribution[1]);
        assertEquals(0, distribution[2]);
        assertEquals(0, distribution[3]);
        assertEquals(1, distribution[4]);
    }

    // ---- Immutability ----

    @Test
    public void summarize_doesNotMutateActivityManagerOrActivities() throws Exception {
        FixedActivity activity = fixed(1, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 2, 2);
        ActivityManager manager = managerWith(List.of(activity));
        DashboardPeriod period = DashboardService.resolveDate(MONDAY);
        int sizeBefore = manager.size();
        boolean completeBefore = activity.isComplete();
        ActivityOrder orderBefore = manager.getDefaultOrder();

        DashboardService.summarize(manager, period, LocalDateTime.of(2026, 8, 17, 0, 0));

        assertEquals(sizeBefore, manager.size());
        assertEquals(completeBefore, activity.isComplete());
        assertEquals(orderBefore, manager.getDefaultOrder());
    }

    @Test
    public void ratingSummary_getDistribution_returnsDefensiveCopy() {
        RatingSummary summary = new RatingSummary(5, 1, true, 3.0, 5, new int[] { 1, 1, 1, 1, 1 });

        int[] distribution = summary.getDistribution();
        distribution[0] = 99;

        assertEquals(1, summary.getDistribution()[0]);
    }
}
