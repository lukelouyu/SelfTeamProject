package seedu.unienable.command.general;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityOrder;

/**
 * Clears every activity and user-created topic, resets the saved default order to chronological,
 * and resets the next activity ID to 1. Facility and connection reference data is untouched, since
 * it is read-only and not owned by this command. Confirmation ("Reset all user data? (y/n)") is a
 * UI-loop concern handled before this command is executed; execute() performs the reset
 * immediately.
 */
public class ResetCommand extends Command {
    private final ActivityManager activityManager;
    private final TopicManager topicManager;

    /**
     * Creates a ResetCommand.
     *
     * @param activityManager the manager whose activities, next-ID counter, and default order
     *     will be reset
     * @param topicManager the manager whose topics will be cleared
     */
    public ResetCommand(ActivityManager activityManager, TopicManager topicManager) {
        this.activityManager = activityManager;
        this.topicManager = topicManager;
    }

    /** Returns the number of activities this command will delete, for the UI loop's preview. */
    public int getActivityCount() {
        return activityManager.size();
    }

    /** Returns the number of topics this command will delete, for the UI loop's preview. */
    public int getTopicCount() {
        return topicManager.getAll().size();
    }

    /**
     * Returns whether there is anything for this reset to actually change: any stored activity,
     * any stored topic, a saved default order other than chronological, or a next-activity-ID
     * counter already past 1. Used to skip the confirmation prompt entirely when nothing needs
     * resetting.
     *
     * @return true if executing this command would change any state
     */
    public boolean hasAnythingToReset() {
        return getActivityCount() > 0
                || getTopicCount() > 0
                || activityManager.getDefaultOrder() != ActivityOrder.CHRONOLOGICAL
                || activityManager.getNextId() != 1;
    }

    @Override
    public CommandResult execute() {
        activityManager.resetAll();
        topicManager.resetAll();
        return new CommandResult("All user data has been reset.\nYour next activity will use ID [1].");
    }
}
