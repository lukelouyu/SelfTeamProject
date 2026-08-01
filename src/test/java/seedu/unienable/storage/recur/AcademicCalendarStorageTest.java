package seedu.unienable.storage.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.recur.AcademicCalendar;

class AcademicCalendarStorageTest {
    @TempDir
    Path tempDir;

    private final AcademicCalendarStorage storage = new AcademicCalendarStorage();

    @Test
    public void load_suppliedAy2026Calendar_readsWeekAndNoClassData() throws Exception {
        AcademicCalendar calendar = storage.load(Path.of("data", "academic-calendar.txt"));

        assertEquals(27, calendar.getWeeks().size());
        assertEquals(LocalDate.of(2026, 9, 28),
                calendar.findWeek("AY2026/2027", "SEM1", 7).orElseThrow().getStartDate());
        assertEquals("NUS Well-Being Day",
                calendar.findNoClassReason(LocalDate.of(2026, 10, 9)).orElseThrow());
    }

    @Test
    public void load_missingFile_throwsStorageExceptionWithoutCreatingIt() {
        Path missing = tempDir.resolve("academic-calendar.txt");

        StorageException exception = assertThrows(StorageException.class,
                () -> storage.load(missing));

        assertTrue(exception.getMessage().contains("file not found"));
        assertTrue(Files.notExists(missing));
    }

    @Test
    public void load_malformedLine_reportsLineNumber() throws Exception {
        Path file = tempDir.resolve("academic-calendar.txt");
        Files.writeString(file, "FORMAT|1\n"
                + "SOURCE|Synthetic AY|Test|2030-01-01\n"
                + "WEEK|Synthetic AY|TERM|one|INSTRUCTIONAL|2030-01-07|2030-01-11\n");

        StorageException exception = assertThrows(StorageException.class,
                () -> storage.load(file));

        assertTrue(exception.getMessage().contains("line 3"));
    }

    @Test
    public void load_duplicateWeekKey_rejectsAmbiguousCalendar() throws Exception {
        Path file = tempDir.resolve("academic-calendar.txt");
        Files.writeString(file, "FORMAT|1\n"
                + "SOURCE|Synthetic AY|Test|2030-01-01\n"
                + "WEEK|Synthetic AY|TERM|1|INSTRUCTIONAL|2030-01-07|2030-01-11\n"
                + "WEEK|Synthetic AY|TERM|1|INSTRUCTIONAL|2030-01-14|2030-01-18\n");

        assertThrows(StorageException.class, () -> storage.load(file));
    }

    @Test
    public void load_syntheticFutureYearAndWeekFourteen_requiresNoJavaChange() throws Exception {
        Path file = tempDir.resolve("academic-calendar.txt");
        Files.writeString(file, "FORMAT|1\n"
                + "SOURCE|AY2027/2028|Test calendar|2027-07-01\n"
                + "WEEK|AY2027/2028|SEM1|14|INSTRUCTIONAL|2027-11-15|2027-11-19\n");

        AcademicCalendar calendar = storage.load(file);

        assertTrue(calendar.findWeek("AY2027/2028", "SEM1", 14).isPresent());
    }

    @Test
    public void load_editedWeekDate_followsTextFileValue() throws Exception {
        Path file = tempDir.resolve("academic-calendar.txt");
        Files.writeString(file, "FORMAT|1\n"
                + "SOURCE|Synthetic AY|Test calendar|2030-01-01\n"
                + "WEEK|Synthetic AY|TERM|1|INSTRUCTIONAL|2030-02-04|2030-02-08\n");

        AcademicCalendar calendar = storage.load(file);

        assertEquals(LocalDate.of(2030, 2, 4),
                calendar.findWeek("Synthetic AY", "TERM", 1).orElseThrow().getStartDate());
    }
}
