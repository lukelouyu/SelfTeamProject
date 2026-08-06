package seedu.unienable.command.general;

import seedu.unienable.command.ReadOnlyCommand;
import seedu.unienable.command.CommandResult;

/** Signals that the application should save and exit, per the User Guide's goodbye message. */
public class ExitCommand extends ReadOnlyCommand {
    public static final String GOODBYE_MESSAGE = "Your data has been saved.\nBye! Take care and see you again.";

    @Override
    public CommandResult execute() {
        return new CommandResult(GOODBYE_MESSAGE, true);
    }
}
