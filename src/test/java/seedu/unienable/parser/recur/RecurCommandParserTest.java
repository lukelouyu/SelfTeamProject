package seedu.unienable.parser.recur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
        RecurCommand command = new RecurCommandParser(calendarFile).parse(manager, RecurrenceTestData.NOW,
                "1 week 1 to 6; 7 to 13");

        assertEquals(11, command.getPlan().getOccurrencesToCreate().size());
    }

    @Test
    public void parse_singleRangeAcrossRecess_matchesSplitRangeInOneCommand() throws Exception {
        // "1 to 13" is one inclusive range spanning the Week 6/Week 7 recess gap in the fixture
        // calendar (Week 6 ends 2026-09-18, Week 7 starts 2026-09-28). It must not need to be
        // split into "1 to 6; 7 to 13" to cover the full semester in one recur command.
        RecurCommand splitRange = new RecurCommandParser(calendarFile).parse(manager, RecurrenceTestData.NOW,
                "1 week 1 to 6; 7 to 13");
        RecurCommand singleRange = new RecurCommandParser(calendarFile).parse(manager, RecurrenceTestData.NOW,
                "1 week 1 to 13");

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                splitRange.getPlan().getRequestedWeeks());
        assertEquals(splitRange.getPlan().getRequestedWeeks(), singleRange.getPlan().getRequestedWeeks());
        assertEquals(splitRange.getPlan().getOccurrencesToCreate().size(),
                singleRange.getPlan().getOccurrencesToCreate().size());
        assertEquals(11, singleRange.getPlan().getOccurrencesToCreate().size());
    }

    @Test
    public void parse_singleWeekRangePlusSourceWeek_generatesExactlyOneOccurrence() throws Exception {
        // "5 to 5" is a range whose start equals its end - it must be accepted and resolve to
        // exactly the one week, generating exactly one new occurrence (the fixture source
        // activity is in Week 1, which the spec must also include and which is always skipped as
        // the source itself, not counted as a new occurrence).
        RecurCommand command = new RecurCommandParser(calendarFile).parse(manager, RecurrenceTestData.NOW,
                "1 week 1;5 to 5");

        assertEquals(List.of(1, 5), command.getPlan().getRequestedWeeks());
        assertEquals(1, command.getPlan().getOccurrencesToCreate().size());
        assertEquals(5, command.getPlan().getOccurrencesToCreate().get(0).getWeekNumber());
    }

    @Test
    public void parse_commandKeywordAndRangeAreCaseInsensitive() throws Exception {
        RecurCommand command = new RecurCommandParser(calendarFile).parse(manager, RecurrenceTestData.NOW,
                "1 WEEK 1 TO 2");

        assertEquals(1, command.getPlan().getOccurrencesToCreate().size());
    }

    @Test
    public void parse_blankOrMalformedSyntax_rejectsClearly() {
        RecurCommandParser parser = new RecurCommandParser(calendarFile);

        assertThrows(MissingInputException.class, () -> parser.parse(manager, RecurrenceTestData.NOW, ""));
        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, RecurrenceTestData.NOW, "1 1 to 6"));
        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, RecurrenceTestData.NOW, "one week 1"));
    }

    @Test
    public void parse_unknownId_throwsInvalidIndexException() {
        assertThrows(InvalidIndexException.class,
                () -> new RecurCommandParser(calendarFile).parse(manager, RecurrenceTestData.NOW, "99 week 1"));
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
        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, RecurrenceTestData.NOW, "2 week 1"));
        assertThrows(InvalidActivityException.class, () -> parser.parse(manager, RecurrenceTestData.NOW, "3 week 1"));
    }

    @Test
    public void parse_missingCalendar_disablesOnlyRecurWithStorageError() {
        Path missing = tempDir.resolve("missing-calendar.txt");

        StorageException exception = assertThrows(StorageException.class,
                () -> new RecurCommandParser(missing).parse(manager, RecurrenceTestData.NOW, "1 week 1"));

        assertTrue(exception.getMessage().contains("file not found"));
        assertEquals(1, manager.size());
    }

    @Test
    public void parse_calendarCreatedAfterFirstFailure_stillRequiresRestart() throws Exception {
        Path missing = tempDir.resolve("late-calendar.txt");
        RecurCommandParser snapshotParser = new RecurCommandParser(missing);

        assertThrows(StorageException.class,
                () -> snapshotParser.parse(manager, RecurrenceTestData.NOW, "1 week 1;2"));
        RecurrenceTestData.writeCalendar(missing);

        assertThrows(StorageException.class,
                () -> snapshotParser.parse(manager, RecurrenceTestData.NOW, "1 week 1;2"));
        assertTrue(new RecurCommandParser(missing)
                .parse(manager, RecurrenceTestData.NOW, "1 week 1;2") instanceof RecurCommand);
    }
}
