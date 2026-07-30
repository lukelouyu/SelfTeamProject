package seedu.unienable.parser;

import seedu.unienable.command.TopicAddCommand;
import seedu.unienable.command.TopicDeleteCommand;
import seedu.unienable.command.TopicListCommand;
import seedu.unienable.command.TopicRenameCommand;
import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.model.enums.ActivityCategory;

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
            throws MissingInputException, InvalidActivityException {
        ActivityCategory category = parseCategory(requireField(args, "c/", "n/", "category"));
        String name = requireField(args, "n/", null, "topic name");
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
    public TopicListCommand parseList(TopicManager topicManager, String args) throws InvalidActivityException {
        if (FieldParser.indexOfMarker(args, "c/", 0) == -1) {
            return new TopicListCommand(topicManager, null);
        }
        return new TopicListCommand(topicManager, parseCategory(FieldParser.extractField(args, "c/", null)));
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
     */
    public TopicRenameCommand parseRename(TopicManager topicManager, String args)
            throws MissingInputException, InvalidActivityException {
        ActivityCategory category = parseCategory(requireField(args, "c/", "old/", "category"));
        String oldName = requireField(args, "old/", "new/", "old topic name");
        String newName = requireField(args, "new/", null, "new topic name");
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
     */
    public TopicDeleteCommand parseDelete(TopicManager topicManager, String args)
            throws MissingInputException, InvalidActivityException {
        ActivityCategory category = parseCategory(requireField(args, "c/", "n/", "category"));
        String name = requireField(args, "n/", null, "topic name");
        return new TopicDeleteCommand(topicManager, category, name);
    }

    private String requireField(String args, String startMarker, String endMarker, String fieldName)
            throws MissingInputException {
        String value = FieldParser.extractField(args, startMarker, endMarker);
        if (value == null || value.isEmpty()) {
            throw new MissingInputException(fieldName + " is required.");
        }
        return value;
    }

    private ActivityCategory parseCategory(String text) throws InvalidActivityException {
        try {
            return ActivityCategory.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidActivityException("category must be one of ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS.");
        }
    }
}
