// lib/persistence/command/definition/CreateTableCommand.java
package lib.persistence.command.definition;

import static lib.persistence.SqlNames.qId;

import java.util.ArrayList;
import java.util.StringJoiner;

import lib.persistence.profile.DbColumn;
import lib.persistence.profile.DbDataType;
import lib.persistence.profile.Mapper;

public class CreateTableCommand {
    private final String query;

    private CreateTableCommand(String query) {
        this.query = query;
    }

    public static CreateTableCommand build(Class<?> type) {
        return build(type, (String[]) null);
    }

    public static CreateTableCommand build(Class<?> type, String... tableConstraints) {
        if (type == null) throw new IllegalArgumentException("type boş olamaz");

        String tableName = Mapper.getTableName(type); // annotation’dan geliyor
        ArrayList<DbColumn> cols = Mapper.classToDbColumns(type);
        if (cols.isEmpty()) throw new IllegalStateException("Kolon tanımı yok: " + type.getName());

        // PK’ları topla
        ArrayList<DbColumn> pks = new ArrayList<>();
        for (DbColumn c : cols) if (c.isPrimaryKey()) pks.add(c);

        StringJoiner defs = new StringJoiner(", ");
        boolean singleIntegerIdentityPk = (pks.size() == 1
                && pks.get(0).isIdentity()
                && pks.get(0).getDataType() == DbDataType.INTEGER);

        for (DbColumn c : cols) {
            StringBuilder d = new StringBuilder();
            //d.append(qId(c.getColumnName())).append(' ').append(toSqlType(c.getDataType()));
            d.append(qId(c.getColumnName())).append(' ').append(columnSqlType(c));

            // Sadece tek PK varsa ve sütun düzeyinde ifade etmek istiyorsak:
            if (pks.size() == 1 && pks.get(0) == c) {
                d.append(" PRIMARY KEY");
                if (singleIntegerIdentityPk) d.append(" AUTOINCREMENT");
                // NOT NULL: PK zaten NOT NULL kabul edilir; ayrıca eklemeye gerek yok.
            } else {
                // Diğer kolonlar için nullable kontrolü
                if (!c.isNullable()) d.append(" NOT NULL");
            }
            defs.add(d.toString());
        }

        // Bileşik PK varsa tablo düzeyi constraint ekle
        if (pks.size() > 1) {
            StringJoiner pkCols = new StringJoiner(", ");
            for (DbColumn pk : pks) pkCols.add(qId(pk.getColumnName()));
            defs.add("PRIMARY KEY (" + pkCols + ")");
        }

        // Opsiyonel tablo-level constraints (FK vs.)
        if (tableConstraints != null) {
            for (String tc : tableConstraints) {
                if (tc != null && !tc.trim().isEmpty()) defs.add(tc.trim());
            }
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + qId(tableName) + " (" + defs + ");";
        return new CreateTableCommand(sql);
    }

    // YENİ: Converter bildirimi varsa onu kullan; yoksa mevcut DbDataType -> SQL mapping
    private static String columnSqlType(DbColumn c) {
        String fromConverter = c.getSqliteType();
        if (fromConverter != null && !fromConverter.isEmpty()) return fromConverter;
        return toSqlType(c.getDataType());
    }

    private static String toSqlType(DbDataType t) {
        switch (t) {
            case INTEGER:
                return "INTEGER";
            case REAL:
                return "REAL";
            case BLOB:
                return "BLOB";
            case TEXT:
            default:
                return "TEXT";
        }
    }

    public String getQuery() {
        return query;
    }
}