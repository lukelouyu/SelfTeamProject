package seedu.unienable.command.activity.crud;

import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.ui.MessageFormatter;

/** Displays every stored field of one activity. */
public class ViewCommand extends ReadOnlyCommand {
    private final ActivityManager activityManager;
    private final int id;

    /**
     * Creates a ViewCommand.
     *
     * @param activityManager the manager holding the activity to view
     * @param id the stable ID of the activity to view
     */
    public ViewCommand(ActivityManager activityManager, int id) {
        this.activityManager = activityManager;
        this.id = id;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        return new CommandResult(MessageFormatter.formatView(activityManager.getById(id)));
    }
}
