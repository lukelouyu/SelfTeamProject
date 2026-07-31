package seedu.unienable.command;

/** The outcome of executing one command: feedback text plus whether the application should exit. */
public class CommandResult {
    private final String feedback;
    private final boolean shouldExit;

    /**
     * Creates a CommandResult.
     *
     * @param feedback the text to display to the user
     * @param shouldExit whether the application should exit after displaying this result
     */
    public CommandResult(String feedback, boolean shouldExit) {
        this.feedback = feedback;
        this.shouldExit = shouldExit;
    }

    /**
     * Creates a non-exiting CommandResult.
     *
     * @param feedback the text to display to the user
     */
    public CommandResult(String feedback) {
        this(feedback, false);
    }

    /** Returns the text to display to the user. */
    public String getFeedback() {
        return feedback;
    }

    /** Returns whether the application should exit after displaying this result. */
    public boolean isShouldExit() {
        return shouldExit;
    }
}
