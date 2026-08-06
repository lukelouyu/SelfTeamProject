package seedu.unienable.command.topic;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;
import seedu.unienable.command.Confirmable;
import seedu.unienable.command.Confirmation;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Deletes a topic that is not assigned to any activity. Confirmation ("Delete this topic?
 * (y/n)") is a UI-loop concern handled before this command is executed; execute() performs the
 * deletion immediately.
 */
public class TopicDeleteCommand extends Command implements Confirmable {
    private final TopicManager topicManager;
    private final ActivityCategory category;
    private final String name;

    /**
     * Creates a TopicDeleteCommand.
     *
     * @param topicManager the manager holding the topic to delete
     * @param category the fixed category the topic belongs to
     * @param name the name of the topic to delete
     */
    public TopicDeleteCommand(TopicManager topicManager, ActivityCategory category, String name) {
        this.topicManager = topicManager;
        this.category = category;
        this.name = name;
    }

    @Override
    public CommandEffect getEffect() {
        return CommandEffect.MUTATING;
    }

    /** Returns the category of the topic this command will delete, for the UI loop's preview. */
    public ActivityCategory getCategory() {
        return category;
    }

    /** Returns the name of the topic this command will delete, for the UI loop's preview. */
    public String getName() {
        return name;
    }

    @Override
    public Confirmation getConfirmation() {
        return Confirmation.ask("Delete topic \"" + name + "\" under " + category + "? (y/n)");
    }

    @Override
    public CommandResult execute() throws InvalidIndexException, DuplicateActivityException {
        topicManager.delete(category, name);
        return new CommandResult("Topic " + name + " has been deleted.");
    }
}
