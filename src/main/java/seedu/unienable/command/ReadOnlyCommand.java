package seedu.unienable.command;

/** Base type for commands that cannot change persistent application state. */
public abstract class ReadOnlyCommand extends Command {
    @Override
    public final CommandEffect getEffect() {
        return CommandEffect.READ_ONLY;
    }
}
