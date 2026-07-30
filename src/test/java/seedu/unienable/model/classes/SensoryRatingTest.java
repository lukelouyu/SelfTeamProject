package seedu.unienable.model.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidActivityException;

class SensoryRatingTest {
    @Test
    public void of_boundaryValues_areAccepted() throws InvalidActivityException {
        assertEquals(1, SensoryRating.of(1).getValue());
        assertEquals(5, SensoryRating.of(5).getValue());
    }

    @Test
    public void of_zero_throwsInvalidActivityException() {
        InvalidActivityException exception = assertThrows(InvalidActivityException.class,
                () -> SensoryRating.of(0));
        assertEquals("sensory must be a whole number from 1 to 5.", exception.getMessage());
    }

    @Test
    public void of_aboveMax_throwsInvalidActivityException() {
        assertThrows(InvalidActivityException.class, () -> SensoryRating.of(6));
    }

    @Test
    public void toString_showsRatingOutOfFive() throws InvalidActivityException {
        assertEquals("3/5", SensoryRating.of(3).toString());
    }
}
