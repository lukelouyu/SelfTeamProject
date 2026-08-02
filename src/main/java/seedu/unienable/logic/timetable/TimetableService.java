package seedu.unienable.logic.timetable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.timetable.TimetableEntry;
import seedu.unienable.model.timetable.TimetableEntryType;
import seedu.unienable.model.timetable.TimetablePeriod;
import seedu.unienable.model.timetable.TimetableView;

/** Resolves timetable periods and builds deterministic, read-only timetable projections. */
public final class TimetableService {
    private TimetableService() {
    }

    /** Resolves a one-day period. */
    public static TimetablePeriod resolveDay(LocalDate date) {
        return new TimetablePeriod(date.toString(), date, date, false);
    }

    /** Resolves the Monday-Sunday week containing the supplied date. */
    public static TimetablePeriod resolveWeek(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new TimetablePeriod(monday + " to " + monday.plusDays(6), monday,
                monday.plusDays(6), true);
    }

    /** Resolves the Monday-Sunday week containing the injected current time. */
    public static TimetablePeriod resolveThisWeek(LocalDateTime now) {
        LocalDate monday = now.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new TimetablePeriod("This week", monday, monday.plusDays(6), true);
    }

    /** Builds one timetable from the manager without mutating it. */
    public static TimetableView build(ActivityManager activityManager, TimetablePeriod period) {
        return build(activityManager.getAll(), period);
    }

    static TimetableView build(List<Activity> activities, TimetablePeriod period) {
        List<FixedActivity> fixed = new ArrayList<>();
        List<FlexibleActivity> flexible = new ArrayList<>();
        for (Activity activity : activities) {
            if (!period.contains(activity.getDate())) {
                continue;
            }
            if (activity instanceof FixedActivity) {
                fixed.add((FixedActivity) activity);
            } else if (activity instanceof FlexibleActivity) {
                flexible.add((FlexibleActivity) activity);
            } else {
                throw new IllegalStateException("unknown activity type: " + activity.getClass());
            }
        }

        fixed.sort(Comparator.comparing(FixedActivity::getDate)
                .thenComparing(FixedActivity::getStartTime)
                .thenComparingInt(FixedActivity::getId));
        flexible.sort(Comparator.comparing(FlexibleActivity::getDate)
                .thenComparing(FlexibleActivity::getEarliestStart)
                .thenComparingInt(FlexibleActivity::getId));

        Set<Integer> overlapIds = findOverlapIds(fixed);
        List<TimetableEntry> fixedEntries = fixed.stream()
                .map(activity -> fromFixed(activity, overlapIds.contains(activity.getId())))
                .toList();
        List<TimetableEntry> flexibleEntries = flexible.stream()
                .map(TimetableService::fromFlexible)
                .toList();
        return new TimetableView(period, fixedEntries, flexibleEntries);
    }

    private static Set<Integer> findOverlapIds(List<FixedActivity> fixed) {
        Set<Integer> overlapIds = new HashSet<>();
        for (int i = 0; i < fixed.size(); i++) {
            FixedActivity first = fixed.get(i);
            for (int j = i + 1; j < fixed.size(); j++) {
                FixedActivity second = fixed.get(j);
                if (second.getDate().isAfter(first.getDate())) {
                    break;
                }
                if (overlaps(first, second)) {
                    overlapIds.add(first.getId());
                    overlapIds.add(second.getId());
                }
            }
        }
        return overlapIds;
    }

    private static boolean overlaps(FixedActivity first, FixedActivity second) {
        return first.getDate().equals(second.getDate())
                && first.getStartTime().isBefore(second.getEndTime())
                && second.getStartTime().isBefore(first.getEndTime());
    }

    private static TimetableEntry fromFixed(FixedActivity activity, boolean overlapping) {
        return new TimetableEntry(activity.getId(), activity.getDescription(), activity.getDate(),
                activity.getStartTime(), activity.getEndTime(), 0, TimetableEntryType.FIXED,
                overlapping, activity.isComplete(), activity.getCategory(),
                activity.getEnergyRating().getValue(), activity.getSensoryRating().getValue(),
                activity.getTopic(), activity.getNote());
    }

    private static TimetableEntry fromFlexible(FlexibleActivity activity) {
        return new TimetableEntry(activity.getId(), activity.getDescription(), activity.getDate(),
                activity.getEarliestStart(), activity.getLatestEnd(), activity.getDurationMinutes(),
                TimetableEntryType.UNSCHEDULED_FLEXIBLE, false, activity.isComplete(),
                activity.getCategory(), activity.getEnergyRating().getValue(),
                activity.getSensoryRating().getValue(), activity.getTopic(), activity.getNote());
    }
}
