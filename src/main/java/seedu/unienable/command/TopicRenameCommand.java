package seedu.unienable.command;

import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

/**
 * Renames a topic, cascading the change to every activity currently assigned to it. The
 * confirmation preview ("Rename this topic? (y/n)") is a UI-loop concern handled before this
 * command is executed; execute() performs the rename immediately.
 */
public class TopicRenameCommand extends Command {
    private final TopicManager topicManager;
    private final ActivityCategory category;
    private final String oldName;
    private final String newName;

    public TopicRenameCommand(TopicManager topicManager, ActivityCategory category, String oldName, String newName) {
        this.topicManager = topicManager;
        this.category = category;
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException, DuplicateActivityException {
        int updated = topicManager.rename(category, oldName, newName);
        return new CommandResult("Topic renamed from " + oldName + " to " + newName + ".\n"
                + "Updated linked activities: " + updated);
    }
}
