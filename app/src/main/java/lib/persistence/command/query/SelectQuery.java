package lib.persistence.command.query;


import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;
import java.util.ArrayList;
import java.util.Collections;

public class SelectQuery {

    private Class<?> type;
    private String tableName;
    private StringBuilder queryBuilder;
    private ArrayList<String> whereClauses;
    private ArrayList<String> whereArgs;

    private SelectQuery(Class<?> type) {
        this.type = type;
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        this.tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        this.queryBuilder = new StringBuilder("SELECT * FROM " + tableName);
        this.whereClauses = new ArrayList<>();
        this.whereArgs = new ArrayList<>();
    }

    public static SelectQuery build(Class<?> type) {
        return new SelectQuery(type);
    }

    public SelectQuery where() {
        return this;
    }

    public SelectQuery Equals(String column, Object value) {
        whereClauses.add(column + " = ?");
        whereArgs.add(String.valueOf(value));
        return this;
    }

    public SelectQuery and() {
        // whereClauses zaten AND ile birleştirilecek
        return this;
    }

    public String getQuery() {
        if (!whereClauses.isEmpty()) {
            queryBuilder.append(" WHERE ");
            queryBuilder.append(String.join(" AND ", whereClauses));
        }
        return queryBuilder.toString();
    }

    public String[] getWhereArgs() {
        return whereArgs.toArray(new String[0]);
    }

    public Class<?> getType() {
        return type;
    }

    // Yeni eklenen metot: Dışarıdan ham where koşulları ve argümanları eklemek için
    public SelectQuery addRawWhereClause(String whereClause, String... args) {
        if (whereClause != null && !whereClause.isEmpty()) {
            whereClauses.add("(" + whereClause + ")");
        }
        if (args != null) {
            Collections.addAll(whereArgs, args);
        }
        return this;
    }
}