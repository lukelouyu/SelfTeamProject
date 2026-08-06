package seedu.unienable.command;

/** Describes whether executing a command can change persistent application state. */
public enum CommandEffect {
    /** Does not change persistent application state. */
    READ_ONLY,
    /** May change persistent application state and therefore requires a transaction. */
    MUTATING
}
