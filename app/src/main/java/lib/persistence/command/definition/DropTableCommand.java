package lib.persistence.command.definition;

import static lib.persistence.SqlNames.qId;

import lib.persistence.annotations.DbTableAnnotation;

public class DropTableCommand {

    private final String query;

    private DropTableCommand(String query) {
        this.query = query;
    }

    public static DropTableCommand build(Class<?> type) {
        DbTableAnnotation ann = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (ann != null && !ann.name().isEmpty())
                ? ann.name()
                : type.getSimpleName();
        return build(tableName);
    }

    public static DropTableCommand build(String tableName) {
        if (tableName == null || tableName.trim().isEmpty())
            throw new IllegalArgumentException("tableName zorunludur");
        String q = "DROP TABLE IF EXISTS " + qId(tableName) + ";";
        return new DropTableCommand(q);
    }

    public String getQuery() { return query; }
}
