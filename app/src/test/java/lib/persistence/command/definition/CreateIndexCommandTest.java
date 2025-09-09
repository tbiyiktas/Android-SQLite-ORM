package lib.persistence.command.definition;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import static org.junit.Assert.*;

public class CreateIndexCommandTest {
    @Test public void createIndex_formatsTableAndColumns() {
        String sql = CreateIndexCommand.build(Todo.class, "idx_todos_title", false, "title").getQuery();
        assertTrue(sql.startsWith("CREATE INDEX IF NOT EXISTS `idx_todos_title` ON `todos` ("));
        assertTrue(sql.contains("`title`"));
        assertTrue(sql.endsWith(");"));
    }
}
