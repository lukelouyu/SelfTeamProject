package seedu.unienable.model.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.LocalTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class PreferenceProfileTest {
    @Test
    public void defaults_exactDocumentedValuesAndTomatoOff() {
        PreferenceProfile profile = PreferenceProfile.defaults();

        assertEquals(LocalTime.of(8, 0), profile.getPreferredStart());
        assertEquals(LocalTime.of(20, 0), profile.getPreferredEnd());
        assertEquals(15, profile.getMinimumBufferMinutes());
        assertEquals(TomatoSuggestion.OFF, profile.getTomatoSuggestion());
    }

    @Test
    public void of_validCustomProfileAndBoundaryBuffers_succeeds() {
        PreferenceProfile zero = PreferenceProfile.of(LocalTime.of(9, 0), LocalTime.of(18, 0),
                0, TomatoSuggestion.ON);
        PreferenceProfile maximum = PreferenceProfile.of(LocalTime.of(9, 0), LocalTime.of(18, 0),
                PreferenceProfile.MAXIMUM_BUFFER_MINUTES, TomatoSuggestion.OFF);

        assertEquals(TomatoSuggestion.ON, zero.getTomatoSuggestion());
        assertEquals(0, zero.getMinimumBufferMinutes());
        assertEquals(PreferenceProfile.MAXIMUM_BUFFER_MINUTES,
                maximum.getMinimumBufferMinutes());
    }

    @Test
    public void of_startEqualOrAfterEnd_rejected() {
        assertThrows(IllegalArgumentException.class, () -> PreferenceProfile.of(
                LocalTime.NOON, LocalTime.NOON, 15, TomatoSuggestion.OFF));
        assertThrows(IllegalArgumentException.class, () -> PreferenceProfile.of(
                LocalTime.of(18, 0), LocalTime.of(9, 0), 15, TomatoSuggestion.OFF));
    }

    @Test
    public void of_bufferOutsideRange_rejected() {
        assertThrows(IllegalArgumentException.class, () -> PreferenceProfile.of(
                LocalTime.of(8, 0), LocalTime.of(20, 0), -1, TomatoSuggestion.OFF));
        assertThrows(IllegalArgumentException.class, () -> PreferenceProfile.of(
                LocalTime.of(8, 0), LocalTime.of(20, 0),
                PreferenceProfile.MAXIMUM_BUFFER_MINUTES + 1, TomatoSuggestion.OFF));
    }

    @Test
    public void valueEqualityAndHashCode_useAllFourFields() {
        PreferenceProfile first = PreferenceProfile.of(LocalTime.of(9, 0), LocalTime.of(18, 0),
                20, TomatoSuggestion.ON);
        PreferenceProfile same = PreferenceProfile.of(LocalTime.of(9, 0), LocalTime.of(18, 0),
                20, TomatoSuggestion.ON);
        PreferenceProfile different = PreferenceProfile.of(LocalTime.of(9, 0), LocalTime.of(18, 0),
                20, TomatoSuggestion.OFF);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, different);
    }

    @Test
    public void classIsImmutableShape_finalFieldsAndNoSetters() {
        assertTrue(Modifier.isFinal(PreferenceProfile.class.getModifiers()));
        assertTrue(Arrays.stream(PreferenceProfile.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
        assertFalse(Arrays.stream(PreferenceProfile.class.getMethods())
                .anyMatch(method -> method.getName().startsWith("set")));
    }
}
