package lib.persistence.command.definition;

import com.example.adbkit.entities.Event;
import com.example.adbkit.entities.Todo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CreateTableCommandTest {

    @Test public void ddl_forTodo_hasIdentityPkAndNotNulls() {
        String sql = CreateTableCommand.build(Todo.class).getQuery();
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `todos`"));
        assertTrue(sql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT"));
        assertTrue(sql.contains("`title` TEXT NOT NULL"));
        assertTrue(sql.contains("`completed` INTEGER"));
    }

    @Test public void ddl_forEvent_usesConverterTypes() {
        String sql = CreateTableCommand.build(Event.class).getQuery();
        assertTrue(sql.contains("`created_at` INTEGER")); // DateMillisConverter
        assertTrue(sql.contains("`event_type` TEXT"));    // EventTypeConverter
    }
}
