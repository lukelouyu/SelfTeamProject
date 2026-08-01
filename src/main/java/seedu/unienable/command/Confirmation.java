package seedu.unienable.command;

/**
 * The confirmation behaviour one {@link Confirmable} command instance needs before it may
 * execute. Exactly one of three outcomes:
 *
 * <ul>
 *   <li>{@link #proceed()} - no prompt is shown; the command executes immediately (e.g. "reset
 *       all" when there is nothing to reset).</li>
 *   <li>{@link #cancel(String)} - no prompt is shown; the command is cancelled and the given
 *       message is shown instead (e.g. an edit whose requested fields already match the stored
 *       activity).</li>
 *   <li>{@link #ask(String)} - the given y/n prompt is shown, and the answer decides whether the
 *       command executes.</li>
 * </ul>
 */
public final class Confirmation {
    private enum Kind { PROCEED, CANCEL, ASK }

    private final Kind kind;
    private final String message;

    private Confirmation(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    /** Returns a Confirmation that lets the command execute with no prompt. */
    public static Confirmation proceed() {
        return new Confirmation(Kind.PROCEED, null);
    }

    /**
     * Returns a Confirmation that cancels the command with no prompt, showing the given message
     * in place of the standard "Cancelled. No changes were made." text.
     *
     * @param message the message to show instead of a y/n prompt
     */
    public static Confirmation cancel(String message) {
        return new Confirmation(Kind.CANCEL, message);
    }

    /**
     * Returns a Confirmation that shows the given prompt and asks the user to answer y/n.
     *
     * @param prompt the confirmation preview and question to show
     */
    public static Confirmation ask(String prompt) {
        return new Confirmation(Kind.ASK, prompt);
    }

    /** Returns whether the command should execute immediately with no prompt. */
    public boolean isProceed() {
        return kind == Kind.PROCEED;
    }

    /** Returns whether the command should be cancelled immediately with no prompt. */
    public boolean isCancel() {
        return kind == Kind.CANCEL;
    }

    /** Returns whether a y/n prompt should be shown. */
    public boolean isAsk() {
        return kind == Kind.ASK;
    }

    /** Returns the cancel message or the ask prompt; null if {@link #isProceed()}. */
    public String getMessage() {
        return message;
    }
}
