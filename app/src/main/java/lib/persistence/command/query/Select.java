package lib.persistence.command.query;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import lib.persistence.profile.Mapper;

import static lib.persistence.SqlNames.qCol;
import static lib.persistence.SqlNames.qId;

/**
 * Güvenli, parametreli ve tipli SELECT builder.
 * - from(Class<T>) -> tablo adı Mapper'dan gelir
 * - Identifier'lar backtick ile kaçışlanır ( `table`, `col` )
 * - Raw ifade gerekiyorsa columnRaw(...) veya expr(...) kullan
 */
public final class Select<T> {

    private final Class<T> type;
    private final String table; // RAW isim; SQL'de backtick'le kullanılacak
    private final List<String> columns = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<String> orderBys = new ArrayList<>();
    private final List<String> groupBys = new ArrayList<>();
    private final List<String> havingClauses = new ArrayList<>();
    private final List<String> args = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private boolean distinct;

    // Opsiyonel özel rowMapper (verilmezse Mapper.cursorToObject kullanılacak)
    private Function<Cursor, T> rowMapper;

    // ---------- CTOR ----------
    private Select(Class<T> type, String table) {
        if (type == null) throw new IllegalArgumentException("type is required");
        if (table == null || table.trim().isEmpty()) throw new IllegalArgumentException("table is required");
        this.type = type;
        this.table = table.trim();
    }

    // ---------- ENTRY ----------
    public static <T> Select<T> from(Class<T> type) {
        return new Select<>(type, Mapper.getTableName(type));
    }

    /** İstersen custom tablo adı/alias ile başlat (örn. view ya da alias) */
    public static <T> Select<T> from(Class<T> type, String tableNameOrAlias) {
        return new Select<>(type, tableNameOrAlias);
    }

    public Select<T> distinct() { this.distinct = true; return this; }

    /** Özel rowMapper ver (opsiyonel). */
    public Select<T> rowMapper(Function<Cursor, T> mapper) {
        this.rowMapper = mapper;
        return this;
    }

    // ---------- COLUMNS ----------
    /** Basit kolon adları (identifier). Gerekirse tablo.alan formatında ver; otomatik kaçışlanır. */
    public Select<T> columns(String... cols) {
        if (cols != null) {
            for (String c : cols) {
                if (c == null || c.trim().isEmpty()) continue;
                //columns.add(qColOrStar(c.trim()));
                String cc = c.trim();
                columns.add("*".equals(cc) ? "*" : qCol(cc)); // ← SqlNames
            }
        }
        return this;
    }

    /** Ham ifade/hesap (kaçış yapmaz). Örn: "strftime('%Y', `created_at`)" */
    public Select<T> columnRaw(String rawExpr) {
        if (rawExpr == null || rawExpr.trim().isEmpty()) throw new IllegalArgumentException("rawExpr required");
        columns.add(rawExpr.trim());
        return this;
    }

    // ---------- AGGREGATES ----------
    public Select<T> count() { columns.add("COUNT(*) AS " + qId("count")); return this; }

    public Select<T> count(String col, String alias) {
        requireCol(col);
        columns.add("COUNT(" + qCol(col) + ")" + aliasSql(alias));
        return this;
    }

    public Select<T> countDistinct(String col, String alias) {
        requireCol(col);
        columns.add("COUNT(DISTINCT " + qCol(col) + ")" + aliasSql(alias));
        return this;
    }

    public Select<T> sum(String col, String alias) { return aggFunc("SUM", col, alias); }
    public Select<T> avg(String col, String alias) { return aggFunc("AVG", col, alias); }
    public Select<T> min(String col, String alias) { return aggFunc("MIN", col, alias); }
    public Select<T> max(String col, String alias) { return aggFunc("MAX", col, alias); }

    /** Parametreli ham ifade (örn: "CASE WHEN `x` > ? THEN 1 ELSE 0 END", alias) */
    public Select<T> expr(String sqlExpr, String alias, Object... params) {
        if (sqlExpr == null || sqlExpr.trim().isEmpty())
            throw new IllegalArgumentException("sqlExpr required");
        validatePlaceholders(sqlExpr, params);
        columns.add("(" + sqlExpr + ")" + aliasSql(alias));
        bind(params);
        return this;
    }

