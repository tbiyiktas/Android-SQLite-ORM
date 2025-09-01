package lib.persistence.command.query;


import android.database.Cursor;
import lib.persistence.profile.Mapper;
import lib.persistence.profile.RowMapper;


import java.util.*;
import java.util.stream.Collectors;


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