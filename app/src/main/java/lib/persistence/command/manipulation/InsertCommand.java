package lib.persistence.command.manipulation;


import android.content.ContentValues;

import lib.persistence.mapping.Mapper;


public class InsertCommand {
    private final String tableName;
    private final ContentValues contentValues;


    private InsertCommand(String tableName, ContentValues contentValues) {
        this.tableName = tableName;
        this.contentValues = contentValues;
    }


    public static InsertCommand build(Object entity) {
        return new InsertCommand(
                Mapper.getTableName(entity.getClass()),
                Mapper.objectToContentValues(entity)
        );
    }


    public String getTableName() {
        return tableName;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }
}