    // ---------- WHERE ----------
    public Select<T> whereRaw(String clause, Object... params) {
        if (clause != null && !clause.trim().isEmpty()) {
            validatePlaceholders(clause, params);
            whereClauses.add("(" + clause.trim() + ")");
            bind(params);
        }
        return this;
    }

    public Select<T> whereEq(String col, Object val) {
        //return (val == null) ? whereNull(col) : whereRaw(qCol(col) + " = ?", val);
        return (val == null) ? whereNull(col) : whereRaw(qCol(col) + " = ?", val);
    }

    public Select<T> whereNe(String col, Object val) {
        //return (val == null) ? whereNotNull(col) : whereRaw(qCol(col) + " <> ?", val);
        return (val == null) ? whereNotNull(col) : whereRaw(qCol(col) + " <> ?", val);
    }

    public Select<T> whereGt(String col, Object val) { requireVal(val); return whereRaw(qCol(col) + " > ?", val); }
    public Select<T> whereGe(String col, Object val) { requireVal(val); return whereRaw(qCol(col) + " >= ?", val); }
    public Select<T> whereLt(String col, Object val) { requireVal(val); return whereRaw(qCol(col) + " < ?", val); }
    public Select<T> whereLe(String col, Object val) { requireVal(val); return whereRaw(qCol(col) + " <= ?", val); }
    public Select<T> whereLike(String col, String pattern) { requireCol(col); return whereRaw(qCol(col) + " LIKE ?", pattern); }

    public Select<T> whereNull(String col) {
        requireCol(col);
        whereClauses.add("(" + qCol(col) + " IS NULL)");
        return this;
    }

    public Select<T> whereNotNull(String col) {
        requireCol(col);
        whereClauses.add("(" + qCol(col) + " IS NOT NULL)");
        return this;
    }

    public Select<T> whereIn(String col, List<?> values) {
        requireCol(col);
        if (values == null || values.isEmpty()) return whereRaw("1 = 0");
        StringBuilder q = new StringBuilder(qCol(col)).append(" IN (");
        for (int i = 0; i < values.size(); i++) q.append(i == 0 ? "?" : ", ?");
        q.append(")");
        whereClauses.add("(" + q + ")");
        bind(values.toArray());
        return this;
    }

    public Select<T> and(String clause, Object... params) {
        if (whereClauses.isEmpty()) return whereRaw(clause, params);
        validatePlaceholders(clause, params);
        whereClauses.add("AND (" + clause.trim() + ")");
        bind(params);
        return this;
    }

    public Select<T> or(String clause, Object... params) {
        if (whereClauses.isEmpty()) return whereRaw(clause, params);
        validatePlaceholders(clause, params);
        whereClauses.add("OR (" + clause.trim() + ")");
        bind(params);
        return this;
    }

    // ---------- GROUP / HAVING / ORDER / LIMIT ----------
    public Select<T> groupBy(String... cols) {
        if (cols != null && cols.length > 0) {
            for (String c : cols) groupBys.add(qCol(c));
        }
        return this;
    }

    public Select<T> having(String clause, Object... params) {
        if (clause != null && !clause.trim().isEmpty()) {
            validatePlaceholders(clause, params);
            havingClauses.add("(" + clause.trim() + ")");
            bind(params);
        }
        return this;
    }

    public Select<T> orderBy(String col, boolean desc) {
        requireCol(col);
        orderBys.add(qCol(col) + (desc ? " DESC" : " ASC"));
        return this;
    }

    public Select<T> limit(int n) { this.limit = n; return this; }
    public Select<T> offset(int n) { this.offset = n; return this; }

