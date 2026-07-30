package seedu.unienable.command;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.enums.ActivityOrder;

/** Displays the saved default activity list order. */
public class OrderViewCommand extends Command {
    private final ActivityManager activityManager;

    public OrderViewCommand(ActivityManager activityManager) {
        this.activityManager = activityManager;
    }

    @Override
    public CommandResult execute() {
        ActivityOrder order = activityManager.getDefaultOrder();
        return new CommandResult("Saved default activity order: " + order.name().toLowerCase()
                + "\n" + describeOrder(order));
    }

    private String describeOrder(ActivityOrder order) {
        switch (order) {
        case INPUT:
            return "Activities are ordered by input (creation) order.";
        case TIME:
            return "Activities are ordered by start time, then ID.";
        case CHRONOLOGICAL:
        default:
            return "Activities are ordered by date, then start time, then ID.";
        }
    }
}
