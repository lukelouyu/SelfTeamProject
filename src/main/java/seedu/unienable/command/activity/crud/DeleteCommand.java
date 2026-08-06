package seedu.unienable.command.activity.crud;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.ui.MessageFormatter;

/**
 * Deletes an activity by stable ID. Confirmation ("Delete this activity? (y/n)") is a UI-loop
 * concern handled before this command is executed; execute() performs the deletion immediately.
 */
public class DeleteCommand extends Command implements Confirmable {
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

    @Override
    public CommandEffect getEffect() {
        return CommandEffect.MUTATING;
    }

    /** Returns the stable ID of the activity this command will delete, for the UI loop's preview. */
    public int getId() {
        return id;
    }

    @Override
    public Confirmation getConfirmation() throws InvalidIndexException {
        Activity activity = activityManager.getById(id);
        return Confirmation.ask("You selected activity [" + id + "]:\n"
                + MessageFormatter.formatConcise(activity) + "\n\nDelete this activity? (y/n)");
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        activityManager.delete(id);
        int count = activityManager.size();
        return new CommandResult("Activity [" + id + "] has been deleted.\nYou now have "
                + count + (count == 1 ? " activity." : " activities."));
    }
}
