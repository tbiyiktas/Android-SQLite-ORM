package lib.persistence;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lib.persistence.command.manipulation.DeleteCommand;
import lib.persistence.command.manipulation.InsertCommand;
import lib.persistence.command.manipulation.UpdateCommand;
import lib.persistence.command.query.GetQuery;
import lib.persistence.command.query.SelectQuery;
import lib.persistence.profile.Mapper;
import java.util.HashMap;

// Tekrarlanan kodları soyutlamak için yeni fonksiyonel arayüz
@FunctionalInterface
interface DbOperation<T> {
    DbResult<T> execute(SQLiteDatabase db) throws Exception;
}

public abstract class ADbContext extends SQLiteOpenHelper {

    private final Object lock = new Object();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ADbContext(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    // Her metotta tekrarlanan boilerplate kodu yöneten genel yardımcı metot
    protected  <T> void runDbOperation(DbOperation<T> operation, DbCallback<T> callback, boolean isWritable) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    db = isWritable ? getWritableDatabase() : getReadableDatabase();
                    result = operation.execute(db);
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                    // db.endTransaction(); gibi özel işlemler için metotlar kendi içlerinde yönetilmeli.
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    // Bu metot, Repository'ler tarafından kullanılacak yeni insert metodu
    public <T> void internalInsert(Object object, DbCallback<T> callback) {
        runDbOperation((db) -> {
            InsertCommand command = InsertCommand.build(object);
            long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());

            if (last_insert_rowid == -1) {
                throw new Exception("Kayıt eklenirken bir hata oluştu.");
            }
            Mapper.setId(object, last_insert_rowid);
            return new DbResult.Success<>((T) object);
        }, callback, true);
    }

    // ... Diğer tüm CRUD metotları benzer şekilde GenericRepository'e taşınır.

    // Ham SQL sorgularını çalıştırmak için mevcut metotlar (kaldırılmadı)
    public void execSql(String sql, DbCallback<Void> callback) {
        runDbOperation((db) -> {
            db.execSQL(sql);
            return new DbResult.Success<>(null);
        }, callback, true);
    }

    public void rawQuery(String sql, String[] selectionArgs, DbCallback<ArrayList<HashMap<String, String>>> callback) {
        runDbOperation((db) -> {
            ArrayList<HashMap<String, String>> rows = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(sql, selectionArgs)) {
                if (cursor.moveToFirst()) {
                    String[] columnNames = cursor.getColumnNames();
                    do {
                        HashMap<String, String> row = new HashMap<>();
                        for (String columnName : columnNames) {
                            int columnIndex = cursor.getColumnIndex(columnName);
                            if (columnIndex != -1) {
                                row.put(columnName, cursor.getString(columnIndex));
                            }
                        }
                        rows.add(row);
                    } while (cursor.moveToNext());
                }
                return new DbResult.Success<>(rows);
            }
        }, callback, false);
    }
}


