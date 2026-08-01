package seedu.unienable.command.activity.general;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ScheduleType;

/** Marks an activity as incomplete. Repeating on an already-incomplete activity is allowed. */
public class UnmarkCommand extends Command {
    private final ActivityManager activityManager;
    private final int id;

    /**
     * Creates an UnmarkCommand.
     *
     * @param activityManager the manager holding the activity to unmark
     * @param id the stable ID of the activity to mark incomplete
     */
    public UnmarkCommand(ActivityManager activityManager, int id) {
        this.activityManager = activityManager;
        this.id = id;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        Activity activity = activityManager.unmark(id);
        return new CommandResult("Activity [" + id + "] is now incomplete:\n" + formatConciseLine(activity));
    }

    private String formatConciseLine(Activity activity) {
        String typeSymbol = activity.getScheduleType() == ScheduleType.FIXED ? "F" : "L";
        return "[ ][" + typeSymbol + "] " + activity.getDescription();
    }
}
