package seedu.unienable.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidActivityException;

class EnergyRatingTest {
    @Test
    public void of_boundaryValues_areAccepted() throws InvalidActivityException {
        assertEquals(1, EnergyRating.of(1).getValue());
        assertEquals(5, EnergyRating.of(5).getValue());
    }

    @Test
    public void of_zero_throwsInvalidActivityException() {
        InvalidActivityException exception = assertThrows(InvalidActivityException.class,
                () -> EnergyRating.of(0));
        assertEquals("energy must be a whole number from 1 to 5.", exception.getMessage());
    }

    @Test
    public void of_aboveMax_throwsInvalidActivityException() {
        assertThrows(InvalidActivityException.class, () -> EnergyRating.of(6));
    }

    @Test
    public void toString_showsRatingOutOfFive() throws InvalidActivityException {
        assertEquals("4/5", EnergyRating.of(4).toString());
    }
}
