package com.example.adbkit;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.adbkit.entities.Todo; // Todo entity'niz
import lib.persistence.migration.Migrations;
import lib.persistence.migration.MigrationStep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DbContextTest {
    private DbContext dbContext;
    private Context appContext;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Her testten önce veritabanını temizle
        appContext.deleteDatabase("test_local.db");
        // Test için farklı bir DB adı veya gerçek DB adı kullanılabilir.
        // Gerçek DB adını kullanıyorsanız, testleriniz uygulamanın DB'sini etkileyebilir.
        dbContext = new DbContext(appContext, "test_local.db", 1); // Test DB adı ve versiyonu
    }

    @After
    public void tearDown() {
        dbContext.close();
        appContext.deleteDatabase("test_local.db");
    }

    @Test
    public void onCreateSchema_shouldCreateTablesAndIndexes() {
        SQLiteDatabase db = dbContext.getWritableDatabase(); // Bu onCreateSchema'yı tetikler

        // Todo tablosu var mı kontrol et
        Cursor tableCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='todos'", null);
        assertTrue("Todo table should exist", tableCursor.moveToFirst());
        tableCursor.close();

        // idx_todos_title indeksi var mı kontrol et
        Cursor indexCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='index' AND name='idx_todos_title'", null);
        assertTrue("idx_todos_title index should exist", indexCursor.moveToFirst());
        indexCursor.close();

        // Tablo kolonlarını kontrol et (daha detaylı yapılabilir)
        Cursor columnCursor = db.rawQuery("PRAGMA table_info(todos)", null);
        assertTrue(columnCursor.moveToFirst());
        boolean idFound = false, titleFound = false;
        int nameColIdx = columnCursor.getColumnIndex("name");
        do {
            String columnName = columnCursor.getString(nameColIdx);
            if ("id".equals(columnName)) idFound = true;
            if ("title".equals(columnName)) titleFound = true;
        } while (columnCursor.moveToNext());
        columnCursor.close();
        assertTrue("Column 'id' should exist in todos table", idFound);
        assertTrue("Column 'title' should exist in todos table", titleFound);
    }

    // onUpgradeSchema ve Migrations.apply testleri daha karmaşıktır.
    // Farklı versiyonlardan yükseltme senaryolarını test etmeniz gerekir.
    // Örnek bir migration adımı ekleyip test edelim:
    @Test
    public void onUpgradeSchema_withMigration_shouldApplyMigration() {
        // Migration'ları test etmek için Migrations.STEPS'i geçici olarak manipüle edebiliriz
        // veya testlere özel bir migration listesi kullanabiliriz.
        // Bu örnek için, Migrations sınıfını doğrudan kullanacağız ve bir adım ekleyeceğiz.
        // Normalde Migrations.STEPS statik ve final olabilir, bu yüzden bu yaklaşım
        // gerçek dünyada daha karmaşık olabilir (örn. reflection veya test-double).

        // Test için geçici bir migration adımı (normalde Migrations.java içinde olurdu)
        MigrationStep testStepV1toV2 = new MigrationStep() {
            @Override public int from() { return 1; }
            @Override public int to() { return 2; }
            @Override public void apply(SQLiteDatabase db) throws Exception {
                db.execSQL("ALTER TABLE `todos` ADD COLUMN `description_v2` TEXT;");
            }
        };

        // ÖNEMLİ: Bu testin çalışması için Migrations.STEPS listesini
        // programatik olarak değiştirebilmeniz veya testlere özel bir
        // Migrations sistemi enjekte edebilmeniz gerekir.
        // Bu örnekte, Migrations.STEPS'in dinamik olduğunu varsayalım (pratikte zor).
        // Alternatif olarak, DbContext'i farklı versiyonlarla açıp onUpgrade'i tetikleyebilirsiniz.

        // Adım 1: DB'yi v1 ile oluştur
        appContext.deleteDatabase("test_migration.db");
        DbContext dbContextV1 = new DbContext(appContext, "test_migration.db", 1);
        SQLiteDatabase dbv1 = dbContextV1.getWritableDatabase(); // onCreate çağrılır
        dbContextV1.close();

        // Adım 2: Migrations.STEPS'e testimizi ekleyelim (Bu kısım kütüphanenizin yapısına göre değişir)
        // Eğer Migrations.STEPS private static final ise bu yapılamaz.
        // Bu durumda, AppDbContext'in onUpgradeSchema'sının Migrations.apply'ı çağırdığını
        // ve Migrations.apply'ın doğru adımları uyguladığını ayrı birim testleriyle test etmelisiniz.
        // Burada, onUpgrade'in tetiklenmesini ve yeni kolonun oluşmasını test edeceğiz.
        // DbContext'in onUpgradeSchema'sının Migrations.apply'ı çağırdığını varsayıyoruz.

        // Adım 3: DB'yi v2 ile aç (bu onUpgrade'i tetikler)
        DbContext dbContextV2 = new DbContext(appContext, "test_migration.db", 2) {
            // Test için onUpgradeSchema'yı override ederek testStep'i uygulatabiliriz
            // ya da Migrations.STEPS'in bu adımı içerdiğinden emin oluruz.
            // Bu örnekte, DbContext'in kendi onUpgradeSchema'sının
            // Migrations.apply'ı çağırdığını varsayalım ve Migrations.STEPS'in
            // testStepV1toV2'yi içerdiğini varsayalım.
        };

        // Eğer Migrations.STEPS'i test sırasında değiştiremiyorsanız,
        // DbContext'in onUpgradeSchema'sını şu şekilde mock bir migration ile test edebilirsiniz:
        // (Bu, Migrations sınıfının kendisini test etmez, sadece onUpgrade'in çağrıldığını test eder)
        // DbContext dbContextV2 = new DbContext(appContext, "test_migration.db", 2) {
        //     @Override
        //     protected void onUpgradeSchema(SQLiteDatabase db, int oldVersion, int newVersion) {
        //         if (oldVersion == 1 && newVersion == 2) {
        //             try {
        //                 testStepV1toV2.apply(db);
        //             } catch (Exception e) {
        //                 fail("Migration step failed: " + e.getMessage());
        //             }
        //         } else {
        //            super.onUpgradeSchema(db, oldVersion, newVersion);
        //         }
        //     }
        // };


        SQLiteDatabase dbv2 = dbContextV2.getWritableDatabase(); // onUpgrade çağrılır
        Cursor columnCursor = dbv2.rawQuery("PRAGMA table_info(todos)", null);
        assertTrue(columnCursor.moveToFirst());
        boolean newColumnFound = false;
        int nameColIdx = columnCursor.getColumnIndex("name");
        do {
            if ("description_v2".equals(columnCursor.getString(nameColIdx))) {
                newColumnFound = true;
                break;
            }
        } while (columnCursor.moveToNext());
        columnCursor.close();
        dbContextV2.close();

        assertTrue("New column 'description_v2' should be added after migration", newColumnFound);
        appContext.deleteDatabase("test_migration.db");
    }
}
