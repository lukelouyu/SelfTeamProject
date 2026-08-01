package seedu.unienable.storage.recur;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import seedu.unienable.exception.StorageException;
import seedu.unienable.model.recur.AcademicCalendar;
import seedu.unienable.model.recur.AcademicWeek;
import seedu.unienable.model.recur.NoClassDate;

/** Loads the external, human-editable academic calendar used by the recurrence feature. */
public class AcademicCalendarStorage {
    private static final String SUPPORTED_FORMAT = "1";

    /**
     * Loads and strictly validates one academic-calendar text file.
     *
     * @param file external calendar file; it is never created or rewritten here
     * @return the validated immutable calendar snapshot
     * @throws StorageException if the file is missing, unreadable, or malformed
     */
    public AcademicCalendar load(Path file) throws StorageException {
        if (!Files.isRegularFile(file)) {
            throw new StorageException("academic calendar file not found: " + file);
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("could not read " + file, e);
        }

        List<AcademicWeek> weeks = new ArrayList<>();
        List<NoClassDate> noClassDates = new ArrayList<>();
        Set<String> sourceYears = new HashSet<>();
        boolean formatSeen = false;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            int lineNumber = index + 1;
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\\|", -1);
            switch (fields[0]) {
            case "FORMAT":
                requireFieldCount(fields, 2, lineNumber);
                if (formatSeen || !SUPPORTED_FORMAT.equals(fields[1])) {
                    throw malformed(lineNumber, "unsupported or duplicate FORMAT record");
                }
                formatSeen = true;
                break;
            case "SOURCE":
                requireFieldCount(fields, 4, lineNumber);
                requireNonBlank(fields, lineNumber);
                sourceYears.add(fields[1].toLowerCase(Locale.ROOT));
                parseDate(fields[3], lineNumber);
                break;
            case "WEEK":
                requireFieldCount(fields, 7, lineNumber);
                requireNonBlank(fields, lineNumber);
                weeks.add(parseWeek(fields, lineNumber));
                break;
            case "NO_CLASS":
                requireFieldCount(fields, 4, lineNumber);
                requireNonBlank(fields, lineNumber);
                noClassDates.add(new NoClassDate(fields[1], parseDate(fields[2], lineNumber), fields[3]));
                break;
            default:
                throw malformed(lineNumber, "unknown record type \"" + fields[0] + "\"");
            }
        }

        if (!formatSeen) {
            throw new StorageException("invalid academic calendar: FORMAT record is missing");
        }
        validateSources(weeks, noClassDates, sourceYears);
        if (weeks.stream().noneMatch(AcademicWeek::isInstructional)) {
            throw new StorageException("invalid academic calendar: no instructional WEEK records");
        }

        try {
            return new AcademicCalendar(weeks, noClassDates);
        } catch (IllegalArgumentException e) {
            throw new StorageException("invalid academic calendar: " + e.getMessage(), e);
        }
    }

    private AcademicWeek parseWeek(String[] fields, int lineNumber) throws StorageException {
        int weekNumber;
        try {
            weekNumber = Integer.parseInt(fields[3]);
        } catch (NumberFormatException e) {
            throw malformed(lineNumber, "week number must be a non-negative integer");
        }
        try {
            return new AcademicWeek(fields[1], fields[2], weekNumber, fields[4],
                    parseDate(fields[5], lineNumber), parseDate(fields[6], lineNumber));
        } catch (IllegalArgumentException e) {
            throw malformed(lineNumber, e.getMessage());
        }
    }

    private LocalDate parseDate(String value, int lineNumber) throws StorageException {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            throw malformed(lineNumber, "date must be a real calendar date in yyyy-MM-dd format");
        }
    }

    private void validateSources(List<AcademicWeek> weeks, List<NoClassDate> noClassDates,
            Set<String> sourceYears) throws StorageException {
        for (AcademicWeek week : weeks) {
            if (!sourceYears.contains(week.getAcademicYear().toLowerCase(Locale.ROOT))) {
                throw new StorageException("invalid academic calendar: no SOURCE record for "
                        + week.getAcademicYear());
            }
        }
        for (NoClassDate noClassDate : noClassDates) {
            if (!sourceYears.contains(noClassDate.getAcademicYear().toLowerCase(Locale.ROOT))) {
                throw new StorageException("invalid academic calendar: no SOURCE record for "
                        + noClassDate.getAcademicYear());
            }
        }
    }

    private void requireFieldCount(String[] fields, int expected, int lineNumber)
            throws StorageException {
        if (fields.length != expected) {
            throw malformed(lineNumber, "expected " + expected + " pipe-delimited fields");
        }
    }

    private void requireNonBlank(String[] fields, int lineNumber) throws StorageException {
        for (String field : fields) {
            if (field.isBlank()) {
                throw malformed(lineNumber, "fields must not be blank");
            }
        }
    }

    private StorageException malformed(int lineNumber, String detail) {
        return new StorageException("invalid academic-calendar.txt line " + lineNumber + ": " + detail);
    }
}
