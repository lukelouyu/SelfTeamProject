package seedu.unienable.logic.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.dashboard.DashboardSummary;
import seedu.unienable.model.dashboard.RatingSummary;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Resolves a {@code dashboard} period selector into concrete boundaries and calculates the
 * resulting {@link DashboardSummary}. Stateless; every method is a pure function of its
 * arguments, mirroring {@code logic.route.AccessibleRouteGraphFactory}'s shape.
 *
 * <p>Every period boundary is half-open ({@code [start, end)}), computed with {@code java.time}
 * so month/year transitions and leap days are handled for free. {@code this week} reuses the
 * exact Monday-Sunday boundary {@code ListCommandParser} already computes for {@code list this
 * week}, not a rolling seven-day window - see {@code docs/tasks/v2/dashboard/README.md}.
 */
public final class DashboardService {
    private static final int HIGH_RATING_THRESHOLD = 4;
    private static final int RATING_SCALE = 5;

    private DashboardService() {
    }

    /**
     * Resolves the {@code today} selector.
     *
     * @param now the injected current date and time
     * @return the period {@code [today 00:00, tomorrow 00:00)}
     */
    public static DashboardPeriod resolveToday(LocalDateTime now) {
        return dayPeriod("Today", now.toLocalDate());
    }

    /**
     * Resolves the {@code tomorrow} selector.
     *
     * @param now the injected current date and time
     * @return the period {@code [tomorrow 00:00, day-after-tomorrow 00:00)}
     */
    public static DashboardPeriod resolveTomorrow(LocalDateTime now) {
        return dayPeriod("Tomorrow", now.toLocalDate().plusDays(1));
    }

    /**
     * Resolves the {@code date/YYYY-MM-DD} selector.
     *
     * @param date the specified calendar date
     * @return the period {@code [date 00:00, next date 00:00)}
     */
    public static DashboardPeriod resolveDate(LocalDate date) {
        return dayPeriod(date.toString(), date);
    }

