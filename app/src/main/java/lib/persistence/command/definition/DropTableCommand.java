package lib.persistence.command.definition;

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
        String q = "DROP TABLE IF EXISTS " + safeId(tableName) + ";";
        return new DropTableCommand(q);
    }

    private static String safeId(String id) {
        String trimmed = id.trim();
        if (!trimmed.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Geçersiz identifier: " + id);
        return trimmed;
    }

    public String getQuery() { return query; }
}
