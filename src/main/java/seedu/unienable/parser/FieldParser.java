package seedu.unienable.parser;

/** Utility methods for extracting field values between known markers in a command's argument text. */
public class FieldParser {
    /**
     * Extracts the value between a start marker and an end marker.
     * If endMarker is null or not found, extracts until the end of the input.
     *
     * @param input the full argument text
     * @param startMarker the marker just before the value, e.g. "n/"
     * @param endMarker the marker just after the value, or null to read until the end of input
     * @return the trimmed value, or null if startMarker is not found
     */
    public static String extractField(String input, String startMarker, String endMarker) {
        int startIndex = input.indexOf(startMarker);
        if (startIndex == -1) {
            return null;
        }
        startIndex += startMarker.length();

        int endIndex;
        if (endMarker != null) {
            endIndex = input.indexOf(endMarker, startIndex);
            if (endIndex == -1) {
                endIndex = input.length();
            }
        } else {
            endIndex = input.length();
        }

        return input.substring(startIndex, endIndex).trim();
    }
}
