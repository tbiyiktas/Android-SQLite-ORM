package lib.persistence.command.query;

import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;

public class GetQuery {
    private String query;
    private Class<?> type;

    private GetQuery(String query, Class<?> type) {
        this.query = query;
        this.type = type;
    }

    public static GetQuery build(Class<?> type, Object id) {
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        String primaryKeyColumn = Mapper.getPrimaryKeyColumnName(type);
        String query = String.format("SELECT * FROM %s WHERE %s = %s", tableName, primaryKeyColumn, String.valueOf(id));
        return new GetQuery(query, type);
    }

    public String getQuery() {
        return query;
    }

    public Class<?> getType() {
        return type;
    }
}