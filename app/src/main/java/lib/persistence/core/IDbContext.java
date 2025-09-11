// lib/persistence/core/IDbContext.java
package lib.persistence.core;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

/**
 * İdeal minimal API:
 * - runRead: read pool, NON-transactional
 * - runWrite: write pool, ALWAYS transactional (BEGIN...COMMIT/ROLLBACK)
 * - Net: ambiguity yok, bayrak yok, tek DbWork sözleşmesi
 */
public interface IDbContext extends AutoCloseable {

    // --- Şema yaşam döngüsü (somut context uygulayacak)
    void onConfigure(@NonNull SQLiteDatabase db);
    void onCreate(@NonNull SQLiteDatabase db);
    void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion);

    // --- Yüksek seviye API
    /** Read-only operasyonları read havuzunda koşturur; transaction açmaz. */
    <T> CancellableFuture<DbResult<T>> runRead(@NonNull DbWork<T> work);

    /** Write operasyonlarını tek bir TRANSACTION içinde koşturur (BEGIN…COMMIT/ROLLBACK). */
    <T> CancellableFuture<DbResult<T>> runWrite(@NonNull DbWork<T> work);

    // --- (İsteğe bağlı) Test kolaylığı: bloklayıcı varyantlar
    /** Testler için: future.get() sarmalayıcısı (main-thread’de kullanılmamalıdır). */
    default <T> DbResult<T> runReadSync(@NonNull DbWork<T> work) {
        try {
            return runRead(work).get(); // CancellableFuture#get: uygulamanızdaki bloklayıcı çağrı
        } catch (Exception e) {
            return new DbResult.Error<>(e);
        }
    }

    /** Testler için: future.get() sarmalayıcısı (main-thread’de kullanılmamalıdır). */
    default <T> DbResult<T> runWriteSync(@NonNull DbWork<T> work) {
        try {
            return runWrite(work).get();
        } catch (Exception e) {
            return new DbResult.Error<>(e);
        }
    }

    @Override
    void close();

    // --- Tek sözleşme
    @FunctionalInterface
    interface DbWork<T> {
        /** İstemci kod transaction/connection yönetmez; yalnızca iş mantığını yazar. */
        @NonNull
        DbResult<T> perform(@NonNull SQLiteDatabase db) throws Exception;

        /** (İsteğe bağlı) Telemetri/izleme için ad. */
        default @NonNull String name() {
            return getClass().getSimpleName();
        }
    }
}
