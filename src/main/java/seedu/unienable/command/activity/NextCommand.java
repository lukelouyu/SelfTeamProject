package seedu.unienable.command.activity;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

import java.time.LocalDateTime;
import java.util.Optional;

import seedu.unienable.logic.ActivityManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.ui.MessageFormatter;

/**
 * Displays the next relevant activity and the count of overdue incomplete activities. Takes the
 * current date and time explicitly (never reads the system clock), matching
 * ActivityManager.next()'s deterministic, directly testable design.
 */
public class NextCommand extends Command {
    private final ActivityManager activityManager;
    private final LocalDateTime now;

    public NextCommand(ActivityManager activityManager, LocalDateTime now) {
        this.activityManager = activityManager;
        this.now = now;
    }

    @Override
    public CommandResult execute() {
        Optional<Activity> next = activityManager.next(now);
        int overdueCount = activityManager.countOverdueIncomplete(now);
        String overdueLine = "\n\nOverdue incomplete activities: " + overdueCount;
        if (next.isEmpty()) {
            return new CommandResult("You have no upcoming relevant activities." + overdueLine);
        }
        return new CommandResult("Your next relevant activity is:\n"
                + MessageFormatter.formatConcise(next.get()) + overdueLine);
    }
}
