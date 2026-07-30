package seedu.unienable.command;

import seedu.unienable.exception.UniEnableException;

/** Base type for all executable user commands. */
public abstract class Command {
    /**
     * Executes this command.
     *
     * @return the result of executing this command
     * @throws UniEnableException if the command cannot be completed
     */
    public abstract CommandResult execute() throws UniEnableException;
}
