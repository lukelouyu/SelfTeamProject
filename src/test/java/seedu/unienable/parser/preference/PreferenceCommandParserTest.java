package seedu.unienable.parser.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.unienable.command.Command;
import seedu.unienable.command.preference.PreferenceResetCommand;
import seedu.unienable.command.preference.PreferenceSetCommand;
import seedu.unienable.command.preference.PreferenceViewCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.model.preference.PreferenceProfile;
import seedu.unienable.model.preference.TomatoSuggestion;

class PreferenceCommandParserTest {
    private PreferenceManager manager;
    private PreferenceCommandParser parser;

    @BeforeEach
    public void setUp() {
        manager = new PreferenceManager();
        parser = new PreferenceCommandParser();
    }

    @Test
    public void parse_viewAndReset_succeedWithoutTrailingText() throws Exception {
        assertInstanceOf(PreferenceViewCommand.class, parser.parse(manager, "view"));
        assertInstanceOf(PreferenceResetCommand.class, parser.parse(manager, "RESET"));
        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, "view extra"));
        assertThrows(InvalidCommandException.class, () -> parser.parse(manager, "reset extra"));
    }

    @Test
    public void parse_eachOneFieldUpdate_buildsCompleteProfile() throws Exception {
        assertProfile("set start/09:00", LocalTime.of(9, 0), LocalTime.of(20, 0), 15,
                TomatoSuggestion.OFF);
        assertProfile("set end/21:00", LocalTime.of(8, 0), LocalTime.of(21, 0), 15,
                TomatoSuggestion.OFF);
        assertProfile("set buffer/0", LocalTime.of(8, 0), LocalTime.of(20, 0), 0,
                TomatoSuggestion.OFF);
        assertProfile("set buffer/1440", LocalTime.of(8, 0), LocalTime.of(20, 0), 1440,
                TomatoSuggestion.OFF);
        assertProfile("set tomato/on", LocalTime.of(8, 0), LocalTime.of(20, 0), 15,
                TomatoSuggestion.ON);
        assertProfile("set tomato/off", LocalTime.of(8, 0), LocalTime.of(20, 0), 15,
                TomatoSuggestion.OFF);
    }

    @Test
    public void parse_multiFieldAndReorderedMarkers_buildOneProfile() throws Exception {
        assertProfile("set end/18:00 start/09:00", LocalTime.of(9, 0), LocalTime.of(18, 0), 15,
                TomatoSuggestion.OFF);
        assertProfile("set buffer/20 start/09:00", LocalTime.of(9, 0), LocalTime.of(20, 0), 20,
                TomatoSuggestion.OFF);
        assertProfile("set tomato/on start/09:00", LocalTime.of(9, 0), LocalTime.of(20, 0), 15,
                TomatoSuggestion.ON);
        assertProfile("set buffer/20 end/18:00", LocalTime.of(8, 0), LocalTime.of(18, 0), 20,
                TomatoSuggestion.OFF);
        assertProfile("set tomato/on end/18:00", LocalTime.of(8, 0), LocalTime.of(18, 0), 15,
                TomatoSuggestion.ON);
        assertProfile("set tomato/on buffer/20", LocalTime.of(8, 0), LocalTime.of(20, 0), 20,
                TomatoSuggestion.ON);
        assertProfile("set buffer/20 tomato/on start/09:00", LocalTime.of(9, 0),
                LocalTime.of(20, 0), 20, TomatoSuggestion.ON);
        assertProfile("set tomato/on buffer/20 end/18:00 start/09:00", LocalTime.of(9, 0),
                LocalTime.of(18, 0), 20, TomatoSuggestion.ON);
    }

    @Test
    public void parse_validationUsesUnchangedCurrentFields() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set start/21:00"));
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
    }

    @Test
    public void parse_equalOrReversedResult_rejectedWithoutMutation() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set start/09:00 end/09:00"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set start/18:00 end/09:00"));
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
    }

    @Test
    public void parse_invalidTimes_rejectedStrictly() {
        for (String time : new String[] {"8:00", "08:0", "24:00", "25:00"}) {
            assertThrows(InvalidDateTimeException.class,
                    () -> parser.parse(manager, "set start/" + time));
        }
    }

    @Test
    public void parse_invalidBuffer_rejected() {
        for (String buffer : new String[] {"-1", "1441", "abc"}) {
            assertThrows(InvalidCommandException.class,
                    () -> parser.parse(manager, "set buffer/" + buffer));
        }
    }

    @Test
    public void parse_invalidTomato_rejected() {
        for (String value : new String[] {"yes", "enabled"}) {
            assertThrows(InvalidCommandException.class,
                    () -> parser.parse(manager, "set tomato/" + value));
        }
    }

    @Test
    public void parse_missingEmptyUnknownDuplicateAndTrailing_rejected() {
        assertThrows(MissingInputException.class, () -> parser.parse(manager, ""));
        assertThrows(MissingInputException.class, () -> parser.parse(manager, "set"));
        assertThrows(MissingInputException.class, () -> parser.parse(manager, "set start/"));
        assertThrows(MissingInputException.class, () -> parser.parse(manager, "set tomato/"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set unknown/value"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set tomato/on tomato/off"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set start/09:00 start/10:00"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set end/18:00 end/19:00"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(manager, "set buffer/20 buffer/30"));
        assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(manager, "set start/09:00 extra"));
        assertEquals(PreferenceProfile.defaults(), manager.getProfile());
    }

    @Test
    public void parse_caseInsensitiveMarkersAndTomato_succeeds() throws Exception {
        assertProfile("SET START/09:00 END/18:00 BUFFER/20 TOMATO/ON",
                LocalTime.of(9, 0), LocalTime.of(18, 0), 20, TomatoSuggestion.ON);
    }

    private void assertProfile(String input, LocalTime start, LocalTime end, int buffer,
            TomatoSuggestion tomato) throws Exception {
        Command command = parser.parse(manager, input);
        PreferenceSetCommand set = assertInstanceOf(PreferenceSetCommand.class, command);
        assertEquals(PreferenceProfile.of(start, end, buffer, tomato), set.getProposedProfile());
        assertEquals(PreferenceProfile.defaults(), manager.getProfile(), "parsing must not mutate");
    }
}
