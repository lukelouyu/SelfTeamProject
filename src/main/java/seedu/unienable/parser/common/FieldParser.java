package seedu.unienable.parser.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import seedu.unienable.accessibility.enums.AccessibilityStatus;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Utility methods for extracting field values between known markers in a command's argument
 * text. Marker matching is case-insensitive (e.g. "N/" matches the "n/" marker), matching the
 * User Guide's documented rule that field prefixes are case-insensitive; the extracted value
 * itself is returned with its original case intact.
 *
 * <p>Also home to a handful of small value parsers and argument-shape checks whose rules are
 * genuinely shared by more than one command-specific parser (category/status enum parsing,
 * the storage-delimiter check, the "no arguments" and "no unrecognised leading text" guards),
 * so those rules can't silently drift apart between commands.
 */
public class FieldParser {
    /**
     * Extracts the value between a start marker and an end marker.
     * If endMarker is null or not found, extracts until the end of the input.
     * Markers are only matched at a field boundary (see {@link #indexOfMarker}), so a marker
     * that happens to appear as a trailing substring of another marker or of a value (e.g. "c/"
     * inside "topic/") is not mistaken for a separate field.
     *
     * @param input the full argument text
     * @param startMarker the marker just before the value, e.g. "n/"
     * @param endMarker the marker just after the value, or null to read until the end of input
     * @return the trimmed value, or null if startMarker is not found
     */
    public static String extractField(String input, String startMarker, String endMarker) {
        int startIndex = indexOfMarker(input, startMarker, 0);
        if (startIndex == -1) {
            return null;
        }
        startIndex += startMarker.length();

        int endIndex;
        if (endMarker != null) {
            endIndex = indexOfMarker(input, endMarker, startIndex);
            if (endIndex == -1) {
                endIndex = input.length();
            }
        } else {
            endIndex = input.length();
        }

        return input.substring(startIndex, endIndex).trim();
    }

    /**
     * Finds the first occurrence of marker (matched case-insensitively) that starts at a field
     * boundary: index 0, or immediately preceded by whitespace. This prevents a marker that is
     * itself a trailing substring of another marker or of a field's value (e.g. "c/" is a
     * substring of "topic/") from being mistaken for a separate field.
     *
     * @param text the text to search
     * @param marker the marker to find
     * @param fromIndex the index to start searching from
     * @return the boundary-matched index, or -1 if none is found
     */
    public static int indexOfMarker(String text, String marker, int fromIndex) {
        int index = indexOfIgnoreCase(text, marker, fromIndex);
        while (index > 0 && !Character.isWhitespace(text.charAt(index - 1))) {
            index = indexOfIgnoreCase(text, marker, index + 1);
        }
        return index;
    }

    /**
     * Returns the trimmed text before the first occurrence of any of the given markers - the
     * portion of a command's argument text that marker-based extraction never looks at, and so
     * would otherwise be silently discarded instead of rejected. If text contains none of the
     * given markers at all, the entire (trimmed) text counts as "before the first marker", since
     * none of it is ever captured as a field value either.
     *
     * @param text the full argument text
     * @param markers every marker the calling command recognises
     * @return the trimmed leading text, or an empty string if text starts with a recognised
     *     marker (or is itself blank)
     */
    public static String leadingUnrecognisedText(String text, String... markers) {
        int firstMarkerIndex = -1;
        for (String marker : markers) {
            int index = indexOfMarker(text, marker, 0);
            if (index != -1 && (firstMarkerIndex == -1 || index < firstMarkerIndex)) {
                firstMarkerIndex = index;
            }
        }
        String leading = firstMarkerIndex == -1 ? text : text.substring(0, firstMarkerIndex);
        return leading.trim();
    }

    /**
     * Extracts the value between startMarker and endMarker, requiring it to be present.
     *
     * @param args the full argument text
     * @param startMarker the marker just before the value
     * @param endMarker the marker just after the value, or null to read until the end of input
     * @param fieldName the field name to use in the error message if the value is missing
     * @return the trimmed, non-empty value
     * @throws MissingInputException if startMarker is absent or its value is blank
     */
    public static String requireField(String args, String startMarker, String endMarker, String fieldName)
            throws MissingInputException {
        String value = extractField(args, startMarker, endMarker);
        if (value == null || value.isEmpty()) {
            throw new MissingInputException(fieldName + " is required.");
        }
        return value;
    }

