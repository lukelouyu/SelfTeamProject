package seedu.unienable.parser.activity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import seedu.unienable.command.activity.general.ListCommand;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.enums.CompletionStatus;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.FieldParser;

/** Parses the list command's argument text into a ListCommand. */
class ListCommandParser {
    private static final String[] LIST_MARKERS = { "view/", "status/", "c/", "topic/", "date/", "order/" };

    /**
     * Parses a list command's argument text into a ListCommand. Every marker field is optional
     * and may appear in any order; an optional relative-date phrase ("today", "tomorrow", "this
     * week", "next week", or "overdue") may additionally appear at the very start of the text,
     * before any markers.
     *
     * @param activityManager the manager the resulting command will read from
     * @param now the current date and time, used to resolve "today"/"tomorrow"/"this week"/
     *     "next week"/"overdue"
     * @param args the text after the "list" command word
     * @return the parsed ListCommand
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidCommandException if status or order is invalid, a relative-date phrase is
     *     unrecognised, a relative-date phrase is combined with date/ or another relative-date
     *     phrase, status/ is combined with overdue, or unrecognised text follows a relative-date
     *     phrase
     * @throws InvalidDateTimeException if the date is invalid
     */
    ListCommand parse(ActivityManager activityManager, LocalDateTime now, String args)
            throws InvalidActivityException, InvalidCommandException, InvalidDateTimeException {
        RelativeDateAndRemainder parsed = extractRelativeDate(now, args);
        Map<String, String> fields = FieldParser.extractPresentFields(parsed.remainder, LIST_MARKERS);
        if (parsed.hasRelativeDate() && fields.containsKey("date/")) {
            throw new InvalidCommandException(
                    "date/ cannot be combined with today, tomorrow, this week, or next week.");
        }
        if (parsed.overdue && fields.containsKey("status/")) {
            throw new InvalidCommandException("status/ cannot be combined with overdue - overdue already "
                    + "means incomplete.");
        }

        boolean detail = parseViewMode(fields.get("view/"));
        CompletionStatus status = parsed.overdue ? null : parseStatus(fields.get("status/"));
        ActivityCategory category = fields.containsKey("c/") ? FieldParser.parseCategory(fields.get("c/")) : null;
        String topic = ActivityCommandParser.blankToNull(fields.get("topic/"));
        LocalDate date = fields.containsKey("date/") ? DateTimeParser.parseDate(fields.get("date/")) : parsed.date;
        ActivityOrder order = fields.containsKey("order/")
                ? ActivityCommandParser.parseActivityOrder(fields.get("order/")) : null;

        ActivityFilter filter = new ActivityFilter(status, category, topic, date, parsed.dateFrom, parsed.dateTo);
        LocalDateTime overdueAsOf = parsed.overdue ? now : null;
        return new ListCommand(activityManager, filter, order, detail, overdueAsOf);
    }

    /**
     * Consumes an optional leading relative-date phrase ("today", "tomorrow", "this week", "next
     * week", or "overdue") from a list command's argument text, resolving it against now. Text
     * that already starts with a recognised list marker is left untouched (no relative-date
     * phrase is possible there), preserving every existing marker-only usage exactly as before.
     *
     * @param now the current date and time
     * @param args the full text after the "list" command word
     * @return the resolved relative date (if any) plus the remaining text to parse as markers
     * @throws InvalidCommandException if the leading text is not blank, does not start with a
     *     known marker, and is not a recognised relative-date phrase; or if a relative-date
     *     phrase is followed by unrecognised text
     */
    private RelativeDateAndRemainder extractRelativeDate(LocalDateTime now, String args)
            throws InvalidCommandException {
        String trimmed = args.trim();
        if (trimmed.isEmpty() || startsWithKnownMarker(trimmed)) {
            return new RelativeDateAndRemainder(null, null, null, false, trimmed);
        }

        String[] words = trimmed.split("\\s+", 3);
        LocalDate today = now.toLocalDate();
        RelativeDateAndRemainder result;
        if ("today".equalsIgnoreCase(words[0])) {
            result = new RelativeDateAndRemainder(today, null, null, false, remainderAfter(trimmed, words, 1));
        } else if ("tomorrow".equalsIgnoreCase(words[0])) {
            result = new RelativeDateAndRemainder(today.plusDays(1), null, null, false,
                    remainderAfter(trimmed, words, 1));
        } else if ("overdue".equalsIgnoreCase(words[0])) {
            result = new RelativeDateAndRemainder(null, null, null, true, remainderAfter(trimmed, words, 1));
        } else if ("this".equalsIgnoreCase(words[0])) {
            if (words.length < 2 || !"week".equalsIgnoreCase(words[1])) {
                throw new InvalidCommandException(
                        "Unknown list option \"this" + (words.length > 1 ? " " + words[1] : "")
                                + "\"; only \"this week\" is supported.");
            }
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            result = new RelativeDateAndRemainder(null, monday, sunday, false, remainderAfter(trimmed, words, 2));
        } else if ("next".equalsIgnoreCase(words[0])) {
            if (words.length < 2 || !"week".equalsIgnoreCase(words[1])) {
                throw new InvalidCommandException(
                        "Unknown list option \"next" + (words.length > 1 ? " " + words[1] : "")
                                + "\"; only \"next week\" is supported.");
            }
            LocalDate nextMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(7);
            LocalDate nextSunday = nextMonday.plusDays(6);
            result = new RelativeDateAndRemainder(null, nextMonday, nextSunday, false,
                    remainderAfter(trimmed, words, 2));
        } else {
            throw new InvalidCommandException("Unknown list option \"" + words[0] + "\".");
        }

        if (!result.remainder.isEmpty() && !startsWithKnownMarker(result.remainder)) {
            String nextWord = result.remainder.split("\\s+", 2)[0];
            if (isRelativeDateWord(nextWord)) {
                throw new InvalidCommandException("today, tomorrow, this week, next week, and overdue "
                        + "cannot be combined with each other.");
            }
            throw new InvalidCommandException("Unknown list option \"" + nextWord + "\".");
        }
        return result;
    }

