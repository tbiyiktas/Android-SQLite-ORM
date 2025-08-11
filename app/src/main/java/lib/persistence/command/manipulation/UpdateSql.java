package lib.persistence.command.manipulation;

import android.content.ContentValues;
import lib.persistence.annotations.DbTableAnnotation;

import java.util.ArrayList;
import java.util.List;

public class UpdateSql {
    private Class<?> type;
    private String tableName;
    private ContentValues contentValues;
    private List<String> whereClauses;
    private List<String> whereArgs;

    private UpdateSql(Class<?> type) {
        this.type = type;
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        this.tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        this.contentValues = new ContentValues();
        this.whereClauses = new ArrayList<>();
        this.whereArgs = new ArrayList<>();
    }

    public static UpdateSql build(Class<?> type) {
        return new UpdateSql(type);
    }

    public UpdateSql set(String column, Object value) {
        // Mevcut Mapper.putInContentValues'ı kullanabiliriz
        // Mapper.putInContentValues(contentValues, column, value);
        // Basit bir örnek için doğrudan ekleyelim:
        contentValues.put(column, String.valueOf(value)); // Veya uygun tip dönüşümü
        return this;
    }

    public UpdateSql where() {
        return this;
    }

    public UpdateSql and() {
        return this;
    }

    public UpdateSql or() {
        return this;
    }

    public UpdateSql Equals(String column, Object value) {
        whereClauses.add(column + " = ?");
        whereArgs.add(String.valueOf(value));
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }

    public String getWhereClause() {
        return String.join(" AND ", whereClauses);
    }

    public String[] getWhereArgs() {
        return whereArgs.toArray(new String[0]);
    }
}