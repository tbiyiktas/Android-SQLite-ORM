package lib.persistence.migration;

import static org.junit.Assert.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MigrationsTest {
    @Test public void apply_createsEventsTable_on_4_to_5() {
        SQLiteDatabase db = SQLiteDatabase.create(null); // mem
        Migrations.apply(db, 4, 5);
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='events'", null);
        boolean exists = c.moveToFirst();
        c.close();
        db.close();
        assertTrue(exists);
    }
}
