package seedu.unienable.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.unienable.command.activity.crud.AddCommand;
import seedu.unienable.command.Command;
import seedu.unienable.command.accessibility.connection.ConnectionFindCommand;
import seedu.unienable.command.accessibility.connection.ConnectionListCommand;
import seedu.unienable.command.accessibility.connection.ConnectionValidateCommand;
import seedu.unienable.command.accessibility.connection.ConnectionViewCommand;
import seedu.unienable.command.activity.crud.DeleteCommand;
import seedu.unienable.command.activity.crud.EditCommand;
import seedu.unienable.command.general.ExitCommand;
import seedu.unienable.command.accessibility.facility.FacilityFindCommand;
import seedu.unienable.command.accessibility.facility.FacilityListCommand;
import seedu.unienable.command.accessibility.facility.FacilityValidateCommand;
import seedu.unienable.command.accessibility.facility.FacilityViewCommand;
import seedu.unienable.command.accessibility.route.RouteCommand;
import seedu.unienable.command.dashboard.DashboardCommand;
import seedu.unienable.command.timetable.TimetableCommand;
import seedu.unienable.command.activity.general.FindCommand;
import seedu.unienable.command.general.GuideCommand;
import seedu.unienable.command.general.ResetCommand;
import seedu.unienable.command.preference.PreferenceResetCommand;
import seedu.unienable.command.preference.PreferenceSetCommand;
import seedu.unienable.command.preference.PreferenceViewCommand;
import seedu.unienable.command.recommend.RecommendAdoptCommand;
import seedu.unienable.command.recommend.RecommendCancelCommand;
import seedu.unienable.command.recommend.RecommendGenerateCommand;
import seedu.unienable.command.recommend.RecommendViewCommand;
import seedu.unienable.command.activity.general.ListCommand;
import seedu.unienable.command.activity.general.MarkCommand;
import seedu.unienable.command.activity.general.NextCommand;
import seedu.unienable.command.activity.general.OrderSetCommand;
import seedu.unienable.command.activity.general.OrderViewCommand;
import seedu.unienable.command.topic.TopicAddCommand;
import seedu.unienable.command.topic.TopicDeleteCommand;
import seedu.unienable.command.topic.TopicListCommand;
import seedu.unienable.command.topic.TopicRenameCommand;
import seedu.unienable.command.activity.general.UnmarkCommand;
import seedu.unienable.command.activity.crud.ViewCommand;
import seedu.unienable.exception.InvalidCommandException;
import seedu.unienable.exception.MissingInputException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.ConnectionManager;
import seedu.unienable.logic.FacilityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.logic.recommend.RecommendationManager;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.storage.Storage;

class CommandDispatcherTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);

    @TempDir
    Path tempDir;

    private final ActivityManager activityManager = new ActivityManager();
    private final TopicManager topicManager = new TopicManager(activityManager);
    private final FacilityManager facilityManager = new FacilityManager(List.of());
    private final ConnectionManager connectionManager = new ConnectionManager(List.of());
    private final PreferenceManager preferenceManager = new PreferenceManager();
    private final RecommendationManager recommendationManager = new RecommendationManager();
    private CommandDispatcher dispatcher;

    @BeforeEach
    public void setUp() {
        // CommandDispatcher needs Storage, which in turn needs @TempDir's injected path - built
        // here rather than as a field initialiser, since @TempDir fields are only populated after
        // instance construction (field initialisers would still see tempDir as null).
        dispatcher = new CommandDispatcher(activityManager, topicManager, facilityManager,
                connectionManager, preferenceManager, recommendationManager, new Storage(tempDir));
    }

    @Test
    public void dispatch_add_returnsAddCommand() throws Exception {
        Command command = dispatcher.dispatch("add n/CG3207 lecture c/ACADEMIC date/2026-08-15 type/FIXED "
                + "from/11:00 to/12:00 energy/4 sensory/3", NOW);

        assertTrue(command instanceof AddCommand);
    }

    @Test
    public void dispatch_list_returnsListCommand() throws Exception {
        assertTrue(dispatcher.dispatch("list", NOW) instanceof ListCommand);
    }

    @Test
    public void dispatch_preferenceCommands_returnExpectedCommandTypes() throws Exception {
        assertTrue(dispatcher.dispatch("preference view", NOW) instanceof PreferenceViewCommand);
        assertTrue(dispatcher.dispatch("preference set tomato/on", NOW) instanceof PreferenceSetCommand);
        assertTrue(dispatcher.dispatch("preference reset", NOW) instanceof PreferenceResetCommand);
    }

    @Test
    public void dispatch_recommendCommands_returnExpectedCommandTypes() throws Exception {
        assertTrue(dispatcher.dispatch("recommend", NOW) instanceof RecommendGenerateCommand);
        assertTrue(dispatcher.dispatch("recommend this week", NOW) instanceof RecommendGenerateCommand);
        assertTrue(dispatcher.dispatch("recommend date/2026-08-15", NOW) instanceof RecommendGenerateCommand);
        recommendationManager.setProposal(new seedu.unienable.model.recommend.RecommendationProposal(
                seedu.unienable.logic.timetable.TimetableService.resolveThisWeek(NOW),
                seedu.unienable.logic.dashboard.DashboardService.resolveThisWeek(NOW), List.of(), List.of()));
        assertTrue(dispatcher.dispatch("recommend view", NOW) instanceof RecommendViewCommand);
        assertTrue(dispatcher.dispatch("recommend cancel", NOW) instanceof RecommendCancelCommand);
        assertTrue(dispatcher.dispatch("recommend adopt", NOW) instanceof RecommendAdoptCommand);
    }

    @Test
    public void dispatch_view_returnsViewCommand() throws Exception {
        assertTrue(dispatcher.dispatch("view 1", NOW) instanceof ViewCommand);
    }

    @Test
    public void dispatch_find_returnsFindCommand() throws Exception {
        assertTrue(dispatcher.dispatch("find k/lecture", NOW) instanceof FindCommand);
    }

    @Test
    public void dispatch_edit_returnsEditCommand() throws Exception {
        activityManager.add(new FixedActivity(activityManager.getNextId(), "Lecture", ActivityCategory.ACADEMIC,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(11, 0),
                EnergyRating.of(4), SensoryRating.of(3), null, null));

        assertTrue(dispatcher.dispatch("edit 1 note/Bring laptop", NOW) instanceof EditCommand);
    }

    @Test
    public void dispatch_delete_returnsDeleteCommand() throws Exception {
        assertTrue(dispatcher.dispatch("delete 1", NOW) instanceof DeleteCommand);
    }

    @Test
    public void dispatch_mark_returnsMarkCommand() throws Exception {
        assertTrue(dispatcher.dispatch("mark 1", NOW) instanceof MarkCommand);
    }

    @Test
    public void dispatch_unmark_returnsUnmarkCommand() throws Exception {
        assertTrue(dispatcher.dispatch("unmark 1", NOW) instanceof UnmarkCommand);
    }

    @Test
    public void dispatch_next_returnsNextCommand() throws Exception {
        assertTrue(dispatcher.dispatch("next", NOW) instanceof NextCommand);
    }

    @Test
    public void dispatch_orderView_returnsOrderViewCommand() throws Exception {
        assertTrue(dispatcher.dispatch("order view", NOW) instanceof OrderViewCommand);
    }

    @Test
    public void dispatch_orderSet_returnsOrderSetCommand() throws Exception {
        assertTrue(dispatcher.dispatch("order set time", NOW) instanceof OrderSetCommand);
    }

    @Test
    public void dispatch_topicAdd_returnsTopicAddCommand() throws Exception {
        assertTrue(dispatcher.dispatch("topic add c/ACADEMIC n/CG3207", NOW) instanceof TopicAddCommand);
    }

    @Test
    public void dispatch_topicList_returnsTopicListCommand() throws Exception {
        assertTrue(dispatcher.dispatch("topic list", NOW) instanceof TopicListCommand);
    }

    @Test
    public void dispatch_topicRename_returnsTopicRenameCommand() throws Exception {
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(dispatcher.dispatch("topic rename c/ACADEMIC old/CG3207 new/CS3207", NOW)
                instanceof TopicRenameCommand);
    }

    @Test
    public void dispatch_topicDelete_returnsTopicDeleteCommand() throws Exception {
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(dispatcher.dispatch("topic delete c/ACADEMIC n/CG3207", NOW) instanceof TopicDeleteCommand);
    }

    @Test
    public void dispatch_facilityList_returnsFacilityListCommand() throws Exception {
        assertTrue(dispatcher.dispatch("facility list", NOW) instanceof FacilityListCommand);
    }

    @Test
    public void dispatch_facilityView_returnsFacilityViewCommand() throws Exception {
        assertTrue(dispatcher.dispatch("facility view COM3", NOW) instanceof FacilityViewCommand);
    }

    @Test
    public void dispatch_facilityFind_returnsFacilityFindCommand() throws Exception {
        assertTrue(dispatcher.dispatch("facility find type/LIFT", NOW) instanceof FacilityFindCommand);
    }

    @Test
    public void dispatch_connectionList_returnsConnectionListCommand() throws Exception {
        assertTrue(dispatcher.dispatch("connection list", NOW) instanceof ConnectionListCommand);
    }

    @Test
    public void dispatch_connectionView_returnsConnectionViewCommand() throws Exception {
        assertTrue(dispatcher.dispatch("connection view 1", NOW) instanceof ConnectionViewCommand);
    }

    @Test
    public void dispatch_connectionFind_returnsConnectionFindCommand() throws Exception {
        assertTrue(dispatcher.dispatch("connection find from/COM3", NOW) instanceof ConnectionFindCommand);
    }

    @Test
    public void dispatch_facilityValidate_returnsFacilityValidateCommand() throws Exception {
        assertTrue(dispatcher.dispatch("facility validate", NOW) instanceof FacilityValidateCommand);
    }

    @Test
    public void dispatch_connectionValidate_returnsConnectionValidateCommand() throws Exception {
        assertTrue(dispatcher.dispatch("connection validate", NOW) instanceof ConnectionValidateCommand);
    }

    @Test
    public void dispatch_route_returnsRouteCommandWiredToLiveManagers() throws Exception {
        Command command = dispatcher.dispatch("route from/AS6 to/AS8", NOW);

        assertTrue(command instanceof RouteCommand);
    }

    @Test
    public void dispatch_routeMissingTo_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> dispatcher.dispatch("route from/AS6", NOW));
    }

    @Test
    public void dispatch_dashboard_returnsDashboardCommandWiredToLiveManager() throws Exception {
        Command command = dispatcher.dispatch("dashboard today", NOW);

        assertTrue(command instanceof DashboardCommand);
    }

    @Test
    public void dispatch_dashboardMissingSelector_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> dispatcher.dispatch("dashboard", NOW));
    }

    @Test
    public void dispatch_timetable_returnsTimetableCommandWiredToLiveManager() throws Exception {
        Command command = dispatcher.dispatch("timetable week/2026-08-17", NOW);

        assertTrue(command instanceof TimetableCommand);
    }

    @Test
    public void dispatch_timetableMissingSelector_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> dispatcher.dispatch("timetable", NOW));
    }

    @Test
    public void dispatch_guide_returnsGuideCommand() throws Exception {
        assertTrue(dispatcher.dispatch("guide", NOW) instanceof GuideCommand);
    }

    @Test
    public void dispatch_guideWithTopic_returnsGuideCommand() throws Exception {
        assertTrue(dispatcher.dispatch("guide add", NOW) instanceof GuideCommand);
    }

    @Test
    public void dispatch_bareMenuNumberOne_returnsGuideCommandForGettingStarted() throws Exception {
        // Regression test: the guide's main menu says "Enter a number from 1 to 12", so a bare
        // "1" entered as its own command (not "guide 1") must resolve the same way.
        Command command = dispatcher.dispatch("1", NOW);

        assertTrue(command instanceof GuideCommand);
        assertTrue(command.execute().getFeedback().startsWith("Getting started"));
    }

    @Test
    public void dispatch_bareMenuNumberTen_returnsGuideCommandForStorage() throws Exception {
        Command command = dispatcher.dispatch("9", NOW);

        assertTrue(command instanceof GuideCommand);
        assertTrue(command.execute().getFeedback().startsWith("Data files and storage"));
    }

    @Test
    public void dispatch_bareMenuNumberEleven_returnsGuideCommandForRoute() throws Exception {
        // "Route search" was added as menu item 11 when v2.0's route shipped.
        Command command = dispatcher.dispatch("10", NOW);

        assertTrue(command instanceof GuideCommand);
        assertTrue(command.execute().getFeedback().startsWith("Accessible routes"));
    }

    @Test
    public void dispatch_bareMenuNumberFive_returnsGuideCommandForDashboard() throws Exception {
        // Item 5 ("Completion and dashboard") was already reserved for the dashboard topic in
        // v1.0's menu; v2.0's dashboard fills that existing slot rather than taking a new number.
        Command command = dispatcher.dispatch("5", NOW);

        assertTrue(command instanceof GuideCommand);
        assertTrue(command.execute().getFeedback().startsWith("Dashboard"));
    }

    @Test
    public void dispatch_bareMenuNumberTwelve_returnsGuideCommandForTimetable() throws Exception {
        Command command = dispatcher.dispatch("11", NOW);

        assertTrue(command instanceof GuideCommand);
        assertTrue(command.execute().getFeedback().startsWith("Text timetable"));
    }

    @Test
    public void dispatch_bareMenuNumberThirteen_returnsGuideCommandForReturn() throws Exception {
        Command command = dispatcher.dispatch("12", NOW);

        assertTrue(command instanceof GuideCommand);
        assertTrue(command.execute().getFeedback().startsWith("Returning to the command prompt"));
    }

    @Test
    public void dispatch_bareZero_throwsInvalidCommandException() {
        // "0" is deliberately outside the recognised 1-12 menu range, so it must still fall
        // through to the normal "unknown command" error rather than being treated as a menu item.
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("0", NOW));
    }

    @Test
    public void dispatch_bareThirteen_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("13", NOW));
    }

    @Test
    public void dispatch_bye_returnsExitCommand() throws Exception {
        assertTrue(dispatcher.dispatch("bye", NOW) instanceof ExitCommand);
    }

    @Test
    public void dispatch_byeWithTrailingArguments_throwsInvalidCommandException() {
        // Regression test: "bye" is documented as taking no arguments, but trailing text was
        // previously silently ignored and the application still exited.
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("bye ignored text", NOW));
    }

    @Test
    public void dispatch_commandWordIsCaseInsensitive() throws Exception {
        assertTrue(dispatcher.dispatch("BYE", NOW) instanceof ExitCommand);
    }

    @Test
    public void dispatch_resetAll_returnsResetCommand() throws Exception {
        assertTrue(dispatcher.dispatch("reset all", NOW) instanceof ResetCommand);
    }

    @Test
    public void dispatch_resetAllIsCaseInsensitive_returnsResetCommand() throws Exception {
        assertTrue(dispatcher.dispatch("reset ALL", NOW) instanceof ResetCommand);
    }

    @Test
    public void dispatch_resetWithNoArguments_throwsMissingInputException() {
        assertThrows(MissingInputException.class, () -> dispatcher.dispatch("reset", NOW));
    }

    @Test
    public void dispatch_resetAllWithTrailingArguments_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("reset all extra", NOW));
    }

    @Test
    public void dispatch_resetUnknownOption_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("reset everything", NOW));
    }

    @Test
    public void dispatch_uppercaseTopicSubCommand_returnsTopicAddCommand() throws Exception {
        // Regression test: the top-level command word was already lower-cased before dispatch,
        // but the nested sub-command word ("ADD" in "TOPIC ADD") was compared unchanged against
        // lower-case literals in dispatchTopic()'s switch, so upper/mixed-case nested sub-commands
        // were rejected as unknown even though the top-level word worked in any case.
        assertTrue(dispatcher.dispatch("TOPIC ADD c/ACADEMIC n/CG3207", NOW) instanceof TopicAddCommand);
    }

    @Test
    public void dispatch_mixedCaseTopicRename_returnsTopicRenameCommand() throws Exception {
        topicManager.add(ActivityCategory.ACADEMIC, "CG3207");

        assertTrue(dispatcher.dispatch("ToPiC ReNaMe c/ACADEMIC old/CG3207 new/CS3207", NOW)
                instanceof TopicRenameCommand);
    }

    @Test
    public void dispatch_uppercaseFacilitySubCommand_returnsFacilityListCommand() throws Exception {
        assertTrue(dispatcher.dispatch("FACILITY LIST", NOW) instanceof FacilityListCommand);
    }

    @Test
    public void dispatch_mixedCaseFacilityFind_returnsFacilityFindCommand() throws Exception {
        assertTrue(dispatcher.dispatch("FaCiLiTy FiNd type/LIFT", NOW) instanceof FacilityFindCommand);
    }

    @Test
    public void dispatch_uppercaseConnectionSubCommand_returnsConnectionListCommand() throws Exception {
        assertTrue(dispatcher.dispatch("CONNECTION LIST", NOW) instanceof ConnectionListCommand);
    }

    @Test
    public void dispatch_mixedCaseConnectionView_returnsConnectionViewCommand() throws Exception {
        assertTrue(dispatcher.dispatch("CoNnEcTiOn ViEw 1", NOW) instanceof ConnectionViewCommand);
    }

    @Test
    public void dispatch_unknownCommand_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("banana", NOW));
    }

    @Test
    public void dispatch_unknownTopicSubCommand_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("topic bogus", NOW));
    }

    @Test
    public void dispatch_unknownFacilitySubCommand_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("facility bogus", NOW));
    }

    @Test
    public void dispatch_unknownConnectionSubCommand_throwsInvalidCommandException() {
        assertThrows(InvalidCommandException.class, () -> dispatcher.dispatch("connection bogus", NOW));
    }
}
