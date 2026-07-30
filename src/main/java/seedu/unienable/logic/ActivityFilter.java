package seedu.unienable.logic;

import java.time.LocalDate;

import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.CompletionStatus;

/** Optional filters for narrowing an activity list or search; a null field means "no filter" for it. */
public class ActivityFilter {
    private final CompletionStatus status;
    private final ActivityCategory category;
    private final String topic;
    private final LocalDate date;

    public ActivityFilter(CompletionStatus status, ActivityCategory category, String topic, LocalDate date) {
        this.status = status;
        this.category = category;
        this.topic = topic;
        this.date = date;
    }

    /** Returns whether the given activity satisfies every filter that was supplied (non-null). */
    public boolean matches(Activity activity) {
        if (status != null && activity.getStatus() != status) {
            return false;
        }
        if (category != null && activity.getCategory() != category) {
            return false;
        }
        if (topic != null && !topic.equalsIgnoreCase(activity.getTopic())) {
            return false;
        }
        return date == null || date.equals(activity.getDate());
    }
}
