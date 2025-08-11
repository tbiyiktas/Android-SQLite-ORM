package lib.persistence.profile;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lib.persistence.annotations.DbColumnAnnotation;
import lib.persistence.annotations.DbTableAnnotation;


public class Mapper {

    // DbColumn listesini önbellekte saklamak için tek bir cache kullanıyoruz
    private static final Map<Class<?>, List<DbColumn>> dbColumnCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- YENİ YAPI: DbColumn Üreten ve Önbelleğe Alan Metot ---

    /**
     * Bir model sınıfını tarar ve DbColumn nesnelerinin bir listesini döndürür.
     * Sonuçlar bir önbellekte saklanır.
     */
    public static List<DbColumn> classToDbColumns(Class<?> type) {
        return dbColumnCache.computeIfAbsent(type, Mapper::createDbColumnsForClass);
    }

    /**
     * Sınıfı yansıma ile tarar ve DbColumn listesini oluşturur.
     * Bu metot sadece önbellek boşsa çağrılır.
     */
    private static List<DbColumn> createDbColumnsForClass(Class<?> type) {
        List<DbColumn> columns = new ArrayList<>();
        Class<?> currentClass = type;

        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
                if (annotation != null) {
                    field.setAccessible(true); // Özel alanlara erişim için
                    // Fabrika metodu çağrısı
                    DbColumn dbColumn = createDbColumnFromField(field, annotation);
                    columns.add(dbColumn);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // ordinal'a göre sırala
        Collections.sort(columns, Comparator.comparingInt(DbColumn::getOrdinal));
        return columns;
    }

    /**
     * Bir Field nesnesi ve annotation'dan bir DbColumn nesnesi oluşturan fabrika metodu.
     */
    private static DbColumn createDbColumnFromField(Field field, DbColumnAnnotation annotation) {
        String columnName = annotation.name().isEmpty() ? field.getName() : annotation.name();
        DbDataType dataType = getDbDataTypeFromFieldType(field.getType());

        return new DbColumn(
                annotation.ordinal(),
                field.getName(), // fieldName eklendi
                columnName,
                dataType.toString(),
                annotation.isPrimaryKey(),
                annotation.isIdentity(),
                annotation.isNullable()
        );
    }

    // --- NESNE-VERİTABANI EŞLEME METOTLARI ---

    /**
     * Bir Cursor'dan gelen veriyi, verilen tipteki bir nesneye dönüştürür.
     * Bu metot doğrudan getRowMapper yerine kullanılır ve tek sorumluluk taşır.
     */
    public static <T> T cursorToObject(Cursor cursor, Class<T> type) {
        try {
            T object = type.getDeclaredConstructor().newInstance();
            List<DbColumn> columns = classToDbColumns(type);
            for (DbColumn column : columns) {
                int columnIndex = cursor.getColumnIndex(column.getColumnName());
                if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
                    Field field = type.getDeclaredField(column.getFieldName());
                    field.setAccessible(true);
                    setFieldValueFromCursor(field, object, cursor, columnIndex);
                }
            }
            return object;
        } catch (Exception e) {
            throw new IllegalStateException("Nesne eşleme hatası: " + type.getSimpleName(), e);
        }
    }