    private static DashboardPeriod dayPeriod(String label, LocalDate date) {
        return new DashboardPeriod(label, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /**
     * Resolves the {@code this week} selector: the same Monday-Sunday week
     * {@code ListCommandParser} already uses for {@code list this week}.
     *
     * @param now the injected current date and time
     * @return the period {@code [Monday 00:00, following Monday 00:00)}
     */
    public static DashboardPeriod resolveThisWeek(LocalDateTime now) {
        LocalDate monday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new DashboardPeriod("This week", monday.atStartOfDay(), monday.plusDays(7).atStartOfDay());
    }

    /**
     * Calculates the full dashboard summary for the given period.
     *
     * @param activityManager the manager to read activities from (read-only; never mutated)
     * @param period the period to calculate over
     * @param now the injected current date and time, used only for completion eligibility
     * @return the calculated summary
     */
    public static DashboardSummary summarize(ActivityManager activityManager, DashboardPeriod period,
            LocalDateTime now) {
        return summarize(activityManager.getAll(), period, now);
    }

    /** Calculates the full dashboard summary for an already prepared activity list. */
    public static DashboardSummary summarize(Iterable<Activity> activities, DashboardPeriod period,
            LocalDateTime now) {
        Accumulator acc = new Accumulator();
        for (Activity activity : activities) {
            accumulate(acc, activity, period, now);
        }
        return acc.toSummary(period);
    }

    private static void accumulate(Accumulator acc, Activity activity, DashboardPeriod period, LocalDateTime now) {
        boolean isFixed = activity instanceof FixedActivity;
        LocalDateTime activityStart;
        LocalDateTime activityEnd;
        if (isFixed) {
            FixedActivity fixed = (FixedActivity) activity;
            activityStart = LocalDateTime.of(fixed.getDate(), fixed.getStartTime());
            activityEnd = LocalDateTime.of(fixed.getDate(), fixed.getEndTime());
        } else {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            activityStart = LocalDateTime.of(flexible.getDate(),
                    flexible.hasAdoptedPlacement() ? flexible.getAdoptedStartTime() : flexible.getEarliestStart());
            activityEnd = LocalDateTime.of(flexible.getDate(),
                    flexible.hasAdoptedPlacement() ? flexible.getAdoptedEndTime() : flexible.getLatestEnd());
        }
        if (!intersects(activityStart, activityEnd, period.getStart(), period.getEnd())) {
            return;
        }

        long workloadMinutes = isFixed
                ? clip(activityStart, activityEnd, period.getStart(), period.getEnd())
                : ((FlexibleActivity) activity).getDurationMinutes();
        boolean eligible = isCompletionEligible(activity, now);
        acc.add(activity, isFixed, workloadMinutes, eligible);
    }

    /** Returns whether {@code [start, end)} intersects {@code [periodStart, periodEnd)}. */
    private static boolean intersects(LocalDateTime start, LocalDateTime end, LocalDateTime periodStart,
            LocalDateTime periodEnd) {
        return start.isBefore(periodEnd) && end.isAfter(periodStart);
    }

    /**
     * Clips {@code [start, end)} to {@code [periodStart, periodEnd)} and returns the resulting
     * duration in minutes (0 if there is no overlap). Implemented generically over raw
     * {@code LocalDateTime} boundaries - correct for a hypothetical interval spanning midnight
     * even though no activity in this codebase's data model can actually do so; see
     * {@code docs/tasks/v2/dashboard/IMPLEMENTATION_NOTES.md}. Package-private: exercised
     * directly by {@code DashboardServiceTest} rather than exposed as public API.
     *
     * @param start the interval's start
     * @param end the interval's end
     * @param periodStart the clipping period's inclusive start
     * @param periodEnd the clipping period's exclusive end
     * @return the clipped duration in minutes, never negative
     */
    static long clip(LocalDateTime start, LocalDateTime end, LocalDateTime periodStart, LocalDateTime periodEnd) {
        LocalDateTime clippedStart = start.isBefore(periodStart) ? periodStart : start;
        LocalDateTime clippedEnd = end.isAfter(periodEnd) ? periodEnd : end;
        return Math.max(0, Duration.between(clippedStart, clippedEnd).toMinutes());
    }

    private static boolean isCompletionEligible(Activity activity, LocalDateTime now) {
        LocalDateTime eligibleFrom;
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            eligibleFrom = LocalDateTime.of(fixed.getDate(), fixed.getEndTime());
        } else {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            eligibleFrom = LocalDateTime.of(flexible.getDate(),
                    flexible.hasAdoptedPlacement() ? flexible.getAdoptedEndTime() : flexible.getLatestEnd());
        }
        return !eligibleFrom.isAfter(now);
    }

    private static double roundHalfUpOneDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Mutable running totals used only while calculating one summary; never exposed. */
    private static final class Accumulator {
        private int totalActivityCount;
        private long workloadMinutes;
        private int fixedCount;
        private int flexibleCount;
        private int eligibleCount;
        private int completedEligibleCount;
        private int energyTotal;
        private int sensoryTotal;
        private int energyHighCount;
        private int sensoryHighCount;
        private int energyHighest;
        private int sensoryHighest;
        private final int[] energyDistribution = new int[RATING_SCALE];
        private final int[] sensoryDistribution = new int[RATING_SCALE];
        private final Map<ActivityCategory, Integer> categoryCounts = zeroFilledCategoryCounts();

        private void add(Activity activity, boolean isFixed, long activityWorkloadMinutes, boolean eligible) {
            totalActivityCount++;
            workloadMinutes += activityWorkloadMinutes;
            if (isFixed) {
                fixedCount++;
            } else {
                flexibleCount++;
            }
            categoryCounts.merge(activity.getCategory(), 1, Integer::sum);

            int energyRating = activity.getEnergyRating().getValue();
            int sensoryRating = activity.getSensoryRating().getValue();
            energyTotal += energyRating;
            sensoryTotal += sensoryRating;
            energyDistribution[energyRating - 1]++;
            sensoryDistribution[sensoryRating - 1]++;
            energyHighest = Math.max(energyHighest, energyRating);
            sensoryHighest = Math.max(sensoryHighest, sensoryRating);
            if (energyRating >= HIGH_RATING_THRESHOLD) {
                energyHighCount++;
            }
            if (sensoryRating >= HIGH_RATING_THRESHOLD) {
                sensoryHighCount++;
            }

            if (eligible) {
                eligibleCount++;
                if (activity.isComplete()) {
                    completedEligibleCount++;
                }
            }
        }

        private DashboardSummary toSummary(DashboardPeriod period) {
            long capacity = period.getCapacityMinutes();
            long rawBuffer = capacity - workloadMinutes;
            long nominalBuffer = Math.max(0, rawBuffer);
            long overload = Math.max(0, -rawBuffer);

            boolean hasData = totalActivityCount > 0;
            double energyAverage = hasData ? roundHalfUpOneDecimal((double) energyTotal / totalActivityCount) : 0.0;
            double sensoryAverage = hasData
                    ? roundHalfUpOneDecimal((double) sensoryTotal / totalActivityCount) : 0.0;
            RatingSummary energy = new RatingSummary(energyTotal, energyHighCount, hasData, energyAverage,
                    energyHighest, energyDistribution);
            RatingSummary sensory = new RatingSummary(sensoryTotal, sensoryHighCount, hasData, sensoryAverage,
                    sensoryHighest, sensoryDistribution);

            OptionalInt completionPercentage = eligibleCount > 0
                    ? OptionalInt.of((int) Math.round(completedEligibleCount * 100.0 / eligibleCount))
                    : OptionalInt.empty();

            return new DashboardSummary(period, totalActivityCount, workloadMinutes, nominalBuffer, overload,
                    energy, sensory, eligibleCount, completedEligibleCount, completionPercentage, fixedCount,
                    flexibleCount, categoryCounts);
        }
    }

    private static Map<ActivityCategory, Integer> zeroFilledCategoryCounts() {
        Map<ActivityCategory, Integer> counts = new EnumMap<>(ActivityCategory.class);
        for (ActivityCategory category : ActivityCategory.values()) {
            counts.put(category, 0);
        }
        return counts;
    }
}
