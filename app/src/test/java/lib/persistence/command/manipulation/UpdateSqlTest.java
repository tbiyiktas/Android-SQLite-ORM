package lib.persistence.command.manipulation;

import com.example.adbkit.entities.Todo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

import android.content.ContentValues;

@RunWith(RobolectricTestRunner.class)
public class UpdateSqlTest {
    @Test public void builder_setsValuesAndWhereWithQuotes() {
        UpdateSql u = UpdateSql.build(Todo.class)
                .set("title", "B")
                .set("completed", true)
                .where().Equals("id", 5);

        assertEquals("todos", u.getTableName()); // RAW
        assertEquals("`id` = ?", u.getWhereClause());
        assertArrayEquals(new String[]{"5"}, u.getWhereArgs());
        // ContentValues içerik kontrolü (spot)
        ContentValues cv =  u.getContentValues();
        assertEquals("B", cv.getAsString("title"));
        assertEquals(1L, cv.getAsLong("completed").longValue());
    }
}
