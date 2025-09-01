package lib.persistence;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IDbContext extends AutoCloseable {
    void onConfigure(@NonNull SQLiteDatabase db);

    // --- Şema yaşam döngüsü
    void onCreate(@NonNull SQLiteDatabase db);

    void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion);

    // --- Çalıştırıcı
    <T> void runDbOperation(@NonNull DbWork<T> work,
                            @Nullable DbCallback<T> callback,
                            boolean writeTransaction);

    @Override
    void close();

    @FunctionalInterface
    public interface DbWork<T> {
        @NonNull
        DbResult<T> perform(@NonNull SQLiteDatabase db) throws Exception;
    }
}
