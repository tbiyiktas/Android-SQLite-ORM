package com.example.adbkit;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import com.example.adbkit.infrastructure.persistence.DbContext;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = MyApplication.class)
public class DbContextUpgradeTest {

    static class SpyDbContext extends DbContext {
        public SpyDbContext(Context ctx) { super(ctx, "unit-test.db", 5); }
        @Override protected void onCreateSchema(SQLiteDatabase db) { /* no-op */ }
        @Override protected void onUpgradeSchema(SQLiteDatabase db, int oldV, int newV) {
            // ADbContext.onUpgrade(...) bu parametreleri aynen geçiriyor olmalı
            assertEquals(2, oldV);
            assertEquals(3, newV);
        }
    }

    @Test public void onUpgradeSchema_receivesPassedVersions() {
        Context appCtx = ApplicationProvider.getApplicationContext();
        SpyDbContext ctx = new SpyDbContext(appCtx);
        SQLiteDatabase memDb = SQLiteDatabase.create(null);
        try {
            ctx.onUpgrade(memDb, 2, 3);
        } finally {
            memDb.close();
            ctx.close();
        }
    }
}
