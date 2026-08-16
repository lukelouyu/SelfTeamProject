package seedu.unienable.parser.activity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import seedu.unienable.command.activity.general.FindCommand;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.RelativeDateResolver;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.FieldParser;

/** Parses the find command's argument text into a FindCommand. */
class FindCommandParser {
    /** Every marker find recognises, including the repeatable k/ - used to bound field values. */
    private static final String[] FIND_MARKERS = { "k/", "c/", "topic/", "date/", "order/" };

    /** Every marker except k/ - these may each appear at most once, unlike k/. */
    private static final String[] SINGLE_VALUE_FIND_MARKERS = { "c/", "topic/", "date/", "order/" };

    /**
     * Parses a find command's argument text into a FindCommand. At least one keyword or filter is
     * required; find has no view/ option and always uses concise formatting. An optional
     * relative-date phrase ("today", "tomorrow", "this week", or "next week") may appear at the
     * very start of the text, before any markers - the same leading-phrase grammar {@code list}
     * uses, resolved with the same {@link RelativeDateResolver} so the two commands never disagree
     * on what "this week" or "next week" means. k/ may be repeated (e.g. {@code k/CS2113
     * k/review}); every occurrence's words are combined with AND, exactly like the words within a
     * single occurrence already are. Every other marker may still appear at most once.
     *
     * @param activityManager the manager the resulting command will read from
     * @param now the current date and time, used to resolve "today"/"tomorrow"/"this week"/
     *     "next week"
     * @param args the text after the "find" command word
     * @return the parsed FindCommand
     * @throws MissingInputException if neither a keyword, a filter, nor a relative-date phrase is
     *     supplied
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidCommandException if order is invalid, a k/ occurrence is blank or contains
     *     more than two words, a relative-date phrase is unrecognised or combined with date/,
     *     unrecognised text follows a relative-date phrase, or a non-repeatable marker is supplied
     *     more than once
     * @throws InvalidDateTimeException if the date is invalid
     */
    FindCommand parse(ActivityManager activityManager, LocalDateTime now, String args)
            throws MissingInputException, InvalidActivityException, InvalidCommandException,
            InvalidDateTimeException {
        RelativeDateAndRemainder relative = extractRelativeDate(now, args);
        FieldParser.rejectUnrecognisedLeadingText(relative.remainder, FIND_MARKERS);

        FieldParser.RepeatedField keywordOccurrences =
                FieldParser.extractAllOccurrences(relative.remainder, "k/", FIND_MARKERS);
        String remainderWithoutKeywords = keywordOccurrences.getRemainingText();
        FieldParser.rejectDuplicateMarkers(remainderWithoutKeywords, SINGLE_VALUE_FIND_MARKERS);
        Map<String, String> fields =
                FieldParser.extractPresentFields(remainderWithoutKeywords, SINGLE_VALUE_FIND_MARKERS);

        if (relative.isPresent() && fields.containsKey("date/")) {
            throw new InvalidCommandException(
                    "date/ cannot be combined with today, tomorrow, this week, or next week.");
        }
        List<String> keywords = collectKeywords(keywordOccurrences.getValues());
        if (!relative.isPresent() && keywords.isEmpty() && !hasFilter(fields)) {
            throw new MissingInputException("at least one keyword or filter is required.");
        }

        ActivityCategory category = fields.containsKey("c/") ? FieldParser.parseCategory(fields.get("c/")) : null;
        String topic = ActivityCommandParser.blankToNull(fields.get("topic/"));
        LocalDate date = fields.containsKey("date/") ? DateTimeParser.parseDate(fields.get("date/")) : relative.date;
        ActivityOrder order = fields.containsKey("order/")
                ? ActivityCommandParser.parseActivityOrder(fields.get("order/")) : null;

        return new FindCommand(activityManager, keywords,
                new ActivityFilter(null, category, topic, date, relative.dateFrom, relative.dateTo),
                order, false);
    }

    /**
     * Flattens every k/ occurrence's words into one AND-combined keyword list, in the order the
     * occurrences and their words appear. Every explicitly-supplied k/ occurrence must contain a
     * real value: a user who types k/ (or k/ followed only by whitespace) has supplied a
     * malformed keyword, not omitted one, so it is rejected here rather than silently contributing
     * no words - the same fail-fast principle already applied to an over-long occurrence.
     *
     * @param occurrences each k/ occurrence's raw (already-trimmed) value
     * @return every occurrence's words, flattened and in order
     * @throws InvalidCommandException if any occurrence is blank, or contains more than two words
     */
    private List<String> collectKeywords(List<String> occurrences) throws InvalidCommandException {
        List<String> keywords = new ArrayList<>();
        for (String occurrence : occurrences) {
            if (occurrence.isBlank()) {
                throw new InvalidCommandException("k/ value must not be blank.");
            }
            String[] words = occurrence.split("\\s+");
            if (words.length > 2) {
                throw new InvalidCommandException("keyword must contain one or two words.");
            }
            keywords.addAll(Arrays.asList(words));
        }
        return keywords;
    }

