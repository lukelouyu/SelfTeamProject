package seedu.unienable.command.timetable;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.timetable.TimetableService;
import seedu.unienable.model.timetable.TimetableMode;
import seedu.unienable.model.timetable.TimetablePeriod;
import seedu.unienable.model.timetable.TimetableView;
import seedu.unienable.ui.timetable.TimetableFormatter;

/** Shows a deterministic read-only day or week timetable. */
public class TimetableCommand extends Command {
    private final ActivityManager activityManager;
    private final TimetablePeriod period;
    private final TimetableMode mode;

    /**
     * Creates a timetable command over the given manager and resolved period.
     *
     * @param activityManager manager to read without mutation
     * @param period already resolved selected period
     * @param mode selected output mode
     */
    public TimetableCommand(ActivityManager activityManager, TimetablePeriod period,
            TimetableMode mode) {
        this.activityManager = activityManager;
        this.period = period;
        this.mode = mode;
    }

    @Override
    public CommandResult execute() {
        TimetableView view = TimetableService.build(activityManager, period);
        return new CommandResult(TimetableFormatter.format(view, mode));
    }
}
