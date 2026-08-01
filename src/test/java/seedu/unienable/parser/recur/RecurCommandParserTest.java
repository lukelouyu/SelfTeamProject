package seedu.unienable.parser.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.command.recur.RecurCommand;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.exception.StorageException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.testutil.recur.RecurrenceTestData;

class RecurCommandParserTest {
    @TempDir
    Path tempDir;

    private ActivityManager manager;
    private Path calendarFile;

    @BeforeEach
    public void setUp() throws Exception {
        manager = new ActivityManager();
        manager.add(RecurrenceTestData.cs2113Lecture(manager.getNextId()));
        calendarFile = tempDir.resolve("academic-calendar.txt");
        RecurrenceTestData.writeCalendar(calendarFile);
    }

    @Test
    public void parse_completeSemester_returnsPrevalidatedRecurCommand() throws Exception {
        RecurCommand command = new RecurCommandParser(calendarFile).parse(manager,
                "1 week 1 to 6; 7 to 13");

        assertEquals(11, command.getPlan().getOccurrencesToCreate().size());
    }

    @Test
    public void parse_commandKeywordAndRangeAreCaseInsensitive() throws Exception {
        RecurCommand command = new RecurCommandParser(calendarFile).parse(manager,
                "1 WEEK 1 TO 2");

        assertEquals(1, command.getPlan().getOccurrencesToCreate().size());
    }

    @Test
    public void parse_blankOrMalformedSyntax_rejectsClearly() {
        RecurCommandParser parser = new RecurCommandParser(calendarFile);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, ""));
        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, "1 1 to 6"));
        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, "one week 1"));
    }

    @Test
    public void parse_unknownId_throwsInvalidIndexException() {
        assertThrows(InvalidIndexException.class,
                () -> new RecurCommandParser(calendarFile).parse(manager, "99 week 1"));
    }

    @Test
    public void parse_flexibleOrNonAcademicActivity_rejectsEligibility() throws Exception {
        manager.add(new FlexibleActivity(manager.getNextId(), "Tutorial",
                ActivityCategory.ACADEMIC, LocalDate.of(2026, 8, 14), LocalTime.of(9, 0),
                LocalTime.of(12, 0), 60, EnergyRating.of(2), SensoryRating.of(2), null, null));
        manager.add(new FixedActivity(manager.getNextId(), "Public lecture", ActivityCategory.CCA,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                EnergyRating.of(2), SensoryRating.of(2), null, null));

        RecurCommandParser parser = new RecurCommandParser(calendarFile);
        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, "2 week 1"));
        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, "3 week 1"));
    }

    @Test
    public void parse_missingCalendar_disablesOnlyRecurWithStorageError() {
        Path missing = tempDir.resolve("missing-calendar.txt");

        StorageException exception = assertThrows(StorageException.class,
                () -> new RecurCommandParser(missing).parse(manager, "1 week 1"));

        assertTrue(exception.getMessage().contains("file not found"));
        assertEquals(1, manager.size());
    }

    @Test
    public void parse_calendarCreatedAfterFirstFailure_stillRequiresRestart() throws Exception {
        Path missing = tempDir.resolve("late-calendar.txt");
        RecurCommandParser snapshotParser = new RecurCommandParser(missing);

        assertThrows(StorageException.class,
                () -> snapshotParser.parse(manager, "1 week 1;2"));
        RecurrenceTestData.writeCalendar(missing);

        assertThrows(StorageException.class,
                () -> snapshotParser.parse(manager, "1 week 1;2"));
        assertTrue(new RecurCommandParser(missing)
                .parse(manager, "1 week 1;2") instanceof RecurCommand);
    }
}
