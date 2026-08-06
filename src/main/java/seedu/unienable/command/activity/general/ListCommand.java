package seedu.unienable.command.activity.general;

import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.command.CommandResult;

import java.time.LocalDateTime;
import java.util.List;

import seedu.unienable.logic.ActivityFilter;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.ui.MessageFormatter;

/** Lists activities matching an optional filter, in concise or detail view. */
public class ListCommand extends ReadOnlyCommand {
    private final ActivityManager activityManager;
    private final ActivityFilter filter;
    private final ActivityOrder order;
    private final boolean detail;
    private final LocalDateTime overdueAsOf;

    /**
     * Creates a ListCommand for the normal (non-overdue) view.
     *
     * @param activityManager the manager to list activities from
     * @param filter optional structured filter (status/category/topic/date), or null
     * @param order the ordering to apply to the results, or null to use the saved default
     * @param detail whether to render each result in detail view instead of concise view
     */
    public ListCommand(ActivityManager activityManager, ActivityFilter filter, ActivityOrder order,
            boolean detail) {
        this(activityManager, filter, order, detail, null);
    }

    /**
     * Creates a ListCommand.
     *
     * @param activityManager the manager to list activities from
     * @param filter optional structured filter (category/topic/date), or null; status/ is
     *     meaningless when overdueAsOf is supplied, since "overdue" already implies incomplete
     * @param order the ordering to apply to the results, or null to use the saved default
     * @param detail whether to render each result in detail view instead of concise view
     * @param overdueAsOf if non-null, restricts results to overdue incomplete activities as of
     *     this date and time ("list overdue") instead of every activity matching filter
     */
    public ListCommand(ActivityManager activityManager, ActivityFilter filter, ActivityOrder order,
            boolean detail, LocalDateTime overdueAsOf) {
        this.activityManager = activityManager;
        this.filter = filter;
        this.order = order;
        this.detail = detail;
        this.overdueAsOf = overdueAsOf;
    }

    @Override
    public CommandResult execute() {
        List<Activity> activities = overdueAsOf != null
                ? activityManager.listOverdue(overdueAsOf, filter, order)
                : activityManager.list(filter, order);
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
