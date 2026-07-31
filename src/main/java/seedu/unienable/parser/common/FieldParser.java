package seedu.unienable.parser.common;

/**
 * Utility methods for extracting field values between known markers in a command's argument
 * text. Marker matching is case-insensitive (e.g. "N/" matches the "n/" marker), matching the
 * User Guide's documented rule that field prefixes are case-insensitive; the extracted value
 * itself is returned with its original case intact.
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
