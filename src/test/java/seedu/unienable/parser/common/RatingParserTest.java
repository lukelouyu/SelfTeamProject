package seedu.unienable.parser.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidActivityException;

class RatingParserTest {
    @Test
    public void parseEnergyRating_validNumber_returnsEnergyRating() throws InvalidActivityException {
        assertEquals(4, RatingParser.parseEnergyRating("4").getValue());
    }

    @Test
    public void parseEnergyRating_nonNumeric_throwsInvalidActivityException() {
        InvalidActivityException exception = assertThrows(InvalidActivityException.class,
                () -> RatingParser.parseEnergyRating("high"));
        assertEquals("energy must be a whole number from 1 to 5.", exception.getMessage());
    }

    @Test
    public void parseEnergyRating_outOfRange_throwsInvalidActivityException() {
        assertThrows(InvalidActivityException.class, () -> RatingParser.parseEnergyRating("7"));
    }

    @Test
    public void parseEnergyRating_boundaryValues_areAccepted() throws InvalidActivityException {
        assertEquals(1, RatingParser.parseEnergyRating("1").getValue());
        assertEquals(5, RatingParser.parseEnergyRating("5").getValue());
    }

    @Test
    public void parseEnergyRating_justBelowMinimum_throwsInvalidActivityException() {
        assertThrows(InvalidActivityException.class, () -> RatingParser.parseEnergyRating("0"));
    }

    @Test
    public void parseEnergyRating_justAboveMaximum_throwsInvalidActivityException() {
        assertThrows(InvalidActivityException.class, () -> RatingParser.parseEnergyRating("6"));
    }

    @Test
    public void parseSensoryRating_validNumber_returnsSensoryRating() throws InvalidActivityException {
        assertEquals(3, RatingParser.parseSensoryRating("3").getValue());
    }

    @Test
    public void parseSensoryRating_nonNumeric_throwsInvalidActivityException() {
        InvalidActivityException exception = assertThrows(InvalidActivityException.class,
                () -> RatingParser.parseSensoryRating("low"));
        assertEquals("sensory must be a whole number from 1 to 5.", exception.getMessage());
    }

    @Test
    public void parseSensoryRating_boundaryValues_areAccepted() throws InvalidActivityException {
        assertEquals(1, RatingParser.parseSensoryRating("1").getValue());
        assertEquals(5, RatingParser.parseSensoryRating("5").getValue());
    }

    @Test
    public void parseSensoryRating_justBelowMinimum_throwsInvalidActivityException() {
        assertThrows(InvalidActivityException.class, () -> RatingParser.parseSensoryRating("0"));
    }

    @Test
    public void parseSensoryRating_justAboveMaximum_throwsInvalidActivityException() {
        assertThrows(InvalidActivityException.class, () -> RatingParser.parseSensoryRating("6"));
    }
}
