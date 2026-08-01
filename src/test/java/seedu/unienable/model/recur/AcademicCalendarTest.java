package seedu.unienable.model.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class AcademicCalendarTest {
    @Test
    public void findWeek_weekFourteenDefinedInData_returnsItWithoutCodeChange() {
        AcademicWeek week14 = new AcademicWeek("Synthetic AY", "TERM-X", 14,
                "INSTRUCTIONAL", LocalDate.of(2030, 4, 1), LocalDate.of(2030, 4, 5));
        AcademicCalendar calendar = new AcademicCalendar(List.of(week14), List.of());

        assertEquals(week14, calendar.findWeek("synthetic ay", "term-x", 14).orElseThrow());
    }

    @Test
    public void noClassDate_reasonComesFromData() {
        AcademicWeek week = new AcademicWeek("AY", "SEM", 1, "INSTRUCTIONAL",
                LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11));
        AcademicCalendar calendar = new AcademicCalendar(List.of(week), List.of(
                new NoClassDate("AY", LocalDate.of(2030, 1, 9), "University day")));

        assertEquals("University day",
                calendar.findNoClassReason(LocalDate.of(2030, 1, 9)).orElseThrow());
        assertTrue(calendar.findNoClassReason(LocalDate.of(2030, 1, 10)).isEmpty());
    }

    @Test
    public void constructor_overlappingInstructionalWeeks_throwsIllegalArgumentException() {
        AcademicWeek first = new AcademicWeek("AY", "SEM", 1, "INSTRUCTIONAL",
                LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11));
        AcademicWeek second = new AcademicWeek("AY", "SEM", 2, "INSTRUCTIONAL",
                LocalDate.of(2030, 1, 11), LocalDate.of(2030, 1, 15));

        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCalendar(List.of(first, second), List.of()));
    }
}
