// lib/persistence/core/ADbContext.java
package lib.persistence.core;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lib.persistence.DbContextConfig;
import lib.persistence.tx.AfterCommitEvents;

/**
 * Executor-tabanlı DB context (İDEAL API uyumlu):
 * - Bounded read havuzu (CallerRuns backpressure) + tek-yazıcı write kuyruğu (blocking put)
 * - PRAGMA/WAL ayarları DbContextConfig'ten
 * - runWrite: TEK transaction (BEGIN..COMMIT/ROLLBACK), Future commit SONRASI tamamlanır
 * - runRead: NON-transactional
 * - After-commit: yazma işlerinde commit'ten sonra outbox publish
 */
public abstract class ADbContext extends SQLiteOpenHelper implements IDbContext {

    // Kuyruk doluyken yazma işlerini bloklayarak backpressure uygulayan handler
    private static final RejectedExecutionHandler BLOCKING_PUT = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                if (executor.isShutdown()) {
                    throw new RejectedExecutionException("Write executor is shut down");
                }
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while enqueuing write task", e);
            }
        }
    };

    private final ExecutorService readPool;
    private final ExecutorService writePool;

    protected ADbContext(@NonNull Context context, @NonNull String name, int version) {
        super(context, name, null, version);

        final DbContextConfig cfg = DbContextConfig.get();

        // --- Read pool (bounded queue + backpressure: CallerRuns) ---
        final int reads = Math.max(1, cfg.readThreads);
        this.readPool = new ThreadPoolExecutor(
                reads,                               // core
                reads,                               // max
                0L, TimeUnit.MILLISECONDS,           // keepAlive
                new LinkedBlockingQueue<>(cfg.readQueueCapacity),
                r -> {
                    Thread t = new Thread(r, cfg.readThreadNamePrefix + cfg.nextReadIndex());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // --- Write pool (single thread + bounded queue + blocking put) ---
        this.writePool = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(cfg.writeQueueCapacity),
                r -> {
                    Thread t = new Thread(r, cfg.writeThreadName);
                    t.setDaemon(true);
                    return t;
                },
                BLOCKING_PUT
        );

        // Helper seviyesinde WAL tercihini bildir
        setWriteAheadLoggingEnabled(cfg.enableWAL);
    }

    // ---------------------------------------------------------------------
    // SQLiteOpenHelper lifecycle
    // ---------------------------------------------------------------------

    /** Sonuç döndüren PRAGMA/SELECT komutlarını güvenli şekilde çalıştır. */
    protected static void runPragmaQuery(@NonNull SQLiteDatabase db, @NonNull String pragmaSql) {
        Cursor c = null;
        try {
            c = db.rawQuery(pragmaSql, null);
        } finally {
            if (c != null) c.close();
        }
    }

    // CancellableFuture içinde isWorkCancelled() varsa destekle; yoksa sessizce false döner.
    private static boolean isWorkCancelled(CancellableFuture<?> f) {
        return (f instanceof lib.persistence.core.WorkCancellationAware)
                && ((lib.persistence.core.WorkCancellationAware) f).isWorkCancelled();
    }

    @Override
    public final void onConfigure(@NonNull SQLiteDatabase db) {
        super.onConfigure(db);
        final DbContextConfig cfg = DbContextConfig.get();

        // foreign_keys
        runPragmaQuery(db, "PRAGMA foreign_keys=" + (cfg.pragmaForeignKeys ? "ON" : "OFF"));

        // journal_mode & WAL
        if (cfg.enableWAL) {
            db.enableWriteAheadLogging();
        }
        if (cfg.pragmaJournalMode != null && !cfg.pragmaJournalMode.isEmpty()) {
            runPragmaQuery(db, "PRAGMA journal_mode=" + cfg.pragmaJournalMode); // genelde WAL
        }

        // synchronous
        if (cfg.pragmaSynchronous != null) {
            runPragmaQuery(db, "PRAGMA synchronous=" + cfg.pragmaSynchronous); // NORMAL/FULL/...
        }

        // busy_timeout
        if (cfg.pragmaBusyTimeoutMs > 0) {
            runPragmaQuery(db, "PRAGMA busy_timeout=" + cfg.pragmaBusyTimeoutMs);
        }

        onConfigureExtra(db); // Ek ayar gerekiyorsa override (PRAGMA'larda rawQuery kullanın)
    }

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

    /** İsteğe bağlı: ek bağlantı/PRAGMA ayarları (PRAGMA'larda rawQuery kullanın). */
    protected void onConfigureExtra(@NonNull SQLiteDatabase db) { /* no-op */ }

    // ---------------------------------------------------------------------
    // IDbContext: İDEAL API (yalnızca runRead / runWrite)
    // ---------------------------------------------------------------------

    @Override
    public final <T> CancellableFuture<DbResult<T>> runRead(@NonNull DbWork<T> work) {
        final CancellableFuture<DbResult<T>> future = new CancellableFuture<>();
        final Future<?> task = readPool.submit(() -> {
            if (future.isCancelled() || isWorkCancelled(future)) {
                future.completeExceptionally(new java.util.concurrent.CancellationException("Cancelled before start"));
                return;
            }

            SQLiteDatabase db = null;
            try {
                db = getReadableDatabase();
                DbResult<T> result = work.perform(db);
                future.complete(result != null
                        ? result
                        : new DbResult.Error<>(new IllegalStateException("Null DbResult")));
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        future.whenComplete((r, e) -> {
            if (future.isCancelled() || isWorkCancelled(future)) {
                task.cancel(true);
            }
        });

        return future;
    }

    @Override
    public final <T> CancellableFuture<DbResult<T>> runWrite(@NonNull DbWork<T> work) {
        final CancellableFuture<DbResult<T>> future = new CancellableFuture<>();
        final Future<?> task = writePool.submit(() -> {
            if (future.isCancelled() || isWorkCancelled(future)) {
                future.completeExceptionally(new java.util.concurrent.CancellationException("Cancelled before start"));
                return;
            }

            final DbContextConfig cfg = DbContextConfig.get();
            SQLiteDatabase db = null;
            boolean started = false;
            DbResult<T> result = null;
            Exception failure = null;

            try {
                db = getWritableDatabase();
                if (cfg.nonExclusiveTransactions) db.beginTransactionNonExclusive();
                else db.beginTransaction();
                started = true;

                result = work.perform(db);

                db.setTransactionSuccessful();
            } catch (Exception ex) {
                failure = ex;
            } finally {
                if (started) {
                    try { db.endTransaction(); } catch (Throwable ignore) {}
                }

                if (failure == null) {
                    try {
                        AfterCommitEvents.publishAndClear();
                    } catch (Throwable ignore) {}
                    future.complete(result != null
                            ? result
                            : new DbResult.Error<>(new IllegalStateException("Null DbResult")));
                } else {
                    try {
                        AfterCommitEvents.clear();
                    } catch (Throwable ignore) {}
                    future.completeExceptionally(failure);
                }
            }
        });

        future.whenComplete((r, e) -> {
            if (future.isCancelled() || isWorkCancelled(future)) {
                task.cancel(true);
            }
        });

        return future;
    }

    // ---------------------------------------------------------------------
    // Kapanış
    // ---------------------------------------------------------------------

    @Override
    public synchronized void close() {
        super.close();

        readPool.shutdownNow();
        writePool.shutdownNow();
        try {
            readPool.awaitTermination(2, TimeUnit.SECONDS);
            writePool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
