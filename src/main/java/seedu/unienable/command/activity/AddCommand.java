package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;

/** Adds a fully-constructed, already-validated activity to the ActivityManager. */
public class AddCommand extends Command {
    private final ActivityManager activityManager;
    private final Activity activity;

    /**
     * Creates an AddCommand.
     *
     * @param activityManager the manager to add the activity to
     * @param activity the fully-constructed, already-validated activity to add
     */
    public AddCommand(ActivityManager activityManager, Activity activity) {
        this.activityManager = activityManager;
        this.activity = activity;
    }

    @Override
    public CommandResult execute() throws DuplicateActivityException {
        activityManager.add(activity);
        return new CommandResult(formatConfirmation());
    }

    private String formatConfirmation() {
        return "Got it. Activity [" + activity.getId() + "] has been added:\n" + formatSummary()
                + "\n\nYou now have " + activityManager.size() + " activities.";
    }

    private String formatSummary() {
        String statusSymbol = activity.isComplete() ? "X" : " ";
        String topicSuffix = activity.getTopic() == null ? "" : " / " + activity.getTopic();
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            return "[" + statusSymbol + "][F] " + fixed.getDate() + " " + fixed.getStartTime() + "–"
                    + fixed.getEndTime() + " | " + fixed.getDescription() + "\n       " + fixed.getCategory()
                    + topicSuffix + " | Energy " + fixed.getEnergyRating() + " | Sensory " + fixed.getSensoryRating();
        }
        FlexibleActivity flexible = (FlexibleActivity) activity;
        return "[" + statusSymbol + "][L] " + flexible.getDate() + " " + flexible.getEarliestStart() + "–"
                + flexible.getLatestEnd() + " | " + flexible.getDescription() + "\n       Duration "
                + flexible.getDurationMinutes() + " min | " + flexible.getCategory() + topicSuffix
                + "\n       Energy " + flexible.getEnergyRating() + " | Sensory " + flexible.getSensoryRating();
    }
}
