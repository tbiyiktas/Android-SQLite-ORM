package lib.persistence.command.query;

import static org.junit.Assert.*;

import android.database.MatrixCursor;

import com.example.adbkit.entities.Todo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
public class SelectQueryMapperTest {

    @Test public void rowMapperOrDefault_usesProvidedMapper() {
        Function<android.database.Cursor, Todo> rm = c -> {
            Todo t = new Todo();
            t.id = c.getInt(c.getColumnIndex("id"));
            t.title = c.getString(c.getColumnIndex("title"));
            return t;
        };
        SelectQuery<Todo> q = new SelectQuery<>("SELECT 1", new String[0], rm);
        MatrixCursor c = new MatrixCursor(new String[]{"id","title"});
        c.addRow(new Object[]{1,"A"}); c.moveToFirst();
        Todo t = q.getRowMapperOrDefault().apply(c);
        assertEquals(1, t.id);
        assertEquals("A", t.title);
    }

    @Test public void rowMapperOrDefault_defaultsToMapperWhenTypeProvided() {
        SelectQuery<Todo> q = new SelectQuery<>("SELECT * FROM `todos`", new String[0], Todo.class, null);
        MatrixCursor c = new MatrixCursor(new String[]{"id","userId","title","completed"});
        c.addRow(new Object[]{7,1,"X",1}); c.moveToFirst();
        Todo t = q.getRowMapperOrDefault().apply(c); // Mapper.cursorToObject kullanır
        assertEquals(7, t.id);
        assertEquals("X", t.title);
        assertTrue(t.completed);
    }
}