    // ---------- BUILD ----------
    public SelectQuery<T> compile() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        if (distinct) sql.append("DISTINCT ");
        sql.append(columns.isEmpty() ? "*" : String.join(", ", columns));
        sql.append(" FROM ").append(qId(table));

//        if (!whereClauses.isEmpty()) {
//            sql.append(" WHERE ");
//            boolean first = true;
//            for (String c : whereClauses) {
//                if (first) { sql.append(c.replaceFirst("^(AND|OR)\\s+", "")); first = false; }
//                else { sql.append(' ').append(c); }
//            }
//        }

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ");
            boolean first = true;
            for (String c : whereClauses) {
                String part = c.trim();
                boolean hasOp = part.startsWith("AND ") || part.startsWith("OR ");
                if (first) {
                    // İlk parçada baştaki AND/OR varsa kırp
                    sql.append(hasOp ? part.substring(4) : part);
                    first = false;
                } else {
                    // Sonraki parçalarda op yoksa otomatik AND ekle
                    sql.append(' ').append(hasOp ? part : "AND " + part);
                }
            }
        }

        if (!groupBys.isEmpty())  sql.append(" GROUP BY ").append(String.join(", ", groupBys));

        if (!havingClauses.isEmpty()) {
            sql.append(" HAVING ");
            boolean first = true;
            for (String h : havingClauses) {
                if (first) { sql.append(h); first = false; }
                else { sql.append(" AND ").append(h); }
            }
        }

        if (!orderBys.isEmpty())  sql.append(" ORDER BY ").append(String.join(", ", orderBys));
        if (limit  != null)       sql.append(" LIMIT ").append(limit);
        if (offset != null)       sql.append(" OFFSET ").append(offset);

        String[] a = args.toArray(new String[0]);
        // ← Kritik: type'ı geçir
        return new SelectQuery<>(sql.toString(), a, this.type, rowMapper);
    }


    // ---------- HELPERS ----------
    private void bind(Object... params) {
        if (params == null) return;
        for (Object p : params) {
            if (p == null)
                throw new IllegalArgumentException("Null param passed to a '?' placeholder. Use whereNull()/whereNotNull().");
            //args.add(String.valueOf(p));
            if (p instanceof Boolean) {
                args.add(((Boolean) p) ? "1" : "0");  // boolean → "1"/"0"
            }
            else{
                args.add(String.valueOf(p));
            }
        }
    }

    private static void validatePlaceholders(String clause, Object... params) {
        int placeholders = clause.length() - clause.replace("?", "").length();
        int argCount = (params == null ? 0 : params.length);
        if (placeholders != argCount)
            throw new IllegalArgumentException("Placeholder count (" + placeholders + ") doesn't match args (" + argCount + ")");
    }

    private static void requireCol(String col) {
        if (col == null || col.trim().isEmpty())
            throw new IllegalArgumentException("column is required");
    }

    private static void requireVal(Object v) {
        if (v == null) throw new IllegalArgumentException("value is required");
    }

    /** Kolon identifier'ını (ve table.col şeklini) backtick'le kaçışlar. '*' ise olduğu gibi döner. */
//    private static String qCol(String col) {
//        String c = col.trim();
//        if ("*".equals(c)) return "*";
//        int dot = c.indexOf('.');
//        if (dot > 0 && dot < c.length() - 1) {
//            // table.col
//            return qId(c.substring(0, dot)) + "." + qId(c.substring(dot + 1));
//        }
//        return qId(c);
//    }

    /** Kolon listelerinde '*' destekler. */
    private static String qColOrStar(String col) {
        String c = col.trim();
        if ("*".equals(c)) return "*";
        return qCol(c);
    }

    /** Basit identifier (tablo/alias/kolon) için backtick. */
    private static String qId(String id) {
        String s = id.trim();
        // Eğer kullanıcının verdiği ifade zaten bir fonksiyon/ham ifade ise burada kullanma. (columnRaw/expr kullanmalı)
        if (s.contains("(") || s.contains(" ") || s.contains("`") || s.contains("\"")) return s;
        return "`" + s + "`";
    }

    private String aliasSql(String alias) {
        return (alias != null && !alias.trim().isEmpty()) ? " AS " + qId(alias.trim()) : "";
    }

    private Select<T> aggFunc(String fn, String col, String alias) {
        requireCol(col);
        columns.add(fn + "(" + qCol(col) + ")" + aliasSql(alias));
        return this;
    }


}
