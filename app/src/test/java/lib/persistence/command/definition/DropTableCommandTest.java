package lib.persistence.command.definition;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import static org.junit.Assert.*;

public class DropTableCommandTest {
    @Test public void dropTable_quotesName() {
        String sql = DropTableCommand.build(Todo.class).getQuery();
        assertEquals("DROP TABLE IF EXISTS `todos`;", sql);
    }
}
