package lib.persistence;

import android.database.Cursor;

import java.util.ArrayList;

import lib.persistence.command.manipulation.InsertCommand;
import lib.persistence.command.manipulation.UpdateCommand;
import lib.persistence.command.query.Select;
import lib.persistence.domain.entities.Todo;
import lib.persistence.profile.Mapper;

public abstract class GenericRepository<T> {

    protected final ADbContext dbContext;
    protected final Class<T> type;

    public GenericRepository(ADbContext dbContext, Class<T> type) {
        this.dbContext = dbContext;
        this.type = type;
    }

    /**
     * Veritabanına yeni bir kayıt ekler.
     * Başarılı olursa eklenen nesneyi (ID'si güncellenmiş), başarısız olursa bir hata döndürür.
     *
     * @param item Eklenecek nesne (model).
     * @param callback İşlemin sonucunu işleyecek geri çağırma (callback) nesnesi.
     */
    public void insert(T item, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            // InsertCommand'i kullanarak tablo adını ve Content Values'ı hazırla
            InsertCommand command = InsertCommand.build(item);

            long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());

            if (last_insert_rowid == -1) {
                throw new Exception("Kayıt eklenirken bir hata oluştu. Lütfen veritabanı kısıtlamalarını kontrol edin.");
            }

            // Başarılı ekleme sonrası nesnenin ID'sini güncelle
            Mapper.setId(item, last_insert_rowid);

            return new DbResult.Success<>(item);

        }, callback, true);
    }

    /**
     * Bir nesnenin veritabanındaki karşılığını günceller.
     *
     * @param item Güncellenecek nesne.
     * @param callback İşlemin sonucunu işleyecek geri çağırma (callback) nesnesi.
     */
    public void update(T item, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            UpdateCommand command = UpdateCommand.build(item);

            int rowsAffected = db.update(
                    command.getTableName(),
                    command.getContentValues(),
                    command.getWhereClause(),
                    command.getWhereArgs()
            );

            if (rowsAffected <= 0) {
                return new DbResult.Error<>(new Exception("Güncellenecek kayıt bulunamadı veya işlem başarısız oldu."), "Hiçbir kayıt güncellenemedi.");
            }
            return new DbResult.Success<>(item);

        }, callback, true);
    }

