// lib/persistence/core/IDbContext.java
package lib.persistence.core;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import lib.concurrent.CancellableFuture;

public interface IDbContext extends AutoCloseable {
    void onConfigure(@NonNull SQLiteDatabase db);

    // --- Şema yaşam döngüsü
    void onCreate(@NonNull SQLiteDatabase db);
    void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion);

    // --- Çalıştırıcı (temel)
    <T> CancellableFuture<DbResult<T>> runDbOperation(@NonNull DbWork<T> work);
    <T> CancellableFuture<DbResult<T>> runDbOperation(@NonNull DbWork<T> work, boolean writeTransaction);

    // --- Kısayollar (önerilen kullanım)
    default <T> CancellableFuture<DbResult<T>> runTx(@NonNull DbWork<T> work) {
        return runDbOperation(work, true);
    }

    default <T> CancellableFuture<DbResult<T>> runRead(@NonNull DbWork<T> work) {
        return runDbOperation(work, false);
    }

    // Function köprüsü (okunabilir API isteyenler için)
    default <T> CancellableFuture<DbResult<T>> runDbOperation(
            @NonNull java.util.function.Function<SQLiteDatabase, DbResult<T>> work
    ) {
        return runDbOperation((DbWork<T>) db -> safeApply(work, db), /*writeTransaction=*/ false);
    }

    default <T> CancellableFuture<DbResult<T>> runDbOperation(
            @NonNull java.util.function.Function<SQLiteDatabase, DbResult<T>> work,
            boolean writeTransaction
    ) {
        return runDbOperation((DbWork<T>) db -> safeApply(work, db), writeTransaction);
    }

    // Function -> DbWork köprüsü (null/exception güvenli)
    static <T> DbResult<T> safeApply(
            @NonNull java.util.function.Function<SQLiteDatabase, DbResult<T>> f,
            @NonNull SQLiteDatabase db
    ) {
        try {
            DbResult<T> r = f.apply(db);
            return (r != null) ? r : new DbResult.Error<>(new IllegalStateException("Null DbResult"));
        } catch (Throwable t) {
            return new DbResult.Error<>((t instanceof Exception) ? (Exception) t : new RuntimeException(t));
        }
    }

    @Override
    void close();

    @FunctionalInterface
    interface DbWork<T> {
        @NonNull
        DbResult<T> perform(@NonNull SQLiteDatabase db) throws Exception;
    }
}
