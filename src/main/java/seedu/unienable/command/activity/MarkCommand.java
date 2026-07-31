package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ScheduleType;

/** Marks an activity as complete. Repeating on an already-complete activity is allowed. */
public class MarkCommand extends Command {
    private final ActivityManager activityManager;
    private final int id;

    /**
     * Creates a MarkCommand.
     *
     * @param activityManager the manager holding the activity to mark
     * @param id the stable ID of the activity to mark complete
     */
    public MarkCommand(ActivityManager activityManager, int id) {
        this.activityManager = activityManager;
        this.id = id;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException {
        Activity activity = activityManager.mark(id);
        return new CommandResult("Nice! Activity [" + id + "] is now complete:\n" + formatConciseLine(activity));
    }

    private String formatConciseLine(Activity activity) {
        String typeSymbol = activity.getScheduleType() == ScheduleType.FIXED ? "F" : "L";
        return "[X][" + typeSymbol + "] " + activity.getDescription();
    }
}