    /**
     * Rejects any non-blank text for a command documented as taking no arguments at all, so a
     * typo'd trailing word (e.g. "next tomorrow") does not silently execute as if it were never
     * typed.
     *
     * @param commandName the command name to use in the error message
     * @param args the text after the command word
     * @throws InvalidCommandException if args is not blank
     */
    public static void requireNoArguments(String commandName, String args) throws InvalidCommandException {
        if (!args.trim().isEmpty()) {
            throw new InvalidCommandException("\"" + commandName + "\" does not take any arguments.");
        }
    }

    /**
     * Rejects text that appears before the first marker this command recognises (or, if none of
     * the given markers appear at all, any non-blank text at all) - see
     * {@link #leadingUnrecognisedText}.
     *
     * @param args the full argument text
     * @param markers every marker this command recognises
     * @throws InvalidCommandException if unrecognised leading text is present
     */
    public static void rejectUnrecognisedLeadingText(String args, String... markers)
            throws InvalidCommandException {
        String leading = leadingUnrecognisedText(args, markers);
        if (!leading.isEmpty()) {
            throw new InvalidCommandException("Unknown option \"" + leading + "\".");
        }
    }

    /**
     * Rejects a marker that occurs more than once at a field boundary anywhere in the text, e.g.
     * "n/A n/B" - without this, extraction simply reads from the first occurrence to the next
     * <em>different</em> marker, so a repeated marker's literal text (including the second "n/")
     * silently becomes part of the first occurrence's value instead of being reported as an
     * error. Every marker is checked across the whole text, independent of any single field's own
     * extraction span, so a duplicate is caught no matter which other markers separate the two
     * occurrences.
     *
     * @param text the full argument text
     * @param markers every marker this command recognises
     * @throws InvalidCommandException if any marker occurs more than once
     */
    public static void rejectDuplicateMarkers(String text, String... markers) throws InvalidCommandException {
        for (String marker : markers) {
            int firstIndex = indexOfMarker(text, marker, 0);
            if (firstIndex == -1) {
                continue;
            }
            int secondIndex = indexOfMarker(text, marker, firstIndex + marker.length());
            if (secondIndex != -1) {
                throw new InvalidCommandException("Duplicate option \"" + marker + "\".");
            }
        }
    }

    /**
     * Extracts every occurrence of a repeatable marker's value, in the order they appear, plus
     * the text with each occurrence (marker and value) removed. Unlike {@link
     * #extractPresentFields}, which assumes a marker appears at most once, this supports a marker
     * whose command grammar explicitly allows it to repeat (e.g. find's "k/", combined with AND
     * semantics). Each occurrence's value is bounded by the next occurrence of any marker in
     * allMarkers - including marker itself, so consecutive occurrences are correctly split - or
     * the end of input, the same boundary rule {@link #extractField} uses for a single occurrence.
     *
     * <p>The returned remaining text has every occurrence of marker (and its value) spliced out,
     * so it can still be validated and extracted by {@link #rejectDuplicateMarkers} and {@link
     * #extractPresentFields} for the command's other, non-repeatable markers exactly as if marker
     * had never been declared at all.
     *
     * @param text the full argument text
     * @param marker the repeatable marker to collect every occurrence of, e.g. "k/"
     * @param allMarkers every marker this command recognises, including marker itself
     * @return the extracted values (in order) and the marker-stripped remaining text
     */
    public static RepeatedField extractAllOccurrences(String text, String marker, String... allMarkers) {
        List<String> values = new ArrayList<>();
        StringBuilder remaining = new StringBuilder();
        int cursor = 0;
        while (true) {
            int start = indexOfMarker(text, marker, cursor);
            if (start == -1) {
                remaining.append(text, cursor, text.length());
                break;
            }
            remaining.append(text, cursor, start);
            int valueStart = start + marker.length();
            int end = nearestMarkerBoundary(text, valueStart, allMarkers);
            values.add(text.substring(valueStart, end).trim());
            cursor = end;
        }
        return new RepeatedField(values, remaining.toString());
    }

