package lib.persistence.command.manipulation;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeleteSqlTest {
    @Test public void whereBuildsWithOps() {
        DeleteSql d = DeleteSql.build(Todo.class)
                .where().Equals("userId", 1)
                .and().Equals("completed", 0);

        assertEquals("todos", d.getTableName());
        assertEquals("`userId` = ? AND `completed` = ?", d.getWhereClause());
        assertArrayEquals(new String[]{"1","0"}, d.getWhereArgs());
    }
}
