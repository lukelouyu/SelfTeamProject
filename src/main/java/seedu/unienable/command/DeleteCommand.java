package seedu.unienable.command;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;

/**
 * Deletes an activity by stable ID. Confirmation ("Delete this activity? (y/n)") is a UI-loop
 * concern handled before this command is executed; execute() performs the deletion immediately.
 */
public class DeleteCommand extends Command {
    private final ActivityManager activityManager;
    private final int id;

    public DeleteCommand(ActivityManager activityManager, int id) {
        this.activityManager = activityManager;
        this.id = id;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        activityManager.delete(id);
        return new CommandResult("Activity [" + id + "] has been deleted.\nYou now have "
                + activityManager.size() + " activities.");
    }
}
