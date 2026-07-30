package seedu.unienable.ui;

/** Handles text output for UniEnable. */
public class Ui {
    private static final int HORIZONTAL_LINE_LENGTH = 60;
    private static final String HORIZONTAL_LINE = createHorizontalLine();

    /** Displays UniEnable's welcome message. */
    public void showWelcome() {
        showFramed("Hello! Welcome to UniEnable.\n"
                + "Your Uni Friend for planning accessible university routines.\n\n"
                + "Enter \"guide\" if you are unsure what to do next.");
    }

    /**
     * Displays the given text framed by horizontal lines, matching every representative output
     * block in the User Guide. Used for command feedback, errors, and confirmation prompts alike.
     *
     * @param text the text to display
     */
    public void showFramed(String text) {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(text);
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
