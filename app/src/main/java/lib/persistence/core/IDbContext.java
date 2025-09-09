package lib.persistence.core;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

public interface IDbContext extends AutoCloseable {
    void onConfigure(@NonNull SQLiteDatabase db);

    // --- Şema yaşam döngüsü
    void onCreate(@NonNull SQLiteDatabase db);

    void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion);

    // --- Çalıştırıcı
    <T> CancellableFuture<DbResult<T>> runDbOperation(@NonNull DbWork<T> work);
    <T> CancellableFuture<DbResult<T>> runDbOperation(@NonNull DbWork<T> work, boolean writeTransaction);

    default <T> CancellableFuture<DbResult<T>> runDbOperation(
            @NonNull java.util.function.Function<SQLiteDatabase, DbResult<T>> work
    ) {
        // NOT: Lambda'yı açıkça DbWork<T>'e cast ediyoruz → doğru overload çağrılır
        return runDbOperation((DbWork<T>) db -> safeApply(work, db), /*writeTransaction=*/false);
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
            return (r != null) ? r
                    : new DbResult.Error<>(new IllegalStateException("Null DbResult"));
        } catch (Throwable t) {
            return new DbResult.Error<>((t instanceof Exception) ? (Exception) t : new RuntimeException(t));
        }
    }
    @Override
    void close();

    @FunctionalInterface
    public interface DbWork<T> {
        @NonNull
        DbResult<T> perform(@NonNull SQLiteDatabase db) throws Exception;
    }
}
