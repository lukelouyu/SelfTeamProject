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
    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    /**
     * Creates an ActivityFilter with a single exact date filter (or none). Any parameter left
     * null means "no filter" for that field.
     *
     * @param status required completion status, or null
     * @param category required category, or null
     * @param topic required topic name (matched case-insensitively), or null
     * @param date required date, or null
     */
    public ActivityFilter(CompletionStatus status, ActivityCategory category, String topic, LocalDate date) {
        this(status, category, topic, date, null, null);
    }

    /**
     * Creates an ActivityFilter, optionally filtering by an inclusive date range instead of a
     * single exact date - used for relative dates like "this week". date and the
     * dateFrom/dateTo range are mutually exclusive; callers should supply at most one of the two
     * shapes.
     *
     * @param status required completion status, or null
     * @param category required category, or null
     * @param topic required topic name (matched case-insensitively), or null
     * @param date required exact date, or null
     * @param dateFrom the inclusive start of a required date range, or null
     * @param dateTo the inclusive end of a required date range, or null
     */
    public ActivityFilter(CompletionStatus status, ActivityCategory category, String topic, LocalDate date,
            LocalDate dateFrom, LocalDate dateTo) {
        this.status = status;
        this.category = category;
        this.topic = topic;
        this.date = date;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
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
        if (date != null && !date.equals(activity.getDate())) {
            return false;
        }
        if (dateFrom != null && activity.getDate().isBefore(dateFrom)) {
            return false;
        }
        return dateTo == null || !activity.getDate().isAfter(dateTo);
    }
}
