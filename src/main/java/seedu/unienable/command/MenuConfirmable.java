package seedu.unienable.command;

import seedu.unienable.exception.UniEnableException;

/**
 * Implemented by a command whose confirmation is a numbered menu with more than one meaningful
 * choice - not a plain y/n - where the chosen answer determines both whether execute() runs and
 * which of several outcomes it performs (e.g. reset's "delete all" vs "keep class schedule" vs
 * "cancel"). Dispatched generically by {@code CommandConfirmationHandler} via
 * {@code instanceof}, exactly like {@link Confirmable} - a new menu-driven command never
 * requires editing the handler.
 */
public interface MenuConfirmable {
    /**
     * Returns the complete menu text to show, including the final "Enter ..." prompt line, or
     * null if there is nothing to confirm - execute() should proceed immediately with no menu
     * shown at all.
     *
     * @throws UniEnableException if building the menu preview fails
     */
    String getMenuPrompt() throws UniEnableException;

    /**
     * Applies the user's raw answer to this menu, mutating this command's internal state so a
     * subsequent execute() call performs the chosen action.
     *
     * @param rawAnswer the trimmed input line, or an empty string if the input stream ended
     * @return the outcome: proceed to execute(), or cancel with a specific message
     */
    MenuOutcome applyMenuAnswer(String rawAnswer);
}
