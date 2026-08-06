package seedu.unienable.command.general;

import java.util.ArrayList;
import java.util.List;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.MenuConfirmable;
import seedu.unienable.command.MenuOutcome;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recur.ClassSchedulePolicy;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ActivityOrder;

/**
 * Applies one of reset's three explicit outcomes, chosen from a numbered menu: delete all user
 * data, keep fixed academic class schedules while deleting everything else, or cancel. Facility,
 * connection, and academic-calendar reference data are untouched, since none of them are owned
 * by this command. The menu selection is a UI-loop concern handled before this command is
 * executed; {@link #applyMenuAnswer} records the choice, execute() performs it.
 */
public class ResetCommand extends Command implements MenuConfirmable {
    /** The three explicit outcomes offered by the reset menu. */
    public enum Selection {
        DELETE_ALL,
        KEEP_CLASS_SCHEDULE,
        CANCEL
    }

    private final ActivityManager activityManager;
    private final TopicManager topicManager;
    private final PreferenceManager preferenceManager;
    private final ClassSchedulePolicy classSchedulePolicy = new ClassSchedulePolicy();
    private Selection selection = Selection.DELETE_ALL;

    /**
     * Creates a ResetCommand.
     *
     * @param activityManager the manager whose activities, next-ID counter, and default order
     *     will be reset
     * @param topicManager the manager whose topics will be reset
     * @param preferenceManager the manager whose profile option 1 will reset
     */
    public ResetCommand(ActivityManager activityManager, TopicManager topicManager,
            PreferenceManager preferenceManager) {
        this.activityManager = activityManager;
        this.topicManager = topicManager;
        this.preferenceManager = preferenceManager;
    }

    @Override
    public CommandEffect getEffect() {
        return CommandEffect.MUTATING;
    }

    @Override
    public boolean hasStateChange() {
        return hasAnythingToReset();
    }

    /** Returns the number of activities currently stored, for the menu preview. */
    public int getActivityCount() {
        return activityManager.size();
    }

    /** Returns how many currently-stored activities qualify as fixed academic class schedules. */
    public int getClassScheduleCount() {
        int count = 0;
        for (Activity activity : activityManager.getAll()) {
            if (classSchedulePolicy.isClassSchedule(activity)) {
                count++;
            }
        }
        return count;
    }

    /** Returns how many currently-stored activities option 2 would delete. */
    public int getOtherActivityCount() {
        return getActivityCount() - getClassScheduleCount();
    }

    /** Returns the number of topics currently stored, for the menu preview. */
    public int getTopicCount() {
        return topicManager.getAll().size();
    }

    /**
     * Returns whether there is anything for this reset to actually change: any stored activity,
     * any stored topic, a saved default order other than chronological, a next-activity-ID
     * counter already past 1, or a custom preference profile. Used to skip the menu entirely
     * when nothing needs resetting.
     *
     * @return true if executing this command could change any state
     */
    public boolean hasAnythingToReset() {
        return getActivityCount() > 0
                || getTopicCount() > 0
                || activityManager.getDefaultOrder() != ActivityOrder.CHRONOLOGICAL
                || activityManager.getNextId() != 1
                || !preferenceManager.isDefault();
    }

    @Override
    public String getMenuPrompt() {
        if (!hasAnythingToReset()) {
            return null;
        }
        return "Reset user data\n\n"
                + "Activities      : " + getActivityCount() + "\n"
                + "Class schedules : " + getClassScheduleCount() + "\n"
                + "Other activities: " + getOtherActivityCount() + "\n"
                + "Topics          : " + getTopicCount() + "\n"
                + "Preferences     : " + (preferenceManager.isDefault() ? "Default" : "Custom") + "\n\n"
                + "[1] Delete all user data\n"
                + "[2] Delete other activities but keep class schedules\n"
                + "[3] Do not delete anything\n\n"
                + "Facility, connection, and academic-calendar reference data will be kept.\n"
                + "Option 1 resets preferences; options 2 and 3 retain them.\n"
                + "Enter 1, 2, or 3:";
    }

    @Override
    public MenuOutcome applyMenuAnswer(String rawAnswer) {
        switch (rawAnswer) {
        case "1":
            selection = Selection.DELETE_ALL;
            return MenuOutcome.proceed();
        case "2":
            selection = Selection.KEEP_CLASS_SCHEDULE;
            return MenuOutcome.proceed();
        case "3":
            return MenuOutcome.cancel("Cancelled. No changes were made.");
        default:
            return MenuOutcome.cancel("Enter 1, 2, or 3. No changes were made.");
        }
    }

    @Override
    public CommandResult execute() {
        if (selection == Selection.KEEP_CLASS_SCHEDULE) {
            return executeKeepClassSchedule();
        }
        activityManager.resetAll();
        topicManager.resetAll();
        preferenceManager.reset();
        return new CommandResult("All user data has been reset.\nYour next activity will use ID [1].");
    }

    private CommandResult executeKeepClassSchedule() {
        int originalCount = activityManager.size();
        List<Activity> retained = new ArrayList<>();
        for (Activity activity : activityManager.getAll()) {
            if (classSchedulePolicy.isClassSchedule(activity)) {
                retained.add(activity);
            }
        }
        activityManager.loadAll(retained);
        activityManager.setDefaultOrder(ActivityOrder.CHRONOLOGICAL);
        topicManager.retainTopicsUsedBy(retained);
        int deletedCount = originalCount - retained.size();
        return new CommandResult("Reset complete. Kept " + retained.size()
                + (retained.size() == 1 ? " class-schedule activity" : " class-schedule activities")
                + " and deleted " + deletedCount
                + (deletedCount == 1 ? " other activity." : " other activities.")
                + "\nYour next activity will use ID [" + activityManager.getNextId() + "].");
    }
}
