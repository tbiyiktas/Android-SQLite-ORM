package lib.persistence.migration;

import android.database.sqlite.SQLiteDatabase;

public interface MigrationStep {
    int from();
    int to();
    void apply(SQLiteDatabase db) throws Exception;
}