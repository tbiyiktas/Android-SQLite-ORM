// lib/persistence/command/manipulation/UpdateCommand.java
package lib.persistence.command.manipulation;

import android.content.ContentValues;
import lib.persistence.profile.DbColumn;
import lib.persistence.profile.Mapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UpdateCommand {
    private final String tableName;    // RAW (backticksiz)
    private final ContentValues values;
    private final String whereClause;  // `col` = ? AND ...
    private final String[] whereArgs;

    private UpdateCommand(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        this.tableName = tableName; this.values = values; this.whereClause = whereClause; this.whereArgs = whereArgs;
    }

    public static UpdateCommand build(Object entity) {
        Class<?> type = entity.getClass();
        String table = Mapper.getTableName(type);                // RAW
        ArrayList<DbColumn> cols = Mapper.classToDbColumns(type);

        ArrayList<DbColumn> pks = new ArrayList<>();
        for (DbColumn c : cols) if (c.isPrimaryKey()) pks.add(c);
        if (pks.isEmpty()) throw new IllegalStateException("Update requires PK");

        // identity hariç tüm alanları CV'ye alır; sonra PK'ları da CV'den çıkarırız
        ContentValues cv = Mapper.objectToContentValues(entity);
        for (DbColumn pkCol : pks) cv.remove(pkCol.getColumnName());

        String where = pks.stream().map(c -> q(c.getColumnName()) + " = ?").collect(Collectors.joining(" AND "));
        String[] args = new String[pks.size()];
        try {
            for (int i = 0; i < pks.size(); i++) {
                Field f = Mapper.findField(type, pks.get(i).getFieldName());
                f.setAccessible(true);
                Object v = f.get(entity);
                args[i] = v == null ? null : String.valueOf(v);
            }
        } catch (Exception e) { throw new RuntimeException(e); }

        return new UpdateCommand(table, cv, where, args); // table RAW
    }

    private static String q(String id) { return "`" + id + "`"; }

    public String getTableName() { return tableName; }    // RAW
    public ContentValues getValues() { return values; }
    public String getWhereClause() { return whereClause; }
    public String[] getWhereArgs() { return whereArgs; }
}
