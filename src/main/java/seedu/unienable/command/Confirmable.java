package seedu.unienable.command;

import seedu.unienable.exception.UniEnableException;

/**
 * Implemented by a command that needs a confirmation step before it may execute. This lets
 * {@code CommandConfirmationHandler} decide whether to prompt for any command generically, via
 * {@code instanceof Confirmable}, without knowing about any specific command class - a new
 * confirmable command only needs to implement this interface; the handler never needs to change.
 */
public interface Confirmable {
    /**
     * Returns the confirmation this command needs before executing.
     *
     * @return the confirmation outcome: proceed with no prompt, cancel with no prompt, or ask a
     *     y/n question
     * @throws UniEnableException if building the confirmation preview requires state that cannot
     *     be found (e.g. an activity ID that no longer exists)
     */
    Confirmation getConfirmation() throws UniEnableException;
}
