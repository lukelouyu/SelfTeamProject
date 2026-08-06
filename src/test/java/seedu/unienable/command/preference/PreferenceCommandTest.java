package seedu.unienable.command.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmation;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;

class PreferenceCommandTest {
    private PreferenceManager manager;

    @BeforeEach
    public void setUp() {
        manager = new PreferenceManager();
    }

    @Test
    public void view_defaults_exactAllFieldsAndDoesNotMutate() {
        CommandResult result = new PreferenceViewCommand(manager).execute();

        assertEquals("Preference profile\n\n"
                + "Preferred daily start: 08:00\n"
                + "Preferred daily end: 20:00\n"
                + "Minimum buffer: 15 minutes\n"
                + "Tomato suggestion: OFF\n\n"
                + "These preferences apply to every day.", result.getFeedback());
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
    }

    @Test
    public void set_previewDoesNotMutateThenExecuteAppliesCompleteProfile() {
        PreferenceProfile proposed = PreferenceProfile.of(LocalTime.of(9, 0),
                LocalTime.of(18, 0), 20, TomatoSuggestion.ON);
        PreferenceSetCommand command = new PreferenceSetCommand(manager, proposed);

        Confirmation confirmation = command.getConfirmation();
        assertTrue(confirmation.isAsk());
        assertTrue(confirmation.getMessage().contains("Preferred daily start | Old: 08:00 | New: 09:00"));
        assertTrue(confirmation.getMessage().contains("Tomato suggestion | Old: OFF | New: ON"));
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());

        command.execute();
        assertEquals(proposed, manager.getProfile());
    }

    @Test
    public void set_tomatoOnlyPreviewAndNoOpBehavior() {
        PreferenceProfile tomatoOn = PreferenceProfile.of(
                PreferenceProfile.defaults().getPreferredStart(),
                PreferenceProfile.defaults().getPreferredEnd(),
                PreferenceProfile.defaults().getMinimumBufferMinutes(), TomatoSuggestion.ON);
        PreferenceSetCommand change = new PreferenceSetCommand(manager, tomatoOn);

        assertEquals("Proposed preference changes:\nTomato suggestion | Old: OFF | New: ON\n"
                + "Note: activities already adopted from a recommendation keep their existing "
                + "scheduled times - this change does not move them; it only affects future "
                + "recommend proposals.\nApply these preference changes? (y/n)",
                change.getConfirmation().getMessage());
        assertTrue(change.hasChanges());

        PreferenceSetCommand noChange = new PreferenceSetCommand(manager, PreferenceProfile.defaults());
        assertTrue(noChange.getConfirmation().isCancel());
        assertFalse(noChange.hasChanges());
    }

    @Test
    public void reset_previewDoesNotMutateThenExecuteResetsAllFour() {
        PreferenceProfile custom = PreferenceProfile.of(LocalTime.of(9, 0),
                LocalTime.of(18, 0), 20, TomatoSuggestion.ON);
        manager.setProfile(custom);
        PreferenceResetCommand command = new PreferenceResetCommand(manager);

        Confirmation confirmation = command.getConfirmation();
        assertTrue(confirmation.isAsk());
        assertTrue(confirmation.getMessage().contains("Reset preference profile to defaults:"));
        assertEquals(custom, manager.getProfile());

        command.execute();
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
    }

    @Test
    public void reset_alreadyDefaultCancelsWithoutChange() {
        PreferenceResetCommand command = new PreferenceResetCommand(manager);

        assertTrue(command.getConfirmation().isCancel());
        assertFalse(command.hasChanges());
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
    }
}
