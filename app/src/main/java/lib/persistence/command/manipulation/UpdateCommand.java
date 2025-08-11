package lib.persistence.command.manipulation;


import android.content.ContentValues;
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.DbColumn;
import lib.persistence.profile.Mapper;

import java.util.ArrayList;

public class UpdateCommand {
    private String tableName;
    private ContentValues contentValues;
    private String whereClause;
    private String[] whereArgs;

    private UpdateCommand(String tableName, ContentValues contentValues, String whereClause, String[] whereArgs) {
        this.tableName = tableName;
        this.contentValues = contentValues;
        this.whereClause = whereClause;
        this.whereArgs = whereArgs;
    }

    public static UpdateCommand build(Object object) {
        Class<?> type = object.getClass();
        String tableName = Mapper.getTableName(type);
        ContentValues contentValues = Mapper.objectToContentValues(object);

        // Birincil anahtar sütununu ContentVaalues'dan kaldır, çünkü WHERE koşulunda kullanılacak
        String primaryKeyColumn = Mapper.getPrimaryKeyColumnName(type);
        contentValues.remove(primaryKeyColumn);

        Object primaryKeyValue = Mapper.getPrimaryKeyValue(object);

        return new UpdateCommand(
                tableName,
                contentValues,
                primaryKeyColumn + " = ?",
                new String[]{String.valueOf(primaryKeyValue)}
        );
    }

    public String getTableName() {
        return tableName;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public String[] getWhereArgs() {
        return whereArgs;
    }
}