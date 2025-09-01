package lib.persistence.command.query;

import static lib.persistence.SqlNames.qId;

import lib.persistence.profile.DbColumn;
import lib.persistence.profile.Mapper;

import java.util.ArrayList;

/**
 * Tekil kayıt okumak için basit SELECT … WHERE PK = ? LIMIT 1 sorgusu.
 * Not: Çoklu PK durumunda ilk PK kolonu kullanılır (ihtiyaç olursa overload eklenir).
 */
public class GetQuery {
    private final String query;
    private final String[] args;
    private final Class<?> type;

    private GetQuery(String query, String[] args, Class<?> type) {
        this.query = query;
        this.args = args;
        this.type = type;
    }

    public static GetQuery build(Class<?> type, Object id) {
        if (type == null) throw new IllegalArgumentException("type boş olamaz");
        if (id == null) throw new IllegalArgumentException("id boş olamaz");

        // Tablo adı
        String tableName = Mapper.getTableName(type);

        // İlk PK kolonunu bul
        ArrayList<DbColumn> cols = Mapper.classToDbColumns(type);
        DbColumn pk = null;
        for (DbColumn c : cols) {
            if (c.isPrimaryKey()) { pk = c; break; }
        }
        if (pk == null) throw new IllegalStateException("Primary key bulunamadı: " + type.getName());

        // Güvenli parametreli sorgu
        String sql = "SELECT * FROM " + qId(tableName) + " WHERE " + qId(pk.getColumnName()) + " = ? LIMIT 1";
        String[] whereArgs = new String[]{ String.valueOf(id) };

        return new GetQuery(sql, whereArgs, type);
    }

    public String getQuery() { return query; }
    public String[] getArgs() { return args; }
    public Class<?> getType() { return type; }
}
