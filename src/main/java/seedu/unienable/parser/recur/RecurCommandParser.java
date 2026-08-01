package seedu.unienable.parser.recur;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.unienable.command.recur.RecurCommand;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.exception.StorageException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.recur.ClassSchedulePolicy;
import seedu.unienable.logic.recur.RecurrencePlanner;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.recur.AcademicCalendar;
import seedu.unienable.model.recur.RecurrencePlan;
import seedu.unienable.storage.recur.AcademicCalendarStorage;

/** Parses {@code recur TASK_ID week WEEK_SPEC} using one cached calendar snapshot per run. */
public class RecurCommandParser {
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "^(\\d+)\\s+week\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private final Path calendarFile;
    private final AcademicCalendarStorage calendarStorage = new AcademicCalendarStorage();
    private final WeekSpecificationParser weekParser = new WeekSpecificationParser();
    private final ClassSchedulePolicy classSchedulePolicy = new ClassSchedulePolicy();
    private final RecurrencePlanner recurrencePlanner = new RecurrencePlanner();
    private AcademicCalendar loadedCalendar;
    private StorageException calendarLoadFailure;
    private boolean calendarLoadAttempted;

    /** Creates a parser backed by the supplied external calendar path. */
    public RecurCommandParser(Path calendarFile) {
        this.calendarFile = calendarFile;
    }

    /**
     * Parses, validates, and preflights one recurrence command without mutating activities.
     */
    public RecurCommand parse(ActivityManager activityManager, String arguments)
            throws MissingInputException, InvalidCommandException, InvalidIndexException,
            InvalidActivityException, DuplicateActivityException, StorageException {
        if (arguments == null || arguments.isBlank()) {
            throw new MissingInputException("recur requires TASK_ID, week, and WEEK_SPEC.");
        }
        Matcher matcher = COMMAND_PATTERN.matcher(arguments.trim());
        if (!matcher.matches()) {
            throw new InvalidCommandException(
                    "recur format is: recur TASK_ID week WEEK_SPEC.");
        }

        int id = parseId(matcher.group(1));
        Activity source = activityManager.getById(id);
        if (!classSchedulePolicy.isClassSchedule(source)) {
            throw new InvalidActivityException("recur requires a fixed ACADEMIC lecture, "
                    + "tutorial, lab, or section-teaching activity.");
        }

        List<Integer> requestedWeeks = weekParser.parse(matcher.group(2));
        AcademicCalendar calendar = getCalendar();
        RecurrencePlan plan = recurrencePlanner.plan((FixedActivity) source,
                requestedWeeks, calendar, activityManager);
        return new RecurCommand(activityManager, plan);
    }

    private int parseId(String value) throws InvalidCommandException {
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("activity ID must be a positive whole number.");
        }
    }

    private AcademicCalendar getCalendar() throws StorageException {
        if (!calendarLoadAttempted) {
            calendarLoadAttempted = true;
            try {
                loadedCalendar = calendarStorage.load(calendarFile);
            } catch (StorageException e) {
                calendarLoadFailure = e;
            }
        }
        if (calendarLoadFailure != null) {
            throw new StorageException(calendarLoadFailure.getMessage(), calendarLoadFailure);
        }
        return loadedCalendar;
    }
}
