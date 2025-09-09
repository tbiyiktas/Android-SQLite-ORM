package lib.persistence.command.query;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class SelectBuilderTest {

    @Test public void compile_basicWhereOrderLimitOffset() {
        Select<Todo> s = Select.from(Todo.class)
                .columns("*")
                .whereEq("id", 5)
                .and("`title` LIKE ?", "%a%")
                .orderBy("id", true)
                .limit(10)
                .offset(5);

        SelectQuery<Todo> q = s.compile();
        String sql = q.getSql();
        assertTrue(sql.contains("SELECT * FROM `todos`"));
        assertTrue(sql.contains("WHERE (`id` = ?) AND (`title` LIKE ?)"));
        assertTrue(sql.contains("ORDER BY `id` DESC"));
        assertTrue(sql.contains("LIMIT 10"));
        assertTrue(sql.contains("OFFSET 5"));
        assertArrayEquals(new String[]{"5", "%a%"}, q.getArgs());
    }

    @Test public void whereEq_nullUsesIsNull() {
        Select<Todo> s = Select.from(Todo.class).whereEq("title", null);
        String sql = s.compile().getSql();
        assertTrue(sql.contains("WHERE (`title` IS NULL)"));
    }
}
