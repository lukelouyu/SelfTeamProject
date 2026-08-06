package seedu.unienable.command;

import seedu.unienable.exception.UniEnableException;

/** Base type for all executable user commands. */
public abstract class Command {
    /**
     * Returns this command's persistent-state effect.
     *
     * <p>Every concrete command must declare this contract directly or inherit it from an
     * effect-specific base type, keeping transaction metadata out of a central type list.
     *
     * @return this command's persistent-state effect
     */
    public abstract CommandEffect getEffect();

    /**
     * Returns whether this particular execution would change persistent state.
     *
     * <p>Most mutating commands always return true through this default implementation. Commands
     * that can be no-ops may override it after inspecting their current manager state.
     *
     * @return true if execution needs snapshotting and persistence
     */
    public boolean hasStateChange() {
        return getEffect() == CommandEffect.MUTATING;
    }

    /**
     * Executes this command.
     *
     * @return the result of executing this command
     * @throws UniEnableException if the command cannot be completed
     */
    public abstract CommandResult execute() throws UniEnableException;
}
