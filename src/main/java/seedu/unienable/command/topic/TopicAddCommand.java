package seedu.unienable.command.topic;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandEffect;
import seedu.unienable.command.CommandResult;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

/** Creates a new topic under a fixed category. */
public class TopicAddCommand extends Command {
    private final TopicManager topicManager;
    private final ActivityCategory category;
    private final String name;

    /**
     * Creates a TopicAddCommand.
     *
     * @param topicManager the manager to add the topic to
     * @param category the fixed category the topic will belong to
     * @param name the new topic's name, unique within its category
     */
    public TopicAddCommand(TopicManager topicManager, ActivityCategory category, String name) {
        this.topicManager = topicManager;
        this.category = category;
        this.name = name;
    }

    @Override
    public CommandEffect getEffect() {
        return CommandEffect.MUTATING;
    }

    @Override
    public CommandResult execute() throws DuplicateActivityException {
        topicManager.add(category, name);
        return new CommandResult("Topic created:\nCategory: " + category + "\nTopic   : " + name);
    }
}
