package lib.persistence.command.definition;

import lib.persistence.annotations.DbTableAnnotation;

public class DropTableCommand {
    private final String query;

    private DropTableCommand(String query) {
        this.query = query;
    }

    public static DropTableCommand build(Class<?> type) {
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        return new DropTableCommand("DROP TABLE IF EXISTS " + tableName);
    }

    public String getQuery() {
        return query;
    }
}