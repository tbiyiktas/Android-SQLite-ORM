package lib.persistence.command.definition;

import lib.persistence.annotations.DbTableAnnotation;

public class CreateIndexCommand {

    private final String query;

    private CreateIndexCommand(String query) {
        this.query = query;
    }

    public static CreateIndexCommand build(Class<?> type, String indexName, boolean isUnique, String... columns) {
        if (indexName == null || indexName.trim().isEmpty())
            throw new IllegalArgumentException("indexName zorunludur");
        if (columns == null || columns.length == 0)
            throw new IllegalArgumentException("en az bir kolon belirtmelisiniz");

        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (annotation != null && !annotation.name().isEmpty())
                ? annotation.name()
                : type.getSimpleName();

        StringBuilder queryBuilder = new StringBuilder("CREATE ");
        if (isUnique) queryBuilder.append("UNIQUE ");
        queryBuilder.append("INDEX IF NOT EXISTS ")
                .append(safeId(indexName))
                .append(" ON ")
                .append(safeId(tableName))
                .append(" (");

        for (int i = 0; i < columns.length; i++) {
            String col = columns[i];
            if (col == null || col.trim().isEmpty())
                throw new IllegalArgumentException("geçersiz kolon adı");
            queryBuilder.append(safeId(col));
            if (i < columns.length - 1) queryBuilder.append(", ");
        }
        queryBuilder.append(");");

        return new CreateIndexCommand(queryBuilder.toString());
    }

    /** SQLite identifier için basit güvenlik filtresi (harf/rakam/_). İstersen kaldır. */
    private static String safeId(String id) {
        String trimmed = id.trim();
        if (!trimmed.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Geçersiz identifier: " + id);
        return trimmed;
    }

    public String getQuery() {
        return query;
    }
}
