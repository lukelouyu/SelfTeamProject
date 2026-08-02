package seedu.unienable.parser.timetable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import seedu.unienable.command.timetable.TimetableCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.timetable.TimetableService;
import seedu.unienable.model.timetable.TimetableMode;
import seedu.unienable.model.timetable.TimetablePeriod;
import seedu.unienable.parser.common.DateTimeParser;

/** Parses one-shot day and week timetable requests. */
public class TimetableCommandParser {
    private static final String DAY_MARKER = "day/";
    private static final String WEEK_MARKER = "week/";

    /** Parses the text after {@code timetable}. */
    public TimetableCommand parse(ActivityManager activityManager, LocalDateTime now, String args)
            throws MissingInputException, InvalidCommandException, InvalidDateTimeException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new MissingInputException(
                    "timetable requires day/YYYY-MM-DD, week/YYYY-MM-DD, or this week.");
        }
        String[] words = trimmed.split("\\s+");
        TimetablePeriod period;
        int consumedWords;
        if ("this".equalsIgnoreCase(words[0])) {
            if (words.length < 2 || !"week".equalsIgnoreCase(words[1])) {
                throw new InvalidCommandException("Unknown timetable option \"this"
                        + (words.length > 1 ? " " + words[1] : "")
                        + "\"; only \"this week\" is supported.");
            }
            period = TimetableService.resolveThisWeek(now);
            consumedWords = 2;
        } else if (startsWith(words[0], DAY_MARKER)) {
            period = TimetableService.resolveDay(parseDateValue(words[0], DAY_MARKER));
            consumedWords = 1;
        } else if (startsWith(words[0], WEEK_MARKER)) {
            period = TimetableService.resolveWeek(parseDateValue(words[0], WEEK_MARKER));
            consumedWords = 1;
        } else {
            throw new InvalidCommandException("Unknown timetable option \"" + words[0] + "\".");
        }

        TimetableMode mode = parseMode(words, consumedWords, period.isWeekly());
        return new TimetableCommand(activityManager, period, mode);
    }

    private boolean startsWith(String word, String marker) {
        return word.toLowerCase(Locale.ROOT).startsWith(marker);
    }

    private LocalDate parseDateValue(String word, String marker)
            throws MissingInputException, InvalidDateTimeException {
        String value = word.substring(marker.length());
        if (value.isEmpty()) {
            throw new MissingInputException(marker + " requires a value in yyyy-MM-dd format.");
        }
        return DateTimeParser.parseDate(value);
    }

    private TimetableMode parseMode(String[] words, int consumedWords, boolean weekly)
            throws InvalidCommandException {
        if (words.length == consumedWords) {
            return TimetableMode.NORMAL;
        }
        if (words.length != consumedWords + 1) {
            throw new InvalidCommandException(
                    "Unexpected text after the timetable period - use at most one mode.");
        }
        if ("detail".equalsIgnoreCase(words[consumedWords])) {
            return TimetableMode.DETAIL;
        }
        if ("compact".equalsIgnoreCase(words[consumedWords])) {
            if (!weekly) {
                throw new InvalidCommandException("compact is only available for weekly timetables.");
            }
            return TimetableMode.COMPACT;
        }
        throw new InvalidCommandException(
                "Unknown timetable mode \"" + words[consumedWords] + "\".");
    }
}
