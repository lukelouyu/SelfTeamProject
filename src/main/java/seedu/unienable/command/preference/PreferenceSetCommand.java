package seedu.unienable.command.preference;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.ui.preference.PreferenceFormatter;

/** Applies one already validated complete preference profile after confirmation. */
public class PreferenceSetCommand extends Command implements Confirmable {
    private final PreferenceManager preferenceManager;
    private final PreferenceProfile proposedProfile;

    /**
     * Creates a confirmable complete-profile update.
     *
     * @param preferenceManager manager to update after confirmation
     * @param proposedProfile already validated replacement profile
     */
    public PreferenceSetCommand(PreferenceManager preferenceManager,
            PreferenceProfile proposedProfile) {
        this.preferenceManager = preferenceManager;
        this.proposedProfile = proposedProfile;
    }

    @Override
    public CommandEffect getEffect() {
        return CommandEffect.MUTATING;
    }

    @Override
    public boolean hasStateChange() {
        return hasChanges();
    }

    /**
     * Returns the complete proposed profile.
     *
     * @return proposed immutable profile
     */
    public PreferenceProfile getProposedProfile() {
        return proposedProfile;
    }

    /**
     * Returns whether execution would change the active profile.
     *
     * @return true when at least one field differs
     */
    public boolean hasChanges() {
        return !preferenceManager.getProfile().equals(proposedProfile);
    }

    @Override
    public Confirmation getConfirmation() {
        String changes = PreferenceFormatter.formatChanges(preferenceManager.getProfile(),
                proposedProfile);
        if (changes.isEmpty()) {
            return Confirmation.cancel("No preference changes were requested.");
        }
        return Confirmation.ask("Proposed preference changes:\n" + changes
                + "\nNote: activities already adopted from a recommendation keep their existing "
                + "scheduled times - this change does not move them; it only affects future "
                + "recommend proposals.\nApply these preference changes? (y/n)");
    }

    @Override
    public CommandResult execute() {
        preferenceManager.setProfile(proposedProfile);
        return new CommandResult("Preference profile updated.\n\n"
                + PreferenceFormatter.formatProfile(proposedProfile));
    }
}
