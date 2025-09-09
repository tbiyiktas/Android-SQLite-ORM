package lib.persistence.command.manipulation;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeleteCommandTest {

    @Test public void build_fromEntity_readsPkAndQuotes() {
        Todo t = new Todo(); t.id = 42;
        DeleteCommand cmd = DeleteCommand.build(t);
        assertEquals("todos", cmd.getTableName()); // quoted
        assertEquals("`id` = ?", cmd.getWhereClause());
        assertArrayEquals(new String[]{"42"}, cmd.getWhereArgs());
    }

    @Test public void build_fromClassAndPk_usesArgs() {
        DeleteCommand cmd = DeleteCommand.build(Todo.class, 9);
        assertEquals("todos", cmd.getTableName());
        assertEquals("`id` = ?", cmd.getWhereClause());
        assertArrayEquals(new String[]{"9"}, cmd.getWhereArgs());
    }

    @Test public void buildAll_producesNoWhere() {
        DeleteCommand all = DeleteCommand.buildAll(Todo.class);
        assertEquals("todos", all.getTableName());
        assertNull(all.getWhereClause());
        assertNull(all.getWhereArgs());
    }
}
