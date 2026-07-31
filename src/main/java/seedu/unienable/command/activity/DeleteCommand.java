package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;

/**
 * Deletes an activity by stable ID. Confirmation ("Delete this activity? (y/n)") is a UI-loop
 * concern handled before this command is executed; execute() performs the deletion immediately.
 */
public class DeleteCommand extends Command {
    private final ActivityManager activityManager;
    private final int id;

    /**
     * Creates a DeleteCommand.
     *
     * @param activityManager the manager holding the activity to delete
     * @param id the stable ID of the activity to delete
     */
    public DeleteCommand(ActivityManager activityManager, int id) {
        this.activityManager = activityManager;
        this.id = id;
    }

    /** Returns the stable ID of the activity this command will delete, for the UI loop's preview. */
    public int getId() {
        return id;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        activityManager.delete(id);
        return new CommandResult("Activity [" + id + "] has been deleted.\nYou now have "
                + activityManager.size() + " activities.");
    }
}
