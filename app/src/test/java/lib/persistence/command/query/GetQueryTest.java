package lib.persistence.command.query;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import static org.junit.Assert.*;

public class GetQueryTest {
    @Test public void getQuery_selectsByFirstPk() {
        GetQuery q = GetQuery.build(Todo.class, 1);
        String sql = q.getQuery();
        assertTrue(sql.contains("SELECT * FROM `todos` WHERE `id` = ? LIMIT 1"));
        assertArrayEquals(new String[]{"1"}, q.getArgs());
        assertEquals(Todo.class, q.getType());
    }
}
