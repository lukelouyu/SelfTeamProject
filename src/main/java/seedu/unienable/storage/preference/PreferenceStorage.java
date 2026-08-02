package seedu.unienable.storage.preference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;
import seedu.unienable.storage.LoadResult;

/** Loads and saves one complete global preference profile in deterministic text form. */
public class PreferenceStorage {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private enum Key {
        PREFERRED_START,
        PREFERRED_END,
        MINIMUM_BUFFER,
        TOMATO_SUGGESTION
    }

    /**
     * Loads a complete profile or falls back atomically to documented defaults.
     *
     * @param filePath the preferences text file
     * @return the one loaded/default profile plus recovery warnings
     * @throws StorageException if an existing file cannot be read
     */
    public LoadResult<PreferenceProfile> load(Path filePath) throws StorageException {
        if (!Files.exists(filePath)) {
            return defaultsWithoutWarnings();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("could not read " + filePath, e);
        }

        List<String> warnings = new ArrayList<>();
        Map<Key, String> values = new EnumMap<>(Key.class);
        if (lines.isEmpty()) {
            warnings.add("Preference profile is empty; all defaults were loaded.");
        }
        for (int index = 0; index < lines.size(); index++) {
            parseLine(lines.get(index), index + 1, values, warnings);
        }
        for (Key key : Key.values()) {
            if (!values.containsKey(key)) {
                warnings.add("Missing required preference field " + key
                        + "; all defaults were loaded.");
            }
        }
        if (!warnings.isEmpty()) {
            return defaultsWithWarnings(warnings);
        }

        PreferenceProfile profile = buildProfile(values, warnings);
        if (!warnings.isEmpty()) {
            return defaultsWithWarnings(warnings);
        }
        return new LoadResult<>(List.of(profile), List.of());
    }

    /**
     * Saves all four profile fields in stable order using uppercase stored enum values.
     *
     * @param filePath the destination preferences text file
     * @param profile the complete profile to save
     * @throws StorageException if the file cannot be written
     */
    public void save(Path filePath, PreferenceProfile profile) throws StorageException {
        List<String> lines = List.of(
                Key.PREFERRED_START + "|" + profile.getPreferredStart(),
                Key.PREFERRED_END + "|" + profile.getPreferredEnd(),
                Key.MINIMUM_BUFFER + "|" + profile.getMinimumBufferMinutes(),
                Key.TOMATO_SUGGESTION + "|" + profile.getTomatoSuggestion());
        try {
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("could not write " + filePath, e);
        }
    }

    private void parseLine(String line, int lineNumber, Map<Key, String> values,
            List<String> warnings) {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 2 || fields[0].isEmpty() || fields[1].isEmpty()) {
            warnings.add("Line " + lineNumber
                    + " has malformed preference structure; all defaults were loaded.");
            return;
        }
        Key key;
        try {
            key = Key.valueOf(fields[0]);
        } catch (IllegalArgumentException e) {
            warnings.add("Line " + lineNumber + " has an unknown preference field; all defaults were loaded.");
            return;
        }
        if (values.putIfAbsent(key, fields[1]) != null) {
            warnings.add("Line " + lineNumber + " duplicates " + key
                    + "; all defaults were loaded.");
        }
    }

    private PreferenceProfile buildProfile(Map<Key, String> values, List<String> warnings) {
        LocalTime start = parseTime(values.get(Key.PREFERRED_START), Key.PREFERRED_START, warnings);
        LocalTime end = parseTime(values.get(Key.PREFERRED_END), Key.PREFERRED_END, warnings);
        Integer buffer = parseBuffer(values.get(Key.MINIMUM_BUFFER), warnings);
        TomatoSuggestion tomato = parseTomato(values.get(Key.TOMATO_SUGGESTION), warnings);
        if (!warnings.isEmpty()) {
            return PreferenceProfile.defaults();
        }
        try {
            return PreferenceProfile.of(start, end, buffer, tomato);
        } catch (IllegalArgumentException e) {
            warnings.add("Preference profile is internally inconsistent (" + e.getMessage()
                    + "); all defaults were loaded.");
            return PreferenceProfile.defaults();
        }
    }

    private LocalTime parseTime(String value, Key key, List<String> warnings) {
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException e) {
            warnings.add(key + " must use valid HH:mm; all defaults were loaded.");
            return PreferenceProfile.defaults().getPreferredStart();
        }
    }

    private Integer parseBuffer(String value, List<String> warnings) {
        int buffer;
        try {
            buffer = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            warnings.add("MINIMUM_BUFFER must be a whole number; all defaults were loaded.");
            return PreferenceProfile.defaults().getMinimumBufferMinutes();
        }
        if (buffer < 0 || buffer > PreferenceProfile.MAXIMUM_BUFFER_MINUTES) {
            warnings.add("MINIMUM_BUFFER must be from 0 to "
                    + PreferenceProfile.MAXIMUM_BUFFER_MINUTES + "; all defaults were loaded.");
            return PreferenceProfile.defaults().getMinimumBufferMinutes();
        }
        return buffer;
    }

    private TomatoSuggestion parseTomato(String value, List<String> warnings) {
        try {
            return TomatoSuggestion.valueOf(value);
        } catch (IllegalArgumentException e) {
            warnings.add("TOMATO_SUGGESTION must be ON or OFF; all defaults were loaded.");
            return PreferenceProfile.defaults().getTomatoSuggestion();
        }
    }

    private LoadResult<PreferenceProfile> defaultsWithoutWarnings() {
        return new LoadResult<>(List.of(PreferenceProfile.defaults()), List.of());
    }

    private LoadResult<PreferenceProfile> defaultsWithWarnings(List<String> warnings) {
        return new LoadResult<>(List.of(PreferenceProfile.defaults()), warnings);
    }
}