    /** Returns whether word is the leading word of one of list's own relative-date phrases. */
    private boolean isRelativeDateWord(String word) {
        return "today".equalsIgnoreCase(word) || "tomorrow".equalsIgnoreCase(word)
                || "this".equalsIgnoreCase(word) || "next".equalsIgnoreCase(word)
                || "overdue".equalsIgnoreCase(word);
    }

    private boolean startsWithKnownMarker(String text) {
        for (String marker : LIST_MARKERS) {
            if (FieldParser.indexOfMarker(text, marker, 0) == 0) {
                return true;
            }
        }
        return false;
    }

    private String remainderAfter(String trimmed, String[] words, int wordCount) {
        int consumed = 0;
        for (int i = 0; i < wordCount; i++) {
            consumed = trimmed.indexOf(words[i], consumed) + words[i].length();
        }
        return trimmed.substring(consumed).trim();
    }

    /**
     * Carries a resolved relative date (exact or range), or the "overdue" flag, plus the marker
     * text left to parse. date/dateFrom/dateTo and overdue are mutually exclusive - "overdue"
     * doesn't resolve to any date, since it's a completion+time condition, not a date filter.
     */
    private static final class RelativeDateAndRemainder {
        private final LocalDate date;
        private final LocalDate dateFrom;
        private final LocalDate dateTo;
        private final boolean overdue;
        private final String remainder;

        private RelativeDateAndRemainder(LocalDate date, LocalDate dateFrom, LocalDate dateTo, boolean overdue,
                String remainder) {
            this.date = date;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.overdue = overdue;
            this.remainder = remainder;
        }

        private boolean hasRelativeDate() {
            return date != null || dateFrom != null;
        }
    }

    /**
     * Parses list's optional view/ marker into a detail/concise flag. Unlike status/ (which
     * accepts null to mean "all"), a present-but-unrecognised view/ value is rejected rather than
     * silently falling back to concise, so a typo like "view/nonsense" is not mistaken for a
     * request that simply happens to match the concise default.
     *
     * @param text the raw view/ value, or null if not supplied
     * @return true for "detail", false if not supplied or "concise"
     * @throws InvalidCommandException if text is supplied and is neither "concise" nor "detail"
     */
    private boolean parseViewMode(String text) throws InvalidCommandException {
        if (text == null || "concise".equalsIgnoreCase(text)) {
            return false;
        }
        if ("detail".equalsIgnoreCase(text)) {
            return true;
        }
        throw new InvalidCommandException("view must be concise or detail.");
    }

    private CompletionStatus parseStatus(String text) throws InvalidCommandException {
        if (text == null || "all".equalsIgnoreCase(text)) {
            return null;
        }
        if ("completed".equalsIgnoreCase(text)) {
            return CompletionStatus.COMPLETE;
        }
        if ("incomplete".equalsIgnoreCase(text)) {
            return CompletionStatus.INCOMPLETE;
        }
        throw new InvalidCommandException("status must be all, completed, or incomplete.");
    }
}
