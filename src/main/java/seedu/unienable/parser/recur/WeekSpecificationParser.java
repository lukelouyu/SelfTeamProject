package seedu.unienable.parser.recur;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;

/** Parses semicolon-separated week numbers and inclusive ranges for {@code recur}. */
public class WeekSpecificationParser {
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "^(\\d+)(?:\\s*to\\s*(\\d+))?$", Pattern.CASE_INSENSITIVE);
    private static final int MAX_ITEMS_PER_COMMAND = 10_000;

    /**
     * Parses and normalizes a week specification into strictly increasing week numbers.
     *
     * @throws MissingInputException if the specification is blank
     * @throws InvalidCommandException if an item/range is invalid, duplicated, or overlapping
     */
    public List<Integer> parse(String specification)
            throws MissingInputException, InvalidCommandException {
        if (specification == null || specification.isBlank()) {
            throw new MissingInputException("week specification is required.");
        }

        Set<Integer> weeks = new TreeSet<>();
        String[] items = specification.trim().split(";", -1);
        for (String rawItem : items) {
            String item = rawItem.trim();
            if (item.isEmpty()) {
                throw invalidSpecification();
            }
            Matcher matcher = ITEM_PATTERN.matcher(item);
            if (!matcher.matches()) {
                throw invalidSpecification();
            }
            int start = parsePositiveWeek(matcher.group(1));
            int end = matcher.group(2) == null ? start : parsePositiveWeek(matcher.group(2));
            if (end < start) {
                throw new InvalidCommandException("week range must run from a smaller week "
                        + "number to a larger one.");
            }
            if ((long) end - start + 1 > MAX_ITEMS_PER_COMMAND) {
                throw new InvalidCommandException("week specification contains too many weeks.");
            }
            for (int week = start; ; week++) {
                if (weeks.size() >= MAX_ITEMS_PER_COMMAND) {
                    throw new InvalidCommandException(
                            "week specification contains too many weeks.");
                }
                if (!weeks.add(week)) {
                    throw new InvalidCommandException("week specification contains duplicate "
                            + "or overlapping Week " + week + ".");
                }
                if (week == end) {
                    break;
                }
            }
        }
        return new ArrayList<>(weeks);
    }

    private int parsePositiveWeek(String value) throws InvalidCommandException {
        try {
            int week = Integer.parseInt(value);
            if (week <= 0) {
                throw new NumberFormatException();
            }
            return week;
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("week number must be a positive whole number.");
        }
    }

    private InvalidCommandException invalidSpecification() {
        return new InvalidCommandException("week specification must use numbers or inclusive "
                + "ranges separated by semicolons, for example 1 to 6; 7 to 13.");
    }
}
