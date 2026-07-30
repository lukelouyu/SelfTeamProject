package seedu.unienable.command;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

/** Creates a new topic under a fixed category. */
public class TopicAddCommand extends Command {
    private final TopicManager topicManager;
    private final ActivityCategory category;
    private final String name;

    public TopicAddCommand(TopicManager topicManager, ActivityCategory category, String name) {
        this.topicManager = topicManager;
        this.category = category;
        this.name = name;
    }

    @Override
    public CommandResult execute() throws DuplicateActivityException {
        topicManager.add(category, name);
        return new CommandResult("Topic created:\nCategory: " + category + "\nTopic   : " + name);
    }
}
