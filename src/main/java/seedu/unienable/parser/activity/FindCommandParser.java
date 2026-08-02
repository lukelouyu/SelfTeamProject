package seedu.unienable.parser.activity;

import java.time.LocalDate;
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
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.FieldParser;

/** Parses the find command's argument text into a FindCommand. */
class FindCommandParser {
    private static final String[] FIND_MARKERS = { "k/", "c/", "topic/", "date/", "order/" };

    /**
     * Parses a find command's argument text into a FindCommand. At least one keyword or filter is
     * required; find has no view/ option and always uses concise formatting.
     *
     * @param activityManager the manager the resulting command will read from
     * @param args the text after the "find" command word
     * @return the parsed FindCommand
     * @throws MissingInputException if neither a keyword nor a filter is supplied
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidCommandException if order is invalid, or k/ contains more than two words
     * @throws InvalidDateTimeException if the date is invalid
     */
    FindCommand parse(ActivityManager activityManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidCommandException,
            InvalidDateTimeException {
        FieldParser.rejectUnrecognisedLeadingText(args, FIND_MARKERS);
        Map<String, String> fields = FieldParser.extractPresentFields(args, FIND_MARKERS);
        if (!hasKeywordOrFilter(fields)) {
            throw new MissingInputException("at least one keyword or filter is required.");
        }

        String rawKeywords = ActivityCommandParser.blankToNull(fields.get("k/"));
        List<String> keywords = rawKeywords != null
                ? Arrays.asList(rawKeywords.split("\\s+"))
                : List.of();
        if (keywords.size() > 2) {
            throw new InvalidCommandException("keyword must contain one or two words.");
        }
        ActivityCategory category = fields.containsKey("c/") ? FieldParser.parseCategory(fields.get("c/")) : null;
        String topic = ActivityCommandParser.blankToNull(fields.get("topic/"));
        LocalDate date = fields.containsKey("date/") ? DateTimeParser.parseDate(fields.get("date/")) : null;
        ActivityOrder order = fields.containsKey("order/")
                ? ActivityCommandParser.parseActivityOrder(fields.get("order/")) : null;

        return new FindCommand(activityManager, keywords, new ActivityFilter(null, category, topic, date),
                order, false);
    }

    /**
     * Checks whether a find command's extracted fields include at least one real keyword or
     * filter. order/ alone does not count: it is a display-ordering directive, not something to
     * search by, so "find order/time" with nothing else must still be rejected. A whitespace-only
     * topic/ also does not count, the same way it does not count as a stored topic anywhere else:
     * "find topic/   " with nothing else must still be rejected rather than matching everything.
     * A whitespace-only k/ does not count either, for the same reason: without this check, the
     * blank keyword survives as a single empty-string token that every activity's fields trivially
     * "contain," so "find k/   " with nothing else would silently match every activity instead of
     * being rejected.
     *
     * @param fields the fields extracted from a find command's argument text
     * @return true if at least one of a non-blank k/, c/, a non-blank topic/, or date/ is present
     */
    private boolean hasKeywordOrFilter(Map<String, String> fields) {
        return ActivityCommandParser.blankToNull(fields.get("k/")) != null || fields.containsKey("c/")
                || ActivityCommandParser.blankToNull(fields.get("topic/")) != null || fields.containsKey("date/");
    }
}
