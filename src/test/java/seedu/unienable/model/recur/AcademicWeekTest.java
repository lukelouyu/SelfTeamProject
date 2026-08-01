package seedu.unienable.model.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AcademicWeekTest {
    @Test
    public void findDate_weekIncludesRequestedDay_returnsDate() {
        AcademicWeek week = new AcademicWeek("Synthetic AY", "TERM-A", 14,
                "INSTRUCTIONAL", LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11));

        assertEquals(LocalDate.of(2030, 1, 10), week.findDate(DayOfWeek.THURSDAY).orElseThrow());
        assertTrue(week.isInstructional());
        assertTrue(week.contains(LocalDate.of(2030, 1, 11)));
    }

    @Test
    public void findDate_dayOutsideShortWeek_returnsEmpty() {
        AcademicWeek week = new AcademicWeek("Synthetic AY", "TERM-A", 1,
                "INSTRUCTIONAL", LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11));

        assertTrue(week.findDate(DayOfWeek.SATURDAY).isEmpty());
        assertFalse(week.contains(LocalDate.of(2030, 1, 12)));
    }

    @Test
    public void constructor_reversedDates_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new AcademicWeek(
                "AY", "TERM", 1, "INSTRUCTIONAL",
                LocalDate.of(2030, 1, 8), LocalDate.of(2030, 1, 7)));
    }
}
