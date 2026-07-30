package seedu.unienable.command;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ScheduleType;

/** Marks an activity as incomplete. Repeating on an already-incomplete activity is allowed. */
public class UnmarkCommand extends Command {
    private final ActivityManager activityManager;
    private final int id;

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
