package seedu.unienable.logic.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;

class PreferenceManagerTest {
    @Test
    public void freshManager_usesAuthoritativeDefaults() {
        PreferenceManager manager = new PreferenceManager();

        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
        assertTrue(manager.isDefault());
    }

    @Test
    public void setThenReset_replacesWholeImmutableProfile() {
        PreferenceManager manager = new PreferenceManager();
        PreferenceProfile custom = PreferenceProfile.of(LocalTime.of(9, 0),
                LocalTime.of(18, 0), 20, TomatoSuggestion.ON);

        manager.setProfile(custom);
        assertEquals(custom, manager.getProfile());
        assertFalse(manager.isDefault());

        manager.reset();
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
        assertTrue(manager.isDefault());
    }

    @Test
    public void setNull_rejectedWithoutChangingState() {
        PreferenceManager manager = new PreferenceManager();

        assertThrows(IllegalArgumentException.class, () -> manager.setProfile(null));
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
    }
}
