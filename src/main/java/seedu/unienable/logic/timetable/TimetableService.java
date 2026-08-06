package seedu.unienable.logic.timetable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.RelativeDateResolver;
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
        LocalDate monday = RelativeDateResolver.mondayOfWeekContaining(date);
        return new TimetablePeriod(monday + " to " + monday.plusDays(6), monday,
                monday.plusDays(6), true);
    }

    /** Resolves the Monday-Sunday week containing the injected current time. */
    public static TimetablePeriod resolveThisWeek(LocalDateTime now) {
        LocalDate monday = RelativeDateResolver.mondayOfThisWeek(now);
        return new TimetablePeriod("This week", monday, monday.plusDays(6), true);
    }

    /** Resolves the Monday-Sunday week immediately following {@link #resolveThisWeek}. */
    public static TimetablePeriod resolveNextWeek(LocalDateTime now) {
        LocalDate monday = RelativeDateResolver.mondayOfNextWeek(now);
        return new TimetablePeriod("Next week", monday, monday.plusDays(6), true);
    }

    /** Builds one timetable from the manager without mutating it. */
    public static TimetableView build(ActivityManager activityManager, TimetablePeriod period) {
        return build(activityManager.getAll(), period);
    }

    public static TimetableView build(List<Activity> activities, TimetablePeriod period) {
        List<ScheduledEntrySource> scheduled = new ArrayList<>();
        List<FlexibleActivity> flexible = new ArrayList<>();
        for (Activity activity : activities) {
            if (!period.contains(activity.getDate())) {
                continue;
            }
            if (activity instanceof FixedActivity) {
                scheduled.add(ScheduledEntrySource.fromFixed((FixedActivity) activity));
            } else if (activity instanceof FlexibleActivity) {
                FlexibleActivity flexibleActivity = (FlexibleActivity) activity;
                if (flexibleActivity.hasAdoptedPlacement()) {
                    scheduled.add(ScheduledEntrySource.fromAdoptedFlexible(flexibleActivity));
                } else {
                    flexible.add(flexibleActivity);
                }
            } else {
                throw new IllegalStateException("unknown activity type: " + activity.getClass());
            }
        }

        scheduled.sort(Comparator.comparing(ScheduledEntrySource::date)
                .thenComparing(ScheduledEntrySource::startTime)
                .thenComparingInt(ScheduledEntrySource::id));
        flexible.sort(Comparator.comparing(FlexibleActivity::getDate)
                .thenComparing(FlexibleActivity::getEarliestStart)
                .thenComparingInt(FlexibleActivity::getId));

        Set<Integer> overlapIds = findOverlapIds(scheduled);
        List<TimetableEntry> fixedEntries = scheduled.stream()
                .map(activity -> activity.toEntry(overlapIds.contains(activity.id())))
                .toList();
        List<TimetableEntry> flexibleEntries = flexible.stream()
                .map(TimetableService::fromFlexible)
                .toList();
        return new TimetableView(period, fixedEntries, flexibleEntries);
    }

    private static Set<Integer> findOverlapIds(List<ScheduledEntrySource> scheduled) {
        Set<Integer> overlapIds = new HashSet<>();
        for (int i = 0; i < scheduled.size(); i++) {
            ScheduledEntrySource first = scheduled.get(i);
            for (int j = i + 1; j < scheduled.size(); j++) {
                ScheduledEntrySource second = scheduled.get(j);
                if (second.date().isAfter(first.date())) {
                    break;
                }
                if (overlaps(first, second)) {
                    overlapIds.add(first.id());
                    overlapIds.add(second.id());
                }
            }
        }
        return overlapIds;
    }

    private static boolean overlaps(ScheduledEntrySource first, ScheduledEntrySource second) {
        return first.date().equals(second.date())
                && first.startTime().isBefore(second.endTime())
                && second.startTime().isBefore(first.endTime());
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

    private record ScheduledEntrySource(int id, String description, LocalDate date, LocalTime startTime,
            LocalTime endTime, TimetableEntryType type, boolean complete,
            seedu.unienable.model.enums.ActivityCategory category, int energyRating, int sensoryRating,
            String topic, String note) {
        private static ScheduledEntrySource fromFixed(FixedActivity activity) {
            return new ScheduledEntrySource(activity.getId(), activity.getDescription(), activity.getDate(),
                    activity.getStartTime(), activity.getEndTime(), TimetableEntryType.FIXED,
                    activity.isComplete(), activity.getCategory(), activity.getEnergyRating().getValue(),
                    activity.getSensoryRating().getValue(), activity.getTopic(), activity.getNote());
        }

        private static ScheduledEntrySource fromAdoptedFlexible(FlexibleActivity activity) {
            return new ScheduledEntrySource(activity.getId(), activity.getDescription(), activity.getDate(),
                    activity.getAdoptedStartTime(), activity.getAdoptedEndTime(),
                    TimetableEntryType.ADOPTED_FLEXIBLE, activity.isComplete(), activity.getCategory(),
                    activity.getEnergyRating().getValue(), activity.getSensoryRating().getValue(),
                    activity.getTopic(), activity.getNote());
        }

        private TimetableEntry toEntry(boolean overlapping) {
            return new TimetableEntry(id, description, date, startTime, endTime, 0, type, overlapping,
                    complete, category, energyRating, sensoryRating, topic, note);
        }
    }
}
