package seedu.unienable.command.activity.general;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import java.util.List;

import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.ui.MessageFormatter;

/** Finds activities matching every given keyword and an optional filter, in concise or detail view. */
public class FindCommand extends Command {
    private final ActivityManager activityManager;
    private final List<String> keywords;
    private final ActivityFilter filter;
    private final ActivityOrder order;
    private final boolean detail;

    /**
     * Creates a FindCommand.
     *
     * @param activityManager the manager to search
     * @param keywords keywords that must all match a description, topic, or note (AND logic)
     * @param filter optional structured filter (status/category/topic/date), or null
     * @param order the ordering to apply to the results, or null to use the saved default
     * @param detail whether to render each result in detail view instead of concise view
     */
    public FindCommand(ActivityManager activityManager, List<String> keywords, ActivityFilter filter,
            ActivityOrder order, boolean detail) {
        this.activityManager = activityManager;
        this.keywords = keywords;
        this.filter = filter;
        this.order = order;
        this.detail = detail;
    }

    @Override
    public CommandResult execute() {
        List<Activity> activities = activityManager.find(keywords, filter, order);
        return new CommandResult(formatResult(activities));
    }

    private String formatResult(List<Activity> activities) {
        if (activities.isEmpty()) {
            return "No activities found.";
        }
        StringBuilder result = new StringBuilder();
        result.append("Found ").append(activities.size())
                .append(activities.size() == 1 ? " activity:\n" : " activities:\n");
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
