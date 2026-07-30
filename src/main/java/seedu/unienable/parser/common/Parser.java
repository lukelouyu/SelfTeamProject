package seedu.unienable.parser.common;

/** Identifies the command word and remaining argument text from raw user input. */
public class Parser {
    /**
     * Extracts the command word from the given input.
     *
     * @param input the full raw user input
     * @return the command word, lower-cased
     */
    public static String getCommandWord(String input) {
        return input.trim().split("\\s+", 2)[0].toLowerCase();
    }

    /**
     * Extracts the argument text after the command word.
     *
     * @param input the full raw user input
     * @return the remaining argument text, or an empty string if none
     */
    public static String getArguments(String input) {
        String[] parts = input.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1].trim() : "";
    }
}
