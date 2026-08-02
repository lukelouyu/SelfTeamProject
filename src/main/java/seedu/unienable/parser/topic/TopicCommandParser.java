package seedu.unienable.parser.topic;

import seedu.unienable.command.topic.TopicAddCommand;
import seedu.unienable.command.topic.TopicDeleteCommand;
import seedu.unienable.command.topic.TopicListCommand;
import seedu.unienable.command.topic.TopicRenameCommand;
import seedu.unienable.exception.DuplicateActivityException;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidIndexException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.parser.common.FieldParser;

/** Parses topic-related commands (topic add, topic list, topic rename, topic delete) into Command objects. */
public class TopicCommandParser {
    /**
     * Parses a topic add command's argument text into a TopicAddCommand. Fields must appear in
     * the order documented in the User Guide: c/ then n/.
     *
     * @param topicManager the manager the resulting command will add to
     * @param args the text after the "topic add" command words
     * @return the parsed TopicAddCommand
     * @throws MissingInputException if the category or name is missing
     * @throws InvalidActivityException if the category is invalid
     */
    public TopicAddCommand parseAdd(TopicManager topicManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidCommandException {
        FieldParser.rejectUnrecognisedLeadingText(args, "c/", "n/");
        ActivityCategory category = FieldParser.parseCategory(
                FieldParser.requireField(args, "c/", "n/", "category"));
        String name = FieldParser.requireField(args, "n/", null, "topic name");
        FieldParser.validateNoDelimiter(name, "topic name");
        return new TopicAddCommand(topicManager, category, name);
    }

    /**
     * Parses a topic list command's argument text into a TopicListCommand. The category filter is
     * optional.
     *
     * @param topicManager the manager the resulting command will read from
     * @param args the text after the "topic list" command words
     * @return the parsed TopicListCommand
     * @throws InvalidActivityException if the category is invalid
     */
    public TopicListCommand parseList(TopicManager topicManager, String args)
            throws InvalidActivityException, InvalidCommandException {
        FieldParser.rejectUnrecognisedLeadingText(args, "c/");
        if (FieldParser.indexOfMarker(args, "c/", 0) == -1) {
            return new TopicListCommand(topicManager, null);
        }
        return new TopicListCommand(topicManager,
                FieldParser.parseCategory(FieldParser.extractField(args, "c/", null)));
    }

    /**
     * Parses a topic rename command's argument text into a TopicRenameCommand. Fields must appear
     * in the order documented in the User Guide: c/ then old/ then new/.
     *
     * @param topicManager the manager the resulting command will update
     * @param args the text after the "topic rename" command words
     * @return the parsed TopicRenameCommand
     * @throws MissingInputException if the category, old name, or new name is missing
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidIndexException if the old topic name does not exist under the category
     * @throws DuplicateActivityException if the new name already exists under the category as a
     *     different topic
     */
    public TopicRenameCommand parseRename(TopicManager topicManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidIndexException,
            DuplicateActivityException, InvalidCommandException {
        FieldParser.rejectUnrecognisedLeadingText(args, "c/", "old/", "new/");
        ActivityCategory category = FieldParser.parseCategory(
                FieldParser.requireField(args, "c/", "old/", "category"));
        String oldName = FieldParser.requireField(args, "old/", "new/", "old topic name");
        String newName = FieldParser.requireField(args, "new/", null, "new topic name");
        FieldParser.validateNoDelimiter(newName, "new topic name");
        topicManager.checkCanRename(category, oldName, newName);
        return new TopicRenameCommand(topicManager, category, oldName, newName);
    }

    /**
     * Parses a topic delete command's argument text into a TopicDeleteCommand. Fields must appear
     * in the order documented in the User Guide: c/ then n/.
     *
     * @param topicManager the manager the resulting command will delete from
     * @param args the text after the "topic delete" command words
     * @return the parsed TopicDeleteCommand
     * @throws MissingInputException if the category or name is missing
     * @throws InvalidActivityException if the category is invalid
     * @throws InvalidIndexException if the topic does not exist under the category
     * @throws DuplicateActivityException if any activity under the category is still assigned to
     *     the topic
     */
    public TopicDeleteCommand parseDelete(TopicManager topicManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidIndexException,
            DuplicateActivityException, InvalidCommandException {
        FieldParser.rejectUnrecognisedLeadingText(args, "c/", "n/");
        ActivityCategory category = FieldParser.parseCategory(
                FieldParser.requireField(args, "c/", "n/", "category"));
        String name = FieldParser.requireField(args, "n/", null, "topic name");
        topicManager.checkCanDelete(category, name);
        return new TopicDeleteCommand(topicManager, category, name);
    }
}
