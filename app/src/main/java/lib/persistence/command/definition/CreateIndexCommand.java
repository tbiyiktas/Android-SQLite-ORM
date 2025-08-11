package lib.persistence.command.definition;



import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;

public class CreateIndexCommand {

    private final String query;

    private CreateIndexCommand(String query) {
        this.query = query;
    }

    public static CreateIndexCommand build(Class<?> type, String indexName, boolean isUnique, String... columns) {
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();

        StringBuilder queryBuilder = new StringBuilder("CREATE ");
        if (isUnique) {
            queryBuilder.append("UNIQUE ");
        }
        queryBuilder.append("INDEX IF NOT EXISTS ");
        queryBuilder.append(indexName).append(" ON ").append(tableName).append(" (");

        for (int i = 0; i < columns.length; i++) {
            queryBuilder.append(columns[i]);
            if (i < columns.length - 1) {
                queryBuilder.append(", ");
            }
        }
        queryBuilder.append(");");

        return new CreateIndexCommand(queryBuilder.toString());
    }

    public String getQuery() {
        return query;
    }
}