// lib/persistence/DbContext.java
package com.example.adbkit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import lib.persistence.ADbContext;
import lib.persistence.command.definition.CreateTableCommand;
import lib.persistence.command.definition.CreateIndexCommand;
import lib.persistence.command.definition.DropTableCommand;
import lib.persistence.migration.Migrations;

import com.example.adbkit.entities.Todo;

public class DbContext extends ADbContext {
    private static final String dbName = "local.db";
    private static final int version = 3;
    public DbContext(Context context) {
        super(context, dbName, version);
    }

    // PRAGMA eklemek istersen:
    // @Override protected void onConfigureExtra(SQLiteDatabase db) {
    //     runPragmaQuery(db, "PRAGMA cache_size=2000");
    // }

    @Override
    protected void onCreateSchema(SQLiteDatabase db) {
        // Tablo oluştur
        db.execSQL(CreateTableCommand.build(Todo.class).getQuery());

        // Örnek indeks (title üzerinde)
        db.execSQL(CreateIndexCommand.build(Todo.class,
                "idx_todos_title",      // index adı
                false,                   // unique mi?
                new String[]{"title"}  // kolonlar
        ).getQuery());
    }

    @Override
    protected void onUpgradeSchema(SQLiteDatabase db, int oldVersion, int newVersion) {
       // Migrations.apply(db, oldVersion, newVersion);

        // Basit senaryoda drop + recreate
        db.execSQL(DropTableCommand.build(Todo.class).getQuery());
        onCreateSchema(db);
    }
}
