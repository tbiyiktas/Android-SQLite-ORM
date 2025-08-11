package lib.persistence.command.manipulation;

import lib.persistence.annotations.DbTableAnnotation;

import java.util.ArrayList;
import java.util.List;

public class DeleteSql {
    private Class<?> type;
    private String tableName;
    private List<String> whereClauses;
    private List<String> whereArgs;

    private DeleteSql(Class<?> type) {
        this.type = type;
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        this.tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        this.whereClauses = new ArrayList<>();
        this.whereArgs = new ArrayList<>();
    }

    public static DeleteSql build(Class<?> type) {
        return new DeleteSql(type);
    }

    public DeleteSql where() {
        return this;
    }

    public DeleteSql and() {
        return this;
    }

    public DeleteSql or() {
        return this;
    }

    public DeleteSql Equals(String column, Object value) {
        whereClauses.add(column + " = ?");
        whereArgs.add(String.valueOf(value));
        return this;
    }
    // Diğer WHERE koşulları da buraya eklenebilir.

    public String getTableName() {
        return tableName;
    }

    public String getWhereClause() {
        return String.join(" AND ", whereClauses);
    }

    public String[] getWhereArgs() {
        return whereArgs.toArray(new String[0]);
    }
}