package seedu.unienable.parser.topic;

import java.util.Locale;

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
        rejectUnrecognisedLeadingText(args, "c/", "n/");
        ActivityCategory category = parseCategory(requireField(args, "c/", "n/", "category"));
        String name = requireField(args, "n/", null, "topic name");
        validateNoDelimiter(name, "topic name");
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
        rejectUnrecognisedLeadingText(args, "c/");
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
     * @throws InvalidIndexException if the old topic name does not exist under the category
     * @throws DuplicateActivityException if the new name already exists under the category as a
     *     different topic
     */
    public TopicRenameCommand parseRename(TopicManager topicManager, String args)
            throws MissingInputException, InvalidActivityException, InvalidIndexException,
            DuplicateActivityException, InvalidCommandException {
        rejectUnrecognisedLeadingText(args, "c/", "old/", "new/");
        ActivityCategory category = parseCategory(requireField(args, "c/", "old/", "category"));
        String oldName = requireField(args, "old/", "new/", "old topic name");
        String newName = requireField(args, "new/", null, "new topic name");
        validateNoDelimiter(newName, "new topic name");
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
        rejectUnrecognisedLeadingText(args, "c/", "n/");
        ActivityCategory category = parseCategory(requireField(args, "c/", "n/", "category"));
        String name = requireField(args, "n/", null, "topic name");
        topicManager.checkCanDelete(category, name);
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
            return ActivityCategory.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidActivityException("category must be one of ACADEMIC, CCA, WORK_INTERNSHIP, OTHERS.");
        }
    }

    /**
     * Rejects a value that contains the storage delimiter '|', since topics.txt cannot escape it
     * (see TopicStorage). Catching this here, before the topic is created, avoids the
     * alternative: the topic is accepted and reported as added, then permanently fails to
     * persist because Storage.save() rejects the same value on every later save.
     *
     * @param value the already-extracted field value
     * @param fieldName the field name to use in the error message
     * @throws InvalidActivityException if value contains '|'
     */
    private void validateNoDelimiter(String value, String fieldName) throws InvalidActivityException {
        if (value.contains("|")) {
            throw new InvalidActivityException(fieldName + " must not contain the '|' character.");
        }
    }

    /**
     * Rejects text that appears before the first marker this command recognises (or, if none of
     * the given markers appear at all, any non-blank text at all) - see
     * {@link FieldParser#leadingUnrecognisedText}.
     *
     * @param args the full argument text
     * @param markers every marker this command recognises
     * @throws InvalidCommandException if unrecognised leading text is present
     */
    private void rejectUnrecognisedLeadingText(String args, String... markers) throws InvalidCommandException {
        String leading = FieldParser.leadingUnrecognisedText(args, markers);
        if (!leading.isEmpty()) {
            throw new InvalidCommandException("Unknown option \"" + leading + "\".");
        }
    }
}