    /**
     * Bir nesneyi ContentValues'a dönüştürür.
     */
    public static ContentValues objectToContentValues(Object object) {
        ContentValues contentValues = new ContentValues();
        List<DbColumn> columns = classToDbColumns(object.getClass());
        try {
            for (DbColumn column : columns) {
                if (!column.isIdentity()) {
                    Field field = object.getClass().getDeclaredField(column.getFieldName());
                    field.setAccessible(true);
                    Object value = field.get(object);
                    putInContentValues(contentValues, column.getColumnName(), value);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("ContentValues oluşturulurken hata oluştu.", e);
        }
        return contentValues;
    }

    // --- YARDIMCI METOTLAR ---

    public static String getTableName(Class<?> type) {
        String name = type.getSimpleName();
        DbTableAnnotation tableAnnotation = type.getAnnotation(DbTableAnnotation.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            name = tableAnnotation.name();
        }
        return name;
    }

    private static void putInContentValues(ContentValues values, String key, Object value) {
        if (value == null) {
            values.putNull(key);
        } else if (value instanceof String) {
            values.put(key, (String) value);
        } else if (value instanceof Integer) {
            values.put(key, (Integer) value);
        } else if (value instanceof Long) {
            values.put(key, (Long) value);
        } else if (value instanceof Double) {
            values.put(key, (Double) value);
        } else if (value instanceof Float) {
            values.put(key, (Float) value);
        } else if (value instanceof Boolean) {
            values.put(key, (Boolean) value ? 1 : 0);
        } else if (value instanceof byte[]) {
            values.put(key, (byte[]) value);
        } else if (value instanceof LocalDateTime) {
            values.put(key, ((LocalDateTime) value).format(dateTimeFormatter));
        } else if (value instanceof LocalDate) {
            values.put(key, ((LocalDate) value).format(dateFormatter));
        }
    }

    private static void setFieldValueFromCursor(Field field, Object object, Cursor cursor, int columnIndex) throws IllegalAccessException {
        Class<?> fieldType = field.getType();

        if (fieldType == int.class || fieldType == Integer.class) {
            field.set(object, cursor.getInt(columnIndex));
        } else if (fieldType == String.class) {
            field.set(object, cursor.getString(columnIndex));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(object, cursor.getInt(columnIndex) == 1);
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(object, cursor.getLong(columnIndex));
        } else if (fieldType == float.class || fieldType == Float.class) {
            field.set(object, cursor.getFloat(columnIndex));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(object, cursor.getDouble(columnIndex));
        } else if (fieldType == byte[].class) {
            field.set(object, cursor.getBlob(columnIndex));
        } else if (fieldType == LocalDateTime.class) {
            String value = cursor.getString(columnIndex);
            if (value != null) {
                field.set(object, LocalDateTime.parse(value, dateTimeFormatter));
            }
        } else if (fieldType == LocalDate.class) {
            String value = cursor.getString(columnIndex);
            if (value != null) {
                field.set(object, LocalDate.parse(value, dateFormatter));
            }
        }
    }

    private static DbDataType getDbDataTypeFromFieldType(Class<?> fieldType) {
        if (fieldType == String.class) {
            return DbDataType.TEXT;
        } else if (fieldType == int.class || fieldType == Integer.class || fieldType == long.class || fieldType == Long.class || fieldType == boolean.class || fieldType == Boolean.class) {
            return DbDataType.INTEGER;
        } else if (fieldType == double.class || fieldType == Double.class || fieldType == float.class || fieldType == Float.class) {
            return DbDataType.REAL;
        } else if (fieldType == byte[].class) {
            return DbDataType.BLOB;
        }
        return DbDataType.TEXT;
    }

    /**
     * Otomatik artan (identity) birincil anahtar alanını bulur ve değerini set eder.
     * Bu metot, başarılı bir insert işleminden sonra nesnenin ID'sini güncellemek için kullanılır.
     *
     * @param object ID'si güncellenecek olan nesne.
     * @param id Veritabanı tarafından oluşturulan yeni ID değeri.
     */
    public static void setId(Object object, long id) {
        List<DbColumn> columns = classToDbColumns(object.getClass());
        try {
            for (DbColumn column : columns) {
                if (column.isIdentity()) {
                    Field field = object.getClass().getDeclaredField(column.getFieldName());
                    field.setAccessible(true);

                    Class<?> fieldType = field.getType();

                    // long, int veya Integer tiplerini destekle
                    if (fieldType == long.class || fieldType == Long.class) {
                        field.set(object, id);
                    } else if (fieldType == int.class || fieldType == Integer.class) {
                        field.set(object, (int) id);
                    }
                    return; // Sadece bir identity alan olduğu varsayıldı
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Hata durumunda loglama yapmak önemlidir.
            System.err.println("setId: Nesneye ID atanırken hata oluştu. " + e.getMessage());
        }
    }

    /**
     * Bir model sınıfının birincil anahtar sütununun adını döndürür.
     * Eğer tanımlanmış bir birincil anahtar yoksa, varsayılan olarak "id" döner.
     *
     * @param type Birincil anahtar sütunu aranan model sınıfı.
     * @return Birincil anahtar sütununun adı.
     */
    public static String getPrimaryKeyColumnName(Class<?> type) {
        List<DbColumn> columns = classToDbColumns(type);
        for (DbColumn column : columns) {
            if (column.isPrimaryKey()) {
                return column.getColumnName();
            }
        }
        // Varsayılan olarak "id" adını döndürür.
        return "id";
    }

    /**
     * Bir nesnenin birincil anahtar (primary key) alanındaki değeri döndürür.
     *
     * @param object Değeri alınacak olan nesne.
     * @return Birincil anahtar alanının değeri veya null.
     */
    public static Object getPrimaryKeyValue(Object object) {
        List<DbColumn> columns = classToDbColumns(object.getClass());
        try {
            for (DbColumn column : columns) {
                if (column.isPrimaryKey()) {
                    Field field = object.getClass().getDeclaredField(column.getFieldName());
                    field.setAccessible(true);
                    return field.get(object);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Hata durumunda loglama yapmak önemlidir.
            System.err.println("getPrimaryKeyValue: Birincil anahtar değeri alınırken hata oluştu. " + e.getMessage());
        }
        return null;
    }
}

//public class Mapper {
//    private static final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();
//    private static final Map<Class<?>, List<DbColumn>> dbColumnCache = new ConcurrentHashMap<>();
//
//    public static String getColumnName(Field field) {
//        DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
//        return (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : field.getName();
//    }
//
//    /**
//     * Bir Cursor satırını, belirtilen tipteki bir nesneye dönüştürmek için
//     * bir RowMapper döndürür. Bu RowMapper reflection cache kullanır.
//     */
//    public static <T> RowMapper<T> getRowMapper(Class<T> type) {
//        return cursor -> {
//            try {
//                T item = type.getDeclaredConstructor().newInstance();
//                List<Field> fields = getClassFields(type); // Önbelleklenmiş alanları kullan
//
//                for (Field field : fields) {
//                    // Sütun adını al
//                    String columnName = getColumnName(field);
//                    // Cursor'da o sütunun indeksini al
//                    int columnIndex = cursor.getColumnIndex(columnName);
//
//                    // --- BURADAKİ KONTROL ÇOK KRİTİK! ---
//                    // Eğer sütun yoksa (getColumnIndex -1 döndürür), bu alana değer atamadan devam et.
//                    if (columnIndex == -1) {
//                        continue;
//                    }
//
//                    // Alanın tipine göre değeri Cursor'dan oku ve alana set et
//                    Class<?> fieldType = field.getType();
//
//                    if (fieldType == String.class) {
//                        field.set(item, cursor.getString(columnIndex));
//                    } else if (fieldType == int.class || fieldType == Integer.class) {
//                        field.set(item, cursor.getInt(columnIndex));
//                    } else if (fieldType == long.class || fieldType == Long.class) {
//                        field.set(item, cursor.getLong(columnIndex));
//                    } else if (fieldType == boolean.class || fieldType == Boolean.class) {
//                        // INTEGER 0 veya 1 olarak saklandığı varsayıldı
//                        field.set(item, cursor.getInt(columnIndex) == 1);
//                    } else if (fieldType == float.class || fieldType == Float.class) {
//                        field.set(item, cursor.getFloat(columnIndex));
//                    } else if (fieldType == double.class || fieldType == Double.class) {
//                        field.set(item, cursor.getDouble(columnIndex));
//                    } else if (fieldType == byte[].class) {
//                        field.set(item, cursor.getBlob(columnIndex));
//                    }
//                    // Diğer tipler için mantık eklenebilir
//                }
//                return item;
//            } catch (Exception e) {
//                // Hata yönetimini iyileştirin. Loglama yapmak faydalı olacaktır.
//                throw new IllegalStateException("Nesne eşleme hatası: " + type.getSimpleName(), e);
//            }
//        };
//    }
//
//    // --- Önbellekli yansıma metodu ---
//    /**
//     * Bir model sınıfına ait DbColumnAnnotation ile işaretlenmiş alanları bulur ve önbelleğe alır.
//     *
//     * @param type Alanları alınacak sınıf.
//     * @return DbColumnAnnotation ile işaretlenmiş alanların listesi.
//     */
//    public static List<Field> getClassFields(Class<?> type) {
//        // 1. Önbellekte varsa, doğrudan geri dön
//        if (fieldCache.containsKey(type)) {
//            return fieldCache.get(type);
//        }
//
//        // 2. Önbellekte yoksa, yansıma ile bul
//        List<Field> fields = new ArrayList<>();
//        Class<?> currentClass = type;
//
//        while (currentClass != null && currentClass != Object.class) {
//            for (Field field : currentClass.getDeclaredFields()) {
//                if (field.isAnnotationPresent(DbColumnAnnotation.class)) {
//                    field.setAccessible(true); // Özel alanlara erişim için
//                    fields.add(field);
//                }
//            }
//            currentClass = currentClass.getSuperclass();
//        }
//
//        // 3. Sonucu önbelleğe al ve geri dön
//        fieldCache.put(type, fields);
//        return fields;
//    }
//
//
//    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    private static ArrayList<Field> getFields(Class<?> type) {
//        ArrayList<Field> list = new ArrayList<>();
//        for (Class<?> superClass = type; superClass != null && superClass != Object.class; superClass = superClass.getSuperclass()) {
//            Field[] fields = superClass.getDeclaredFields();
//            Collections.addAll(list, fields);
//        }
//        return list;
//    }
//
//    private static DbColumn fieldToDbColumn(Field field) {
//        int ordinal = 1010;
//        boolean isIdentity = false;
//        boolean isPrimaryKey = false;
//        boolean isNullable = true;
//
//        String fieldName = field.getName();
//        String columnName = fieldName;
//        String dataType = getSqliteDataType(field.getType());
//
//        DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
//        if (annotation != null) {
//            ordinal = annotation.ordinal();
//            isIdentity = annotation.isIdentity();
//            isPrimaryKey = annotation.isPrimaryKey();
//            isNullable = annotation.isNullable();
//            if (!annotation.name().isEmpty()) {
//                columnName = annotation.name();
//            }
//        } else if (fieldName.equals("id")) {
//            isPrimaryKey = true;
//            isIdentity = true;
//            isNullable = false;
//        }
//
//        return new DbColumn(ordinal, fieldName, columnName, dataType, isPrimaryKey, isIdentity, isNullable);
//    }
//
//    public static String getTableName(Class<?> type) {
//        String name = type.getSimpleName();
//        DbTableAnnotation tableAnnotation = type.getAnnotation(DbTableAnnotation.class);
//        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
//            name = tableAnnotation.name();
//        }
//        return name;
//    }
//
//    public static ArrayList<DbColumn> classToDbColumns(Class<?> type) {
//        ArrayList<DbColumn> columns = new ArrayList<>();
//        ArrayList<Field> fields = getFields(type);
//        for (Field field : fields) {
//            columns.add(fieldToDbColumn(field));
//        }
//        columns.sort(Comparator.comparingInt(DbColumn::getOrdinal));
//        return columns;
//    }
//
//    public static <T> T cursorToObject(Cursor cursor, Class<T> type) {
//        try {
//            T object = type.newInstance();
//            ArrayList<Field> fields = getFields(type);
//            for (Field field : fields) {
//                DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
//                String columnName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : field.getName();
//
//                int columnIndex = cursor.getColumnIndex(columnName);
//                if (columnIndex != -1) {
//                    field.setAccessible(true);
//
//                    Class<?> fieldType = field.getType();
//
//                    if (cursor.isNull(columnIndex)) {
//                        field.set(object, null);
//                    } else if (fieldType == int.class || fieldType == Integer.class) {
//                        field.set(object, cursor.getInt(columnIndex));
//                    } else if (fieldType == String.class) {
//                        field.set(object, cursor.getString(columnIndex));
//                    } else if (fieldType == boolean.class || fieldType == Boolean.class) {
//                        field.set(object, cursor.getInt(columnIndex) == 1);
//                    } else if (fieldType == long.class || fieldType == Long.class) {
//                        field.set(object, cursor.getLong(columnIndex));
//                    } else if (fieldType == float.class || fieldType == Float.class) {
//                        field.set(object, cursor.getFloat(columnIndex));
//                    } else if (fieldType == double.class || fieldType == Double.class) {
//                        field.set(object, cursor.getDouble(columnIndex));
//                    } else if (fieldType == byte[].class) {
//                        field.set(object, cursor.getBlob(columnIndex));
//                    } else if (fieldType == LocalDateTime.class) {
//                        String value = cursor.getString(columnIndex);
//                        if (value != null) {
//                            field.set(object, LocalDateTime.parse(value, dateTimeFormatter));
//                        }
//                    } else if (fieldType == LocalDate.class) {
//                        String value = cursor.getString(columnIndex);
//                        if (value != null) {
//                            field.set(object, LocalDate.parse(value, dateFormatter));
//                        }
//                    }
//                }
//            }
//            return object;
//        } catch (InstantiationException | IllegalAccessException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    public static ContentValues objectToContentValues(Object object) {
//        ContentValues contentValues = new ContentValues();
//        ArrayList<DbColumn> columns = classToDbColumns(object.getClass());
//        try {
//            for (DbColumn column : columns) {
//                if (!column.isIdentity()) {
//                    Field field = object.getClass().getDeclaredField(column.getFieldName());
//                    field.setAccessible(true);
//                    Object value = field.get(object);
//                    putInContentValues(contentValues, column.getColumnName(), value);
//                }
//            }
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            e.printStackTrace();
//        }
//        return contentValues;
//    }
//
//    public static void putInContentValues(ContentValues values, String key, Object value) {
//        if (value == null) {
//            values.putNull(key);
//            return;
//        }
//        if (value instanceof String) {
//            values.put(key, (String) value);
//        } else if (value instanceof Integer) {
//            values.put(key, (Integer) value);
//        } else if (value instanceof Long) {
//            values.put(key, (Long) value);
//        } else if (value instanceof Double) {
//            values.put(key, (Double) value);
//        } else if (value instanceof Float) {
//            values.put(key, (Float) value);
//        } else if (value instanceof Boolean) {
//            values.put(key, (Boolean) value ? 1 : 0);
//        } else if (value instanceof byte[]) {
//            values.put(key, (byte[]) value);
//        } else if (value instanceof LocalDateTime) {
//            values.put(key, ((LocalDateTime) value).format(dateTimeFormatter));
//        } else if (value instanceof LocalDate) {
//            values.put(key, ((LocalDate) value).format(dateFormatter));
//        }
//    }
//
//    public static String getPrimaryKeyColumnName(Class<?> type) {
//        for (DbColumn column : classToDbColumns(type)) {
//            if (column.isPrimaryKey()) {
//                return column.getColumnName();
//            }
//        }
//        return "id";
//    }
//
//    public static Object getPrimaryKeyValue(Object object) {
//        for (DbColumn column : classToDbColumns(object.getClass())) {
//            if (column.isPrimaryKey()) {
//                try {
//                    Field field = object.getClass().getDeclaredField(column.getFieldName());
//                    field.setAccessible(true);
//                    return field.get(object);
//                } catch (NoSuchFieldException | IllegalAccessException e) {
//                    e.printStackTrace();
//                    return null;
//                }
//            }
//        }
//        return null;
//    }
//
//    public static void setId(Object object, long id) {
//        for (DbColumn column : classToDbColumns(object.getClass())) {
//            if (column.isIdentity()) {
//                try {
//                    Field field = object.getClass().getDeclaredField(column.getFieldName());
//                    field.setAccessible(true);
//                    field.set(object, (int) id);
//                    return;
//                } catch (NoSuchFieldException | IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    private static String getSqliteDataType(Class<?> javaType) {
//        if (javaType == String.class) {
//            return "TEXT";
//        } else if (javaType == int.class || javaType == Integer.class || javaType == boolean.class || javaType == Boolean.class) {
//            return "INTEGER";
//        } else if (javaType == long.class || javaType == Long.class) {
//            return "INTEGER";
//        } else if (javaType == float.class || javaType == Float.class || javaType == double.class || javaType == Double.class) {
//            return "REAL";
//        } else if (javaType == byte[].class) {
//            return "BLOB";
//        } else if (javaType == LocalDateTime.class) {
//            return "TEXT";
//        } else if (javaType == LocalDate.class) {
//            return "TEXT";
//        }
//        return "TEXT";
//    }
//
//    /**
//     * Bir model sınıfını tarar ve DbColumn nesnelerinin bir listesini döndürür.
//     * Bu metodun sonuçları bir önbellekte saklanır.
//     */
////    public static List<DbColumn> classToDbColumns(Class<?> type) {
////        return dbColumnCache.computeIfAbsent(type, Mapper::createDbColumnsForClass);
////    }
//
//    private static List<DbColumn> createDbColumnsForClass(Class<?> type) {
//        List<DbColumn> columns = new ArrayList<>();
//        Field[] fields = type.getDeclaredFields();
//        for (Field field : fields) {
//            DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
//            if (annotation != null) {
//                // Fabrika metodu çağrısı
//                DbColumn dbColumn = createDbColumnFromField(field, annotation);
//                columns.add(dbColumn);
//            }
//        }
//        return columns;
//    }
//
//    /**
//     * Bir Field nesnesi ve onun annotation'ından bir DbColumn nesnesi oluşturan fabrika metodu.
//     */
//    private static DbColumn createDbColumnFromField(Field field, DbColumnAnnotation annotation) {
//        String columnName = annotation.name().isEmpty() ? field.getName() : annotation.name();
//        DbDataType dataType = getDbDataTypeFromFieldType(field.getType());
//
//        return new DbColumn(
//                annotation.ordinal(),
//                field.getName(),
//                columnName,
//                dataType.toString(),
//                annotation.isPrimaryKey(),
//                annotation.isIdentity(),
//                annotation.isNullable()
//        );
//    }
//
//    /**
//     * Java veri tipini SQLite veri tipine dönüştüren yardımcı metot.
//     * @param fieldType Java veri tipi (örn. String.class, long.class)
//     * @return SQLite'a uygun DbDataType
//     */
//    private static DbDataType getDbDataTypeFromFieldType(Class<?> fieldType) {
//        if (fieldType == String.class) {
//            return DbDataType.TEXT;
//        } else if (fieldType == int.class || fieldType == Integer.class || fieldType == long.class || fieldType == Long.class || fieldType == boolean.class || fieldType == Boolean.class) {
//            return DbDataType.INTEGER;
//        } else if (fieldType == double.class || fieldType == Double.class || fieldType == float.class || fieldType == Float.class) {
//            return DbDataType.REAL;
//        } else if (fieldType == byte[].class) {
//            return DbDataType.BLOB;
//        }
//        return DbDataType.TEXT; // Varsayılan değer
//    }
//
//}