    private static int nearestMarkerBoundary(String text, int from, String[] markers) {
        int nearest = text.length();
        for (String candidate : markers) {
            int index = indexOfMarker(text, candidate, from);
            if (index != -1 && index < nearest) {
                nearest = index;
            }
        }
        return nearest;
    }

    /**
     * The result of {@link #extractAllOccurrences}: every occurrence's value, in order, plus the
     * marker-stripped remaining text.
     */
    public static final class RepeatedField {
        private final List<String> values;
        private final String remainingText;

        private RepeatedField(List<String> values, String remainingText) {
            this.values = values;
            this.remainingText = remainingText;
        }

        /** Returns every occurrence's trimmed value, in the order they appeared in the text. */
        public List<String> getValues() {
            return values;
        }

        /** Returns the text with every occurrence of the repeatable marker (and its value) removed. */
        public String getRemainingText() {
            return remainingText;
        }
    }

    /**
     * Extracts every marker (from the given candidates) that is actually present in the text,
     * into a map of marker to its value, using each field's neighbouring present marker (by
     * position) as its end boundary. Supports an arbitrary subset of markers in any order.
     *
     * @param text the text to extract fields from
     * @param markers every marker that could appear, e.g. "n/", "c/", "date/"
     * @return a map from each present marker to its trimmed value, in the order the markers
     *     appear in the text
     */
    public static Map<String, String> extractPresentFields(String text, String... markers) {
        List<String> present = new ArrayList<>();
        for (String marker : markers) {
            if (indexOfMarker(text, marker, 0) != -1) {
                present.add(marker);
            }
        }
        present.sort(Comparator.comparingInt(marker -> indexOfMarker(text, marker, 0)));

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < present.size(); i++) {
            String marker = present.get(i);
            String endMarker = i + 1 < present.size() ? present.get(i + 1) : null;
            result.put(marker, extractField(text, marker, endMarker));
        }
        return result;
    }

    /**
     * Parses text as an {@link ActivityCategory}, case-insensitively.
     *
     * @param text the raw category text
     * @return the matching category
     * @throws InvalidActivityException if text does not match any category
     */
    public static ActivityCategory parseCategory(String text) throws InvalidActivityException {
        try {
            return ActivityCategory.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidActivityException("category must be one of ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS.");
        }
    }

    /**
     * Rejects a value that contains the storage delimiter '|', which activity/topic storage
     * files cannot escape. Catching this here, before the record is built, avoids the
     * alternative: the record is accepted and reported as saved, then permanently fails to
     * persist because Storage.save() rejects the same value on every later save.
     *
     * @param value the already-extracted field value
     * @param fieldName the field name to use in the error message
     * @throws InvalidActivityException if value contains '|'
     */
    public static void validateNoDelimiter(String value, String fieldName) throws InvalidActivityException {
        if (value.contains("|")) {
            throw new InvalidActivityException(fieldName + " must not contain the '|' character.");
        }
    }

    /**
     * Parses text as an {@link AccessibilityStatus}, case-insensitively.
     *
     * @param text the raw status text
     * @return the matching status
     * @throws InvalidCommandException if text does not match any status
     */
    public static AccessibilityStatus parseAccessibilityStatus(String text) throws InvalidCommandException {
        try {
            return AccessibilityStatus.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("status must be YES, NO, or UNKNOWN.");
        }
    }

    /**
     * Finds the first case-insensitive occurrence of marker at or after fromIndex.
     *
     * @param text the text to search
     * @param marker the marker to find
     * @param fromIndex the index to start searching from
     * @return the matched index, or -1 if none is found
     */
    private static int indexOfIgnoreCase(String text, String marker, int fromIndex) {
        int maxStart = text.length() - marker.length();
        for (int i = Math.max(fromIndex, 0); i <= maxStart; i++) {
            if (text.regionMatches(true, i, marker, 0, marker.length())) {
                return i;
            }
        }
        return -1;
    }
}
