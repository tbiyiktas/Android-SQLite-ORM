package lib.persistence.command.query;


import lib.persistence.ADbContext;
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.RowMapper;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQL SELECT sorgusu oluşturmak için Builder desenini uygulayan generic bir sınıf.
 * Tip güvenliği sağlar ve SQL enjeksiyonu zafiyetlerine karşı koruma sunar.
 *
 * @param <T> Sorgunun döndüreceği nesne tipi (örneğin: Todo, Person).
 */
public class Select<T> {

    private final Class<T> type;
    private final String tableName;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<String> whereArgs = new ArrayList<>();
    private final List<String> orderByClauses = new ArrayList<>();
    private final List<String> columnsToSelect = new ArrayList<>();
    private Integer limitCount;
    private Integer limitOffset;

    private Select(Class<T> type) {
        this.type = type;
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        this.tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        this.columnsToSelect.add("*");
    }

    public static <T> Select<T> from(Class<T> type) {
        return new Select<>(type);
    }

    // --- SELECT CLAUSE ---

    public Select<T> select(String... columns) {
        this.columnsToSelect.clear();
        if (columns != null && columns.length > 0) {
            Collections.addAll(this.columnsToSelect, columns);
        } else {
            this.columnsToSelect.add("*");
        }
        return this;
    }

    // --- WHERE CLAUSES (SQL Injection güvenli) ---

    public Select<T> where() {
        return this;
    }

    public Select<T> And() {
        // Okunabilirlik için bırakıldı. Koşullar otomatik olarak AND ile birleşiyor.
        return this;
    }

    public Select<T> Equals(String column, Object value) {
        return addCondition(column, "=", value);
    }

    public Select<T> NotEquals(String column, Object value) {
        return addCondition(column, "!=", value);
    }

    public Select<T> Like(String column, String value) {
        return addCondition(column, "LIKE", value);
    }

    public Select<T> GreaterThan(String column, Object value) {
        return addCondition(column, ">", value);
    }

    public Select<T> LessThan(String column, Object value) {
        return addCondition(column, "<", value);
    }

    public Select<T> GreaterThanOrEqualTo(String column, Object value) {
        return addCondition(column, ">=", value);
    }

    public Select<T> LessThanOrEqualTo(String column, Object value) {
        return addCondition(column, "<=", value);
    }

    public Select<T> IsNull(String column) {
        whereClauses.add(column + " IS NULL");
        return this;
    }

    public Select<T> IsNotNull(String column) {
        whereClauses.add(column + " IS NOT NULL");
        return this;
    }

    private Select<T> addCondition(String column, String operator, Object value) {
        whereClauses.add(column + " " + operator + " ?");
        whereArgs.add(String.valueOf(value));
        return this;
    }

    // --- ORDER BY CLAUSES ---

    public Select<T> orderBy(String column) {
        return orderBy(column, "ASC");
    }

    public Select<T> orderByDesc(String column) {
        return orderBy(column, "DESC");
    }

    private Select<T> orderBy(String column, String direction) {
        orderByClauses.add(column + " " + direction);
        return this;
    }

    // --- LIMIT / OFFSET CLAUSES ---

    public Select<T> limit(int count) {
        return limit(0, count);
    }

    public Select<T> limit(int offset, int count) {
        this.limitOffset = offset;
        this.limitCount = count;
        return this;
    }

    // --- FINAL QUERY BUILDER ---

    public String getQuery() {
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(String.join(", ", columnsToSelect));
        query.append(" FROM ").append(tableName);

        if (!whereClauses.isEmpty()) {
            query.append(" WHERE ");
            query.append(String.join(" AND ", whereClauses));
        }

        if (!orderByClauses.isEmpty()) {
            query.append(" ORDER BY ");
            query.append(String.join(", ", orderByClauses));
        }

        if (limitCount != null) {
            query.append(" LIMIT ").append(limitCount);
            if (limitOffset != null && limitOffset > 0) {
                query.append(" OFFSET ").append(limitOffset);
            }
        }

        return query.toString();
    }

    public String[] getWhereArgs() {
        return whereArgs.toArray(new String[0]);
    }

    public Class<T> getType() {
        return type;
    }
}