    /**
     * Consumes an optional leading relative-date phrase ("today", "tomorrow", "this week", or
     * "next week") from a find command's argument text, resolving it against now. Text that
     * already starts with a recognised find marker is left untouched (no relative-date phrase is
     * possible there); text that doesn't start with "today"/"tomorrow"/"this"/"next" either is
     * also left untouched, so an unrelated typo still surfaces as the existing "Unknown option"
     * message from {@link FieldParser#rejectUnrecognisedLeadingText} rather than a relative-date
     * specific one.
     *
     * @param now the current date and time
     * @param args the full text after the "find" command word
     * @return the resolved relative date (if any) plus the remaining text to parse as markers
     * @throws InvalidCommandException if "this"/"next" is not followed by "week"
     */
    private RelativeDateAndRemainder extractRelativeDate(LocalDateTime now, String args)
            throws InvalidCommandException {
        String trimmed = args.trim();
        if (trimmed.isEmpty() || startsWithKnownMarker(trimmed)) {
            return new RelativeDateAndRemainder(null, null, null, trimmed);
        }

        String[] words = trimmed.split("\\s+", 3);
        if ("today".equalsIgnoreCase(words[0])) {
            return new RelativeDateAndRemainder(RelativeDateResolver.today(now), null, null,
                    remainderAfter(trimmed, words, 1));
        }
        if ("tomorrow".equalsIgnoreCase(words[0])) {
            return new RelativeDateAndRemainder(RelativeDateResolver.tomorrow(now), null, null,
                    remainderAfter(trimmed, words, 1));
        }
        if ("this".equalsIgnoreCase(words[0])) {
            if (words.length < 2 || !"week".equalsIgnoreCase(words[1])) {
                throw new InvalidCommandException(
                        "Unknown find option \"this" + (words.length > 1 ? " " + words[1] : "")
                                + "\"; only \"this week\" is supported.");
            }
            LocalDate monday = RelativeDateResolver.mondayOfThisWeek(now);
            return new RelativeDateAndRemainder(null, monday, monday.plusDays(6),
                    remainderAfter(trimmed, words, 2));
        }
        if ("next".equalsIgnoreCase(words[0])) {
            if (words.length < 2 || !"week".equalsIgnoreCase(words[1])) {
                throw new InvalidCommandException(
                        "Unknown find option \"next" + (words.length > 1 ? " " + words[1] : "")
                                + "\"; only \"next week\" is supported.");
            }
            LocalDate monday = RelativeDateResolver.mondayOfNextWeek(now);
            return new RelativeDateAndRemainder(null, monday, monday.plusDays(6),
                    remainderAfter(trimmed, words, 2));
        }
        return new RelativeDateAndRemainder(null, null, null, trimmed);
    }

    private boolean startsWithKnownMarker(String text) {
        for (String marker : FIND_MARKERS) {
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
     * Carries a resolved relative date (exact or range) plus the marker text left to parse. date
     * and dateFrom/dateTo are mutually exclusive, matching {@link ActivityFilter}'s own contract.
     */
    private static final class RelativeDateAndRemainder {
        private final LocalDate date;
        private final LocalDate dateFrom;
        private final LocalDate dateTo;
        private final String remainder;

        private RelativeDateAndRemainder(LocalDate date, LocalDate dateFrom, LocalDate dateTo, String remainder) {
            this.date = date;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.remainder = remainder;
        }

        private boolean isPresent() {
            return date != null || dateFrom != null;
        }
    }

    /**
     * Checks whether a find command's extracted fields include at least one real filter. order/
     * alone does not count: it is a display-ordering directive, not something to search by, so
     * "find order/time" with nothing else must still be rejected. A whitespace-only topic/ also
     * does not count, the same way it does not count as a stored topic anywhere else: "find
     * topic/   " with nothing else must still be rejected rather than matching everything.
     * Whether any keyword was supplied is checked separately, via the flattened keyword list.
     *
     * @param fields the non-keyword fields extracted from a find command's argument text
     * @return true if at least one of c/, a non-blank topic/, or date/ is present
     */
    private boolean hasFilter(Map<String, String> fields) {
        return fields.containsKey("c/")
                || ActivityCommandParser.blankToNull(fields.get("topic/")) != null
                || fields.containsKey("date/");
    }
}
