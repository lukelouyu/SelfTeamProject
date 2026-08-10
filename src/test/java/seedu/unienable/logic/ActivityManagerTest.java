package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.enums.CompletionStatus;

class ActivityManagerTest {
    private static FixedActivity newFixedActivity(int id) throws Exception {
        return new FixedActivity(id, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    private static FixedActivity newFixedActivity(int id, String description, LocalTime start, LocalTime end)
            throws Exception {
        return new FixedActivity(id, description, ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), start, end, EnergyRating.of(4), SensoryRating.of(3), null, null);
    }

    private static FlexibleActivity newFlexibleActivity(int id, String description, LocalTime earliestStart,
            LocalTime latestEnd) throws Exception {
        return new FlexibleActivity(id, description, ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 15),
                earliestStart, latestEnd, 90, EnergyRating.of(5), SensoryRating.of(2), null, null);
    }

    @Test
    public void getNextId_startsAtOneAndIncrementsOnlyOnAdd() throws Exception {
        ActivityManager manager = new ActivityManager();

        assertEquals(1, manager.getNextId());
        assertEquals(1, manager.getNextId());

        manager.add(newFixedActivity(manager.getNextId()));

        assertEquals(2, manager.getNextId());
    }

    @Test
    public void getById_existingId_returnsActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        assertEquals(1, manager.getById(1).getId());
    }

    @Test
    public void getById_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        InvalidIndexException exception = assertThrows(InvalidIndexException.class, () -> manager.getById(999));
        assertEquals("Activity [999] does not exist.", exception.getMessage());
    }

    @Test
    public void getAll_isUnmodifiable() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        assertThrows(UnsupportedOperationException.class,
                () -> manager.getAll().add(newFixedActivity(99)));
    }

    @Test
    public void size_reflectsNumberOfActivities() throws Exception {
        ActivityManager manager = new ActivityManager();

        assertEquals(0, manager.size());

        manager.add(newFixedActivity(manager.getNextId()));

        assertEquals(1, manager.size());
    }

    @Test
    public void add_exactDuplicateFixedActivity_throwsDuplicateActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> manager.add(newFixedActivity(manager.getNextId())));
        assertEquals("An identical activity already exists.", exception.getMessage());
        assertEquals(2, manager.getNextId());
    }

    @Test
    public void add_overlappingFixedActivity_throwsDuplicateActivityExceptionWithGuideWording() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "CG3207 lecture", LocalTime.of(9, 0), LocalTime.of(11, 0)));

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> manager.add(newFixedActivity(manager.getNextId(), "Consultation",
                        LocalTime.of(10, 30), LocalTime.of(11, 30))));
        assertEquals("This timing overlaps activity [1], CG3207 lecture (09:00 -> 11:00).", exception.getMessage());
    }

    @Test
    public void addAllAtomically_emptyBatch_doesNotMutateState() throws Exception {
        ActivityManager manager = new ActivityManager();

        manager.addAllAtomically(List.of());

        assertEquals(0, manager.size());
        assertEquals(1, manager.getNextId());
    }

    @Test
    public void addAllAtomically_candidateConflictsWithExisting_rollsBackBatch() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Existing", LocalTime.of(9, 0), LocalTime.of(11, 0)));
        FixedActivity candidate = newFixedActivity(2, "Candidate", LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThrows(DuplicateActivityException.class, () -> manager.addAllAtomically(List.of(candidate)));

        assertEquals(1, manager.size());
        assertEquals(2, manager.getNextId());
    }

    @Test
    public void add_wrongId_rejectsWithoutMutatingState() throws Exception {
        ActivityManager manager = new ActivityManager();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> manager.add(newFixedActivity(42)));

        assertEquals("expected activity ID 1 but received 42", exception.getMessage());
        assertEquals(0, manager.size());
        assertEquals(1, manager.getNextId());
    }

    @Test
    public void addAllAtomically_candidateConflictsWithEarlierCandidate_rollsBackBatch() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity first = newFixedActivity(1, "First", LocalTime.of(9, 0), LocalTime.of(11, 0));
        FixedActivity second = newFixedActivity(2, "Second", LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThrows(DuplicateActivityException.class,
                () -> manager.addAllAtomically(List.of(first, second)));

        assertEquals(0, manager.size());
        assertEquals(1, manager.getNextId());
    }

    @Test
    public void addAllAtomically_duplicateCandidates_rollsBackBatch() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity first = newFixedActivity(1);
        FixedActivity duplicate = newFixedActivity(2);

        DuplicateActivityException exception = assertThrows(DuplicateActivityException.class,
                () -> manager.addAllAtomically(List.of(first, duplicate)));

        assertEquals("An identical activity already exists.", exception.getMessage());
        assertEquals(0, manager.size());
        assertEquals(1, manager.getNextId());
    }

    @Test
    public void addAllAtomically_nonSequentialId_rejectsBeforeThatCandidateConflict() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));
        FixedActivity wrongIdDuplicate = newFixedActivity(99);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> manager.addAllAtomically(List.of(wrongIdDuplicate)));

        assertEquals("expected activity ID 2 but received 99", exception.getMessage());
        assertEquals(1, manager.size());
        assertEquals(2, manager.getNextId());
    }

    @Test
    public void addAllAtomically_earlierConflictPrecedesLaterIdMismatch() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));
        FixedActivity duplicate = newFixedActivity(2);
        FixedActivity laterWrongId = newFixedActivity(99, "Later", LocalTime.of(12, 0), LocalTime.of(13, 0));

        assertThrows(DuplicateActivityException.class,
                () -> manager.addAllAtomically(List.of(duplicate, laterWrongId)));

        assertEquals(1, manager.size());
        assertEquals(2, manager.getNextId());
    }

    @Test
    public void addAllAtomically_validSequentialIds_commitsWholeBatch() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity first = newFixedActivity(1, "First", LocalTime.of(9, 0), LocalTime.of(10, 0));
        FixedActivity second = newFixedActivity(2, "Second", LocalTime.of(10, 0), LocalTime.of(11, 0));

        manager.addAllAtomically(List.of(first, second));

        assertEquals(2, manager.size());
        assertEquals(3, manager.getNextId());
    }

    @Test
    public void delete_existingId_removesActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        manager.delete(1);

        assertEquals(0, manager.size());
        assertThrows(InvalidIndexException.class, () -> manager.getById(1));
    }

    @Test
    public void delete_doesNotChangeOtherActivityIds() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));

        manager.delete(1);

        assertEquals(2, manager.getById(2).getId());
    }

    @Test
    public void delete_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> manager.delete(999));
    }

    @Test
    public void add_afterDeletingMostRecentActivity_doesNotReuseDeletedId() throws Exception {
        // Pinning test: IDs are permanent and never resequenced or reused, even when the
        // deleted activity was the most recently added one (the case most likely to tempt a
        // "reuse the freed slot" implementation).
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));

        manager.delete(2);
        manager.add(newFixedActivity(manager.getNextId(), "Third", LocalTime.of(13, 0), LocalTime.of(14, 0)));

        assertEquals(3, manager.getById(3).getId());
        assertThrows(InvalidIndexException.class, () -> manager.getById(2));
    }

    @Test
    public void mark_existingId_marksActivityComplete() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        Activity marked = manager.mark(1);

        assertTrue(marked.isComplete());
        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void mark_alreadyComplete_isAllowed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));
        manager.mark(1);

        manager.mark(1);

        assertTrue(manager.getById(1).isComplete());
    }

    @Test
    public void unmark_afterMark_marksActivityIncomplete() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));
        manager.mark(1);

        Activity unmarked = manager.unmark(1);

        assertFalse(unmarked.isComplete());
    }

    @Test
    public void mark_unknownId_throwsInvalidIndexException() {
        ActivityManager manager = new ActivityManager();

        assertThrows(InvalidIndexException.class, () -> manager.mark(999));
    }

    @Test
    public void list_withNoFilters_returnsEveryActivity() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), ActivityOrder.INPUT);

        assertEquals(2, result.size());
    }

    @Test
    public void list_withFilter_returnsOnlyMatchingActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));
        manager.mark(1);

        List<Activity> result = manager.list(
                new ActivityFilter(CompletionStatus.COMPLETE, null, null, null), ActivityOrder.INPUT);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    public void getDefaultOrder_startsAsChronological() {
        assertEquals(ActivityOrder.CHRONOLOGICAL, new ActivityManager().getDefaultOrder());
    }

    @Test
    public void list_inputOrder_preservesAddedSequence() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Later time", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Earlier time", LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), ActivityOrder.INPUT);

        assertEquals(1, result.get(0).getId());
        assertEquals(2, result.get(1).getId());
    }

    @Test
    public void list_timeOrder_sortsByStartTimeThenId() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Later time", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Earlier time", LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), ActivityOrder.TIME);

        assertEquals(2, result.get(0).getId());
        assertEquals(1, result.get(1).getId());
    }

    @Test
    public void list_chronologicalOrder_sortsByDateThenTime() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity laterDate = new FixedActivity(manager.getNextId(), "Later date", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(3), SensoryRating.of(2), null, null);
        manager.add(laterDate);
        manager.add(newFixedActivity(manager.getNextId(), "Earlier date", LocalTime.of(14, 0), LocalTime.of(15, 0)));

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null),
                ActivityOrder.CHRONOLOGICAL);

        assertEquals(2, result.get(0).getId());
        assertEquals(1, result.get(1).getId());
    }

    @Test
    public void list_nullOrder_usesSavedDefault() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Later time", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Earlier time", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.setDefaultOrder(ActivityOrder.TIME);

        List<Activity> result = manager.list(new ActivityFilter(null, null, null, null), null);

        assertEquals(2, result.get(0).getId());
        assertEquals(1, result.get(1).getId());
    }

    private static FixedActivity newSearchableActivity(int id, String description, String topic, String note)
            throws Exception {
        LocalTime start = LocalTime.of(8, 0).plusHours(id);
        return new FixedActivity(id, description, ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), start, start.plusHours(1),
                EnergyRating.of(4), SensoryRating.of(3), topic, note);
    }

    @Test
    public void find_keywordMatchesDescription_isCaseInsensitiveAndPartial() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newSearchableActivity(manager.getNextId(), "Finish assignment 1", null, null));

        List<Activity> result = manager.find(List.of("ASSIGN"), new ActivityFilter(null, null, null, null), null);

        assertEquals(1, result.size());
    }

    @Test
    public void find_keywordMatchesTopicOrNote() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newSearchableActivity(manager.getNextId(), "Lecture", "CG3207", null));
        manager.add(newSearchableActivity(manager.getNextId(), "Consultation", null, "Bring laptop"));

        assertEquals(1, manager.find(List.of("cg3207"), new ActivityFilter(null, null, null, null), null).size());
        assertEquals(1, manager.find(List.of("laptop"), new ActivityFilter(null, null, null, null), null).size());
    }

    @Test
    public void find_multipleKeywords_requiresAllToMatch() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newSearchableActivity(manager.getNextId(), "Finish assignment 1", null, null));
        manager.add(newSearchableActivity(manager.getNextId(), "Finish reading", null, null));

        List<Activity> result = manager.find(List.of("finish", "assignment"),
                new ActivityFilter(null, null, null, null), null);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    public void find_keywordAndFilterCombine_withAnd() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newSearchableActivity(manager.getNextId(), "Finish assignment 1", "CG3207", null));
        manager.add(newSearchableActivity(manager.getNextId(), "Finish assignment 2", "CS2113", null));

        List<Activity> result = manager.find(List.of("finish"),
                new ActivityFilter(null, null, "CG3207", null), null);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    public void find_noKeywordMatch_returnsEmptyList() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newSearchableActivity(manager.getNextId(), "Finish assignment 1", null, null));

        List<Activity> result = manager.find(List.of("unrelated"), new ActivityFilter(null, null, null, null), null);

        assertEquals(0, result.size());
    }

    @Test
    public void find_emptyKeywords_reliesOnFilterAlone() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newSearchableActivity(manager.getNextId(), "Finish assignment 1", null, null));

        List<Activity> result = manager.find(List.of(), new ActivityFilter(null, null, null, null), null);

        assertEquals(1, result.size());
    }

    @Test
    public void next_fixedActivityInProgress_isSelectedOverUpcomingOnes() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "In progress", LocalTime.of(9, 0), LocalTime.of(11, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Later today", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 10, 0);

        Optional<Activity> result = manager.next(now);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    public void next_noActivityInProgress_selectsNearestUpcomingFixed() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Later", LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Sooner", LocalTime.of(12, 0), LocalTime.of(13, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 10, 0);

        Optional<Activity> result = manager.next(now);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
    }

    @Test
    public void next_noFixedCandidates_selectsFlexibleWithSoonestWindowEnd() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFlexibleActivity(manager.getNextId(), "Later deadline",
                LocalTime.of(10, 0), LocalTime.of(20, 0)));
        manager.add(newFlexibleActivity(manager.getNextId(), "Sooner deadline",
                LocalTime.of(11, 0), LocalTime.of(18, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        Optional<Activity> result = manager.next(now);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
    }

    @Test
    public void next_flexibleTie_choosesEarlierStartThenLowerId() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFlexibleActivity(manager.getNextId(), "Later start, same deadline",
                LocalTime.of(12, 0), LocalTime.of(18, 0)));
        manager.add(newFlexibleActivity(manager.getNextId(), "Earlier start, same deadline",
                LocalTime.of(10, 0), LocalTime.of(18, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        Optional<Activity> result = manager.next(now);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
    }

    @Test
    public void next_completedActivityIsExcluded() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Sooner but complete",
                LocalTime.of(12, 0), LocalTime.of(13, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Later but incomplete",
                LocalTime.of(14, 0), LocalTime.of(15, 0)));
        manager.mark(1);
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        Optional<Activity> result = manager.next(now);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
    }

    @Test
    public void next_nowExactlyAtFixedStartTime_isInProgress() throws Exception {
        // Boundary: the in-progress window is start-inclusive, so "now" landing exactly on the
        // start time must already count as in progress.
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Starting now",
                LocalTime.of(9, 0), LocalTime.of(11, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        Optional<Activity> result = manager.next(now);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    public void next_nowExactlyAtFixedEndTime_isNeitherInProgressNorUpcoming() throws Exception {
        // Boundary: the in-progress window is end-exclusive, so "now" landing exactly on the end
        // time is the single instant where the activity is no longer in progress but is not yet
        // overdue either (see countOverdueIncomplete_nowExactlyAtEndTime_isNotYetOverdue).
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Just ending",
                LocalTime.of(9, 0), LocalTime.of(11, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 11, 0);

        assertFalse(manager.next(now).isPresent());
    }

    @Test
    public void next_overdueActivityIsExcluded() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Already passed",
                LocalTime.of(7, 0), LocalTime.of(8, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        Optional<Activity> result = manager.next(now);

        assertFalse(result.isPresent());
    }

    @Test
    public void next_noQualifyingActivities_returnsEmpty() {
        ActivityManager manager = new ActivityManager();

        assertFalse(manager.next(LocalDateTime.of(2026, 8, 15, 9, 0)).isPresent());
    }

    @Test
    public void countOverdueIncomplete_countsOnlyIncompleteAndOverdue() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Overdue incomplete",
                LocalTime.of(6, 0), LocalTime.of(7, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Overdue but complete",
                LocalTime.of(7, 30), LocalTime.of(8, 0)));
        manager.mark(2);
        manager.add(newFixedActivity(manager.getNextId(), "Not yet due",
                LocalTime.of(14, 0), LocalTime.of(15, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        assertEquals(1, manager.countOverdueIncomplete(now));
    }

    @Test
    public void countOverdueIncomplete_nowExactlyAtEndTime_isNotYetOverdue() throws Exception {
        // Boundary: overdue is end-exclusive ("end.isBefore(now)"), so "now" landing exactly on
        // the end time must not yet count as overdue; one minute later, it must.
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Just ending",
                LocalTime.of(9, 0), LocalTime.of(11, 0)));
        LocalDateTime exactlyAtEnd = LocalDateTime.of(2026, 8, 15, 11, 0);

        assertEquals(0, manager.countOverdueIncomplete(exactlyAtEnd));
        assertEquals(1, manager.countOverdueIncomplete(exactlyAtEnd.plusMinutes(1)));
    }

    @Test
    public void listOverdue_returnsOnlyIncompleteAndOverdue() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Overdue incomplete",
                LocalTime.of(6, 0), LocalTime.of(7, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Overdue but complete",
                LocalTime.of(7, 30), LocalTime.of(8, 0)));
        manager.mark(2);
        manager.add(newFixedActivity(manager.getNextId(), "Not yet due",
                LocalTime.of(14, 0), LocalTime.of(15, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        List<Activity> overdue = manager.listOverdue(now, new ActivityFilter(null, null, null, null), null);

        assertEquals(1, overdue.size());
        assertEquals("Overdue incomplete", overdue.get(0).getDescription());
    }

    @Test
    public void listOverdue_doesNotAffectPlainList() throws Exception {
        // The additive-scope guarantee at the manager level: listOverdue is a separate query,
        // list() keeps returning every activity regardless of overdue status.
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Overdue incomplete",
                LocalTime.of(6, 0), LocalTime.of(7, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        List<Activity> plain = manager.list(new ActivityFilter(null, null, null, null), null);

        assertEquals(1, plain.size());
        List<Activity> overdue = manager.listOverdue(now, new ActivityFilter(null, null, null, null), null);
        assertEquals(1, overdue.size());
    }

    @Test
    public void listOverdue_appliesAdditionalFilterAndOrder() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Overdue academic",
                LocalTime.of(6, 0), LocalTime.of(7, 0)));
        Activity overdueCca = new FixedActivity(manager.getNextId(), "Overdue cca", ActivityCategory.CCA,
                LocalDate.of(2026, 8, 15), LocalTime.of(7, 30), LocalTime.of(8, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null);
        manager.add(overdueCca);
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        List<Activity> result = manager.listOverdue(now,
                new ActivityFilter(null, ActivityCategory.ACADEMIC, null, null), ActivityOrder.TIME);

        assertEquals(1, result.size());
        assertEquals("Overdue academic", result.get(0).getDescription());
    }

    @Test
    public void listOverdue_noOverdueActivities_returnsEmptyList() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Upcoming",
                LocalTime.of(14, 0), LocalTime.of(15, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 9, 0);

        assertTrue(manager.listOverdue(now, new ActivityFilter(null, null, null, null), null).isEmpty());
    }

    @Test
    public void replace_validReplacement_swapsInPlaceKeepingSameId() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Old description",
                LocalTime.of(9, 0), LocalTime.of(10, 0)));

        FixedActivity replacement = newFixedActivity(1, "New description", LocalTime.of(9, 0), LocalTime.of(10, 0));
        manager.replace(1, replacement);

        assertEquals("New description", manager.getById(1).getDescription());
        assertEquals(1, manager.size());
    }

    @Test
    public void replace_mismatchedId_rejectsWithoutMutatingState() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(1, "Original", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        FixedActivity wrongId = newFixedActivity(99, "Replacement", LocalTime.of(11, 0), LocalTime.of(12, 0));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> manager.replace(1, wrongId));

        assertEquals("replacement ID 99 does not match stable activity ID 1", exception.getMessage());
        assertEquals("Original", manager.getById(1).getDescription());
        assertThrows(InvalidIndexException.class, () -> manager.getById(99));
    }

    @Test
    public void replace_ownUnchangedTiming_isNotRejectedAsSelfOverlap() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "Original",
                LocalTime.of(9, 0), LocalTime.of(10, 0)));

        FixedActivity replacement = newFixedActivity(1, "Renamed", LocalTime.of(9, 0), LocalTime.of(10, 0));
        manager.replace(1, replacement);

        assertEquals("Renamed", manager.getById(1).getDescription());
    }

    @Test
    public void replace_overlapsAnotherActivity_throwsDuplicateActivityException() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First",
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second",
                LocalTime.of(14, 0), LocalTime.of(15, 0)));

        FixedActivity replacement = newFixedActivity(2, "Second", LocalTime.of(9, 30), LocalTime.of(10, 30));

        assertThrows(DuplicateActivityException.class, () -> manager.replace(2, replacement));
        assertEquals(LocalTime.of(14, 0), ((FixedActivity) manager.getById(2)).getStartTime());
    }

    @Test
    public void replace_stateChangesAfterPreflight_defensivelyRechecksConflict() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity first = newFixedActivity(manager.getNextId(), "First",
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        manager.add(first);
        manager.add(newFixedActivity(manager.getNextId(), "Second",
                LocalTime.of(14, 0), LocalTime.of(15, 0)));
        FixedActivity replacement = newFixedActivity(2, "Second",
                LocalTime.of(11, 0), LocalTime.of(12, 0));
        manager.checkNoConflicts(replacement, 2);

        first.setEndTime(LocalTime.of(11, 30));

        assertThrows(DuplicateActivityException.class, () -> manager.replace(2, replacement));
        assertEquals(LocalTime.of(14, 0), ((FixedActivity) manager.getById(2)).getStartTime());
    }

    @Test
    public void replace_unknownId_throwsInvalidIndexException() throws Exception {
        ActivityManager manager = new ActivityManager();
        FixedActivity replacement = newFixedActivity(999, "Ghost", LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThrows(InvalidIndexException.class, () -> manager.replace(999, replacement));
    }

    @Test
    public void loadAll_populatesActivitiesAndSyncsNextIdPastHighestLoadedId() throws Exception {
        ActivityManager manager = new ActivityManager();

        manager.loadAll(List.of(newFixedActivity(3, "Loaded", LocalTime.of(9, 0), LocalTime.of(10, 0)),
                newFixedActivity(7, "Loaded later", LocalTime.of(11, 0), LocalTime.of(12, 0))));

        assertEquals(2, manager.size());
        assertEquals(8, manager.getNextId());
    }

    @Test
    public void loadAll_doesNotRevalidateDuplicatesOrOverlaps() throws Exception {
        ActivityManager manager = new ActivityManager();

        manager.loadAll(List.of(newFixedActivity(1, "Same slot", LocalTime.of(9, 0), LocalTime.of(10, 0)),
                newFixedActivity(2, "Same slot", LocalTime.of(9, 0), LocalTime.of(10, 0))));

        assertEquals(2, manager.size());
    }

    @Test
    public void loadAll_emptyList_resetsNextIdToOne() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        manager.loadAll(List.of());

        assertEquals(0, manager.size());
        assertEquals(1, manager.getNextId());
    }

    @Test
    public void loadAll_replacesPreviouslyLoadedActivities() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(5, "First load", LocalTime.of(9, 0), LocalTime.of(10, 0))));

        manager.loadAll(List.of(newFixedActivity(1, "Second load", LocalTime.of(9, 0), LocalTime.of(10, 0))));

        assertEquals(1, manager.size());
        assertEquals("Second load", manager.getById(1).getDescription());
    }

    @Test
    public void hasAllocatedAnyId_freshManager_returnsFalse() {
        assertFalse(new ActivityManager().hasAllocatedAnyId());
    }

    @Test
    public void hasAllocatedAnyId_afterOneAdd_returnsTrue() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId()));

        assertTrue(manager.hasAllocatedAnyId());
    }

    @Test
    public void hasAllocatedAnyId_afterIdSpaceExhausted_returnsTrueWithoutThrowing() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE)));

        assertTrue(manager.hasAllocatedAnyId());
    }

    @Test
    public void tryGetNextId_freshManager_returnsPresentOne() {
        assertEquals(OptionalInt.of(1), new ActivityManager().tryGetNextId());
    }

    @Test
    public void tryGetNextId_afterIdSpaceExhausted_returnsEmptyWithoutThrowing() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE)));

        assertEquals(OptionalInt.empty(), manager.tryGetNextId());
    }

    @Test
    public void snapshotIdAllocation_capturesExhaustedStateWithoutThrowing() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE)));

        ActivityManager.IdAllocationState snapshot = manager.snapshotIdAllocation();

        assertTrue(snapshot.idSpaceExhausted());
    }

    @Test
    public void restoreState_exhaustedSnapshot_restoresExhaustionFlagNotJustNextId() throws Exception {
        // Regression test: restoreState() used to unconditionally set idSpaceExhausted = false,
        // so restoring a snapshot taken while genuinely exhausted would silently un-exhaust the
        // manager instead of reproducing the exact pre-command state.
        ActivityManager manager = new ActivityManager();
        Activity maxIdActivity = newFixedActivity(Integer.MAX_VALUE);
        manager.loadAll(List.of(maxIdActivity));
        ActivityManager.IdAllocationState exhaustedSnapshot = manager.snapshotIdAllocation();
        manager.resetAll();
        assertFalse(manager.hasAllocatedAnyId(), "test setup: resetAll() must clear exhaustion first");

        manager.restoreState(List.of(maxIdActivity), exhaustedSnapshot, ActivityOrder.CHRONOLOGICAL);

        assertThrows(IllegalStateException.class, manager::getNextId);
    }

    @Test
    public void loadAll_highestLoadedIdIsIntegerMaxValue_marksIdSpaceExhausted() throws Exception {
        // A loaded id of Integer.MAX_VALUE means there is no valid int left to represent "the
        // next id" - Math.max(1, id + 1) used to silently overflow to Integer.MIN_VALUE and then
        // reset nextId back down to 1, masking the fact that the ID was ever taken at all.
        ActivityManager manager = new ActivityManager();

        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE)));

        assertEquals(1, manager.size());
        IllegalStateException exception = assertThrows(IllegalStateException.class, manager::getNextId);
        assertTrue(exception.getMessage().contains("exhausted"));
    }

    @Test
    public void add_consumesFinalValidId_thenExhaustsIdSpace() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE - 1)));
        assertEquals(Integer.MAX_VALUE, manager.getNextId());

        manager.add(newFixedActivity(Integer.MAX_VALUE, "Last possible activity",
                LocalTime.of(12, 0), LocalTime.of(13, 0)));

        assertEquals(2, manager.size());
        assertThrows(IllegalStateException.class, manager::getNextId);
    }

    @Test
    public void add_afterIdSpaceExhausted_throwsIllegalStateExceptionWithoutMutating() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE)));
        FixedActivity rejected = newFixedActivity(1, "Should never be added",
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThrows(IllegalStateException.class, () -> manager.add(rejected));

        assertEquals(1, manager.size(), "an exhausted ID space must reject the add without mutating state");
    }

    @Test
    public void addAllAtomically_batchWouldExceedIntegerMaxValue_throwsIllegalStateExceptionWithoutMutating()
            throws Exception {
        // Only one ID (Integer.MAX_VALUE itself) remains available; a 2-activity batch cannot fit
        // and must be rejected atomically before either candidate is validated or added.
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE - 1)));
        FixedActivity first = newFixedActivity(Integer.MAX_VALUE, "First", LocalTime.of(9, 0), LocalTime.of(10, 0));
        FixedActivity second = newFixedActivity(1, "Second", LocalTime.of(11, 0), LocalTime.of(12, 0));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> manager.addAllAtomically(List.of(first, second)));

        assertTrue(exception.getMessage().contains("Activity ID space cannot fit"));
        assertEquals(1, manager.size(), "the batch must be rejected atomically, not partially applied");
        assertEquals(Integer.MAX_VALUE, manager.getNextId(), "the single remaining ID must still be available");
    }

    @Test
    public void addAllAtomically_batchExactlyFillsToIntegerMaxValue_commitsAndThenExhausts() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE - 2)));
        FixedActivity first = newFixedActivity(
                Integer.MAX_VALUE - 1, "First", LocalTime.of(12, 0), LocalTime.of(13, 0));
        FixedActivity second = newFixedActivity(Integer.MAX_VALUE, "Second", LocalTime.of(14, 0), LocalTime.of(15, 0));

        manager.addAllAtomically(List.of(first, second));

        assertEquals(3, manager.size());
        assertThrows(IllegalStateException.class, manager::getNextId);
    }

    @Test
    public void resetAll_afterIdSpaceExhausted_clearsExhaustionAndRestartsAtOne() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.loadAll(List.of(newFixedActivity(Integer.MAX_VALUE)));

        manager.resetAll();

        assertEquals(1, manager.getNextId());
        manager.add(newFixedActivity(manager.getNextId(), "After reset", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertEquals(1, manager.getById(1).getId());
    }

    @Test
    public void resetAll_clearsActivitiesResetsNextIdAndDefaultOrder() throws Exception {
        ActivityManager manager = new ActivityManager();
        manager.add(newFixedActivity(manager.getNextId(), "First", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        manager.add(newFixedActivity(manager.getNextId(), "Second", LocalTime.of(11, 0), LocalTime.of(12, 0)));
        manager.setDefaultOrder(ActivityOrder.INPUT);

        manager.resetAll();

        assertEquals(0, manager.size());
        assertEquals(1, manager.getNextId());
        assertEquals(ActivityOrder.CHRONOLOGICAL, manager.getDefaultOrder());

        manager.add(newFixedActivity(manager.getNextId(), "After reset", LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertEquals(1, manager.getById(1).getId());
    }
}
