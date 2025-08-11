package lib.persistence.command.query;


import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;
import java.util.ArrayList;
import java.util.Collections;
import lib.persistence.annotations.DbTableAnnotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lib.persistence.annotations.DbTableAnnotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQL SELECT sorgusu oluşturmak için Builder desenini uygulayan genel bir sınıf.
 * Tip güvenliğini sağlamak için generic <T> tipini kullanır.
 *
 * @param <T> Sorgunun döndüreceği nesne tipi (örneğin: Todo, Person).
 */
public class SelectQuery<T> {

    private final Class<T> type;
    private final String tableName;
    private final List<String> whereClauses;
    private final List<String> whereArgs;
    private final List<String> orderByClauses;
    private final List<String> columnsToSelect;
    private Integer limitCount;
    private Integer limitOffset;

    private SelectQuery(Class<T> type) {
        this.type = type;
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        this.tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        this.whereClauses = new ArrayList<>();
        this.whereArgs = new ArrayList<>();
        this.orderByClauses = new ArrayList<>();
        this.columnsToSelect = new ArrayList<>();
        // Varsayılan olarak tüm sütunları seçer
        this.columnsToSelect.add("*");
    }

    /**
     * SELECT sorgusu oluşturmak için statik bir başlangıç metodu.
     *
     * @param type Sorgu nesnesinin tipi.
     * @param <T>  Genel tip.
     * @return Yeni bir SelectQuery<T> örneği.
     */
    public static <T> SelectQuery<T> build(Class<T> type) {
        return new SelectQuery<>(type);
    }

    // --- SELECT CLAUSE ---

    /**
     * Sorgulanacak sütunları belirler.
     * "SELECT *" yerine bu metotla belirli sütunları seçebilirsiniz.
     *
     * @param columns Seçilecek sütun adları.
     * @return SelectQuery<T> örneği.
     */
    public SelectQuery<T> select(String... columns) {
        this.columnsToSelect.clear();
        if (columns != null && columns.length > 0) {
            Collections.addAll(this.columnsToSelect, columns);
        } else {
            this.columnsToSelect.add("*");
        }
        return this;
    }

    // --- WHERE CLAUSES (SQL Injection güvenli) ---

    /**
     * WHERE koşulunu başlatır.
     *
     * @return SelectQuery<T> örneği.
     */
    public SelectQuery<T> where() {
        return this;
    }

    public SelectQuery<T> Equals(String column, Object value) {
        return addCondition(column, "=", value);
    }

    public SelectQuery<T> NotEquals(String column, Object value) {
        return addCondition(column, "!=", value);
    }

    public SelectQuery<T> GreaterThan(String column, Object value) {
        return addCondition(column, ">", value);
    }

    public SelectQuery<T> LessThan(String column, Object value) {
        return addCondition(column, "<", value);
    }

    public SelectQuery<T> IsNull(String column) {
        whereClauses.add(column + " IS NULL");
        return this;
    }

    public SelectQuery<T> IsNotNull(String column) {
        whereClauses.add(column + " IS NOT NULL");
        return this;
    }

    public SelectQuery<T> And() {
        // Bu metot sadece okunabilirlik için var. Koşullar zaten AND ile birleşiyor.
        return this;
    }

    private SelectQuery<T> addCondition(String column, String operator, Object value) {
        whereClauses.add(column + " " + operator + " ?");
        whereArgs.add(String.valueOf(value));
        return this;
    }

    // --- ORDER BY CLAUSES ---

    public SelectQuery<T> orderBy(String column) {
        return orderBy(column, "ASC");
    }

    public SelectQuery<T> orderByDesc(String column) {
        return orderBy(column, "DESC");
    }

    private SelectQuery<T> orderBy(String column, String direction) {
        orderByClauses.add(column + " " + direction);
        return this;
    }

    // --- LIMIT / OFFSET CLAUSES ---

    public SelectQuery<T> limit(int count) {
        return limit(0, count);
    }