/*
public abstract class ADbContext extends SQLiteOpenHelper {

    private final Object lock = new Object();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ADbContext(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    protected Object getLock() {
        return lock;
    }

    public <T> void insert(Object object, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                try {
                    InsertCommand command = InsertCommand.build(object);
                    db = getWritableDatabase();
                    long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());

                    if (last_insert_rowid == -1) {
                        result = new DbResult.Error<>(new Exception("Kayıt eklenirken bir hata oluştu."), "Kayıt eklenirken bir hata oluştu. - `db.insert()` metodu -1 döndürdü.");
                    } else {
                        Mapper.setId(object, last_insert_rowid);
                        result = new DbResult.Success<>((T) object);
                    }
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void insertAll(List<T> objects, DbCallback<ArrayList<T>> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<ArrayList<T>> result;
                SQLiteDatabase db = null;
                ArrayList<T> insertedObjects = new ArrayList<>();
                try {
                    db = getWritableDatabase();
                    db.beginTransaction();

                    for (T object : objects) {
                        InsertCommand command = InsertCommand.build(object);
                        long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());
                        if (last_insert_rowid != -1) {
                            Mapper.setId(object, last_insert_rowid);
                            insertedObjects.add(object);
                        } else {
                            // Hata durumunda işlemi durdur
                            throw new Exception("Toplu kayıt eklenirken bir hata oluştu. İşlem geri alındı.");
                        }
                    }

                    db.setTransactionSuccessful();
                    result = new DbResult.Success<>(insertedObjects);
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (db != null) {
                        db.endTransaction();
                        if (db.isOpen()) {
                            db.close();
                        }
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void update(Object object, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                try {
                    UpdateCommand command = UpdateCommand.build(object);
                    db = getWritableDatabase();
                    int affectedRows = db.update(command.getTableName(), command.getContentValues(), command.getWhereClause(), command.getWhereArgs());

                    if (affectedRows <= 0) {
                        result = new DbResult.Error<>(new Exception("Kayıt güncellenirken bir hata oluştu."), "Güncellenecek kayıt bulunamadı veya işlem başarısız oldu.");
                    } else {
                        result = new DbResult.Success<>((T) object);
                    }
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void delete(Object object, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                try {
                    DeleteCommand command = DeleteCommand.build(object);
                    db = getWritableDatabase();
                    int affectedRows = db.delete(command.getTableName(), command.getWhereClause(), command.getWhereArgs());

                    if (affectedRows <= 0) {
                        result = new DbResult.Error<>(new Exception("Kayıt silinirken bir hata oluştu."), "Silinecek kayıt bulunamadı veya işlem başarısız oldu.");
                    } else {
                        result = new DbResult.Success<>((T) object);
                    }
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void get(Class<T> type, Object id, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    GetQuery command = GetQuery.build(type, id);
                    db = getReadableDatabase();
                    cursor = db.rawQuery(command.getQuery(), null);

                    T item = null;
                    if (cursor.moveToFirst()) {
                        item = Mapper.cursorToObject(cursor, type);
                    }
                    result = new DbResult.Success<>(item);
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void select(SelectQuery command, DbCallback<ArrayList<T>> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<ArrayList<T>> result;
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    ArrayList<T> items = new ArrayList<>();
                    db = getReadableDatabase();
                    cursor = db.rawQuery(command.getQuery(), command.getWhereArgs());

                    if (cursor.moveToFirst()) {
                        do {
                            T item = (T) Mapper.cursorToObject(cursor, command.getType());
                            items.add(item);
                        } while (cursor.moveToNext());
                    }
                    result = new DbResult.Success<>(items);
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public void execSql(String sql, DbCallback<Void> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<Void> result;
                SQLiteDatabase db = null;
                try {
                    db = getWritableDatabase();
                    db.execSQL(sql);
                    result = new DbResult.Success<>(null);
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public void rawQuery(String sql, String[] selectionArgs, DbCallback<ArrayList<HashMap<String, String>>> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<ArrayList<HashMap<String, String>>> result;
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    ArrayList<HashMap<String, String>> rows = new ArrayList<>();
                    db = getReadableDatabase();
                    cursor = db.rawQuery(sql, selectionArgs);

                    if (cursor.moveToFirst()) {
                        String[] columnNames = cursor.getColumnNames();
                        do {
                            HashMap<String, String> row = new HashMap<>();
                            for (String columnName : columnNames) {
                                int columnIndex = cursor.getColumnIndex(columnName);
                                if (columnIndex != -1) {
                                    row.put(columnName, cursor.getString(columnIndex));
                                }
                            }
                            rows.add(row);
                        } while (cursor.moveToNext());
                    }
                    result = new DbResult.Success<>(rows);
                } catch (Exception e) {
                    result = new DbResult.Error<>(e, e.getMessage());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }
}
*/
/*
public abstract class ADbContext extends SQLiteOpenHelper {

    private final Object lock = new Object();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ADbContext(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    protected Object getLock() {
        return lock;
    }

    public <T> void insert(Object object, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                try {
                    InsertCommand command = InsertCommand.build(object);
                    db = getWritableDatabase();
                    long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());

                    if (last_insert_rowid == -1) {
                        result = new DbResult.Error(new Exception("Kayıt eklenirken bir hata oluştu."));
                    } else {
                        Mapper.setId(object, last_insert_rowid);
                        result = new DbResult.Success<>((T) object);
                    }
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void insertAll(List<T> objects, DbCallback<ArrayList<T>> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<ArrayList<T>> result;
                SQLiteDatabase db = null;
                ArrayList<T> insertedObjects = new ArrayList<>();
                try {
                    db = getWritableDatabase();
                    db.beginTransaction();

                    for (T object : objects) {
                        InsertCommand command = InsertCommand.build(object);
                        long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());
                        if (last_insert_rowid != -1) {
                            Mapper.setId(object, last_insert_rowid);
                            insertedObjects.add(object);
                        }
                    }

                    db.setTransactionSuccessful();
                    result = new DbResult.Success<>(insertedObjects);
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (db != null) {
                        db.endTransaction();
                        if (db.isOpen()) {
                            db.close();
                        }
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void update(Object object, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                try {
                    UpdateCommand command = UpdateCommand.build(object);
                    db = getWritableDatabase();
                    int affectedRows = db.update(command.getTableName(), command.getContentValues(), command.getWhereClause(), command.getWhereArgs());

                    if (affectedRows <= 0) {
                        result = new DbResult.Error(new Exception("Kayıt güncellenirken bir hata oluştu."));
                    } else {
                        result = new DbResult.Success<>((T) object);
                    }
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void delete(Object object, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                try {
                    DeleteCommand command = DeleteCommand.build(object);
                    db = getWritableDatabase();
                    int affectedRows = db.delete(command.getTableName(), command.getWhereClause(), command.getWhereArgs());

                    if (affectedRows <= 0) {
                        result = new DbResult.Error(new Exception("Kayıt silinirken bir hata oluştu."));
                    } else {
                        result = new DbResult.Success<>((T) object);
                    }
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void get(Class<T> type, Object id, DbCallback<T> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<T> result;
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    GetQuery command = GetQuery.build(type, id);
                    db = getReadableDatabase();
                    cursor = db.rawQuery(command.getQuery(), null);

                    T item = null;
                    if (cursor.moveToFirst()) {
                        item = Mapper.cursorToObject(cursor, type);
                    }
                    result = new DbResult.Success<>(item);
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    public <T> void select(SelectQuery command, DbCallback<ArrayList<T>> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<ArrayList<T>> result;
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    ArrayList<T> items = new ArrayList<>();
                    db = getReadableDatabase();
                    cursor = db.rawQuery(command.getQuery(), command.getWhereArgs());

                    if (cursor.moveToFirst()) {
                        do {
                            T item = (T) Mapper.cursorToObject(cursor, command.getType());
                            items.add(item);
                        } while (cursor.moveToNext());
                    }
                    result = new DbResult.Success<>(items);
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    // Yeni: Ham (raw) SQL sorgularını çalıştıran metot
    public void execSql(String sql, DbCallback<Void> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<Void> result;
                SQLiteDatabase db = null;
                try {
                    db = getWritableDatabase();
                    db.execSQL(sql);
                    result = new DbResult.Success<>(null);
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }

    // Yeni: Ham (raw) SELECT sorgusunu çalıştıran metot
    public void rawQuery(String sql, String[] selectionArgs, DbCallback<ArrayList<HashMap<String, String>>> callback) {
        executorService.execute(() -> {
            synchronized (lock) {
                DbResult<ArrayList<HashMap<String, String>>> result;
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    ArrayList<HashMap<String, String>> rows = new ArrayList<>();
                    db = getReadableDatabase();
                    cursor = db.rawQuery(sql, selectionArgs);

                    if (cursor.moveToFirst()) {
                        String[] columnNames = cursor.getColumnNames();
                        do {
                            HashMap<String, String> row = new HashMap<>();
                            for (String columnName : columnNames) {
                                int columnIndex = cursor.getColumnIndex(columnName);
                                if (columnIndex != -1) {
                                    row.put(columnName, cursor.getString(columnIndex));
                                }
                            }
                            rows.add(row);
                        } while (cursor.moveToNext());
                    }
                    result = new DbResult.Success<>(rows);
                } catch (Exception e) {
                    result = new DbResult.Error(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
                callback.onResult(result);
            }
        });
    }
}

 */