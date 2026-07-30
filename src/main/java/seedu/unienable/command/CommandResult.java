package seedu.unienable.command;

/** The outcome of executing one command: feedback text plus whether the application should exit. */
public class CommandResult {
    private final String feedback;
    private final boolean shouldExit;

    public CommandResult(String feedback, boolean shouldExit) {
        this.feedback = feedback;
        this.shouldExit = shouldExit;
    }

    public CommandResult(String feedback) {
        this(feedback, false);
    }

    public String getFeedback() {
        return feedback;
    }

    public boolean isShouldExit() {
        return shouldExit;
    }
}
