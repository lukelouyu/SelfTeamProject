package seedu.unienable.command.activity.general;

import java.util.Locale;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.enums.ActivityOrder;

/** Updates the saved default activity list order. */
public class OrderSetCommand extends Command {
    private final ActivityManager activityManager;
    private final ActivityOrder order;

    /**
     * Creates an OrderSetCommand.
     *
     * @param activityManager the manager whose default order will be updated
     * @param order the new default order
     */
    public OrderSetCommand(ActivityManager activityManager, ActivityOrder order) {
        this.activityManager = activityManager;
        this.order = order;
    }

    @Override
    public CommandResult execute() {
        activityManager.setDefaultOrder(order);
        return new CommandResult("Default activity order updated: " + order.name().toLowerCase(Locale.ROOT)
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
