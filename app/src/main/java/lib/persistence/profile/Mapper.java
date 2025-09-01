package lib.persistence.profile;


import android.content.ContentValues;
import android.database.Cursor;


import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


import lib.persistence.annotations.DbColumnAnnotation;
import lib.persistence.annotations.DbTableAnnotation;

public final class Mapper {
    private static final Map<Class<?>, List<DbColumn>> COLUMNS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String> TABLE_NAME_CACHE = new ConcurrentHashMap<>();


    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;


    private Mapper() {}

    // --- Table & Columns ---
    public static String getTableName(Class<?> type) {
        return TABLE_NAME_CACHE.computeIfAbsent(type, t -> {
            DbTableAnnotation ann = t.getAnnotation(DbTableAnnotation.class);
            if (ann == null || ann.name().trim().isEmpty())
                throw new IllegalStateException("@DbTableAnnotation(name) zorunludur: " + t.getName());
            return ann.name().trim();
        });
    }

    public static ArrayList<DbColumn> classToDbColumns(Class<?> type) {
        List<DbColumn> cols = COLUMNS_CACHE.computeIfAbsent(type, Mapper::scanColumns);
// Sıralama garantisi (ordinal’a göre)
        ArrayList<DbColumn> copy = new ArrayList<>(cols);
        copy.sort(Comparator.comparingInt(DbColumn::getOrdinal));
        return copy;
    }

    private static List<DbColumn> scanColumns(Class<?> type) {
        ArrayList<DbColumn> list = new ArrayList<>();

        Class<?> cur = type;
        while (cur != null && cur != Object.class) {
            for (Field f : cur.getDeclaredFields()) {
                DbColumnAnnotation ann = f.getAnnotation(DbColumnAnnotation.class);
                if (ann == null) continue;
                f.setAccessible(true);
                String columnName = ann.name().isEmpty() ? f.getName() : ann.name();
                DbDataType dataType = toDbDataType(f.getType());
                list.add(new DbColumn(
                        ann.ordinal(),
                        f.getName(),
                        columnName,
                        dataType,
                        ann.isPrimaryKey(),
                        ann.isIdentity(),
                        ann.isNullable()
                ));
            }
            cur = cur.getSuperclass();
        }
        return list;
    }

    private static DbDataType toDbDataType(Class<?> javaType) {
        if (javaType == int.class || javaType == Integer.class ||
                javaType == long.class || javaType == Long.class ||
                javaType == boolean.class || javaType == Boolean.class) {
            return DbDataType.INTEGER;
        }
        if (javaType == float.class || javaType == Float.class ||
                javaType == double.class || javaType == Double.class) {
            return DbDataType.REAL;
        }
        if (javaType == byte[].class) return DbDataType.BLOB;
// LocalDate/LocalDateTime string olarak tutulacak
        if (javaType == LocalDate.class || javaType == LocalDateTime.class) return DbDataType.TEXT;
        return DbDataType.TEXT; // default
    }

    // --- Object → ContentValues ---
    public static ContentValues objectToContentValues(Object entity) {
        if (entity == null) throw new IllegalArgumentException("entity null");
        Class<?> type = entity.getClass();
        ContentValues cv = new ContentValues();

        for (DbColumn col : classToDbColumns(type)) {
            // Identity alanlar DB tarafından set edilir → atla
            if (col.isIdentity()) continue;

            Object value;
            try {
                Field f = findField(type, col.getFieldName());
                f.setAccessible(true);
                value = f.get(entity);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Alan okunamadı: " + col.getFieldName(), e);
            }

            // Tek noktadan tip yazımı
            putInContentValues(cv, col, value);
        }
        return cv;
    }

    public static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> cur = type;
        while (cur != null && cur != Object.class) {
            try { return cur.getDeclaredField(fieldName); } catch (NoSuchFieldException ignored) {}
            cur = cur.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

//    private static void putValue(ContentValues cv, DbColumn c, Object val) {
//        if (val == null) {
//            cv.putNull(c.getColumnName());
//            return;
//        }
//        switch (c.getDataType()) {
//            case INTEGER:
//                if (val instanceof Boolean) cv.put(c.getColumnName(), ((Boolean) val) ? 1 : 0);
//                else if (val instanceof Number) cv.put(c.getColumnName(), ((Number) val).longValue());
//                else cv.put(c.getColumnName(), Long.parseLong(val.toString()));
//                break;
//            case REAL:
//                if (val instanceof Number) cv.put(c.getColumnName(), ((Number) val).doubleValue());
//                else cv.put(c.getColumnName(), Double.parseDouble(val.toString()));
//                break;
//            case BLOB:
//                cv.put(c.getColumnName(), (byte[]) val); break;
//            case TEXT:
//            default:
//                if (val instanceof LocalDate) cv.put(c.getColumnName(), ((LocalDate) val).format(ISO_DATE));
//                else if (val instanceof LocalDateTime) cv.put(c.getColumnName(), ((LocalDateTime) val).format(ISO_DATE_TIME));
//                else cv.put(c.getColumnName(), String.valueOf(val));
//        }
//    }

    // --- Cursor → Object ---
    public static <T> T cursorToObject(Cursor cursor, Class<T> type) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            ArrayList<DbColumn> cols = classToDbColumns(type);
            for (DbColumn c : cols) {
                int idx = cursor.getColumnIndex(c.getColumnName());
                if (idx < 0) continue; // seçilmemiş olabilir
                setFieldValue(instance, c, cursor, idx);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("cursorToObject hata", e);
        }
    }

