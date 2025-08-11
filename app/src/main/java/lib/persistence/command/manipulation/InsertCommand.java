package lib.persistence.command.manipulation;

import android.content.ContentValues;
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;

import java.util.ArrayList;

public class InsertCommand {

    private String tableName;
    private ContentValues contentValues;

    private InsertCommand(String tableName, ContentValues contentValues) {
        this.tableName = tableName;
        this.contentValues = contentValues;
    }

    public static InsertCommand build(Object object) {
        Class<?> type = object.getClass();
        String tableName = Mapper.getTableName(type);
        ContentValues contentValues = Mapper.objectToContentValues(object);

        return new InsertCommand(tableName, contentValues);
    }

    public String getTableName() {
        return tableName;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }
}