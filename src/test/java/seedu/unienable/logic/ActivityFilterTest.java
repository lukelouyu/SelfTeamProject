package seedu.unienable.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.CompletionStatus;

class ActivityFilterTest {
    private static FixedActivity newActivity() throws Exception {
        return new FixedActivity(12, "CG3207 lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), "CG3207", null);
    }

    @Test
    public void matches_allFieldsNull_matchesEverything() throws Exception {
        assertTrue(new ActivityFilter(null, null, null, null).matches(newActivity()));
    }

    @Test
    public void matches_statusMismatch_returnsFalse() throws Exception {
        assertFalse(new ActivityFilter(CompletionStatus.COMPLETE, null, null, null).matches(newActivity()));
    }

    @Test
    public void matches_categoryMismatch_returnsFalse() throws Exception {
        assertFalse(new ActivityFilter(null, ActivityCategory.CCA, null, null).matches(newActivity()));
    }

    @Test
    public void matches_topicIsCaseInsensitive() throws Exception {
        assertTrue(new ActivityFilter(null, null, "cg3207", null).matches(newActivity()));
    }

    @Test
    public void matches_topicMismatch_returnsFalse() throws Exception {
        assertFalse(new ActivityFilter(null, null, "CS2113", null).matches(newActivity()));
    }

    @Test
    public void matches_topicFilterWithNoActivityTopic_returnsFalse() throws Exception {
        FixedActivity noTopic = new FixedActivity(13, "Untitled", ActivityCategory.OTHERS,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(1), SensoryRating.of(1), null, null);

        assertFalse(new ActivityFilter(null, null, "CG3207", null).matches(noTopic));
    }

    @Test
    public void matches_dateMismatch_returnsFalse() throws Exception {
        assertFalse(new ActivityFilter(null, null, null, LocalDate.of(2026, 8, 16)).matches(newActivity()));
    }

    @Test
    public void matches_allFiltersSatisfied_returnsTrue() throws Exception {
        ActivityFilter filter = new ActivityFilter(CompletionStatus.INCOMPLETE, ActivityCategory.ACADEMIC,
                "CG3207", LocalDate.of(2026, 8, 15));

        assertTrue(filter.matches(newActivity()));
    }
}
