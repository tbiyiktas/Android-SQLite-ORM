package lib.persistence;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import lib.persistence.command.manipulation.DeleteCommand;
import lib.persistence.command.manipulation.InsertCommand;
import lib.persistence.command.manipulation.UpdateCommand;
import lib.persistence.command.query.GetQuery;
import lib.persistence.command.query.SelectQuery;
import lib.persistence.profile.Mapper;


public abstract class GenericRepository<T> {

    private final ADbContext dbContext;
    private final Class<T> type;

    public GenericRepository(ADbContext dbContext, Class<T> type) {
        this.dbContext = dbContext;
        this.type = type;
    }

    public void insert(T object, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            InsertCommand command = InsertCommand.build(object);
            long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());

            if (last_insert_rowid == -1) {
                throw new Exception("Kayıt eklenirken bir hata oluştu.");
            }
            Mapper.setId(object, last_insert_rowid);
            return new DbResult.Success<>(object);
        }, callback, true);
    }

    public void insertAll(List<T> objects, DbCallback<ArrayList<T>> callback) {
        dbContext.runDbOperation((db) -> {
            ArrayList<T> insertedObjects = new ArrayList<>();
            db.beginTransaction();
            try {
                for (T object : objects) {
                    InsertCommand command = InsertCommand.build(object);
                    long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());
                    if (last_insert_rowid != -1) {
                        Mapper.setId(object, last_insert_rowid);
                        insertedObjects.add(object);
                    } else {
                        throw new Exception("Toplu kayıt eklenirken bir hata oluştu.");
                    }
                }
                db.setTransactionSuccessful();
                return new DbResult.Success<>(insertedObjects);
            } finally {
                db.endTransaction();
            }
        }, callback, true);
    }

    public void update(T object, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            UpdateCommand command = UpdateCommand.build(object);
            int affectedRows = db.update(command.getTableName(), command.getContentValues(), command.getWhereClause(), command.getWhereArgs());

            if (affectedRows <= 0) {
                throw new Exception("Güncellenecek kayıt bulunamadı veya işlem başarısız oldu.");
            }
            return new DbResult.Success<>(object);
        }, callback, true);
    }

    public void delete(T object, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            DeleteCommand command = DeleteCommand.build(object);
            int affectedRows = db.delete(command.getTableName(), command.getWhereClause(), command.getWhereArgs());

            if (affectedRows <= 0) {
                throw new Exception("Silinecek kayıt bulunamadı veya işlem başarısız oldu.");
            }
            return new DbResult.Success<>(object);
        }, callback, true);
    }

    public void getById(Object id, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            GetQuery command = GetQuery.build(type, id);
            T item = null;
            try (Cursor cursor = db.rawQuery(command.getQuery(), null)) {
                if (cursor.moveToFirst()) {
                    item = Mapper.cursorToObject(cursor, type);
                }
            }
            return new DbResult.Success<>(item);
        }, callback, false);
    }

    public void selectAll(DbCallback<ArrayList<T>> callback) {
        dbContext.runDbOperation((db) -> {
            // SelectQuery artık generic olduğu için derleme hatası almayız
            SelectQuery<T> command = SelectQuery.build(type);
            ArrayList<T> items = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
                if (cursor.moveToFirst()) {
                    do {
                        // (T) cast işlemine gerek kalmadı, kod tip güvenli
                        T item = Mapper.cursorToObject(cursor, command.getType());
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            }
            return new DbResult.Success<>(items);
        }, callback, false);
    }

    public void selectWhere(String whereClause, String[] whereArgs, DbCallback<ArrayList<T>> callback) {
        dbContext.runDbOperation((db) -> {
            // SelectQuery artık generic olduğu için derleme hatası almayız
            SelectQuery<T> command = SelectQuery.build(type);
            command.addRawWhereClause(whereClause, whereArgs);
            ArrayList<T> items = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
                if (cursor.moveToFirst()) {
                    do {
                        // (T) cast işlemine gerek kalmadı, kod tip güvenli
                        T item = Mapper.cursorToObject(cursor, command.getType());
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            }
            return new DbResult.Success<>(items);
        }, callback, false);
    }
}

/*
public abstract class GenericRepository<T> {

    private final ADbContext dbContext;
    private final Class<T> type;

    public GenericRepository(ADbContext dbContext, Class<T> type) {
        this.dbContext = dbContext;
        this.type = type;
    }

    public void insert(T object, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            InsertCommand command = InsertCommand.build(object);
            long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());

            if (last_insert_rowid == -1) {
                throw new Exception("Kayıt eklenirken bir hata oluştu.");
            }
            Mapper.setId(object, last_insert_rowid);
            return new DbResult.Success<>(object);
        }, callback, true);
    }

    public void insertAll(List<T> objects, DbCallback<ArrayList<T>> callback) {
        dbContext.runDbOperation((db) -> {
            ArrayList<T> insertedObjects = new ArrayList<>();
            db.beginTransaction();
            try {
                for (T object : objects) {
                    InsertCommand command = InsertCommand.build(object);
                    long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());
                    if (last_insert_rowid != -1) {
                        Mapper.setId(object, last_insert_rowid);
                        insertedObjects.add(object);
                    } else {
                        throw new Exception("Toplu kayıt eklenirken bir hata oluştu.");
                    }
                }
                db.setTransactionSuccessful();
                return new DbResult.Success<>(insertedObjects);
            } finally {
                db.endTransaction();
            }
        }, callback, true);
    }

    public void update(T object, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            UpdateCommand command = UpdateCommand.build(object);
            int affectedRows = db.update(command.getTableName(), command.getContentValues(), command.getWhereClause(), command.getWhereArgs());

            if (affectedRows <= 0) {
                throw new Exception("Güncellenecek kayıt bulunamadı veya işlem başarısız oldu.");
            }
            return new DbResult.Success<>(object);
        }, callback, true);
    }

    public void delete(T object, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            DeleteCommand command = DeleteCommand.build(object);
            int affectedRows = db.delete(command.getTableName(), command.getWhereClause(), command.getWhereArgs());

            if (affectedRows <= 0) {
                throw new Exception("Silinecek kayıt bulunamadı veya işlem başarısız oldu.");
            }
            return new DbResult.Success<>(object);
        }, callback, true);
    }

    public void getById(Object id, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            GetQuery command = GetQuery.build(type, id);
            T item = null;
            try (Cursor cursor = db.rawQuery(command.getQuery(), null)) {
                if (cursor.moveToFirst()) {
                    item = Mapper.cursorToObject(cursor, type);
                }
            }
            return new DbResult.Success<>(item);
        }, callback, false);
    }


    public void selectAll(DbCallback<ArrayList<T>> callback) {
        dbContext.runDbOperation((db) -> {
            SelectQuery command = SelectQuery.build(type); // Generic olmayan SelectQuery
            ArrayList<T> items = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
                if (cursor.moveToFirst()) {
                    do {
                        // Derleme hatasını gidermek için zorunlu (T) tip dönüştürmesi
                        T item = (T) Mapper.cursorToObject(cursor, command.getType());
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            }
            return new DbResult.Success<>(items);
        }, callback, false);
    }

    public void selectWhere(String whereClause, String[] whereArgs, DbCallback<ArrayList<T>> callback) {
        dbContext.runDbOperation((db) -> {
            SelectQuery command = SelectQuery.build(type); // Generic olmayan SelectQuery
            command.addRawWhereClause(whereClause, whereArgs);
            ArrayList<T> items = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
                if (cursor.moveToFirst()) {
                    do {
                        // Derleme hatasını gidermek için zorunlu (T) tip dönüştürmesi
                        T item = (T) Mapper.cursorToObject(cursor, command.getType());
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            }
            return new DbResult.Success<>(items);
        }, callback, false);
    }

}
*/

/*
public abstract class GenericRepository<T> {

    private final ADbContext dbContext;
    private final Class<T> type;

    public GenericRepository(ADbContext dbContext, Class<T> type) {
        this.dbContext = dbContext;
        this.type = type;
    }

    public void insert(T object, DbCallback<T> callback) {
        dbContext.insert(object, callback);
    }

    public void insertAll(List<T> objects, DbCallback<ArrayList<T>> callback) {
        dbContext.insertAll(objects, callback);
    }

    public void update(T object, DbCallback<T> callback) {
        dbContext.update(object, callback);
    }

    public void delete(T object, DbCallback<T> callback) {
        dbContext.delete(object, callback);
    }

    public void getById(Object id, DbCallback<T> callback) {
        dbContext.get(type, id, callback);
    }

    public void selectAll(DbCallback<ArrayList<T>> callback) {
        SelectQuery command = SelectQuery.build(type);
        dbContext.select(command, callback);
    }

    public void selectWhere(String whereClause, String[] whereArgs, DbCallback<ArrayList<T>> callback) {
        SelectQuery command = SelectQuery.build(type);
        command.addRawWhereClause(whereClause, whereArgs);
        dbContext.select(command, callback);
    }


    // Diğer özel sorgu metotları buraya eklenebilir.
}
*/