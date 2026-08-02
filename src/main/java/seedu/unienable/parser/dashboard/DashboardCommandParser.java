package seedu.unienable.parser.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import seedu.unienable.command.dashboard.DashboardCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.dashboard.DashboardService;
import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.parser.common.DateTimeParser;

/**
 * Parses {@code dashboard today|tomorrow|date/YYYY-MM-DD|this week [detail]} into a
 * {@link DashboardCommand}. Exactly one period selector is required; {@code detail} is an
 * optional single trailing keyword. See
 * {@code docs/tasks/v2/dashboard/ACCEPTANCE_CRITERIA.md} for the full accept/reject list.
 */
public class DashboardCommandParser {
    private static final String DATE_MARKER = "date/";

    /**
     * Parses a dashboard command's argument text.
     *
     * @param activityManager the manager the resulting command will read from
     * @param now the current date and time, used to resolve {@code today}/{@code tomorrow}/
     *     {@code this week}
     * @param args the text after the "dashboard" command word
     * @return the parsed DashboardCommand
     * @throws MissingInputException if no selector, or {@code date/} with no value, is supplied
     * @throws InvalidCommandException if the selector is unrecognised, or trailing text after the
     *     selector is anything other than a single {@code detail} keyword
     * @throws InvalidDateTimeException if a supplied {@code date/} value is malformed or names a
     *     calendar date that does not exist
     */
    public DashboardCommand parse(ActivityManager activityManager, LocalDateTime now, String args)
            throws MissingInputException, InvalidCommandException, InvalidDateTimeException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new MissingInputException(
                    "dashboard requires a period: today, tomorrow, date/YYYY-MM-DD, or this week.");
        }
        String[] words = trimmed.split("\\s+");

        DashboardPeriod period;
        int consumedWords;
        if ("today".equalsIgnoreCase(words[0])) {
            period = DashboardService.resolveToday(now);
            consumedWords = 1;
        } else if ("tomorrow".equalsIgnoreCase(words[0])) {
            period = DashboardService.resolveTomorrow(now);
            consumedWords = 1;
        } else if ("this".equalsIgnoreCase(words[0])) {
            if (words.length < 2 || !"week".equalsIgnoreCase(words[1])) {
                throw new InvalidCommandException("Unknown dashboard option \"this"
                        + (words.length > 1 ? " " + words[1] : "") + "\"; only \"this week\" is supported.");
            }
            period = DashboardService.resolveThisWeek(now);
            consumedWords = 2;
        } else if (startsWithDateMarker(words[0])) {
            period = DashboardService.resolveDate(parseDateValue(words[0]));
            consumedWords = 1;
        } else {
            throw new InvalidCommandException("Unknown dashboard option \"" + words[0] + "\".");
        }

        boolean detail = parseDetailFlag(words, consumedWords);
        return new DashboardCommand(activityManager, period, now, detail);
    }

    private boolean startsWithDateMarker(String word) {
        return word.toLowerCase(Locale.ROOT).startsWith(DATE_MARKER);
    }

    private LocalDate parseDateValue(String word) throws MissingInputException, InvalidDateTimeException {
        String value = word.substring(DATE_MARKER.length());
        if (value.isEmpty()) {
            throw new MissingInputException("date/ requires a value in yyyy-MM-dd format.");
        }
        return DateTimeParser.parseDate(value);
    }

    /**
     * Checks the words after the period selector: either nothing, or exactly one {@code detail}
     * keyword.
     *
     * @param words every whitespace-separated word in the argument text
     * @param consumedWords how many leading words the period selector itself used
     * @return true if {@code detail} was supplied
     * @throws InvalidCommandException if anything other than a single trailing {@code detail} follows
     */
    private boolean parseDetailFlag(String[] words, int consumedWords) throws InvalidCommandException {
        if (consumedWords == words.length) {
            return false;
        }
        if (consumedWords + 1 != words.length || !"detail".equalsIgnoreCase(words[consumedWords])) {
            throw new InvalidCommandException(
                    "Unexpected text after the dashboard period - only a single trailing \"detail\" is accepted.");
        }
        return true;
    }
}
