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
    private static final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();


    public static String getColumnName(Field field) {
        DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
        return (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : field.getName();
    }

    /**
     * Bir Cursor satırını, belirtilen tipteki bir nesneye dönüştürmek için
     * bir RowMapper döndürür. Bu RowMapper reflection cache kullanır.
     */
    public static <T> RowMapper<T> getRowMapper(Class<T> type) {
        return cursor -> {
            try {
                T item = type.getDeclaredConstructor().newInstance();
                List<Field> fields = getClassFields(type); // Önbelleklenmiş alanları kullan

                for (Field field : fields) {
                    // Sütun adını al
                    String columnName = getColumnName(field);
                    // Cursor'da o sütunun indeksini al
                    int columnIndex = cursor.getColumnIndex(columnName);

                    // --- BURADAKİ KONTROL ÇOK KRİTİK! ---
                    // Eğer sütun yoksa (getColumnIndex -1 döndürür), bu alana değer atamadan devam et.
                    if (columnIndex == -1) {
                        continue;
                    }

                    // Alanın tipine göre değeri Cursor'dan oku ve alana set et
                    Class<?> fieldType = field.getType();

                    if (fieldType == String.class) {
                        field.set(item, cursor.getString(columnIndex));
                    } else if (fieldType == int.class || fieldType == Integer.class) {
                        field.set(item, cursor.getInt(columnIndex));
                    } else if (fieldType == long.class || fieldType == Long.class) {
                        field.set(item, cursor.getLong(columnIndex));
                    } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                        // INTEGER 0 veya 1 olarak saklandığı varsayıldı
                        field.set(item, cursor.getInt(columnIndex) == 1);
                    } else if (fieldType == float.class || fieldType == Float.class) {
                        field.set(item, cursor.getFloat(columnIndex));
                    } else if (fieldType == double.class || fieldType == Double.class) {
                        field.set(item, cursor.getDouble(columnIndex));
                    } else if (fieldType == byte[].class) {
                        field.set(item, cursor.getBlob(columnIndex));
                    }
                    // Diğer tipler için mantık eklenebilir
                }
                return item;
            } catch (Exception e) {
                // Hata yönetimini iyileştirin. Loglama yapmak faydalı olacaktır.
                throw new IllegalStateException("Nesne eşleme hatası: " + type.getSimpleName(), e);
            }
        };
    }

    // --- Önbellekli yansıma metodu ---
    /**
     * Bir model sınıfına ait DbColumnAnnotation ile işaretlenmiş alanları bulur ve önbelleğe alır.
     *
     * @param type Alanları alınacak sınıf.
     * @return DbColumnAnnotation ile işaretlenmiş alanların listesi.
     */
    public static List<Field> getClassFields(Class<?> type) {
        // 1. Önbellekte varsa, doğrudan geri dön
        if (fieldCache.containsKey(type)) {
            return fieldCache.get(type);
        }

        // 2. Önbellekte yoksa, yansıma ile bul
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = type;

        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(DbColumnAnnotation.class)) {
                    field.setAccessible(true); // Özel alanlara erişim için
                    fields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // 3. Sonucu önbelleğe al ve geri dön
        fieldCache.put(type, fields);
        return fields;
    }


    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static ArrayList<Field> getFields(Class<?> type) {
        ArrayList<Field> list = new ArrayList<>();
        for (Class<?> superClass = type; superClass != null && superClass != Object.class; superClass = superClass.getSuperclass()) {
            Field[] fields = superClass.getDeclaredFields();
            Collections.addAll(list, fields);
        }
        return list;
    }

    private static DbColumn fieldToDbColumn(Field field) {
        int ordinal = 1010;
        boolean isIdentity = false;
        boolean isPrimaryKey = false;
        boolean isNullable = true;

        String fieldName = field.getName();
        String columnName = fieldName;
        String dataType = getSqliteDataType(field.getType());

        DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
        if (annotation != null) {
            ordinal = annotation.ordinal();
            isIdentity = annotation.isIdentity();
            isPrimaryKey = annotation.isPrimaryKey();
            isNullable = annotation.isNullable();
            if (!annotation.name().isEmpty()) {
                columnName = annotation.name();
            }
        } else if (fieldName.equals("id")) {
            isPrimaryKey = true;
            isIdentity = true;
            isNullable = false;
        }

        return new DbColumn(ordinal, fieldName, columnName, dataType, isPrimaryKey, isIdentity, isNullable);
    }

    public static String getTableName(Class<?> type) {
        String name = type.getSimpleName();
        DbTableAnnotation tableAnnotation = type.getAnnotation(DbTableAnnotation.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            name = tableAnnotation.name();
        }
        return name;
    }

    public static ArrayList<DbColumn> classToDbColumns(Class<?> type) {
        ArrayList<DbColumn> columns = new ArrayList<>();
        ArrayList<Field> fields = getFields(type);
        for (Field field : fields) {
            columns.add(fieldToDbColumn(field));
        }
        columns.sort(Comparator.comparingInt(DbColumn::getOrdinal));
        return columns;
    }

    public static <T> T cursorToObject(Cursor cursor, Class<T> type) {
        try {
            T object = type.newInstance();
            ArrayList<Field> fields = getFields(type);
            for (Field field : fields) {
                DbColumnAnnotation annotation = field.getAnnotation(DbColumnAnnotation.class);
                String columnName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : field.getName();

                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex != -1) {
                    field.setAccessible(true);

                    Class<?> fieldType = field.getType();

                    if (cursor.isNull(columnIndex)) {
                        field.set(object, null);
                    } else if (fieldType == int.class || fieldType == Integer.class) {
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
            }
            return object;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ContentValues objectToContentValues(Object object) {
        ContentValues contentValues = new ContentValues();
        ArrayList<DbColumn> columns = classToDbColumns(object.getClass());
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
            e.printStackTrace();
        }
        return contentValues;
    }

    public static void putInContentValues(ContentValues values, String key, Object value) {
        if (value == null) {
            values.putNull(key);
            return;
        }
        if (value instanceof String) {
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

    public static String getPrimaryKeyColumnName(Class<?> type) {
        for (DbColumn column : classToDbColumns(type)) {
            if (column.isPrimaryKey()) {
                return column.getColumnName();
            }
        }
        return "id";
    }

    public static Object getPrimaryKeyValue(Object object) {
        for (DbColumn column : classToDbColumns(object.getClass())) {
            if (column.isPrimaryKey()) {
                try {
                    Field field = object.getClass().getDeclaredField(column.getFieldName());
                    field.setAccessible(true);
                    return field.get(object);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    public static void setId(Object object, long id) {
        for (DbColumn column : classToDbColumns(object.getClass())) {
            if (column.isIdentity()) {
                try {
                    Field field = object.getClass().getDeclaredField(column.getFieldName());
                    field.setAccessible(true);
                    field.set(object, (int) id);
                    return;
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getSqliteDataType(Class<?> javaType) {
        if (javaType == String.class) {
            return "TEXT";
        } else if (javaType == int.class || javaType == Integer.class || javaType == boolean.class || javaType == Boolean.class) {
            return "INTEGER";
        } else if (javaType == long.class || javaType == Long.class) {
            return "INTEGER";
        } else if (javaType == float.class || javaType == Float.class || javaType == double.class || javaType == Double.class) {
            return "REAL";
        } else if (javaType == byte[].class) {
            return "BLOB";
        } else if (javaType == LocalDateTime.class) {
            return "TEXT";
        } else if (javaType == LocalDate.class) {
            return "TEXT";
        }
        return "TEXT";
    }


}