//    /**
//     * Veritabanından belirli bir ID'ye sahip kaydı siler.
//     *
//     * @param id Silinecek kaydın birincil anahtar değeri.
//     * @param callback İşlemin sonucunu işleyecek geri çağırma (callback) nesnesi.
//     */
//    public void delete(Object id, DbCallback<Todo> callback) {
//        dbContext.runDbOperation((db) -> {
//            String tableName = Mapper.getTableName(type);
//            String primaryKeyColumn = Mapper.getPrimaryKeyColumnName(type);
//
//            int rowsAffected = db.delete(
//                    tableName,
//                    primaryKeyColumn + " = ?",
//                    new String[]{String.valueOf(id)}
//            );
//
//            if (rowsAffected <= 0) {
//                return new DbResult.Error<>(new Exception("Silinecek kayıt bulunamadı veya işlem başarısız oldu."), "Hiçbir kayıt silinemedi.");
//            }
//            return new DbResult.Success<>(true);
//
//        }, callback, true);
//    }

    /**
     * Veritabanından belirtilen nesneye ait kaydı siler ve silinen nesneyi döndürür.
     *
     * @param object Silinecek nesne (model).
     * @param callback İşlemin sonucunu işleyecek geri çağırma (callback) nesnesi.
     */
    public void delete(T object, DbCallback<T> callback) { // Parametre Object id yerine T object olarak değiştirildi.
        dbContext.runDbOperation((db) -> {
            // Nesneden birincil anahtar değerini alalım
            Object primaryKeyValue = Mapper.getPrimaryKeyValue(object);

            // Eğer birincil anahtar değeri null ise, hata döndür
            if (primaryKeyValue == null) {
                return new DbResult.Error<>(new IllegalArgumentException("Nesnenin birincil anahtar değeri null olamaz."), "Geçersiz nesne: birincil anahtar değeri bulunamadı.");
            }

            // Silme işlemini gerçekleştir
            String tableName = Mapper.getTableName(type);
            String primaryKeyColumn = Mapper.getPrimaryKeyColumnName(type);

            int rowsAffected = db.delete(
                    tableName,
                    primaryKeyColumn + " = ?",
                    new String[]{String.valueOf(primaryKeyValue)} // Nesneden alınan değeri kullanıyoruz.
            );

            if (rowsAffected <= 0) {
                // Silme işlemi başarısız olursa
                return new DbResult.Error<>(new Exception("Silme işlemi başarısız oldu."), "Kayıt silinemedi.");
            }

            // Başarılı olursa, silinen nesneyi döndür
            return new DbResult.Success<>(object);

        }, callback, true); // true: yazılabilir veritabanı erişimi
    }

    /**
     * Tüm kayıtları seçmek için kısayol metot.
     * @param callback İşlemin sonucunu işleyecek geri çağırma (callback) nesnesi.
     */
    public void selectAll(DbCallback<ArrayList<T>> callback) {
        selectWith(Select.from(type), callback);
    }

    /**
     * Dinamik olarak oluşturulmuş bir Select komutu ile sorgu çalıştırır.
     * @param command Select komutu nesnesi.
     * @param callback İşlemin sonucunu işleyecek geri çağırma (callback) nesnesi.
     */
    public void selectWith(Select<T> command, DbCallback<ArrayList<T>> callback) {
        dbContext.runDbOperation((db) -> {
            ArrayList<T> items = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
                if (cursor.moveToFirst()) {
                    do {
                        T item = Mapper.cursorToObject(cursor, command.getType());
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            }
            return new DbResult.Success<>(items);
        }, callback, false);
    }


    /**
     * Veritabanından belirli bir ID'ye sahip tek bir kaydı getirir.
     *
     * @param id Kaydın birincil anahtar değeri.
     * @param callback İşlemin sonucunu işleyecek geri çağırma (callback) nesnesi.
     */
    public void getById(Object id, DbCallback<T> callback) {
        dbContext.runDbOperation((db) -> {
            String primaryKeyColumn = Mapper.getPrimaryKeyColumnName(type);

            Select<T> command = Select.from(type)
                    .where()
                    .Equals(primaryKeyColumn, id)
                    .limit(1); // Sadece bir kayıt istediğimizi belirtir.

            T resultItem = null;
            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
                if (cursor.moveToFirst()) {
                    resultItem = Mapper.cursorToObject(cursor, command.getType());
                }
            }

            if (resultItem == null) {
                return new DbResult.Error<>(new Exception("Kayıt bulunamadı."), "Belirtilen ID'ye sahip kayıt bulunamadı.");
            }

            return new DbResult.Success<>(resultItem);

        }, callback, false);
    }
}

