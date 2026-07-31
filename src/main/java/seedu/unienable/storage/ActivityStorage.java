package seedu.unienable.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import seedu.unienable.exception.InvalidActivityException;
import seedu.unienable.exception.InvalidDateTimeException;
import seedu.unienable.exception.StorageException;
import seedu.unienable.model.classes.Activity;
import seedu.unienable.model.classes.EnergyRating;
import seedu.unienable.model.classes.FixedActivity;
import seedu.unienable.model.classes.FlexibleActivity;
import seedu.unienable.model.classes.SensoryRating;
import seedu.unienable.model.enums.ActivityCategory;
import seedu.unienable.model.enums.CompletionStatus;
import seedu.unienable.parser.common.DateTimeParser;
import seedu.unienable.parser.common.RatingParser;

/**
 * Loads and saves Activity records from/to a pipe-delimited activities.txt-format file.
 *
 * <p>Format:
 * {@code FIXED|id|description|category|date|startTime|endTime|energy|sensory|completion|topic|notes}
 * {@code FLEXIBLE|id|description|category|date|earliestStart|latestEnd|durationMinutes|energy|
 * sensory|completion|topic|notes}
 * topic and notes are optional trailing fields. Fields must not contain the '|' delimiter; this is
 * not escaped in v1.0, so save() rejects any field that contains one.
 *
 * <p>Loading applies the same semantic checks add/edit already enforce through the command
 * parser (positive/unique ID, non-blank description, end-after-start, a flexible window/duration
 * that fits, no exact duplicate, no fixed-activity overlap) so a manually edited or otherwise
 * corrupted file cannot silently load an activity the application would never let a user create.
 * A line failing any of these checks is skipped with a warning, exactly like a malformed line.
 */
public class ActivityStorage {
    private static final String FIXED_TAG = "FIXED";
    private static final String FLEXIBLE_TAG = "FLEXIBLE";
    private static final String DELIMITER = "|";

