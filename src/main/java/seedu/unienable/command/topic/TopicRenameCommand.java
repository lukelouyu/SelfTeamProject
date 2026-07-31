package seedu.unienable.command.topic;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;

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

    /**
     * Creates a TopicRenameCommand.
     *
     * @param topicManager the manager holding the topic to rename
     * @param category the fixed category the topic belongs to
     * @param oldName the topic's current name
     * @param newName the topic's proposed new name, unique within its category
     */
    public TopicRenameCommand(TopicManager topicManager, ActivityCategory category, String oldName, String newName) {
        this.topicManager = topicManager;
        this.category = category;
        this.oldName = oldName;
        this.newName = newName;
    }

    /** Returns the category of the topic this command will rename, for the UI loop's preview. */
    public ActivityCategory getCategory() {
        return category;
    }

    /** Returns the topic's current name, for the UI loop's preview. */
    public String getOldName() {
        return oldName;
    }

    /** Returns the topic's proposed new name, for the UI loop's preview. */
    public String getNewName() {
        return newName;
    }

    @Override
    public CommandResult execute() throws InvalidIndexException, DuplicateActivityException {
        int updated = topicManager.rename(category, oldName, newName);
        return new CommandResult("Topic renamed from " + oldName + " to " + newName + ".\n"
                + "Updated linked activities: " + updated);
    }
}