//public abstract class GenericRepository<T> {
//
//    private final ADbContext dbContext;
//    private final Class<T> type;
//    private final RowMapper<T> defaultMapper;
//
//    public GenericRepository(ADbContext dbContext, Class<T> type) {
//        this.dbContext = dbContext;
//        this.type = type;
//        this.defaultMapper = Mapper.getRowMapper(type);
//    }
//
//    public void insert(T object, DbCallback<T> callback) {
//        dbContext.runDbOperation((db) -> {
//            InsertCommand command = InsertCommand.build(object);
//            long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());
//
//            if (last_insert_rowid == -1) {
//                throw new Exception("Kayıt eklenirken bir hata oluştu.");
//            }
//            Mapper.setId(object, last_insert_rowid);
//            return new DbResult.Success<>(object);
//        }, callback, true);
//    }
//
//    public void insertAll(List<T> objects, DbCallback<ArrayList<T>> callback) {
//        dbContext.runDbOperation((db) -> {
//            ArrayList<T> insertedObjects = new ArrayList<>();
//            db.beginTransaction();
//            try {
//                for (T object : objects) {
//                    InsertCommand command = InsertCommand.build(object);
//                    long last_insert_rowid = db.insert(command.getTableName(), null, command.getContentValues());
//                    if (last_insert_rowid != -1) {
//                        Mapper.setId(object, last_insert_rowid);
//                        insertedObjects.add(object);
//                    } else {
//                        throw new Exception("Toplu kayıt eklenirken bir hata oluştu.");
//                    }
//                }
//                db.setTransactionSuccessful();
//                return new DbResult.Success<>(insertedObjects);
//            } finally {
//                db.endTransaction();
//            }
//        }, callback, true);
//    }
//
//    public void update(T item, DbCallback<T> callback) {
//        dbContext.runDbOperation((db) -> {
//            String tableName = Mapper.getTableName(type);
//
//            Field primaryKeyField = null;
//            Object primaryKeyValue = null;
//            ContentValues contentValues = new ContentValues();
//
//            try {
//                // Get all annotated fields from the cache
//                List<Field> fields = Mapper.getClassFields(type);
//
//                for (Field field : fields) {
//                    field.setAccessible(true);
//                    DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
//
//                    if (annotation != null && annotation.isPrimaryKey()) {
//                        primaryKeyField = field;
//                        primaryKeyValue = field.get(item);
//                        if (primaryKeyValue == null || (primaryKeyValue instanceof Number && ((Number)primaryKeyValue).longValue() <= 0)) {
//                            throw new IllegalArgumentException("Güncelleme için geçerli bir birincil anahtar (ID) gereklidir.");
//                        }
//                    } else {
//                        // Handle other fields and populate ContentValues
//                        String columnName = Mapper.getColumnName(field);
//                        Object value = field.get(item);
//
//                        if (value == null) {
//                            contentValues.putNull(columnName);
//                        } else if (value instanceof String) {
//                            contentValues.put(columnName, (String) value);
//                        } else if (value instanceof Integer) {
//                            contentValues.put(columnName, (Integer) value);
//                        } else if (value instanceof Long) {
//                            contentValues.put(columnName, (Long) value);
//                        } else if (value instanceof Boolean) {
//                            contentValues.put(columnName, (Boolean) value);
//                        } else if (value instanceof Float) {
//                            contentValues.put(columnName, (Float) value);
//                        } else if (value instanceof Double) {
//                            contentValues.put(columnName, (Double) value);
//                        } else if (value instanceof byte[]) {
//                            contentValues.put(columnName, (byte[]) value);
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                throw new IllegalStateException("Güncelleme için nesne verisi hazırlanırken hata oluştu.", e);
//            }
//
//            if (primaryKeyField == null) {
//                throw new IllegalStateException("Nesnede birincil anahtar alanı bulunamadı.");
//            }
//
//            if (contentValues.size() == 0) {
//                return new DbResult.Error<>(new Exception("Güncellenecek veri bulunamadı (ContentValues boş)."), "Güncellenecek veri bulunamadı.");
//            }
//
//            String whereClause = Mapper.getColumnName(primaryKeyField) + " = ?";
//            String[] whereArgs = {String.valueOf(primaryKeyValue)};
//
//            int rowsAffected = db.update(tableName, contentValues, whereClause, whereArgs);
//
//            if (rowsAffected <= 0) {
//                return new DbResult.Error<>(new Exception("Güncellenecek kayıt bulunamadı veya işlem başarısız oldu."), "Hiçbir kayıt güncellenemedi. ID: " + primaryKeyValue);
//            }
//            return new DbResult.Success<>(item);
//
//        }, callback, true);
//    }
//
//    /*
//    public void update(T object, DbCallback<T> callback) {
//        dbContext.runDbOperation((db) -> {
//            UpdateCommand command = UpdateCommand.build(object);
//            int affectedRows = db.update(command.getTableName(), command.getContentValues(), command.getWhereClause(), command.getWhereArgs());
//
//            if (affectedRows <= 0) {
//                throw new Exception("Güncellenecek kayıt bulunamadı veya işlem başarısız oldu.");
//            }
//            return new DbResult.Success<>(object);
//        }, callback, true);
//    }
//*/
//    public void delete(T object, DbCallback<T> callback) {
//        dbContext.runDbOperation((db) -> {
//            DeleteCommand command = DeleteCommand.build(object);
//            int affectedRows = db.delete(command.getTableName(), command.getWhereClause(), command.getWhereArgs());
//
//            if (affectedRows <= 0) {
//                throw new Exception("Silinecek kayıt bulunamadı veya işlem başarısız oldu.");
//            }
//            return new DbResult.Success<>(object);
//        }, callback, true);
//    }
//
//    public void getById(Object id, DbCallback<T> callback) {
//        dbContext.runDbOperation((db) -> {
//            GetQuery command = GetQuery.build(type, id);
//            T item = null;
//            try (Cursor cursor = db.rawQuery(command.getQuery(), null)) {
//                if (cursor.moveToFirst()) {
//                    item = Mapper.cursorToObject(cursor, type);
//                }
//            }
//            return new DbResult.Success<>(item);
//        }, callback, false);
//    }
//
//    public void selectAll_old(DbCallback<ArrayList<T>> callback) {
//        dbContext.runDbOperation((db) -> {
//            // SelectQuery artık generic olduğu için derleme hatası almayız
//            SelectQuery<T> command = SelectQuery.build(type);
//            ArrayList<T> items = new ArrayList<>();
//            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
//                if (cursor.moveToFirst()) {
//                    do {
//                        // (T) cast işlemine gerek kalmadı, kod tip güvenli
//                        T item = Mapper.cursorToObject(cursor, command.getType());
//                        items.add(item);
//                    } while (cursor.moveToNext());
//                }
//            }
//            return new DbResult.Success<>(items);
//        }, callback, false);
//    }
//
//    /**
//     * Tablodaki tüm kayıtları seçer.
//     * Bu metot, `Select` sınıfını kullanarak basit bir "SELECT *" sorgusu oluşturur.
//     */
//   public void selectAll(DbCallback<ArrayList<T>> callback) {
//       Select<T> query = Select.from(type);
//       executeSelect(query, callback);
//   }
//
//    /**
//     * Önceden oluşturulmuş bir `Select` nesnesini kullanarak sorguyu çalıştırır.
//     * Bu, en esnek ve güvenli sorgulama yöntemidir.
//     *
//     * @param query    Builder deseniyle oluşturulmuş sorgu nesnesi.
//     * @param callback Asenkron sonuçları işleyecek geri çağırma (callback) nesnesi.
//     */
//    public void selectWith(Select<T> query, DbCallback<ArrayList<T>> callback) {
//        executeSelect(query, callback);
//    }
//
//    /**
//     * `Select` nesnesini kullanarak sorguyu çalıştıran genel yardımcı metot.
//     * selectAll ve selectWith metotlarındaki kod tekrarını önler.
//     *
//     * @param query    Builder deseniyle oluşturulmuş sorgu nesnesi.
//     * @param callback Asenkron sonuçları işleyecek geri çağırma nesnesi.
//     */
//    private void executeSelect(Select<T> query, DbCallback<ArrayList<T>> callback) {
//        dbContext.runDbOperation((db) -> {
//            ArrayList<T> items = new ArrayList<>();
//            // Sorguyu çalıştırır ve RowMapper ile sonuçları nesnelere dönüştürür
//            try (Cursor cursor = db.rawQuery(query.getQuery(), query.getWhereArgs())) {
//                if (cursor.moveToFirst()) {
//                    do {
//                        items.add(defaultMapper.mapRow(cursor));
//                    } while (cursor.moveToNext());
//                }
//            }
//            return new DbResult.Success<>(items);
//        }, callback, false);
//    }
//    /*
//    public void selectWhere(String whereClause, String[] whereArgs, DbCallback<ArrayList<T>> callback) {
//        dbContext.runDbOperation((db) -> {
//            // SelectQuery artık generic olduğu için derleme hatası almayız
//            SelectQuery<T> command = SelectQuery.build(type);
//            command.addRawWhereClause(whereClause, whereArgs);
//            ArrayList<T> items = new ArrayList<>();
//            try (Cursor cursor = db.rawQuery(command.getQuery(), command.getWhereArgs())) {
//                if (cursor.moveToFirst()) {
//                    do {
//                        // (T) cast işlemine gerek kalmadı, kod tip güvenli
//                        T item = Mapper.cursorToObject(cursor, command.getType());
//                        items.add(item);
//                    } while (cursor.moveToNext());
//                }
//            }
//            return new DbResult.Success<>(items);
//        }, callback, false);
//    }
//    */
//}



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