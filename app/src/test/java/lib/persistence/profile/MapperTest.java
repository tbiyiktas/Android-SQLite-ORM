package lib.persistence.profile;

import static org.junit.Assert.*;

import android.content.ContentValues;
import android.database.MatrixCursor;

import com.example.adbkit.entities.Event;
import com.example.adbkit.entities.Todo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import lib.persistence.mapping.DbColumn;
import lib.persistence.mapping.Mapper;

@RunWith(RobolectricTestRunner.class)
public class MapperTest {

    @Test public void objectToContentValues_writesBooleanAsInteger() {
        Todo t = new Todo();
        t.title = "A";
        t.userId = 1;
        t.completed = true;
        ContentValues cv = Mapper.objectToContentValues(t);
        assertEquals(1L, cv.getAsLong("completed").longValue());
        assertEquals("A", cv.getAsString("title"));
        assertEquals(1L, cv.getAsLong("userId").longValue());
    }

    @Test public void classToDbColumns_setsSqliteTypeFromConverters() {
        // Event.created_at -> DateMillisConverter: INTEGER
        DbColumn createdAt = Mapper.getColumnByName(Event.class, "created_at");
        assertEquals("INTEGER", createdAt.getSqliteType());
        // Event.event_type -> EventTypeConverter: TEXT
        DbColumn typeCol = Mapper.getColumnByName(Event.class, "event_type");
        assertEquals("TEXT", typeCol.getSqliteType());
    }

    @Test public void cursorToObject_mapsEventViaConverters() {
        MatrixCursor c = new MatrixCursor(new String[]{"id","event_type","event_message","created_at"});
        c.addRow(new Object[]{1L, "CREATED", "hi", 1000L});
        c.moveToFirst();
        Event e = Mapper.cursorToObject(c, Event.class);
        assertTrue(e.toString().contains("message='hi'"));
    }

    @Test(expected = IllegalStateException.class)
    public void getTableName_requiresAnnotationName() {
        // Person sınıfında @DbTableAnnotation name boş → IllegalStateException beklenir
        Mapper.getTableName(com.example.adbkit.entities.Person.class);
    }
}
