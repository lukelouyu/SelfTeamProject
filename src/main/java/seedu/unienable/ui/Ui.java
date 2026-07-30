package seedu.unienable.ui;

/** Handles text output for UniEnable. */
public class Ui {
    private static final int HORIZONTAL_LINE_LENGTH = 60;
    private static final String HORIZONTAL_LINE = createHorizontalLine();

    /** Displays UniEnable's welcome message. */
    public void showWelcome() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println("Hello! Welcome to UniEnable.");
        System.out.println("Your Uni Friend for planning accessible university routines.");
        System.out.println();
        System.out.println("Enter \"guide\" if you are unsure what to do next.");
        System.out.println(HORIZONTAL_LINE);
    }

    /** Creates the horizontal line used to frame UI messages. */
    private static String createHorizontalLine() {
        StringBuilder horizontalLine = new StringBuilder();
        for (int i = 0; i < HORIZONTAL_LINE_LENGTH; i++) {
            horizontalLine.append("_");
        }
        return horizontalLine.toString();
    }
}
