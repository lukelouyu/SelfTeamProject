package seedu.unienable.app;

import java.util.ArrayList;
import java.util.List;

import seedu.unienable.command.Command;
import seedu.unienable.command.CommandResult;
import seedu.unienable.exception.UniEnableException;
import seedu.unienable.logic.ActivityManager;
import seedu.unienable.logic.TopicManager;
import seedu.unienable.logic.preference.PreferenceManager;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.ActivityCopyFactory;
import seedu.unienable.model.classes.Topic;
import seedu.unienable.model.enums.ActivityOrder;
import seedu.unienable.model.preference.PreferenceProfile;

/** Executes commands inside an in-memory transaction that can be rolled back before persistence. */
final class CommandTransactionExecutor {
    private final ActivityManager activityManager;
    private final TopicManager topicManager;
    private final PreferenceManager preferenceManager;

    CommandTransactionExecutor(ActivityManager activityManager, TopicManager topicManager,
            PreferenceManager preferenceManager) {
        this.activityManager = activityManager;
        this.topicManager = topicManager;
        this.preferenceManager = preferenceManager;
    }

    Execution execute(Command command) throws UniEnableException {
        if (!command.hasStateChange()) {
            return new Execution(command.execute(), null);
        }
        Snapshot snapshot = new Snapshot();
        try {
            return new Execution(command.execute(), snapshot);
        } catch (UniEnableException | RuntimeException failure) {
            restoreAfterFailure(snapshot, failure);
            throw failure;
        }
    }

    private void restoreAfterFailure(Snapshot snapshot, Throwable failure) {
        try {
            snapshot.restore();
        } catch (RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    /** Holds a completed command result and its optional pre-command rollback point. */
    static final class Execution {
        private final CommandResult result;
        private final Snapshot snapshot;

        private Execution(CommandResult result, Snapshot snapshot) {
            this.result = result;
            this.snapshot = snapshot;
        }

        CommandResult getResult() {
            return result;
        }

        boolean hasStateChange() {
            return snapshot != null;
        }

        void rollback() {
            if (snapshot != null) {
                snapshot.restore();
            }
        }
    }

    /** Exact pre-command state for activities, topics, IDs, order, and preferences. */
    private final class Snapshot {
        private final List<Activity> activities = ActivityCopyFactory.copyAll(activityManager.getAll());
        private final List<Topic> topics = copyTopics(topicManager.getAll());
        // Captured via the non-throwing snapshotIdAllocation(), not getNextId(): getNextId()
        // throws once the ID space is exhausted, but a snapshot must be capturable for *every*
        // mutating command (mark, delete, reset, topic/preference changes, ...), not only ones
        // that allocate a new activity ID - otherwise ID exhaustion would block every other
        // mutating command from even starting, not just ones that actually need a new ID.
        private final ActivityManager.IdAllocationState idAllocationState = activityManager.snapshotIdAllocation();
        private final ActivityOrder order = activityManager.getDefaultOrder();
        private final PreferenceProfile preferences = preferenceManager.getProfile();

        private void restore() {
            activityManager.restoreState(activities, idAllocationState, order);
            topicManager.loadAll(topics);
            preferenceManager.setProfile(preferences);
        }
    }

    private static List<Topic> copyTopics(List<Topic> source) {
        List<Topic> copies = new ArrayList<>(source.size());
        for (Topic topic : source) {
            copies.add(new Topic(topic.getCategory(), topic.getName()));
        }
        return copies;
    }
}
