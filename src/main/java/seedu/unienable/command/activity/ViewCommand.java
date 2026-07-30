package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.ui.MessageFormatter;

/** Displays every stored field of one activity. */
public class ViewCommand extends Command {
    private final ActivityManager activityManager;
    private final int id;

    public ViewCommand(ActivityManager activityManager, int id) {
        this.activityManager = activityManager;
        this.id = id;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        return new CommandResult(MessageFormatter.formatView(activityManager.getById(id)));
    }
}