    public SelectQuery<T> limit(int offset, int count) {
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

    // --- RAW CLAUSES ---

    public SelectQuery<T> addRawWhereClause(String whereClause, String... args) {
        if (whereClause != null && !whereClause.isEmpty()) {
            this.whereClauses.add("(" + whereClause + ")");
        }
        if (args != null) {
            Collections.addAll(this.whereArgs, args);
        }
        return this;
    }
}


///**
// * SQL SELECT sorgusu oluşturmak için Builder desenini uygulayan genel bir sınıf.
// * Tip güvenliğini sağlamak için generic <T> tipini kullanır.
// *
// * @param <T> Sorgunun döndüreceği nesne tipi (örneğin: Todo, Person).
// */
//public class SelectQuery<T> {
//
//    private final Class<T> type;
//    private final String tableName;
//    private StringBuilder queryBuilder;
//    private final List<String> whereClauses;
//    private final List<String> whereArgs;
//    private List<String> columnsToSelect;
//
//    private SelectQuery(Class<T> type) {
//        this.type = type;
//        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
//        this.tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
//        this.whereClauses = new ArrayList<>();
//        this.whereArgs = new ArrayList<>();
//        this.columnsToSelect = new ArrayList<>();
//        // Varsayılan olarak tüm sütunları seçer
//        this.columnsToSelect.add("*");
//    }
//
//    /**
//     * SELECT sorgusu oluşturmak için statik bir başlangıç metodu.
//     *
//     * @param type Sorgu nesnesinin tipi.
//     * @param <T>  Genel tip.
//     * @return Yeni bir SelectQuery<T> örneği.
//     */
//    public static <T> SelectQuery<T> build(Class<T> type) {
//        return new SelectQuery<>(type);
//    }
//
//    /**
//     * Sorgulanacak sütunları belirler.
//     * "SELECT *" yerine bu metotla belirli sütunları seçebilirsiniz.
//     *
//     * @param columns Seçilecek sütun adları.
//     * @return SelectQuery<T> örneği.
//     */
//    public SelectQuery<T> select(String... columns) {
//        this.columnsToSelect.clear();
//        if (columns != null && columns.length > 0) {
//            Collections.addAll(this.columnsToSelect, columns);
//        } else {
//            this.columnsToSelect.add("*");
//        }
//        return this;
//    }
//
//    /**
//     * WHERE koşulunu başlatır.
//     *
//     * @return SelectQuery<T> örneği.
//     */
//    public SelectQuery<T> where() {
//        return this;
//    }
//
//    /**
//     * "column = value" şeklinde bir koşul ekler.
//     *
//     * @param column Sütun adı.
//     * @param value  Eşitlik için değer.
//     * @return SelectQuery<T> örneği.
//     */
//    public SelectQuery<T> Equals(String column, Object value) {
//        whereClauses.add(column + " = ?");
//        whereArgs.add(String.valueOf(value));
//        return this;
//    }
//
//    /**
//     * "AND" bağlacı ile bir koşul eklemek için kullanılır.
//     * Bu metot iş yapmaz, sadece Builder zincirinin okunabilirliğini artırır.
//     *
//     * @return SelectQuery<T> örneği.
//     */
//    public SelectQuery<T> and() {
//        return this;
//    }
//
//    /**
//     * SQL sorgusunun tamamını döndürür.
//     *
//     * @return Tamamlanmış SQL sorgusu.
//     */
//    public String getQuery() {
//        StringBuilder query = new StringBuilder("SELECT ");
//        query.append(String.join(", ", columnsToSelect));
//        query.append(" FROM ").append(tableName);
//
//        if (!whereClauses.isEmpty()) {
//            query.append(" WHERE ");
//            query.append(String.join(" AND ", whereClauses));
//        }
//
//        return query.toString();
//    }
//
//    /**
//     * Sorgu argümanlarını döndürür.
//     *
//     * @return Argümanların dizisi.
//     */
//    public String[] getWhereArgs() {
//        return whereArgs.toArray(new String[0]);
//    }
//
//    /**
//     * Sorgunun hedef tipini döndürür.
//     *
//     * @return Hedef tipin Class<T> nesnesi.
//     */
//    public Class<T> getType() {
//        return type;
//    }
//
//    /**
//     * Dışarıdan ham bir WHERE koşulu ve argümanları ekler.
//     *
//     * @param whereClause Eklenecek ham WHERE koşulu.
//     * @param args        Koşulun argümanları.
//     * @return SelectQuery<T> örneği.
//     */
//    public SelectQuery<T> addRawWhereClause(String whereClause, String... args) {
//        if (whereClause != null && !whereClause.isEmpty()) {
//            this.whereClauses.add("(" + whereClause + ")");
//        }
//        if (args != null) {
//            Collections.addAll(this.whereArgs, args);
//        }
//        return this;
//    }
//}

/*
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
*/