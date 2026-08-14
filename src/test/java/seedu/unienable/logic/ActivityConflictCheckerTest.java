package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;

class ActivityConflictCheckerTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 15);
    private static final LocalDate OTHER_DATE = LocalDate.of(2026, 8, 16);
    private static final int NO_EXCLUDED_ID = -1;

    private final ActivityConflictChecker checker = new ActivityConflictChecker();

    private static FixedActivity fixed(int id, String description, LocalDate date,
            LocalTime start, LocalTime end) throws Exception {
        return new FixedActivity(id, description, ActivityCategory.ACADEMIC, date, start, end,
                EnergyRating.of(3), SensoryRating.of(3), null, null);
    }

    private static FlexibleActivity flexible(int id, String description, LocalDate date,
            LocalTime earliestStart, LocalTime latestEnd) throws Exception {
        return new FlexibleActivity(id, description, ActivityCategory.ACADEMIC, date,
                earliestStart, latestEnd, 90, EnergyRating.of(3), SensoryRating.of(3), null, null);
    }

    private static FlexibleActivity adoptedFlexible(int id, String description, LocalDate date,
            LocalTime earliestStart, LocalTime latestEnd, int durationMinutes, LocalTime adoptedStartTime)
            throws Exception {
        return new FlexibleActivity(id, description, ActivityCategory.ACADEMIC, date,
                earliestStart, latestEnd, durationMinutes, EnergyRating.of(3), SensoryRating.of(3), null, null,
                adoptedStartTime);
    }

    @Test
    public void checkNoConflicts_exactFixedDuplicate_throwsExactMessage() throws Exception {
        FixedActivity existing = fixed(1, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity candidate = fixed(2, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));

        assertEquals("An identical activity already exists.", exception.getMessage());
    }

    @Test
    public void checkNoConflicts_exactFlexibleDuplicate_throwsExactMessage() throws Exception {
        FlexibleActivity existing = flexible(1, "Assignment", DATE,
                LocalTime.of(10, 0), LocalTime.of(18, 0));
        FlexibleActivity candidate = flexible(2, "Assignment", DATE,
                LocalTime.of(10, 0), LocalTime.of(18, 0));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));

        assertEquals("An identical activity already exists.", exception.getMessage());
    }

    @Test
    public void checkNoConflicts_fixedCandidateAgainstFlexibleExisting_isAccepted() throws Exception {
        FlexibleActivity existing = flexible(1, "Shared name", DATE,
                LocalTime.of(9, 0), LocalTime.of(12, 0));
        FixedActivity candidate = fixed(2, "Shared name", DATE,
                LocalTime.of(10, 0), LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_flexibleCandidateAgainstFixedExisting_isAccepted() throws Exception {
        FixedActivity existing = fixed(1, "Shared name", DATE,
                LocalTime.of(10, 0), LocalTime.of(11, 0));
        FlexibleActivity candidate = flexible(2, "Shared name", DATE,
                LocalTime.of(9, 0), LocalTime.of(12, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_overlappingFlexibleActivities_areAccepted() throws Exception {
        FlexibleActivity existing = flexible(1, "First", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0));
        FlexibleActivity candidate = flexible(2, "Second", DATE,
                LocalTime.of(10, 0), LocalTime.of(17, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_descriptionComparison_isCaseSensitive() throws Exception {
        FlexibleActivity existing = flexible(1, "Assignment", DATE,
                LocalTime.of(10, 0), LocalTime.of(18, 0));
        FlexibleActivity candidate = flexible(2, "assignment", DATE,
                LocalTime.of(10, 0), LocalTime.of(18, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_duplicateIgnoresMetadata_throwsDuplicate() throws Exception {
        FixedActivity existing = new FixedActivity(1, "Lecture", ActivityCategory.ACADEMIC, DATE,
                LocalTime.of(9, 0), LocalTime.of(11, 0), EnergyRating.of(1), SensoryRating.of(1),
                "CS2113", "Original note");
        FixedActivity candidate = new FixedActivity(2, "Lecture", ActivityCategory.CCA, DATE,
                LocalTime.of(9, 0), LocalTime.of(11, 0), EnergyRating.of(5), SensoryRating.of(5),
                "Club", "Different note");

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));
    }

    @Test
    public void checkNoConflicts_sameScheduleOnDifferentDate_isAccepted() throws Exception {
        FixedActivity existing = fixed(1, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity candidate = fixed(2, "Lecture", OTHER_DATE,
                LocalTime.of(9, 0), LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_candidateStartingAtExistingEnd_isAccepted() throws Exception {
        FixedActivity existing = fixed(1, "First", DATE, LocalTime.of(9, 0), LocalTime.of(10, 0));
        FixedActivity candidate = fixed(2, "Second", DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_candidateEndingAtExistingStart_isAccepted() throws Exception {
        FixedActivity existing = fixed(1, "Second", DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));
        FixedActivity candidate = fixed(2, "First", DATE, LocalTime.of(9, 0), LocalTime.of(10, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_equalFixedIntervalsWithDifferentDescriptions_reportsOverlap() throws Exception {
        FixedActivity existing = fixed(1, "Existing", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity candidate = fixed(2, "Candidate", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));

        assertEquals("This timing overlaps activity [1], Existing (09:00 -> 11:00).", exception.getMessage());
    }

    @Test
    public void checkNoConflicts_candidateContainingExisting_reportsOverlap() throws Exception {
        FixedActivity existing = fixed(1, "Existing", DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));
        FixedActivity candidate = fixed(2, "Candidate", DATE, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));
    }

    @Test
    public void checkNoConflicts_existingContainingCandidate_reportsOverlap() throws Exception {
        FixedActivity existing = fixed(1, "Existing", DATE, LocalTime.of(9, 0), LocalTime.of(12, 0));
        FixedActivity candidate = fixed(2, "Candidate", DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));
    }

    @Test
    public void checkNoConflicts_completedCandidateStillConflicts() throws Exception {
        FixedActivity existing = fixed(1, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity candidate = fixed(2, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        candidate.mark();

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));
    }

    @Test
    public void checkNoConflicts_completedExistingActivityStillConflicts() throws Exception {
        FixedActivity existing = fixed(1, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        existing.mark();
        FixedActivity candidate = fixed(2, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));
    }

    @Test
    public void checkNoConflicts_excludedNumericId_skipsEveryMatchingEntry() throws Exception {
        FixedActivity first = fixed(1, "First", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity second = fixed(1, "Second", DATE, LocalTime.of(10, 0), LocalTime.of(12, 0));
        FixedActivity candidate = fixed(1, "First", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, 1, List.of(first, second));
    }

    @Test
    public void checkNoConflicts_excludedIdAbsent_doesNotSuppressConflict() throws Exception {
        FixedActivity existing = fixed(1, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity candidate = fixed(2, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, 999, List.of(existing)));
    }

    @Test
    public void checkNoConflicts_emptyComparisonList_isAccepted() throws Exception {
        FixedActivity candidate = fixed(1, "Lecture", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of());
    }

    @Test
    public void checkNoConflicts_duplicateAfterEarlierOverlap_reportsDuplicate() throws Exception {
        FixedActivity overlapping = fixed(1, "Different", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity duplicate = fixed(2, "Candidate", DATE, LocalTime.of(9, 30), LocalTime.of(10, 30));
        FixedActivity candidate = fixed(3, "Candidate", DATE, LocalTime.of(9, 30), LocalTime.of(10, 30));
        List<Activity> existing = List.of(overlapping, duplicate);

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, existing));

        assertEquals("An identical activity already exists.", exception.getMessage());
    }

    @Test
    public void checkNoConflicts_multipleOverlaps_reportsFirstInListOrder() throws Exception {
        FixedActivity first = fixed(7, "First", DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity second = fixed(3, "Second", DATE, LocalTime.of(9, 30), LocalTime.of(11, 30));
        FixedActivity candidate = fixed(8, "Candidate", DATE, LocalTime.of(10, 0), LocalTime.of(10, 30));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(first, second)));

        assertEquals("This timing overlaps activity [7], First (09:00 -> 11:00).", exception.getMessage());
    }

    // Bug C regression coverage: an adopted FlexibleActivity is a real scheduled commitment and
    // must be treated as an occupied interval, just like a FixedActivity; an unadopted one remains
    // a mere window and must never block anything.

    @Test
    public void scheduledInterval_unadoptedFlexible_doesNotBlockNormalWindowOverlap() throws Exception {
        FlexibleActivity existing = flexible(1, "Existing window", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0));
        FixedActivity candidate = fixed(2, "New fixed", DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void scheduledInterval_adoptedFlexible_blocksRealOverlap() throws Exception {
        FlexibleActivity existing = adoptedFlexible(1, "Study", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(10, 0));
        FixedActivity candidate = fixed(2, "Meeting", DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));

        assertEquals("This timing overlaps activity [1], Study (10:00 -> 11:00).", exception.getMessage());
    }

    @Test
    public void addFixed_overlappingAdoptedFlexible_isRejected() throws Exception {
        FlexibleActivity existing = adoptedFlexible(1, "Study", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(10, 0));
        FixedActivity candidate = fixed(2, "Meeting", DATE, LocalTime.of(10, 30), LocalTime.of(11, 30));

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));
    }

    @Test
    public void editFixed_overlappingAdoptedFlexible_isRejected() throws Exception {
        // Same as addFixed_overlappingAdoptedFlexible_isRejected, but with the candidate excluding
        // its own prior ID, mirroring how EditCommandParser/ActivityManager.replace() call this.
        FlexibleActivity existing = adoptedFlexible(1, "Study", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(10, 0));
        FixedActivity original = fixed(2, "Meeting", DATE, LocalTime.of(13, 0), LocalTime.of(14, 0));
        FixedActivity candidate = fixed(2, "Meeting", DATE, LocalTime.of(10, 30), LocalTime.of(11, 30));

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, 2, List.of(existing, original)));
    }

    @Test
    public void checkNoConflicts_fixedContainedInsideAdoptedFlexible_reportsOverlap() throws Exception {
        FlexibleActivity existing = adoptedFlexible(1, "Study", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 180, LocalTime.of(9, 0));
        FixedActivity candidate = fixed(2, "Meeting", DATE, LocalTime.of(10, 0), LocalTime.of(10, 30));

        assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));
    }

    @Test
    public void checkNoConflicts_fixedAdjacentToAdoptedFlexible_isAccepted() throws Exception {
        FlexibleActivity existing = adoptedFlexible(1, "Study", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(9, 0));
        FixedActivity candidate = fixed(2, "Meeting", DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }

    @Test
    public void checkNoConflicts_twoAdoptedFlexiblesOverlap_reportsOverlap() throws Exception {
        FlexibleActivity existing = adoptedFlexible(1, "Study", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(10, 0));
        FlexibleActivity candidate = adoptedFlexible(2, "Jogging", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(10, 30));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing)));

        assertEquals("This timing overlaps activity [1], Study (10:00 -> 11:00).", exception.getMessage());
    }

    @Test
    public void checkNoConflicts_twoAdoptedFlexiblesNotOverlapping_isAccepted() throws Exception {
        FlexibleActivity existing = adoptedFlexible(1, "Study", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(10, 0));
        FlexibleActivity candidate = adoptedFlexible(2, "Jogging", DATE,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 60, LocalTime.of(11, 0));

        checker.checkNoConflicts(candidate, NO_EXCLUDED_ID, List.of(existing));
    }
}
