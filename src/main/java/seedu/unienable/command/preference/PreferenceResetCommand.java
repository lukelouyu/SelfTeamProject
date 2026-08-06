package seedu.unienable.command.preference;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.ui.preference.PreferenceFormatter;

/** Restores the whole preference profile to documented defaults after confirmation. */
public class PreferenceResetCommand extends Command implements Confirmable {
    private final PreferenceManager preferenceManager;

    /**
     * Creates a confirmable profile reset.
     *
     * @param preferenceManager manager to restore to defaults after confirmation
     */
    public PreferenceResetCommand(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
    }

    /**
     * Returns whether execution would change the active profile.
     *
     * @return true when the profile is not already default
     */
    public boolean hasChanges() {
        return !preferenceManager.isDefault();
    }

    @Override
    public Confirmation getConfirmation() {
        String changes = PreferenceFormatter.formatChanges(preferenceManager.getProfile(),
                PreferenceProfile.defaults());
        if (changes.isEmpty()) {
            return Confirmation.cancel("Preference profile already uses all defaults.");
        }
        return Confirmation.ask("Reset preference profile to defaults:\n" + changes
                + "\nNote: activities already adopted from a recommendation keep their existing "
                + "scheduled times - this reset does not move them; it only affects future "
                + "recommend proposals.\nReset these preferences? (y/n)");
    }

    @Override
    public CommandResult execute() {
        preferenceManager.reset();
        return new CommandResult("Preference profile reset to defaults.\n\n"
                + PreferenceFormatter.formatProfile(preferenceManager.getProfile()));
    }
}
