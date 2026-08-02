package seedu.unienable.command.dashboard;

import java.time.LocalDateTime;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.dashboard.DashboardService;
import seedu.unienable.model.dashboard.DashboardPeriod;
import seedu.unienable.model.dashboard.DashboardSummary;
import seedu.unienable.ui.dashboard.DashboardFormatter;

/**
 * Shows a read-only planning summary for a period. Never mutates activities, topics, settings,
 * or any file, and requires no confirmation - executing it is always safe.
 */
public class DashboardCommand extends Command {
    private final ActivityManager activityManager;
    private final DashboardPeriod period;
    private final LocalDateTime now;
    private final boolean detail;

    /**
     * Creates a DashboardCommand.
     *
     * @param activityManager the manager to read activities from
     * @param period the already-resolved period to summarise
     * @param now the current date and time, captured once at parse time and used only for
     *     completion eligibility - never re-read from the system clock at execution time
     * @param detail whether to include the detail section
     */
    public DashboardCommand(ActivityManager activityManager, DashboardPeriod period, LocalDateTime now,
            boolean detail) {
        this.activityManager = activityManager;
        this.period = period;
        this.now = now;
        this.detail = detail;
    }

    @Override
    public CommandResult execute() {
        DashboardSummary summary = DashboardService.summarize(activityManager, period, now);
        return new CommandResult(DashboardFormatter.format(summary, detail));
    }
}
