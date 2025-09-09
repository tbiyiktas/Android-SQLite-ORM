package lib.persistence;

import org.junit.Test;
import static org.junit.Assert.*;

public class SqlNamesTest {

    @Test public void qId_quotesIdentifier() {
        assertEquals("`todos`", SqlNames.qId("todos"));
        assertEquals("`t`", SqlNames.qId("t"));
    }

    @Test public void qCol_supportsTableDotCol() {
        assertEquals("`t`.`id`", SqlNames.qCol("t.id"));
        assertEquals("`id`", SqlNames.qCol("id"));
    }

    @Test public void safeId_allowsLettersDigitsUnderscore() {
        assertEquals("idx_todos_title", SqlNames.safeId("idx_todos_title"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeId_rejectsWeirdInput() {
        SqlNames.safeId("DROP TABLE x;"); // invalid
    }
}
