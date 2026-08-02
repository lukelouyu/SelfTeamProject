package seedu.unienable.parser.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.unienable.command.Command;
import seedu.unienable.command.recommend.RecommendAdoptCommand;
import seedu.unienable.command.recommend.RecommendCancelCommand;
import seedu.unienable.command.recommend.RecommendGenerateCommand;
import seedu.unienable.command.recommend.RecommendViewCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.dashboard.DashboardService;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.logic.timetable.TimetableService;
import seedu.unienable.model.recommend.RecommendationProposal;

class RecommendCommandParserTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 10, 0);

    private RecommendCommandParser parser;
    private ActivityManager activityManager;
    private PreferenceManager preferenceManager;
    private RecommendationManager recommendationManager;

    @BeforeEach
    public void setUp() {
        parser = new RecommendCommandParser();
        activityManager = new ActivityManager();
        preferenceManager = new PreferenceManager();
        recommendationManager = new RecommendationManager();
    }

    @Test
    public void parse_generateVariants_returnGenerateCommandWithoutMutation() throws Exception {
        assertInstanceOf(RecommendGenerateCommand.class,
                parser.parse(activityManager, preferenceManager, recommendationManager, NOW, ""));
        assertInstanceOf(RecommendGenerateCommand.class,
                parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "this week"));
        assertInstanceOf(RecommendGenerateCommand.class,
                parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "date/2099-01-01"));
        assertEquals(0, activityManager.size());
        assertTrue(recommendationManager.getProposal().isEmpty());
    }

    @Test
    public void parse_viewAndCancel_returnCommandTypesWithoutExistingProposal() throws Exception {
        Command view = parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "view");
        Command cancel = parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "cancel");

        assertInstanceOf(RecommendViewCommand.class, view);
        assertInstanceOf(RecommendCancelCommand.class, cancel);
        assertTrue(recommendationManager.getProposal().isEmpty());
    }

    @Test
    public void parse_adopt_requiresExistingProposal() {
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "adopt"));

        recommendationManager.setProposal(new RecommendationProposal(
                TimetableService.resolveThisWeek(NOW),
                DashboardService.resolveThisWeek(NOW), List.of(), List.of()));

        assertInstanceOf(RecommendAdoptCommand.class,
                assertDoesNotThrowParse("adopt"));
    }

    @Test
    public void parse_rejectsPastTrailingAndUnknownInputs() {
        assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "date/2026-08-16"));
        assertThrows(InvalidDateTimeException.class,
                () -> parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "date/"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, preferenceManager, recommendationManager, NOW,
                        "date/2099-01-01 extra"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "this"));
        assertThrows(InvalidCommandException.class,
                () -> parser.parse(activityManager, preferenceManager, recommendationManager, NOW, "unknown"));
        assertEquals(0, activityManager.size());
        assertTrue(recommendationManager.getProposal().isEmpty());
    }

    private Command assertDoesNotThrowParse(String args) {
        try {
            return parser.parse(activityManager, preferenceManager, recommendationManager, NOW, args);
        } catch (Exception e) {
            throw new AssertionError("Expected parse to succeed for: " + args, e);
        }
    }
}
