package lib.persistence.command.query;


import lib.persistence.profile.Mapper;
import lib.persistence.profile.RowMapper;
import android.database.Cursor;
import java.util.*;
/**
 * Tip güvenli SELECT builder. SQL ve argümanları ayrı tutar.
 */
public class Select<T> {
    private final Class<T> type;
    private final String table;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<String> whereArgs = new ArrayList<>();
    private final List<String> orderBy = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private boolean distinct = false;
    private String[] selectColumns = null; // null → *
    private RowMapper<T> rowMapper = null; // null → Mapper.cursorToObject


    private Select(Class<T> type) {
        this.type = type;
        this.table = Mapper.getTableName(type);
    }


    public static <T> Select<T> from(Class<T> type) { return new Select<>(type); }


    public Select<T> distinct() { this.distinct = true; return this; }
    public Select<T> columns(String... cols) { this.selectColumns = cols; return this; }


    public Select<T> where(String column, String op, Object value) {
        whereClauses.add(column + " " + op + " ?");
        whereArgs.add(String.valueOf(value));
        return this;
    }
    public Select<T> isNull(String column) { whereClauses.add(column + " IS NULL"); return this; }
    public Select<T> isNotNull(String column) { whereClauses.add(column + " IS NOT NULL"); return this; }


    public Select<T> orderByAsc(String column) { orderBy.add(column + " ASC"); return this; }
    public Select<T> orderByDesc(String column) { orderBy.add(column + " DESC"); return this; }


    public Select<T> limit(int n) { this.limit = n; return this; }
    public Select<T> offset(int n) { this.offset = n; return this; }


    public Select<T> mapWith(RowMapper<T> mapper) { this.rowMapper = mapper; return this; }


    // --- Compile ---
    public SelectQuery<T> compile() {
        return new SelectQuery<>(type, table, distinct, selectColumns,
                new ArrayList<>(whereClauses), new ArrayList<>(whereArgs),
                new ArrayList<>(orderBy), limit, offset, rowMapper);
    }
}