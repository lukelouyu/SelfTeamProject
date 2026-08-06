package seedu.unienable.logic.recommend;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.dashboard.DashboardService;
import seedu.unienable.logic.timetable.TimetableService;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;
import seedu.unienable.model.recommend.RecommendationProposal;
import seedu.unienable.model.recommend.RecommendedPlacement;
import seedu.unienable.model.timetable.TimetablePeriod;

/** Builds deterministic recommendation proposals without mutating stored activities. */
public final class RecommendationService {
    private static final int TOMATO_MINUTES = 50;

    /**
     * Number of a single day's remaining flexible activities small enough to search exhaustively
     * (every ordering tried). {@code PERMUTATION_CAP}! orderings is the resulting worst case -
     * 8! = 40320, cheap even with a per-ordering placement pass - so real-world days (a handful of
     * flexible activities) always get a provably optimal schedule; only implausibly large single
     * days fall back to {@link #heuristicOrderings}.
     */
    private static final int PERMUTATION_CAP = 8;

    private RecommendationService() {
    }

    /** Builds a weekly proposal resolved from the injected current time. */
    public static RecommendationProposal recommendThisWeek(ActivityManager activityManager,
            PreferenceProfile preferences, LocalDateTime now) {
        TimetablePeriod timetablePeriod = TimetableService.resolveThisWeek(now);
        return build(activityManager, preferences, timetablePeriod,
                DashboardService.resolveThisWeek(now), now);
    }

    /**
     * Builds a weekly proposal for the Monday-Sunday week immediately following
     * {@link #recommendThisWeek}'s week. Every candidate date in this proposal is necessarily
     * after now's date, so the not-before-now clamp in {@link #effectiveEarliestStart} never
     * restricts anything here - it is still passed through for the same reason every other
     * builder receives it, rather than special-cased away.
     */
    public static RecommendationProposal recommendNextWeek(ActivityManager activityManager,
            PreferenceProfile preferences, LocalDateTime now) {
        TimetablePeriod timetablePeriod = TimetableService.resolveNextWeek(now);
        return build(activityManager, preferences, timetablePeriod,
                DashboardService.resolveNextWeek(now), now);
    }

    /**
     * Builds a one-day proposal for the specified date.
     *
     * @param now the current date and time, threaded down from the application's single
     *     {@code now} seam so that a candidate start on today's date is never before now,
     *     rather than read via a new {@code LocalDateTime.now()} call
     */
    public static RecommendationProposal recommendDate(ActivityManager activityManager,
            PreferenceProfile preferences, LocalDate date, LocalDateTime now) {
        TimetablePeriod timetablePeriod = TimetableService.resolveDay(date);
        return build(activityManager, preferences, timetablePeriod,
                DashboardService.resolveDate(date), now);
    }

