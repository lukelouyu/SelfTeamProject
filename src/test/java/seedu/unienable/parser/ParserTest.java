package seedu.unienable.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParserTest {
    @Test
    public void getCommandWord_lowerCasesAndTrims() {
        assertEquals("list", Parser.getCommandWord("  LIST  "));
    }

    @Test
    public void getCommandWord_ignoresArguments() {
        assertEquals("add", Parser.getCommandWord("add n/CG3207 lecture c/ACADEMIC"));
    }

    @Test
    public void getArguments_returnsTextAfterCommandWord() {
        assertEquals("n/CG3207 lecture c/ACADEMIC", Parser.getArguments("add n/CG3207 lecture c/ACADEMIC"));
    }

    @Test
    public void getArguments_noArguments_returnsEmptyString() {
        assertEquals("", Parser.getArguments("bye"));
    }
}
