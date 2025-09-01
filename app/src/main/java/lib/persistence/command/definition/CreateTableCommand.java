package lib.persistence.command.definition;

import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.DbColumn;
import lib.persistence.profile.DbDataType;
import lib.persistence.profile.Mapper;

import java.util.ArrayList;
import java.util.StringJoiner;

public class CreateTableCommand {

    private final String query;

    private CreateTableCommand(String query) {
        this.query = query;
    }

    /** Basit kullanım: yalnızca entity'den tabloyu üretir. */
    public static CreateTableCommand build(Class<?> type) {
        return build(type, (String[]) null);
    }

    /**
     * Tablo-level constraint eklemek için (örn. FOREIGN KEY) kullan.
     * Ör: build(UserTodo.class, "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE")
     */
    public static CreateTableCommand build(Class<?> type, String... tableConstraints) {
        if (type == null) throw new IllegalArgumentException("type boş olamaz");

        // Tablo adı
        DbTableAnnotation ann = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (ann != null && !ann.name().trim().isEmpty())
                ? ann.name().trim()
                : type.getSimpleName();

        // Kolonlar
        ArrayList<DbColumn> cols = Mapper.classToDbColumns(type);
        if (cols.isEmpty()) {
            throw new IllegalStateException("Kolon tanımı bulunamadı: " + type.getName());
        }

        StringJoiner defs = new StringJoiner(", ");

        for (DbColumn c : cols) {
            StringBuilder d = new StringBuilder();
            d.append(c.getColumnName()).append(" ").append(toSqlType(c.getDataType()));

            // PRIMARY KEY / AUTOINCREMENT (yalnızca INTEGER PK'da)
            if (c.isPrimaryKey()) {
                d.append(" PRIMARY KEY");
                if (c.isIdentity() && c.getDataType() == DbDataType.INTEGER) {
                    d.append(" AUTOINCREMENT");
                }
            }

            // NOT NULL
            if (!c.isNullable()) {
                d.append(" NOT NULL");
            }

            defs.add(d.toString());
        }

        // Tablo-level constraint’ler (opsiyonel)
        if (tableConstraints != null) {
            for (String tc : tableConstraints) {
                if (tc != null && !tc.trim().isEmpty()) {
                    defs.add(tc.trim());
                }
            }
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + defs + ");";
        return new CreateTableCommand(sql);
    }

    private static String toSqlType(DbDataType t) {
        switch (t) {
            case INTEGER: return "INTEGER";
            case REAL:    return "REAL";
            case BLOB:    return "BLOB";
            case TEXT:
            default:      return "TEXT";
        }
    }

    public String getQuery() {
        return query;
    }
}
