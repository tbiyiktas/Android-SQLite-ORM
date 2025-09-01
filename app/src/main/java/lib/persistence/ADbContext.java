// lib/persistence/ADbContext.java
package lib.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ADbContext extends SQLiteOpenHelper implements IDbContext {

    private final ExecutorService readPool;
    private final ExecutorService writePool;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected ADbContext(@NonNull Context context, @NonNull String name, int version) {
        super(context, name, null, version);

        DbContextConfig cfg = DbContextConfig.get();
        final AtomicInteger n = new AtomicInteger(1);
        // Okuma havuzu (4 thread default)
        final int reads = Math.max(1, cfg.readThreads);
//        this.readPool = Executors.newFixedThreadPool(reads, new ThreadFactory() {
//            private final AtomicInteger n = new AtomicInteger(1);
//            @Override public Thread newThread(@NonNull Runnable r) {
//                Thread t = new Thread(r, cfg.readThreadNamePrefix + n.getAndIncrement());
//                t.setDaemon(true);
//                return t;
//            }
//        });

        this.readPool = new ThreadPoolExecutor(
                reads,                       // corePoolSize
                reads,                       // maximumPoolSize
                0L, TimeUnit.MILLISECONDS,             // keepAliveTime
                new LinkedBlockingQueue<>(256),        // bounded queue (geri basınç)
                r -> {                                 // ThreadFactory (lambda)
                    Thread t = new Thread(r, cfg.readThreadNamePrefix + n.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // backpressure
        );

        // Yazma havuzu (tek thread)
//        this.writePool = Executors.newSingleThreadExecutor(r -> {
//            Thread t = new Thread(r, cfg.writeThreadName);
//            t.setDaemon(true);
//            return t;
//        });

        this.writePool = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, cfg.writeThreadName);
            t.setDaemon(true);
            return t;
        });

        // WAL tercihini bildir (helper seviyesinde)
        setWriteAheadLoggingEnabled(cfg.enableWAL);
    }

    @Override
    public final void onConfigure(@NonNull SQLiteDatabase db) {
        super.onConfigure(db);
        DbContextConfig cfg = DbContextConfig.get();

        // foreign_keys
        // (Aşağıdaki PRAGMA'yı rawQuery ile de çalıştırıyoruz ki tüm cihazlarda tutarlı olsun)
        runPragmaQuery(db, "PRAGMA foreign_keys=" + (cfg.pragmaForeignKeys ? "ON" : "OFF"));

        // WAL
        if (cfg.enableWAL) {
            db.enableWriteAheadLogging();
            // PRAGMA journal_mode=WAL sonucu döndürür → rawQuery ile
            runPragmaQuery(db, "PRAGMA journal_mode=" + cfg.pragmaJournalMode); // "WAL"
        }

        // synchronous
        if (cfg.pragmaSynchronous != null) {
            runPragmaQuery(db, "PRAGMA synchronous=" + cfg.pragmaSynchronous); // "NORMAL", "FULL", ...
        }

        // busy_timeout
        if (cfg.pragmaBusyTimeoutMs > 0) {
            runPragmaQuery(db, "PRAGMA busy_timeout=" + cfg.pragmaBusyTimeoutMs);
        }

        onConfigureExtra(db); // burada PRAGMA/SELECT benzeri çağrı yapma; gerekiyorsa rawQuery kullan
    }

    /** Sonuç döndüren PRAGMA/SELECT komutlarını güvenli şekilde çalıştır. */
    protected static void runPragmaQuery(@NonNull SQLiteDatabase db, @NonNull String pragmaSql) {
        Cursor c = null;
        try {
            c = db.rawQuery(pragmaSql, null);
            // İstersen log/teyit:
            // if (c.moveToFirst()) { String v = c.getString(0); Log.d("DB", pragmaSql + " -> " + v); }
        } finally {
            if (c != null) c.close();
        }
    }

    // --- Şema yaşam döngüsü
    @Override
    public final void onCreate(@NonNull SQLiteDatabase db) {
        db.beginTransaction();
        try {
            onCreateSchema(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public final void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            onUpgradeSchema(db, oldVersion, newVersion);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    protected abstract void onCreateSchema(@NonNull SQLiteDatabase db);
    protected abstract void onUpgradeSchema(@NonNull SQLiteDatabase db, int oldVersion, int newVersion);

    // --- Çalıştırıcı
    @Override
    public final <T> void runDbOperation(@NonNull DbWork<T> work,
                                         @Nullable DbCallback<T> callback,
                                         boolean writeTransaction) {
        ExecutorService exec = writeTransaction ? writePool : readPool;
        exec.submit(() -> {
            DbResult<T> result;
            SQLiteDatabase db = null;
            boolean started = false;
            try {
                db = writeTransaction ? getWritableDatabase() : getReadableDatabase();
                if (writeTransaction) { db.beginTransaction(); started = true; }
                result = work.perform(db);
                if (writeTransaction && started) db.setTransactionSuccessful();
            } catch (Exception ex) {
                result = new DbResult.Error<>(ex);
            } finally {
                if (writeTransaction && db != null) {
                    try { db.endTransaction(); } catch (Throwable ignored) {}
                }
            }
            if (callback != null) {
                DbResult<T> out = result;
                //mainHandler.post(() -> callback.onResult(out));
                mainHandler.post(() -> {
                    try {
                        callback.onResult(out);
                    } catch (Throwable t) {
                        android.util.Log.e("ADbContext", "Callback error", t);
                    }
                });
            }
        });
    }

    @Override
    public synchronized void close() {
        super.close();
        readPool.shutdown();
        writePool.shutdown();
    }

    /** Alt sınıflar gerekiyorsa ekstra bağlantı ayarı ekleyebilir (PRAGMA gerekiyorsa rawQuery ile). */
    protected void onConfigureExtra(@NonNull SQLiteDatabase db) { /* no-op */ }
}
