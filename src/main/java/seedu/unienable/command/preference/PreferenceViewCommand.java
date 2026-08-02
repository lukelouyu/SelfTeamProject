package seedu.unienable.command.preference;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.ui.preference.PreferenceFormatter;

/** Displays the active global preference profile without mutation or persistence. */
public class PreferenceViewCommand extends Command {
    private final PreferenceManager preferenceManager;

    /**
     * Creates a read-only preference view command.
     *
     * @param preferenceManager manager containing the profile to display
     */
    public PreferenceViewCommand(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
    }

    @Override
    public CommandResult execute() {
        return new CommandResult(PreferenceFormatter.formatProfile(preferenceManager.getProfile()));
    }
}