    /**
     * Loads activities from the given file.
     *
     * @param filePath path to the activities text file
     * @return the loaded activities plus warnings for any skipped malformed or invalid lines
     * @throws StorageException if the file cannot be read
     */
    public LoadResult<Activity> load(Path filePath) throws StorageException {
        List<String> lines = readLines(filePath);
        List<Activity> activities = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                Activity activity = parseLine(line);
                validateAgainstAlreadyLoaded(activity, activities);
                activities.add(activity);
            } catch (IllegalArgumentException | InvalidActivityException | InvalidDateTimeException e) {
                warnings.add("Line " + (i + 1) + " was skipped: " + e.getMessage());
            }
        }
        return new LoadResult<>(activities, warnings);
    }

    /**
     * Rejects a candidate activity that conflicts with one already accepted earlier in this same
     * load() call: a reused ID, an exact duplicate (same description/date/timing), or (for a
     * fixed activity) a timing overlap with another fixed activity on the same date.
     *
     * @param candidate the just-parsed activity, not yet added to alreadyLoaded
     * @param alreadyLoaded every activity accepted so far in this load() call
     * @throws InvalidActivityException if candidate conflicts with an already-loaded activity
     */
    private void validateAgainstAlreadyLoaded(Activity candidate, List<Activity> alreadyLoaded)
            throws InvalidActivityException {
        for (Activity existing : alreadyLoaded) {
            if (existing.getId() == candidate.getId()) {
                throw new InvalidActivityException("duplicate activity id " + candidate.getId());
            }
        }
        for (Activity existing : alreadyLoaded) {
            if (isExactDuplicate(existing, candidate)) {
                throw new InvalidActivityException("duplicates activity [" + existing.getId() + "]");
            }
        }
        if (candidate instanceof FixedActivity) {
            FixedActivity fixedCandidate = (FixedActivity) candidate;
            for (Activity existing : alreadyLoaded) {
                if (existing instanceof FixedActivity && overlaps(fixedCandidate, (FixedActivity) existing)) {
                    throw new InvalidActivityException("overlaps activity [" + existing.getId() + "]");
                }
            }
        }
    }

    private boolean isExactDuplicate(Activity existing, Activity candidate) {
        if (!existing.getDescription().equals(candidate.getDescription())
                || !existing.getDate().equals(candidate.getDate())) {
            return false;
        }
        if (existing instanceof FixedActivity && candidate instanceof FixedActivity) {
            FixedActivity a = (FixedActivity) existing;
            FixedActivity b = (FixedActivity) candidate;
            return a.getStartTime().equals(b.getStartTime()) && a.getEndTime().equals(b.getEndTime());
        }
        if (existing instanceof FlexibleActivity && candidate instanceof FlexibleActivity) {
            FlexibleActivity a = (FlexibleActivity) existing;
            FlexibleActivity b = (FlexibleActivity) candidate;
            return a.getEarliestStart().equals(b.getEarliestStart()) && a.getLatestEnd().equals(b.getLatestEnd())
                    && a.getDurationMinutes() == b.getDurationMinutes();
        }
        return false;
    }

    private boolean overlaps(FixedActivity candidate, FixedActivity existing) {
        if (!candidate.getDate().equals(existing.getDate())) {
            return false;
        }
        return existing.getStartTime().isBefore(candidate.getEndTime())
                && candidate.getStartTime().isBefore(existing.getEndTime());
    }

    /**
     * Saves the given activities to the given file, overwriting any existing content.
     *
     * @param filePath path to the activities text file
     * @param activities the activities to save
     * @throws StorageException if a field contains the '|' delimiter, or the file cannot be written
     */
    public void save(Path filePath, List<Activity> activities) throws StorageException {
        List<String> lines = new ArrayList<>();
        for (Activity activity : activities) {
            lines.add(toLine(activity));
        }
        try {
            Files.write(filePath, lines);
        } catch (IOException e) {
            throw new StorageException("could not write " + filePath, e);
        }
    }

    private Activity parseLine(String line) throws InvalidActivityException, InvalidDateTimeException {
        String[] fields = line.split("\\|", -1);
        switch (fields[0]) {
        case FIXED_TAG:
            return parseFixed(fields);
        case FLEXIBLE_TAG:
            return parseFlexible(fields);
        default:
            throw new IllegalArgumentException("unknown record type \"" + fields[0] + "\"");
        }
    }

    private Activity parseFixed(String[] fields) throws InvalidActivityException, InvalidDateTimeException {
        if (fields.length < 10) {
            throw new IllegalArgumentException("FIXED line is missing required fields");
        }
        int id = parsePositiveWholeNumber(fields[1], "id");
        String description = requireNonBlank(fields[2], "description");
        ActivityCategory category = parseCategory(fields[3]);
        LocalDate date = DateTimeParser.parseDate(fields[4]);
        LocalTime startTime = DateTimeParser.parseTime(fields[5]);
        LocalTime endTime = DateTimeParser.parseTime(fields[6]);
        if (!endTime.isAfter(startTime)) {
            throw new InvalidActivityException("end time must be later than start time");
        }
        EnergyRating energy = RatingParser.parseEnergyRating(fields[7]);
        SensoryRating sensory = RatingParser.parseSensoryRating(fields[8]);
        CompletionStatus status = parseCompletionStatus(fields[9]);
        String topic = optionalField(fields, 10);
        String notes = optionalField(fields, 11);

        FixedActivity activity = new FixedActivity(id, description, category, date, startTime, endTime,
                energy, sensory, topic, notes);
        applyStatus(activity, status);
        return activity;
    }

    private Activity parseFlexible(String[] fields) throws InvalidActivityException, InvalidDateTimeException {
        if (fields.length < 11) {
            throw new IllegalArgumentException("FLEXIBLE line is missing required fields");
        }
        int id = parsePositiveWholeNumber(fields[1], "id");
        String description = requireNonBlank(fields[2], "description");
        ActivityCategory category = parseCategory(fields[3]);
        LocalDate date = DateTimeParser.parseDate(fields[4]);
        LocalTime earliestStart = DateTimeParser.parseTime(fields[5]);
        LocalTime latestEnd = DateTimeParser.parseTime(fields[6]);
        if (!latestEnd.isAfter(earliestStart)) {
            throw new InvalidActivityException("latest end time must be after earliest start time");
        }
        int durationMinutes = parsePositiveWholeNumber(fields[7], "duration");
        long windowMinutes = Duration.between(earliestStart, latestEnd).toMinutes();
        if (durationMinutes > windowMinutes) {
            throw new InvalidActivityException("dur must fit inside the earliest/latest window ("
                    + windowMinutes + " min available)");
        }
        EnergyRating energy = RatingParser.parseEnergyRating(fields[8]);
        SensoryRating sensory = RatingParser.parseSensoryRating(fields[9]);
        CompletionStatus status = parseCompletionStatus(fields[10]);
        String topic = optionalField(fields, 11);
        String notes = optionalField(fields, 12);

        FlexibleActivity activity = new FlexibleActivity(id, description, category, date, earliestStart,
                latestEnd, durationMinutes, energy, sensory, topic, notes);
        applyStatus(activity, status);
        return activity;
    }

    private void applyStatus(Activity activity, CompletionStatus status) {
        if (status == CompletionStatus.COMPLETE) {
            activity.mark();
        }
    }

    private int parseWholeNumber(String field, String fieldName) {
        try {
            return Integer.parseInt(field);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number");
        }
    }

    /**
     * Parses a whole number that must be strictly positive (used for both id and duration, which
     * a hand-edited file could otherwise set to zero or negative).
     *
     * @param field the raw field text
     * @param fieldName the field name to use in the error message
     * @return the parsed value
     * @throws IllegalArgumentException if field is not a whole number, or is not positive
     */
    private int parsePositiveWholeNumber(String field, String fieldName) {
        int value = parseWholeNumber(field, fieldName);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive whole number");
        }
        return value;
    }

    /**
     * Rejects a blank field value (empty, or whitespace only).
     *
     * @param field the raw field text
     * @param fieldName the field name to use in the error message
     * @return field, unchanged
     * @throws IllegalArgumentException if field is blank
     */
    private String requireNonBlank(String field, String fieldName) {
        if (field.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return field;
    }

    private ActivityCategory parseCategory(String field) {
        try {
            return ActivityCategory.valueOf(field);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid category \"" + field + "\"");
        }
    }

    private CompletionStatus parseCompletionStatus(String field) {
        try {
            return CompletionStatus.valueOf(field);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid completion status \"" + field + "\"");
        }
    }

    private String optionalField(String[] fields, int index) {
        return fields.length > index && !fields[index].isEmpty() ? fields[index] : null;
    }

    private String toLine(Activity activity) throws StorageException {
        if (activity instanceof FixedActivity) {
            FixedActivity fixed = (FixedActivity) activity;
            return join(FIXED_TAG, String.valueOf(fixed.getId()), fixed.getDescription(),
                    fixed.getCategory().name(), fixed.getDate().toString(), fixed.getStartTime().toString(),
                    fixed.getEndTime().toString(), String.valueOf(fixed.getEnergyRating().getValue()),
                    String.valueOf(fixed.getSensoryRating().getValue()), fixed.getStatus().name(),
                    emptyIfNull(fixed.getTopic()), emptyIfNull(fixed.getNote()));
        }
        if (activity instanceof FlexibleActivity) {
            FlexibleActivity flexible = (FlexibleActivity) activity;
            return join(FLEXIBLE_TAG, String.valueOf(flexible.getId()), flexible.getDescription(),
                    flexible.getCategory().name(), flexible.getDate().toString(),
                    flexible.getEarliestStart().toString(), flexible.getLatestEnd().toString(),
                    String.valueOf(flexible.getDurationMinutes()),
                    String.valueOf(flexible.getEnergyRating().getValue()),
                    String.valueOf(flexible.getSensoryRating().getValue()), flexible.getStatus().name(),
                    emptyIfNull(flexible.getTopic()), emptyIfNull(flexible.getNote()));
        }
        throw new StorageException("unknown activity type: " + activity.getClass());
    }

    private String join(String... fields) throws StorageException {
        for (String field : fields) {
            if (field.contains(DELIMITER)) {
                throw new StorageException("field \"" + field + "\" must not contain '" + DELIMITER + "'");
            }
        }
        return String.join(DELIMITER, fields);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private List<String> readLines(Path filePath) throws StorageException {
        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new StorageException("could not read " + filePath, e);
        }
    }
}
