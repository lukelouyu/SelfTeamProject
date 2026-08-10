package seedu.unienable.command;

import java.time.LocalDateTime;

import seedu.unienable.exception.UniEnableException;

/**
 * Implemented by a {@link Confirmable} command whose validity can go stale while the user is
 * still answering its confirmation prompt - e.g. a "must not be in the past" check that was only
 * ever validated against the {@code now} sampled once at dispatch time, before the prompt was
 * even shown. Real time keeps advancing during the (potentially long) wait for the user's y/n
 * answer, so a value that was valid when the prompt was built can be stale by the time execution
 * actually happens.
 *
 * <p>{@code ApplicationRunner} calls {@link #validateBeforeExecution} with a freshly-sampled
 * {@code now}, once, immediately after confirmation and immediately before the command's
 * snapshot/execute - so a confirmation answered too late is rejected instead of silently
 * executing against data that was only valid a moment earlier. A command that has nothing
 * time-sensitive to recheck simply does not implement this interface; {@code ApplicationRunner}
 * never needs to know about any specific command class to support a new one.
 */
public interface PreExecutionValidatable {
    /**
     * Re-validates this command against the current time, immediately before execution.
     *
     * @param now the current date and time, freshly sampled after the user answered confirmation
     * @throws UniEnableException if the command is no longer valid at this later time
     */
    void validateBeforeExecution(LocalDateTime now) throws UniEnableException;
}
