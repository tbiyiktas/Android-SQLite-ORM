package com.example.adbkit;

import android.app.Application;

import com.example.adbkit.events.TodoCreatedEvent;
import com.example.adbkit.infrastructure.persistence.DbContext;

import lib.persistence.DbContextConfig;
import lib.persistence.core.IDbContext;
import lib.persistence.tx.AfterCommitEvents;

public class MyApplication extends Application {
    private static MyApplication instance = null;
    private volatile IDbContext dbContextInstance; // lazy

    @Override
    public void onCreate() {
        super.onCreate();

        DbContextConfig.apply(
                DbContextConfig.builder()
                        .readThreads(4)
                        .readQueueCapacity(512)
                        .nonExclusiveTransactions(true)
                        .readThreadNamePrefix("DB-Read-")
                        .writeThreadName("DB-Write")
                        .enableWAL(true)
                        .pragmaForeignKeys(true)
                        .pragmaJournalMode("WAL")
                        .pragmaSynchronous("NORMAL")
                        .pragmaBusyTimeoutMs(10_000)
                        .build()
        );

        // 2) After-commit event sink (örnek)
        AfterCommitEvents.register(TodoCreatedEvent.class, ev ->
                android.util.Log.i("AfterCommit", "TodoCreated #" + ev.todo.id)
        );

        dbContextInstance = null; // lazy init

        instance = this;
    }

    /**
     * Lazy + thread-safe
     */
    public synchronized  static IDbContext getDbContext() {
        if (instance == null) {
            throw new IllegalStateException("Uygulama henüz başlatılmadı.");
        }

        if(instance.dbContextInstance== null){
            instance.dbContextInstance = new DbContext(instance);
        }

        return instance.dbContextInstance;
    }
}