    /**
     * Returns whether any placement already proposed in proposal has a start date/time that now
     * has passed - used to reject adopting a stale proposal generated earlier in the session
     * rather than silently persisting a placement that can no longer be acted on.
     *
     * @param proposal the proposal to check
     * @param now the current date and time
     * @return true if now is strictly after any placement's proposed start
     */
    public static boolean hasElapsedPlacement(RecommendationProposal proposal, LocalDateTime now) {
        for (RecommendedPlacement placement : proposal.getPlacements()) {
            if (now.isAfter(placement.date().atTime(placement.startTime()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether any placement already proposed in proposal falls outside preferences'
     * current preferred daily start/end - used to reject adopting a proposal generated under a
     * profile that has since been changed (via {@code preference set}/{@code preference reset})
     * to a boundary the proposal's placements were never checked against. Preferred start/end are
     * a hard scheduling boundary, never a soft one (see {@link #withinPreferredRange}), so a
     * proposal that no longer satisfies the current profile must never be silently adopted as-is.
     *
     * @param proposal the proposal to check
     * @param preferences the current preference profile to check every placement against
     * @return true if any placement starts before preferences' preferred start or ends after its
     *     preferred end
     */
    public static boolean hasOutOfPreferredRangePlacement(RecommendationProposal proposal,
            PreferenceProfile preferences) {
        for (RecommendedPlacement placement : proposal.getPlacements()) {
            if (!withinPreferredRange(placement.startTime(), placement.endTime(), preferences)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The final boundary guard: true only if start is not before preferences' preferred start and
     * end is not after its preferred end. {@link #earliestValidSlot} already restricts every
     * candidate it ever searches to this exact range, so in normal operation this can never
     * evaluate false - it exists as defense-in-depth so that a future change to slot generation
     * cannot silently reintroduce an out-of-range placement without also being caught here, at the
     * point placements are committed to a proposal ({@link #build}) or adopted
     * ({@link #hasOutOfPreferredRangePlacement}). Scoring inside {@link DaySchedule#betterThan} -
     * count, duration, window tightness, aggregate earliest-placement, ID - never overrides this;
     * it only ever ranks among candidates that already satisfy it.
     */
    private static boolean withinPreferredRange(LocalTime start, LocalTime end, PreferenceProfile preferences) {
        return !start.isBefore(preferences.getPreferredStart()) && !end.isAfter(preferences.getPreferredEnd());
    }

    /** Returns preview activities with proposal placements applied to flexible activities only. */
    public static List<Activity> applyPreview(List<Activity> activities, RecommendationProposal proposal) {
        Map<Integer, LocalTime> startsById = new HashMap<>();
        for (RecommendedPlacement placement : proposal.getPlacements()) {
            startsById.put(placement.activityId(), placement.startTime());
        }
        List<Activity> copies = new ArrayList<>(activities.size());
        for (Activity activity : activities) {
            copies.add(copyOf(activity, startsById.get(activity.getId())));
        }
        return copies;
    }

    private static RecommendationProposal build(ActivityManager activityManager, PreferenceProfile preferences,
            TimetablePeriod timetablePeriod, seedu.unienable.model.dashboard.DashboardPeriod dashboardPeriod,
            LocalDateTime now) {
        List<Activity> all = activityManager.getAll();
        List<Commitment> commitments = existingCommitments(all);
        List<FlexibleActivity> remaining = eligibleFlexible(all, timetablePeriod);
        Map<LocalDate, List<FlexibleActivity>> byDate = new LinkedHashMap<>();
        for (FlexibleActivity activity : remaining) {
            byDate.computeIfAbsent(activity.getDate(), date -> new ArrayList<>()).add(activity);
        }
        List<LocalDate> dates = new ArrayList<>(byDate.keySet());
        dates.sort(Comparator.naturalOrder());

        List<RecommendedPlacement> placements = new ArrayList<>();
        List<Integer> unscheduled = new ArrayList<>();
        for (LocalDate date : dates) {
            List<FlexibleActivity> dayActivities = byDate.get(date);
            DaySchedule daySchedule = optimizeDay(dayActivities, commitments, preferences, now);
            List<Integer> guardRejectedIds = new ArrayList<>();
            for (ScheduledItem item : daySchedule.items()) {
                LocalTime end = item.start().plusMinutes(item.activity().getDurationMinutes());
                if (!withinPreferredRange(item.start(), end, preferences)) {
                    // Final guard: earliestValidSlot's own search range already makes this
                    // unreachable in normal operation (see withinPreferredRange's javadoc), but a
                    // slot that somehow fails it is discarded here rather than ever reaching the
                    // proposal, per the hard-boundary design rule.
                    guardRejectedIds.add(item.activity().getId());
                    continue;
                }
                commitments.add(Commitment.fromFlexible(item.activity(), item.start()));
                placements.add(new RecommendedPlacement(item.activity().getId(), item.activity().getDescription(),
                        item.activity().getDate(), item.start(), end,
                        shouldSuggestTomato(item.activity(), preferences)));
            }
            for (FlexibleActivity activity : dayActivities) {
                if (!daySchedule.containsId(activity.getId()) || guardRejectedIds.contains(activity.getId())) {
                    unscheduled.add(activity.getId());
                }
            }
        }
        unscheduled.sort(Integer::compareTo);
        placements.sort(Comparator.comparing(RecommendedPlacement::date)
                .thenComparing(RecommendedPlacement::startTime)
                .thenComparingInt(RecommendedPlacement::activityId));
        return new RecommendationProposal(timetablePeriod, dashboardPeriod, placements, unscheduled);
    }

    private static List<Commitment> existingCommitments(List<Activity> activities) {
        List<Commitment> commitments = new ArrayList<>();
        for (Activity activity : activities) {
            if (activity instanceof FixedActivity) {
                commitments.add(Commitment.fromFixed((FixedActivity) activity));
            } else if (activity instanceof FlexibleActivity) {
                FlexibleActivity flexible = (FlexibleActivity) activity;
                if (flexible.hasAdoptedPlacement()) {
                    commitments.add(Commitment.fromFlexible(flexible, flexible.getAdoptedStartTime()));
                }
            }
        }
        commitments.sort(Comparator.comparing(Commitment::date)
                .thenComparing(Commitment::startTime)
                .thenComparingInt(Commitment::activityId));
        return commitments;
    }

    private static List<FlexibleActivity> eligibleFlexible(List<Activity> activities, TimetablePeriod period) {
        List<FlexibleActivity> flexible = new ArrayList<>();
        for (Activity activity : activities) {
            if (!(activity instanceof FlexibleActivity)) {
                continue;
            }
            FlexibleActivity candidate = (FlexibleActivity) activity;
            if (candidate.isComplete() || candidate.hasAdoptedPlacement() || !period.contains(candidate.getDate())) {
                continue;
            }
            flexible.add(candidate);
        }
        return flexible;
    }

    /**
     * Finds the best whole-day placement for date's remaining flexible activities, searching
     * combinations rather than placing each activity independently - a purely greedy,
     * activity-by-activity search can leave activities unscheduled even when a valid schedule
     * containing them exists (e.g. a short activity greedily claims a slot that was the only way
     * to fit a longer one later the same day). Every candidate ordering is evaluated by placing
     * each activity, in that order, at its own earliest feasible slot given the activities already
     * placed earlier in the ordering; the best-scoring resulting {@link DaySchedule} across all
     * orderings tried is returned. See {@link DaySchedule#betterThan} for the scoring rule.
     */
    private static DaySchedule optimizeDay(List<FlexibleActivity> dayActivities, List<Commitment> commitments,
            PreferenceProfile preferences, LocalDateTime now) {
        List<FlexibleActivity> candidates = new ArrayList<>();
        for (FlexibleActivity activity : dayActivities) {
            if (earliestValidSlot(activity, commitments, preferences, now).isPresent()) {
                candidates.add(activity);
            }
        }
        if (candidates.isEmpty()) {
            return DaySchedule.empty();
        }

        DaySchedule best = null;
        if (candidates.size() <= PERMUTATION_CAP) {
            List<List<FlexibleActivity>> orderings = new ArrayList<>();
            permute(candidates, 0, orderings);
            for (List<FlexibleActivity> ordering : orderings) {
                DaySchedule attempt = placeInOrder(ordering, commitments, preferences, now);
                if (best == null || attempt.betterThan(best)) {
                    best = attempt;
                }
            }
        } else {
            for (List<FlexibleActivity> ordering : heuristicOrderings(candidates)) {
                DaySchedule attempt = placeInOrder(ordering, commitments, preferences, now);
                if (best == null || attempt.betterThan(best)) {
                    best = attempt;
                }
            }
        }
        return best;
    }

    /** Fills orderings with every permutation of candidates via in-place backtracking. */
    private static void permute(List<FlexibleActivity> candidates, int index,
            List<List<FlexibleActivity>> orderings) {
        if (index == candidates.size()) {
            orderings.add(new ArrayList<>(candidates));
            return;
        }
        for (int i = index; i < candidates.size(); i++) {
            Collections.swap(candidates, index, i);
            permute(candidates, index + 1, orderings);
            Collections.swap(candidates, index, i);
        }
    }

    /**
     * A small, fixed set of deterministic orderings used in place of exhaustive permutation once a
     * single day has more remaining flexible activities than {@link #PERMUTATION_CAP} - factorial
     * search is no longer practical, so this trades a guaranteed-optimal result for a bounded-cost
     * search over orderings that tend to perform well: tightest window first (least slack to place
     * elsewhere), longest duration first, earliest own-window first, and stable ID order.
     */
    private static List<List<FlexibleActivity>> heuristicOrderings(List<FlexibleActivity> candidates) {
        List<List<FlexibleActivity>> orderings = new ArrayList<>();
        orderings.add(sortedCopy(candidates, Comparator.comparingLong(RecommendationService::slackMinutes)
                .thenComparingInt(Activity::getId)));
        orderings.add(sortedCopy(candidates, Comparator.comparingInt(FlexibleActivity::getDurationMinutes)
                .reversed().thenComparingInt(Activity::getId)));
        orderings.add(sortedCopy(candidates, Comparator.comparing(FlexibleActivity::getEarliestStart)
                .thenComparingInt(Activity::getId)));
        orderings.add(sortedCopy(candidates, Comparator.comparingInt(Activity::getId)));
        return orderings;
    }

    private static List<FlexibleActivity> sortedCopy(List<FlexibleActivity> source,
            Comparator<FlexibleActivity> order) {
        List<FlexibleActivity> copy = new ArrayList<>(source);
        copy.sort(order);
        return copy;
    }

    /**
     * Places ordering's activities greedily in that exact sequence, each at its own earliest
     * feasible slot given commitments plus every earlier activity in this same ordering that was
     * successfully placed; an activity with no feasible slot at its turn is skipped (left out of
     * the returned schedule) rather than aborting the rest of the ordering.
     */
    private static DaySchedule placeInOrder(List<FlexibleActivity> ordering, List<Commitment> baseCommitments,
            PreferenceProfile preferences, LocalDateTime now) {
        List<Commitment> working = new ArrayList<>(baseCommitments);
        List<ScheduledItem> items = new ArrayList<>();
        for (FlexibleActivity activity : ordering) {
            Optional<LocalTime> start = earliestValidSlot(activity, working, preferences, now);
            if (start.isEmpty()) {
                continue;
            }
            items.add(new ScheduledItem(activity, start.get()));
            working.add(Commitment.fromFlexible(activity, start.get()));
        }
        return new DaySchedule(items);
    }

    /**
     * Returns the earliest start minute that fits inside the intersection of activity's own window
     * and preferences' preferred daily start/end - the preferred range is a hard boundary on the
     * searchable range, not merely a tie-break preference, so a candidate placed entirely outside
     * it is never proposed when a compliant slot exists. If the intersection is empty (or too
     * narrow for activity's duration to fit inside it at all), or no minute within it clears every
     * commitment's buffer, returns empty rather than risking a wrapped-past-midnight
     * {@code LocalTime} range or a non-existent slot. This is the slot-generation stage of the
     * boundary-enforcement pipeline: the loop below never even considers a candidate before
     * {@code effectiveEarliestStart} or after {@code effectiveLatestEnd}, so every value this
     * method can return already satisfies {@link #withinPreferredRange} - {@link #build} still
     * re-checks it as a final guard before committing to the proposal (defense-in-depth, not
     * because this stage is expected to fail).
     */
    private static Optional<LocalTime> earliestValidSlot(FlexibleActivity activity, List<Commitment> commitments,
            PreferenceProfile preferences, LocalDateTime now) {
        LocalTime earliestStart = effectiveEarliestStart(activity, preferences, now);
        if (earliestStart == null) {
            return Optional.empty();
        }
        LocalTime effectiveEnd = effectiveLatestEnd(activity, preferences);
        int durationMinutes = activity.getDurationMinutes();
        if (Duration.between(earliestStart, effectiveEnd).toMinutes() < durationMinutes) {
            return Optional.empty();
        }
        LocalTime latestStart = effectiveEnd.minusMinutes(durationMinutes);
        for (LocalTime candidate = earliestStart; !candidate.isAfter(latestStart);
                candidate = candidate.plusMinutes(1)) {
            if (fits(activity, candidate, commitments, preferences.getMinimumBufferMinutes())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** An activity's intrinsic slack: how much wider its own window is than its duration. */
    private static long slackMinutes(FlexibleActivity activity) {
        return Duration.between(activity.getEarliestStart(), activity.getLatestEnd()).toMinutes()
                - activity.getDurationMinutes();
    }

    /**
     * Returns the earliest candidate start time for activity: no earlier than the later of its
     * own declared window and preferences' preferred daily start, and never before now - so a
     * today window that has already fully or partly elapsed by now is never proposed in the past.
     * A date strictly before now's date has no valid start at all (returns null); a date equal to
     * now's date is clipped to now, rounded up to the next whole minute if now carries seconds; a
     * future date is unrestricted by now (though still restricted by the preferred start).
     *
     * @param activity the flexible activity being placed
     * @param preferences the profile whose preferred daily start bounds the search
     * @param now the current date and time
     * @return the earliest allowed start time, or null if the entire date has already elapsed
     */
    private static LocalTime effectiveEarliestStart(FlexibleActivity activity, PreferenceProfile preferences,
            LocalDateTime now) {
        LocalDateTime notBefore = ceilingToWholeMinute(now);
        LocalDate activityDate = activity.getDate();
        if (activityDate.isBefore(notBefore.toLocalDate())) {
            return null;
        }
        LocalTime earliest = laterOf(activity.getEarliestStart(), preferences.getPreferredStart());
        if (activityDate.isEqual(notBefore.toLocalDate()) && notBefore.toLocalTime().isAfter(earliest)) {
            return notBefore.toLocalTime();
        }
        return earliest;
    }

    /** Returns the earlier of activity's own declared window end and preferences' preferred daily end. */
    private static LocalTime effectiveLatestEnd(FlexibleActivity activity, PreferenceProfile preferences) {
        return earlierOf(activity.getLatestEnd(), preferences.getPreferredEnd());
    }

    private static LocalTime laterOf(LocalTime first, LocalTime second) {
        return first.isAfter(second) ? first : second;
    }

    private static LocalTime earlierOf(LocalTime first, LocalTime second) {
        return first.isBefore(second) ? first : second;
    }

    private static LocalDateTime ceilingToWholeMinute(LocalDateTime now) {
        return now.getSecond() == 0 && now.getNano() == 0 ? now : now.plusMinutes(1).withSecond(0).withNano(0);
    }

    private static boolean fits(FlexibleActivity activity, LocalTime start, List<Commitment> commitments, int buffer) {
        LocalTime end = start.plusMinutes(activity.getDurationMinutes());
        if (start.isBefore(activity.getEarliestStart()) || end.isAfter(activity.getLatestEnd())) {
            return false;
        }
        for (Commitment commitment : commitments) {
            if (!commitment.date().equals(activity.getDate())) {
                continue;
            }
            if (commitment.startTime().isBefore(end) && start.isBefore(commitment.endTime())) {
                return false;
            }
            if (!commitment.endTime().isAfter(start)) {
                long gap = Duration.between(commitment.endTime(), start).toMinutes();
                if (gap < buffer) {
                    return false;
                }
            } else if (!end.isAfter(commitment.startTime())) {
                long gap = Duration.between(end, commitment.startTime()).toMinutes();
                if (gap < buffer) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean shouldSuggestTomato(FlexibleActivity activity, PreferenceProfile preferences) {
        if (preferences.getTomatoSuggestion() != TomatoSuggestion.ON) {
            return false;
        }
        String topic = activity.getTopic() == null ? "" : activity.getTopic().toLowerCase();
        String description = activity.getDescription().toLowerCase();
        boolean studyLike = description.contains("study") || description.contains("revision")
                || description.contains("reading") || description.contains("assignment")
                || topic.contains("study") || topic.contains("lab") || topic.contains("tutorial")
                || activity.getCategory().name().equals("ACADEMIC");
        return studyLike && activity.getDurationMinutes() >= TOMATO_MINUTES;
    }

    private static Activity copyOf(Activity activity, LocalTime proposedStart) {
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            Activity copy = new FixedActivity(fixed.getId(), fixed.getDescription(), fixed.getCategory(),
                    fixed.getDate(), fixed.getStartTime(), fixed.getEndTime(), fixed.getEnergyRating(),
                    fixed.getSensoryRating(), fixed.getTopic(), fixed.getNote());
            if (fixed.isComplete()) {
                copy.mark();
            }
            return copy;
        }
        FlexibleActivity flexible = (FlexibleActivity) activity;
        LocalTime adopted = proposedStart != null ? proposedStart : flexible.getAdoptedStartTime();
        Activity copy = new FlexibleActivity(flexible.getId(), flexible.getDescription(), flexible.getCategory(),
                flexible.getDate(), flexible.getEarliestStart(), flexible.getLatestEnd(),
                flexible.getDurationMinutes(), flexible.getEnergyRating(), flexible.getSensoryRating(),
                flexible.getTopic(), flexible.getNote(), adopted);
        if (flexible.isComplete()) {
            copy.mark();
        }
        return copy;
    }

    private record Commitment(int activityId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        private static Commitment fromFixed(FixedActivity activity) {
            return new Commitment(activity.getId(), activity.getDate(), activity.getStartTime(),
                    activity.getEndTime());
        }

        private static Commitment fromFlexible(FlexibleActivity activity, LocalTime startTime) {
            return new Commitment(activity.getId(), activity.getDate(), startTime,
                    startTime.plusMinutes(activity.getDurationMinutes()));
        }
    }

    private record ScheduledItem(FlexibleActivity activity, LocalTime start) {
    }

    /**
     * One candidate whole-day outcome: which of a day's remaining flexible activities got
     * scheduled, and at what times. Compared against alternative outcomes via {@link #betterThan},
     * which implements the fix's priority order: (1) more scheduled activities, (2) more total
     * scheduled duration, (3) a lower total intrinsic slack among the scheduled activities (prefer
     * activities whose own window is tighter - they have less room to be placed some other way),
     * (4) earlier placements in aggregate, (5) the lower sorted set of activity IDs as a fully
     * deterministic final tie-break.
     */
    private record DaySchedule(List<ScheduledItem> items) {
        private static DaySchedule empty() {
            return new DaySchedule(List.of());
        }

        private boolean containsId(int activityId) {
            for (ScheduledItem item : items) {
                if (item.activity().getId() == activityId) {
                    return true;
                }
            }
            return false;
        }

        private int totalDurationMinutes() {
            int total = 0;
            for (ScheduledItem item : items) {
                total += item.activity().getDurationMinutes();
            }
            return total;
        }

        private long totalSlackMinutes() {
            long total = 0;
            for (ScheduledItem item : items) {
                total += slackMinutes(item.activity());
            }
            return total;
        }

        private long totalStartMinutesOfDay() {
            long total = 0;
            for (ScheduledItem item : items) {
                total += item.start().toSecondOfDay() / 60;
            }
            return total;
        }

        private List<Integer> sortedIds() {
            List<Integer> ids = new ArrayList<>();
            for (ScheduledItem item : items) {
                ids.add(item.activity().getId());
            }
            ids.sort(Integer::compareTo);
            return ids;
        }

        private boolean betterThan(DaySchedule other) {
            if (items.size() != other.items.size()) {
                return items.size() > other.items.size();
            }
            if (totalDurationMinutes() != other.totalDurationMinutes()) {
                return totalDurationMinutes() > other.totalDurationMinutes();
            }
            if (totalSlackMinutes() != other.totalSlackMinutes()) {
                return totalSlackMinutes() < other.totalSlackMinutes();
            }
            if (totalStartMinutesOfDay() != other.totalStartMinutesOfDay()) {
                return totalStartMinutesOfDay() < other.totalStartMinutesOfDay();
            }
            List<Integer> theseIds = sortedIds();
            List<Integer> otherIds = other.sortedIds();
            for (int i = 0; i < theseIds.size(); i++) {
                int compare = theseIds.get(i).compareTo(otherIds.get(i));
                if (compare != 0) {
                    return compare < 0;
                }
            }
            return false;
        }
    }
}
