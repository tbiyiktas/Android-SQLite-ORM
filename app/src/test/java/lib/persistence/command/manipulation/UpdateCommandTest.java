package lib.persistence.command.manipulation;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class UpdateCommandTest {

    @Test public void build_removesPkFromSet_andBuildsWhere() {
        Todo t = new Todo(); t.id = 7; t.title = "X"; t.userId = 1;
        UpdateCommand cmd = UpdateCommand.build(t);

        // table RAW (db.update için)
        assertEquals("todos", cmd.getTableName());
        // SET kısmında PK olmamalı
        assertFalse(cmd.getValues().containsKey("id"));
        // WHERE quoted
        assertEquals("`id` = ?", cmd.getWhereClause());
        assertArrayEquals(new String[]{"7"}, cmd.getWhereArgs());
    }
}
