package lib.persistence.command.query;


import android.database.Cursor;

import java.util.function.Function;

import lib.persistence.profile.Mapper;
/*

public class SelectQuery<T> {
    final Class<T> type;
    final String table;
    final boolean distinct;
    final String[] selectColumns; // null → *
    final List<String> whereClauses;
    final List<String> whereArgs;
    final List<String> orderBy;
    final Integer limit;
    final Integer offset;
    final RowMapper<T> rowMapper; // null → Mapper.cursorToObject


    public SelectQuery(Class<T> type,
                       String table,
                       boolean distinct,
                       String[] selectColumns,
                       List<String> whereClauses,
                       List<String> whereArgs,
                       List<String> orderBy,
                       Integer limit,
                       Integer offset,
                       RowMapper<T> rowMapper) {
        this.type = type; this.table = table; this.distinct = distinct;
        this.selectColumns = selectColumns; this.whereClauses = whereClauses;
        this.whereArgs = whereArgs; this.orderBy = orderBy;
        this.limit = limit; this.offset = offset; this.rowMapper = rowMapper;
    }


    public String getSql() {
        String sel = (selectColumns == null || selectColumns.length == 0)
                ? "*" : String.join(", ", selectColumns);
        StringBuilder sb = new StringBuilder("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        sb.append(sel).append(" FROM ").append(table);
        if (!whereClauses.isEmpty()) sb.append(" WHERE ").append(String.join(" AND ", whereClauses));
        if (!orderBy.isEmpty()) sb.append(" ORDER BY ").append(String.join(", ", orderBy));
        if (limit != null) sb.append(" LIMIT ").append(limit);
        if (offset != null) sb.append(" OFFSET ").append(offset);
        return sb.toString();
    }


    public String[] getArgs() { return whereArgs.toArray(new String[0]); }


    public java.util.function.Function<Cursor, T> getRowMapperOrDefault() {
        if (rowMapper != null) return c -> {
            try { return rowMapper.mapRow(c); } catch (Exception e) { throw new RuntimeException(e); }
        };
        return c -> Mapper.cursorToObject(c, type);
    }
}
*/


/**
 * SELECT derleme çıktısı: SQL, argümanlar ve opsiyonel satır eşleyici.
 * - rowMapper verilmezse ve type sağlanmışsa, Mapper.cursorToObject(...) varsayılan kullanılır.
 * - Her iki ctor da desteklenir:
 *     new SelectQuery<>(sql, args, rowMapper)
 *     new SelectQuery<>(sql, args, type, rowMapper)
 */
public final class SelectQuery<T> {

    private final String sql;
    private final String[] args;
    private final Function<Cursor, T> rowMapper; // null olabilir
    private final Class<T> type;                  // null olabilir (tip verilmeden ctor kullanılırsa)

    /** Tip vermeden: sadece özel rowMapper ile. */
    public SelectQuery(String sql, String[] args, Function<Cursor, T> rowMapper) {
        this(sql, args, null, rowMapper);
    }

    /** Tip vererek: rowMapper null ise Mapper.cursorToObject(...) varsayılanı kullanılır. */
    public SelectQuery(String sql, String[] args, Class<T> type, Function<Cursor, T> rowMapper) {
        if (sql == null || sql.trim().isEmpty()) throw new IllegalArgumentException("sql is required");
        this.sql = sql;
        this.args = (args == null) ? new String[0] : args;
        this.type = type;
        this.rowMapper = rowMapper;
    }

    public String getSql() {
        return sql;
    }

    public String[] getArgs() {
        return args;
    }

    /**
     * Row mapper:
     * - Eğer özel mapper verildiyse onu döndürür.
     * - Değilse ve type sağlanmışsa Mapper.cursorToObject(...) kullanan bir mapper döndürür.
     * - İkisi de yoksa IllegalStateException atar (tip tahmini yapılamaz).
     */
    public Function<Cursor, T> getRowMapperOrDefault() {
        if (rowMapper != null) return rowMapper;
        if (type != null) {
            return c -> Mapper.cursorToObject(c, type);
        }
        throw new IllegalStateException(
                "No rowMapper provided and type is null; cannot map Cursor to T. " +
                        "Use SelectQuery(sql,args,type,null) or provide a rowMapper."
        );
    }


}
