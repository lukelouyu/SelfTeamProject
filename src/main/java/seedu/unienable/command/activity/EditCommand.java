package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;

/**
 * Replaces an activity with a fully-rebuilt version reflecting the requested field changes. The
 * replacement is expected to already carry the same ID and the original completion status, so
 * this command only needs to hand it to ActivityManager.replace() for atomic validation and swap.
 */
public class EditCommand extends Command {
    private final ActivityManager activityManager;
    private final int id;
    private final Activity newActivity;

    public EditCommand(ActivityManager activityManager, int id, Activity newActivity) {
        this.activityManager = activityManager;
        this.id = id;
        this.newActivity = newActivity;
    }

    /** Returns the stable ID of the activity this command will update, for the UI loop's preview. */
    public int getId() {
        return id;
    }

    /** Returns the proposed replacement activity, for the UI loop's preview. */
    public Activity getNewActivity() {
        return newActivity;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException, DuplicateActivityException {
        activityManager.replace(id, newActivity);
        return new CommandResult("Activity [" + id + "] has been updated.");
    }
}
