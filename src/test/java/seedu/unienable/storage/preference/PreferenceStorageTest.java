package seedu.unienable.storage.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;
import seedu.unienable.storage.LoadResult;

class PreferenceStorageTest {
    @TempDir
    Path tempDir;

    @Test
    public void load_missingFile_returnsSilentCompleteDefaults() throws Exception {
        LoadResult<PreferenceProfile> result = new PreferenceStorage()
                .load(tempDir.resolve("missing.txt"));

        assertEquals(PreferenceProfile.defaults(), result.getRecords().get(0));
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void saveThenLoad_roundTripsInDeterministicUtf8Order() throws Exception {
        Path file = tempDir.resolve("preferences.txt");
        PreferenceProfile profile = PreferenceProfile.of(LocalTime.of(9, 0),
                LocalTime.of(18, 0), 20, TomatoSuggestion.ON);

        PreferenceStorage storage = new PreferenceStorage();
        storage.save(file, profile);
        assertEquals(List.of("PREFERRED_START|09:00", "PREFERRED_END|18:00",
                "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"),
                Files.readAllLines(file, StandardCharsets.UTF_8));
        assertEquals(profile, storage.load(file).getRecords().get(0));

        PreferenceProfile off = PreferenceProfile.of(LocalTime.of(9, 0),
                LocalTime.of(18, 0), 20, TomatoSuggestion.OFF);
        storage.save(file, off);
        assertEquals(off, storage.load(file).getRecords().get(0));
    }

    @Test
    public void load_everyMalformedShapeFallsBackToWholeDefaults() throws Exception {
        List<List<String>> invalidFiles = List.of(
                List.of(),
                List.of("PREFERRED_START09:00", "PREFERRED_END|18:00",
                        "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"),
                List.of("PREFERRED_START|", "PREFERRED_END|18:00",
                        "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"),
                List.of("PREFERRED_START|09:00|extra", "PREFERRED_END|18:00",
                        "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"),
                List.of("PREFERRED_END|18:00", "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"),
                List.of("PREFERRED_START|09:00", "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"),
                List.of("PREFERRED_START|09:00", "PREFERRED_END|18:00", "TOMATO_SUGGESTION|ON"),
                List.of("PREFERRED_START|09:00", "PREFERRED_END|18:00", "MINIMUM_BUFFER|20"),
                validWithReplacement("PREFERRED_START|9:00"),
                validWithReplacement("PREFERRED_END|25:00"),
                validWithReplacement("MINIMUM_BUFFER|-1"),
                validWithReplacement("MINIMUM_BUFFER|1441"),
                validWithReplacement("MINIMUM_BUFFER|abc"),
                validWithReplacement("TOMATO_SUGGESTION|YES"),
                validWithReplacement("TOMATO_SUGGESTION|on"),
                validWithReplacement("UNKNOWN|value"),
                List.of("PREFERRED_START|09:00", "PREFERRED_START|10:00",
                        "PREFERRED_END|18:00", "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"),
                List.of("PREFERRED_START|18:00", "PREFERRED_END|09:00",
                        "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"));

        for (int index = 0; index < invalidFiles.size(); index++) {
            Path file = tempDir.resolve("invalid-" + index + ".txt");
            Files.write(file, invalidFiles.get(index), StandardCharsets.UTF_8);
            LoadResult<PreferenceProfile> result = new PreferenceStorage().load(file);
            assertEquals(PreferenceProfile.defaults(), result.getRecords().get(0),
                    "invalid file index " + index + " retained partial values");
            assertFalse(result.getWarnings().isEmpty(), "invalid file index " + index);
        }
    }

    @Test
    public void load_oneInvalidFieldNeverRetainsOtherValidFields() throws Exception {
        Path file = tempDir.resolve("preferences.txt");
        Files.write(file, List.of("PREFERRED_START|09:00", "PREFERRED_END|18:00",
                "MINIMUM_BUFFER|30", "TOMATO_SUGGESTION|YES"), StandardCharsets.UTF_8);

        LoadResult<PreferenceProfile> result = new PreferenceStorage().load(file);

        assertEquals(PreferenceProfile.defaults(), result.getRecords().get(0));
        assertTrue(result.getWarnings().stream().anyMatch(message -> message.contains("ON or OFF")));
    }

    private List<String> validWithReplacement(String replacement) {
        String key = replacement.substring(0, replacement.indexOf('|'));
        List<String> lines = new java.util.ArrayList<>(List.of(
                "PREFERRED_START|09:00", "PREFERRED_END|18:00",
                "MINIMUM_BUFFER|20", "TOMATO_SUGGESTION|ON"));
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).startsWith(key + "|")) {
                lines.set(index, replacement);
                return lines;
            }
        }
        lines.set(0, replacement);
        return lines;
    }
}
