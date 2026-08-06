package seedu.unienable.logic.recommend;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int HIGH_RATING_THRESHOLD = 4;
    private static final int TOMATO_MINUTES = 50;

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
        List<RecommendedPlacement> placements = new ArrayList<>();
        List<Integer> unscheduled = new ArrayList<>();

        while (true) {
            CandidateSelection next = chooseNextActivity(remaining, commitments, preferences, now);
            if (next == null) {
                break;
            }
            remaining.remove(next.activity());
            commitments.add(Commitment.fromFlexible(next.activity(), next.bestStart()));
            placements.add(new RecommendedPlacement(next.activity().getId(), next.activity().getDescription(),
                    next.activity().getDate(), next.bestStart(),
                    next.bestStart().plusMinutes(next.activity().getDurationMinutes()),
                    shouldSuggestTomato(next.activity(), preferences)));
        }
        for (FlexibleActivity activity : remaining) {
            unscheduled.add(activity.getId());
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

    private static CandidateSelection chooseNextActivity(List<FlexibleActivity> remaining, List<Commitment> commitments,
            PreferenceProfile preferences, LocalDateTime now) {
        CandidateSelection best = null;
        for (FlexibleActivity activity : remaining) {
            List<LocalTime> validSlots = validSlots(activity, commitments, preferences.getMinimumBufferMinutes(), now);
            if (validSlots.isEmpty()) {
                continue;
            }
            LocalTime bestSlot = chooseBestSlot(activity, validSlots, commitments, preferences);
            CandidateSelection candidate = new CandidateSelection(activity, validSlots.size(), bestSlot);
            if (best == null || candidate.betterThan(best, commitments, preferences)) {
                best = candidate;
            }
        }
        return best;
    }

    private static List<LocalTime> validSlots(FlexibleActivity activity, List<Commitment> commitments, int buffer,
            LocalDateTime now) {
        List<LocalTime> valid = new ArrayList<>();
        LocalTime latestStart = activity.getLatestEnd().minusMinutes(activity.getDurationMinutes());
        LocalTime earliestStart = effectiveEarliestStart(activity, now);
        if (earliestStart == null) {
            return valid;
        }
        for (LocalTime candidate = earliestStart; !candidate.isAfter(latestStart);
                candidate = candidate.plusMinutes(1)) {
            if (fits(activity, candidate, commitments, buffer)) {
                valid.add(candidate);
            }
        }
        return valid;
    }

    /**
     * Returns the earliest candidate start time for activity, no earlier than its own declared
     * window and never before now - so a today window that has already fully or partly elapsed by
     * now is never proposed in the past. A date strictly before now's date has no valid start at
     * all (returns null); a date equal to now's date is clipped to now, rounded up to the next
     * whole minute if now carries seconds; a future date is unrestricted.
     *
     * @param activity the flexible activity being placed
     * @param now the current date and time
     * @return the earliest allowed start time, or null if the entire date has already elapsed
     */
    private static LocalTime effectiveEarliestStart(FlexibleActivity activity, LocalDateTime now) {
        LocalDateTime notBefore = ceilingToWholeMinute(now);
        LocalDate activityDate = activity.getDate();
        if (activityDate.isBefore(notBefore.toLocalDate())) {
            return null;
        }
        LocalTime earliest = activity.getEarliestStart();
        if (activityDate.isEqual(notBefore.toLocalDate()) && notBefore.toLocalTime().isAfter(earliest)) {
            return notBefore.toLocalTime();
        }
        return earliest;
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

    private static LocalTime chooseBestSlot(FlexibleActivity activity, List<LocalTime> validSlots,
            List<Commitment> commitments, PreferenceProfile preferences) {
        LocalTime best = validSlots.get(0);
        SlotScore bestScore = score(activity, best, commitments, preferences);
        for (int i = 1; i < validSlots.size(); i++) {
            LocalTime candidate = validSlots.get(i);
            SlotScore candidateScore = score(activity, candidate, commitments, preferences);
            if (candidateScore.betterThan(bestScore, candidate, best)) {
                best = candidate;
                bestScore = candidateScore;
            }
        }
        return best;
    }

    private static SlotScore score(FlexibleActivity activity, LocalTime slot, List<Commitment> commitments,
            PreferenceProfile preferences) {
        LocalTime end = slot.plusMinutes(activity.getDurationMinutes());
        long bufferPreservation = bufferPreservation(activity.getDate(), slot, end, commitments,
                preferences.getMinimumBufferMinutes());
        long energySpread = spreadDistance(activity.getDate(), slot, end, commitments, HIGH_RATING_THRESHOLD,
                true, activity.getEnergyRating().getValue() >= HIGH_RATING_THRESHOLD);
        long sensorySpread = spreadDistance(activity.getDate(), slot, end, commitments, HIGH_RATING_THRESHOLD,
                false, activity.getSensoryRating().getValue() >= HIGH_RATING_THRESHOLD);
        long preferencePenalty = preferredRangePenalty(slot, end, preferences);
        return new SlotScore(bufferPreservation, energySpread, sensorySpread, preferencePenalty);
    }

    private static long bufferPreservation(LocalDate date, LocalTime start, LocalTime end,
            List<Commitment> commitments, int buffer) {
        LocalTime previousEnd = null;
        LocalTime nextStart = null;
        for (Commitment commitment : commitments) {
            if (!commitment.date().equals(date)) {
                continue;
            }
            if (!commitment.endTime().isAfter(start)
                    && (previousEnd == null || commitment.endTime().isAfter(previousEnd))) {
                previousEnd = commitment.endTime();
            }
            if (!end.isAfter(commitment.startTime())
                    && (nextStart == null || commitment.startTime().isBefore(nextStart))) {
                nextStart = commitment.startTime();
            }
        }
        long before = previousEnd == null ? Long.MAX_VALUE / 4
                : Duration.between(previousEnd, start).toMinutes() - buffer;
        long after = nextStart == null ? Long.MAX_VALUE / 4
                : Duration.between(end, nextStart).toMinutes() - buffer;
        return Math.min(before, after);
    }

    private static long spreadDistance(LocalDate date, LocalTime start, LocalTime end,
            List<Commitment> commitments, int threshold, boolean energy, boolean applies) {
        if (!applies) {
            return 0;
        }
        long best = Long.MAX_VALUE / 4;
        for (Commitment commitment : commitments) {
            if (!commitment.date().equals(date)) {
                continue;
            }
            int rating = energy ? commitment.energyRating() : commitment.sensoryRating();
            if (rating < threshold) {
                continue;
            }
            if (!commitment.endTime().isAfter(start)) {
                best = Math.min(best, Duration.between(commitment.endTime(), start).toMinutes());
            } else if (!end.isAfter(commitment.startTime())) {
                best = Math.min(best, Duration.between(end, commitment.startTime()).toMinutes());
            } else {
                best = 0;
            }
        }
        return best == Long.MAX_VALUE / 4 ? Long.MAX_VALUE / 8 : best;
    }

    private static long preferredRangePenalty(LocalTime start, LocalTime end, PreferenceProfile preferences) {
        long penalty = 0;
        if (start.isBefore(preferences.getPreferredStart())) {
            penalty += Duration.between(start, preferences.getPreferredStart()).toMinutes();
        }
        if (end.isAfter(preferences.getPreferredEnd())) {
            penalty += Duration.between(preferences.getPreferredEnd(), end).toMinutes();
        }
        return penalty;
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

    private record Commitment(int activityId, LocalDate date, LocalTime startTime, LocalTime endTime,
            int energyRating, int sensoryRating) {
        private static Commitment fromFixed(FixedActivity activity) {
            return new Commitment(activity.getId(), activity.getDate(), activity.getStartTime(), activity.getEndTime(),
                    activity.getEnergyRating().getValue(), activity.getSensoryRating().getValue());
        }

        private static Commitment fromFlexible(FlexibleActivity activity, LocalTime startTime) {
            return new Commitment(activity.getId(), activity.getDate(), startTime,
                    startTime.plusMinutes(activity.getDurationMinutes()),
                    activity.getEnergyRating().getValue(), activity.getSensoryRating().getValue());
        }
    }

    private record CandidateSelection(FlexibleActivity activity, int validSlotCount, LocalTime bestStart) {
        private boolean betterThan(CandidateSelection other, List<Commitment> commitments,
                PreferenceProfile preferences) {
            if (validSlotCount != other.validSlotCount) {
                return validSlotCount < other.validSlotCount;
            }
            SlotScore thisScore = score(activity, bestStart, commitments, preferences);
            SlotScore otherScore = score(other.activity, other.bestStart, commitments, preferences);
            if (thisScore.betterThan(otherScore, bestStart, other.bestStart)) {
                return true;
            }
            if (otherScore.betterThan(thisScore, other.bestStart, bestStart)) {
                return false;
            }
            return activity.getId() < other.activity.getId();
        }
    }

    private record SlotScore(long bufferPreservation, long energySpread, long sensorySpread, long preferencePenalty) {
        private boolean betterThan(SlotScore other, LocalTime candidate, LocalTime current) {
            if (!candidate.equals(current)) {
                return candidate.isBefore(current);
            }
            if (bufferPreservation != other.bufferPreservation) {
                return bufferPreservation > other.bufferPreservation;
            }
            if (energySpread != other.energySpread) {
                return energySpread > other.energySpread;
            }
            if (sensorySpread != other.sensorySpread) {
                return sensorySpread > other.sensorySpread;
            }
            if (preferencePenalty != other.preferencePenalty) {
                return preferencePenalty < other.preferencePenalty;
            }
            return false;
        }
    }
}
