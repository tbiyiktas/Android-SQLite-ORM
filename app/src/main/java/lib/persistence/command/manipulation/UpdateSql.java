// lib/persistence/command/manipulation/UpdateSql.java
package lib.persistence.command.manipulation;

import android.content.ContentValues;
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;
import lib.persistence.profile.DbColumn;

import java.util.ArrayList;
import java.util.List;

public class UpdateSql {
    private final Class<?> type;
    private final String tableName;
    private final ContentValues contentValues = new ContentValues();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<String> whereArgs = new ArrayList<>();
    private String pendingOp = "AND";

    private UpdateSql(Class<?> type) {
        this.type = type;
        DbTableAnnotation ann = type.getAnnotation(DbTableAnnotation.class);
        this.tableName = (ann != null && !ann.name().isEmpty()) ? ann.name() : type.getSimpleName();
    }

    public static UpdateSql build(Class<?> type) {
        if (type == null) throw new IllegalArgumentException("type boş olamaz");
        return new UpdateSql(type);
    }

    /** SET column = value (tip eşleme merkezi: Mapper) */
    public UpdateSql set(String column, Object value) {
        if (column == null || column.trim().isEmpty())
            throw new IllegalArgumentException("column zorunludur");
        // Kolonu meta’dan bul, tipine göre doğru put yap:
        DbColumn col = Mapper.getColumnByName(type, column.trim());
        Mapper.putInContentValues(contentValues, col, value);
        return this;
    }

    public UpdateSql where() { return this; }
    public UpdateSql and() { this.pendingOp = "AND"; return this; }
    public UpdateSql or()  { this.pendingOp = "OR";  return this; }

    /** WHERE `column` = ? (backtick’li kolon) */
    public UpdateSql Equals(String column, Object value) {
        if (column == null || column.trim().isEmpty())
            throw new IllegalArgumentException("column zorunludur");
        String clause = q(column.trim()) + " = ?";
        if (!whereClauses.isEmpty()) clause = pendingOp + " " + clause;
        whereClauses.add(clause);
        whereArgs.add(String.valueOf(value));
        this.pendingOp = "AND";
        return this;
    }

    private static String q(String id) { return "`" + id + "`"; }

    public String getTableName() { return tableName; } // db.update(...) için backtick YOK
    public ContentValues getContentValues() { return contentValues; }
    public String getWhereClause() {
        if (whereClauses.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < whereClauses.size(); i++) {
            String part = whereClauses.get(i);
            if (i == 0 && (part.startsWith("AND ") || part.startsWith("OR "))) sb.append(part.substring(4));
            else if (i == 0) sb.append(part);
            else sb.append(" ").append(part);
        }
        return sb.toString();
    }
    public String[] getWhereArgs() { return whereArgs.toArray(new String[0]); }
}
