package seedu.unienable.command;

/**
 * The result of applying one raw answer to a {@link MenuConfirmable}'s menu: either the answer
 * selected a valid, non-cancelling option and execute() should now run, or it should be
 * cancelled (either because the choice itself means "do nothing", or because the answer wasn't
 * one of the recognised choices at all) - in which case message carries exactly what to show.
 */
public final class MenuOutcome {
    private final boolean proceed;
    private final String message;

    private MenuOutcome(boolean proceed, String message) {
        this.proceed = proceed;
        this.message = message;
    }

    /** Returns an outcome that proceeds with command execution. */
    public static MenuOutcome proceed() {
        return new MenuOutcome(true, null);
    }

    /**
     * Returns an outcome that cancels execution and displays the supplied message.
     *
     * @param message the framed message to show in place of executing
     */
    public static MenuOutcome cancel(String message) {
        return new MenuOutcome(false, message);
    }

    /** Returns whether execute() should now run. */
    public boolean isProceed() {
        return proceed;
    }

    /** Returns the message to show; only meaningful when {@link #isProceed()} is false. */
    public String getMessage() {
        return message;
    }
}
