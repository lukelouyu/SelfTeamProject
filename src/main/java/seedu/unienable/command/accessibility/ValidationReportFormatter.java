package seedu.unienable.command.accessibility;

import java.util.List;

/**
 * Formats the result of a facility/connection "validate" command into one human-readable report.
 * A small, narrowly-scoped helper shared only between {@link FacilityValidateCommand} and
 * {@link ConnectionValidateCommand}, since both report the exact same shape of result: a file
 * name, and either "no issues found" or a numbered list of the warnings a *Storage load already
 * produced.
 */
final class ValidationReportFormatter {
    private ValidationReportFormatter() {
    }

    /**
     * Formats a validation report.
     *
     * @param fileName the data file that was checked, e.g. "facilities.txt"
     * @param warnings every issue found, in file line order; empty if none
     * @return the formatted report
     */
    static String format(String fileName, List<String> warnings) {
        if (warnings.isEmpty()) {
            return fileName + ": no issues found.";
        }
        StringBuilder report = new StringBuilder(fileName + ": " + warnings.size()
                + (warnings.size() == 1 ? " issue found:" : " issues found:"));
        for (String warning : warnings) {
            report.append("\n").append(warning);
        }
        return report.toString();
    }
}
