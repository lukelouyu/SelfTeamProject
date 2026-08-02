package seedu.unienable.parser.preference;

import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;

import seedu.unienable.command.Command;
import seedu.unienable.command.preference.PreferenceResetCommand;
import seedu.unienable.command.preference.PreferenceSetCommand;
import seedu.unienable.command.preference.PreferenceViewCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;
import seedu.unienable.parser.common.ArgumentMarker;
import seedu.unienable.parser.common.ArgumentTokenizer;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.FieldParser;

/** Parses the view, set, and reset operations under {@code preference}. */
public class PreferenceCommandParser {
    private static final String START = "start/";
    private static final String END = "end/";
    private static final String BUFFER = "buffer/";
    private static final String TOMATO = "tomato/";

    /**
     * Parses one preference subcommand against the active profile.
     *
     * @param preferenceManager manager that commands will read or update
     * @param args text after the top-level preference command word
     * @return a view, set, or reset command
     * @throws MissingInputException if required operation or field input is absent
     * @throws InvalidCommandException if the operation, marker, or value is invalid
     * @throws InvalidDateTimeException if a supplied time is not strict HH:mm
     */
    public Command parse(PreferenceManager preferenceManager, String args)
            throws MissingInputException, InvalidCommandException, InvalidDateTimeException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new MissingInputException("preference requires view, set, or reset.");
        }
        String[] parts = trimmed.split("\\s+", 2);
        String subcommand = parts[0].toLowerCase(Locale.ROOT);
        String remainder = parts.length == 1 ? "" : parts[1];
        switch (subcommand) {
        case "view":
            FieldParser.requireNoArguments("preference view", remainder);
            return new PreferenceViewCommand(preferenceManager);
        case "set":
            return parseSet(preferenceManager, remainder);
        case "reset":
            FieldParser.requireNoArguments("preference reset", remainder);
            return new PreferenceResetCommand(preferenceManager);
        default:
            throw new InvalidCommandException("Unknown preference command \"" + parts[0]
                    + "\". Enter guide preference for help.");
        }
    }

    private PreferenceSetCommand parseSet(PreferenceManager preferenceManager, String args)
            throws MissingInputException, InvalidCommandException, InvalidDateTimeException {
        Map<String, String> fields = ArgumentTokenizer.tokenize(args,
                ArgumentMarker.optional(START), ArgumentMarker.optional(END),
                ArgumentMarker.optional(BUFFER), ArgumentMarker.optional(TOMATO));
        if (fields.isEmpty()) {
            throw new MissingInputException("preference set requires at least one field.");
        }
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (field.getValue().isEmpty()) {
                throw new MissingInputException(field.getKey() + " value is required.");
            }
        }

        PreferenceProfile current = preferenceManager.getProfile();
        LocalTime start = fields.containsKey(START)
                ? DateTimeParser.parseTime(fields.get(START)) : current.getPreferredStart();
        LocalTime end = fields.containsKey(END)
                ? DateTimeParser.parseTime(fields.get(END)) : current.getPreferredEnd();
        int buffer = fields.containsKey(BUFFER)
                ? parseBuffer(fields.get(BUFFER)) : current.getMinimumBufferMinutes();
        TomatoSuggestion tomato = fields.containsKey(TOMATO)
                ? parseTomato(fields.get(TOMATO)) : current.getTomatoSuggestion();
        try {
            return new PreferenceSetCommand(preferenceManager,
                    PreferenceProfile.of(start, end, buffer, tomato));
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException(e.getMessage());
        }
    }

    private int parseBuffer(String text) throws InvalidCommandException {
        int buffer;
        try {
            buffer = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("buffer must be a whole number from 0 to "
                    + PreferenceProfile.MAXIMUM_BUFFER_MINUTES + ".");
        }
        if (buffer < 0 || buffer > PreferenceProfile.MAXIMUM_BUFFER_MINUTES) {
            throw new InvalidCommandException("buffer must be a whole number from 0 to "
                    + PreferenceProfile.MAXIMUM_BUFFER_MINUTES + ".");
        }
        return buffer;
    }

    private TomatoSuggestion parseTomato(String text) throws InvalidCommandException {
        try {
            return TomatoSuggestion.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("tomato must be on or off.");
        }
    }
}
