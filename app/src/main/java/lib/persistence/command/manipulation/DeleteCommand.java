package lib.persistence.command.manipulation;

import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;

public class DeleteCommand {

    private String tableName;
    private String whereClause;
    private String[] whereArgs;

    private DeleteCommand(String tableName, String whereClause, String[] whereArgs) {
        this.tableName = tableName;
        this.whereClause = whereClause;
        this.whereArgs = whereArgs;
    }

    public static DeleteCommand build(Object object) {
        Class<?> type = object.getClass();
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();

        String primaryKeyColumn = Mapper.getPrimaryKeyColumnName(type);
        Object primaryKeyValue = Mapper.getPrimaryKeyValue(object);

        return new DeleteCommand(
                tableName,
                primaryKeyColumn + " = ?",
                new String[]{String.valueOf(primaryKeyValue)}
        );
    }

    public static DeleteCommand build(Class<?> type, Object primaryKeyValue) {
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        String primaryKeyColumn = Mapper.getPrimaryKeyColumnName(type);

        return new DeleteCommand(
                tableName,
                primaryKeyColumn + " = ?",
                new String[]{String.valueOf(primaryKeyValue)}
        );
    }

    public static DeleteCommand buildAll(Class<?> type) {
        DbTableAnnotation annotation = type.getAnnotation(DbTableAnnotation.class);
        String tableName = (annotation != null && !annotation.name().isEmpty()) ? annotation.name() : type.getSimpleName();
        return new DeleteCommand(tableName, null, null);
    }

    public String getTableName() {
        return tableName;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public String[] getWhereArgs() {
        return whereArgs;
    }
}