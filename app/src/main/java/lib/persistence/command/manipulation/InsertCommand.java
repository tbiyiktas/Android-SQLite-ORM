package lib.persistence.command.manipulation;

import android.content.ContentValues;
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.Mapper;

import java.util.ArrayList;


public class InsertCommand {

    private final String tableName;
    private final ContentValues contentValues;

    private InsertCommand(String tableName, ContentValues contentValues) {
        this.tableName = tableName;
        this.contentValues = contentValues;
    }

    /**
     * Verilen nesneden bir InsertCommand oluşturur.
     * Tablo adını ve ContentValues'ı Mapper sınıfını kullanarak elde eder.
     *
     * @param object Eklenecek nesne (model).
     * @return Yeni bir InsertCommand örneği.
     */
    public static InsertCommand build(Object object) {
        // Mapper'dan tablo adını al
        String tableName = Mapper.getTableName(object.getClass());
        // Mapper'dan Content Values'ı al (burası önemli!)
        ContentValues contentValues = Mapper.objectToContentValues(object);

        // Eğer contentValues boşsa veya bir hata varsa, bunu erkenden yakalamak faydalıdır.
        if (contentValues.size() == 0) {
            throw new IllegalArgumentException("InsertCommand: Eklenecek veri bulunamadı. Nesne boş veya eşleştirilecek alan yok.");
        }

        return new InsertCommand(tableName, contentValues);
    }

    public String getTableName() {
        return tableName;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }
}

/*
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
*/