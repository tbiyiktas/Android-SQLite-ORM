package lib.persistence.migration;

import android.database.sqlite.SQLiteDatabase;
import java.util.Arrays;
import java.util.List;

public final class Migrations {
    private Migrations() {}

    // Buraya gerçek adımlarını ekle:
    private static final List<MigrationStep> STEPS = Arrays.asList(
            // ÖRNEK: v1 -> v2 : todos tablosuna notes kolonu ekle
            new MigrationStep() {
                public int from() { return 1; }
                public int to()   { return 2; }
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE `todos` ADD COLUMN `notes` TEXT");
                    // Gerekirse indeks/geri doldurma vb. ekle
                }
            }

            // yeni adımlar...
    );

    public static void apply(SQLiteDatabase db, int oldVersion, int newVersion) {
        int v = oldVersion;
        while (v < newVersion) {
            boolean progressed = false;
            for (MigrationStep s : STEPS) {
                if (s.from() == v && s.to() == v + 1) {
                    try {
                        s.apply(db);
                        v++;
                        progressed = true;
                    } catch (Exception e) {
                        throw new IllegalStateException("Migration failed: " + s.from() + "→" + s.to(), e);
                    }
                }
            }
            if (!progressed) {
                throw new IllegalStateException("Eksik migration adımı: " + v + "→" + (v + 1));
            }
        }
    }
}