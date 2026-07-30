package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.enums.ActivityOrder;

/** Updates the saved default activity list order. */
public class OrderSetCommand extends Command {
    private final ActivityManager activityManager;
    private final ActivityOrder order;

    public OrderSetCommand(ActivityManager activityManager, ActivityOrder order) {
        this.activityManager = activityManager;
        this.order = order;
    }

    @Override
    public CommandResult execute() {
        activityManager.setDefaultOrder(order);
        return new CommandResult("Default activity order updated: " + order.name().toLowerCase()
                + "\n" + describeFutureOrder(order));
    }

    private String describeFutureOrder(ActivityOrder order) {
        switch (order) {
        case INPUT:
            return "Future list and find results will use creation order.";
        case TIME:
            return "Future results will use time, then stable activity ID.";
        case CHRONOLOGICAL:
        default:
            return "Future results will use date, time, then stable activity ID.";
        }
    }
}
