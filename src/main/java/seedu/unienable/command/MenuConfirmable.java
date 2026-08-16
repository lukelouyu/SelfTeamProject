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
     * Returns the complete menu text to show, including the final "Enter ..." prompt line. A
     * menu-confirmable command must always have a menu to show, even when its own state means
     * every choice would currently have no visible effect (e.g. every count in the preview is
     * zero) - implementations must never return null. A null return previously meant "nothing to
     * confirm, proceed immediately with no menu shown," which let a command's default outcome run
     * unconfirmed whenever its own state happened to make the prompt seem unnecessary - exactly
     * how {@code reset all} once silently performed a full reset on an empty state with no prompt
     * at all (DEFECT-01). A command whose confirmation is genuinely optional should decide that
     * before ever reaching this interface - e.g. by not implementing {@link MenuConfirmable} for
     * that case - rather than returning null from a method this interface's only caller,
     * {@code CommandConfirmationHandler}, treats as a fixed non-null contract.
     *
     * @return the menu text to show; never null
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
