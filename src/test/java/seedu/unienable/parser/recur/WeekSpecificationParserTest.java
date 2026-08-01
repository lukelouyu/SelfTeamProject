package seedu.unienable.parser.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;

class WeekSpecificationParserTest {
    private final WeekSpecificationParser parser = new WeekSpecificationParser();

    @Test
    public void parse_completeSemesterRanges_returnsWeeksOneToThirteen() throws Exception {
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                parser.parse("1 to 6; 7 to 13"));
    }

    @Test
    public void parse_sparseScreenshotWeeks_returnsExactWeeks() throws Exception {
        assertEquals(List.of(3, 7, 9, 11), parser.parse("3;7;9;11"));
    }

    @Test
    public void parse_mixedItemsAndOptionalSpaces_normalizesChronologically() throws Exception {
        assertEquals(List.of(3, 5, 6, 7, 10, 12, 13),
                parser.parse("12to13; 3 ; 5 TO 7 ;10"));
    }

    @Test
    public void parse_blank_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> parser.parse("  "));
    }

    @Test
    public void parse_reversedRange_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse("6 to 1"));
    }

    @Test
    public void parse_overlappingRanges_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse("1 to 6;4 to 8"));
    }

    @Test
    public void parse_duplicateIndividualWeek_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse("3;7;3"));
    }

    @Test
    public void parse_zeroOrNegativeStyle_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse("0"));
        assertThrows(InvalidCommandException.class, () -> parser.parse("-1"));
    }

    @Test
    public void parse_emptyItemCommaOrHyphen_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse("3;;7"));
        assertThrows(InvalidCommandException.class, () -> parser.parse("3,7"));
        assertThrows(InvalidCommandException.class, () -> parser.parse("3-7"));
    }

    @Test
    public void parse_trailingText_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> parser.parse("3;7 extra"));
    }

    @Test
    public void parse_multipleRangesExceedTotalSafetyLimit_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse("1 to 6000; 7000 to 11000"));
    }
}
