package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import java.util.List;

import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.ui.MessageFormatter;

/** Lists activities matching an optional filter, in concise or detail view. */
public class ListCommand extends Command {
    private final ActivityManager activityManager;
    private final ActivityFilter filter;
    private final ActivityOrder order;
    private final boolean detail;

    public ListCommand(ActivityManager activityManager, ActivityFilter filter, ActivityOrder order,
            boolean detail) {
        this.activityManager = activityManager;
        this.filter = filter;
        this.order = order;
        this.detail = detail;
    }

    @Override
    public CommandResult execute() {
        List<Activity> activities = activityManager.list(filter, order);
        return new CommandResult(formatResult(activities));
    }

    private String formatResult(List<Activity> activities) {
        if (activities.isEmpty()) {
            return "No activities found.";
        }
        StringBuilder result = new StringBuilder();
        result.append("Here are ").append(activities.size())
                .append(activities.size() == 1 ? " matching activity:\n" : " matching activities:\n");
        for (int i = 0; i < activities.size(); i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(detail ? MessageFormatter.formatDetail(activities.get(i))
                    : MessageFormatter.formatConcise(activities.get(i)));
        }
        return result.toString();
    }
}