    private static <T> void setFieldValue(T instance, DbColumn c, Cursor cursor, int idx) throws Exception {
        Field f = findField(instance.getClass(), c.getFieldName());
        f.setAccessible(true);
        if (cursor.isNull(idx)) { f.set(instance, null); return; }


        switch (c.getDataType()) {
            case INTEGER:
                Class<?> ft = f.getType();
                if (ft == boolean.class || ft == Boolean.class) f.set(instance, cursor.getInt(idx) != 0);
                else if (ft == int.class || ft == Integer.class) f.set(instance, cursor.getInt(idx));
                else if (ft == long.class || ft == Long.class) f.set(instance, cursor.getLong(idx));
                else f.set(instance, cursor.getLong(idx));
                break;
            case REAL:
                if (f.getType() == float.class || f.getType() == Float.class) f.set(instance, (float) cursor.getDouble(idx));
                else f.set(instance, cursor.getDouble(idx));
                break;
            case BLOB:
                f.set(instance, cursor.getBlob(idx));
                break;
            case TEXT:
            default:
                if (f.getType() == LocalDate.class) f.set(instance, LocalDate.parse(cursor.getString(idx), ISO_DATE));
                else if (f.getType() == LocalDateTime.class) f.set(instance, LocalDateTime.parse(cursor.getString(idx), ISO_DATE_TIME));
                else f.set(instance, cursor.getString(idx));
        }
    }


    // --- YENİ: Kolonu ada göre bul (columnName veya fieldName)
    public static DbColumn getColumnByName(Class<?> type, String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("column/field adı boş olamaz");
        String n = name.trim();
        for (DbColumn c : classToDbColumns(type)) {
            if (c.getColumnName().equalsIgnoreCase(n) || c.getFieldName().equalsIgnoreCase(n)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Kolon bulunamadı: " + name + " (type=" + type.getName() + ")");
    }

    // --- YENİ: ContentValues'a, kolon tipine göre güvenli yaz
    public static void putInContentValues(ContentValues cv, DbColumn col, Object val) {
        if (cv == null) throw new IllegalArgumentException("cv null olamaz");
        if (col == null) throw new IllegalArgumentException("col null olamaz");

        String key = col.getColumnName();
        if (val == null) { cv.putNull(key); return; }

        switch (col.getDataType()) {
            case INTEGER:
                if (val instanceof Boolean) {
                    cv.put(key, ((Boolean) val) ? 1 : 0);
                } else if (val instanceof Number) {
                    // Number → long (SQLite INTEGER)
                    cv.put(key, ((Number) val).longValue());
                } else {
                    cv.put(key, Long.parseLong(String.valueOf(val)));
                }
                break;

            case REAL:
                if (val instanceof Number) {
                    cv.put(key, ((Number) val).doubleValue());
                } else {
                    cv.put(key, Double.parseDouble(String.valueOf(val)));
                }
                break;

            case BLOB:
                if (!(val instanceof byte[]))
                    throw new IllegalArgumentException("BLOB kolonuna byte[] dışında tip yazılamaz: " + key);
                cv.put(key, (byte[]) val);
                break;

            case TEXT:
            default:
                if (val instanceof LocalDate) {
                    cv.put(key, ((LocalDate) val).format(ISO_DATE));
                } else if (val instanceof LocalDateTime) {
                    cv.put(key, ((LocalDateTime) val).format(ISO_DATE_TIME));
                } else if (val instanceof Enum<?>) {
                    cv.put(key, ((Enum<?>) val).name()); // Enum'u TEXT olarak sakla
                } else {
                    cv.put(key, String.valueOf(val));
                }
        }
    }

    // --- YENİ: isimden kolonu bulup yaz (kısa yol)
    public static void putInContentValues(ContentValues cv, Class<?> type, String columnName, Object val) {
        DbColumn col = getColumnByName(type, columnName);
        putInContentValues(cv, col, val);
    }


    /** Sınıf hiyerarşisinde alanı bul (üst sınıflarda da arar). */
//    public static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
//        Class<?> cur = type;
//        while (cur != null && cur != Object.class) {
//            try { return cur.getDeclaredField(fieldName); }
//            catch (NoSuchFieldException ignored) { cur = cur.getSuperclass(); }
//        }
//        throw new NoSuchFieldException(fieldName);
//    }

    /** (İsteğe bağlı) PK kolonlarını ordinal sırasına göre döndürür. */
    public static java.util.List<DbColumn> getPrimaryKeyColumns(Class<?> type) {
        java.util.ArrayList<DbColumn> pks = new java.util.ArrayList<>();
        for (DbColumn c : classToDbColumns(type)) if (c.isPrimaryKey()) pks.add(c);
        return pks; // classToDbColumns zaten ordinal’a göre sıralı döner
    }
}