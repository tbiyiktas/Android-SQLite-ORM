// lib/persistence/command/manipulation/DeleteCommand.java
package lib.persistence.command.manipulation;

import static lib.persistence.SqlNames.qCol;
import static lib.persistence.SqlNames.qId;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import lib.persistence.annotations.DbConverterAnnotation;
import lib.persistence.converters.ConverterRegistry;
import lib.persistence.converters.TypeConverter;
import lib.persistence.profile.DbColumn;
import lib.persistence.profile.Mapper;

/**
 * DELETE komutu derleyicisi:
 * - entity'den (build(entity)) PK değerlerini okuyarak
 * - veya Class + PK değerleri (build(Class, pkValues...)) ile
 * bileşik PK dahil güvenli WHERE üretir.
 * Tablolar ve kolonlar backtick ile quote edilir (UpdateCommand ile tutarlı).
 */
public class DeleteCommand {

    private final String tableName;   // quoted: `table`
    private final String whereClause; // örn: `pk1` = ? AND `pk2` = ?
    private final String[] whereArgs; // PK değerleri sırasıyla

    private DeleteCommand(String tableName, String whereClause, String[] whereArgs) {
        this.tableName = tableName;
        this.whereClause = whereClause;
        this.whereArgs = whereArgs;
    }

    /** Entity örneğinden bileşik PK ile DELETE. */
    public static DeleteCommand build(Object entity) {
        if (entity == null) throw new IllegalArgumentException("entity null olamaz");
        Class<?> type = entity.getClass();

        String rawTable = Mapper.getTableName(type);
        List<DbColumn> pks = Mapper.getPrimaryKeyColumns(type);
        if (pks.isEmpty()) throw new IllegalStateException("Primary key tanımı yok: " + type.getName());

        String where = pks.stream()
                .map(c -> qCol(c.getColumnName()) + " = ?")
                .collect(Collectors.joining(" AND "));

        String[] args = new String[pks.size()];
//        try {
//            for (int i = 0; i < pks.size(); i++) {
//                Field f = Mapper.findField(type, pks.get(i).getFieldName());
//                f.setAccessible(true);
//                Object v = f.get(entity);
//                if (v == null) throw new IllegalStateException("PK değeri null olamaz: " + pks.get(i).getFieldName());
//                args[i] = String.valueOf(v);
//            }
//        } catch (ReflectiveOperationException e) {
//            throw new RuntimeException("PK değer(ler)i okunamadı", e);
//        }

        try {
            for (int i = 0; i < pks.size(); i++) {
                DbColumn pk = pks.get(i);
                Field f = Mapper.findField(type, pk.getFieldName());
                f.setAccessible(true);
                Object v = f.get(entity);
                if (v == null) throw new IllegalStateException("PK değeri null olamaz: " + pk.getFieldName());
                // PK alanında converter varsa DB değerine çevir
                DbConverterAnnotation ann = f.getAnnotation(DbConverterAnnotation.class);
                if (ann != null) {
                    TypeConverter<?, ?> conv = ConverterRegistry.getOrCreate(ann.converter());
                    @SuppressWarnings({"rawtypes","unchecked"})
                    Object dbVal = ((TypeConverter) conv).toDatabaseValue(v);
                    args[i] = (dbVal == null) ? null : String.valueOf(dbVal);
                } else {
                    args[i] = String.valueOf(v);
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return new DeleteCommand(qId(rawTable), where, args);
    }

    /**
     * Class + PK değerleriyle DELETE. Bileşik PK için değerleri ordinal PK sırasıyla verin.
     * Örn: (pk1Value, pk2Value, ...)
     */
    public static DeleteCommand build(Class<?> type, Object... primaryKeyValues) {
        if (type == null) throw new IllegalArgumentException("type null olamaz");

        String rawTable = Mapper.getTableName(type);
        List<DbColumn> pks = Mapper.getPrimaryKeyColumns(type);
        if (pks.isEmpty()) throw new IllegalStateException("Primary key tanımı yok: " + type.getName());

        if (primaryKeyValues == null || primaryKeyValues.length != pks.size()) {
            String expected = pks.stream().map(DbColumn::getColumnName).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("PK değeri sayısı uyuşmuyor. Beklenen: " + pks.size() + " [" + expected + "]");
        }

        String where = pks.stream()
                .map(c -> qCol(c.getColumnName()) + " = ?")
                .collect(Collectors.joining(" AND "));

        String[] args = new String[pks.size()];
//        for (int i = 0; i < pks.size(); i++) {
//            Object v = primaryKeyValues[i];
//            if (v == null) throw new IllegalArgumentException("PK değeri null olamaz: " + pks.get(i).getColumnName());
//            args[i] = String.valueOf(v);
//        }

        for (int i = 0; i < pks.size(); i++) {
            Object v = primaryKeyValues[i];
            if (v == null) throw new IllegalArgumentException("PK değeri null olamaz: " + pks.get(i).getColumnName());
            // Burada çağıran taraf DB tipini verebilir; tip dönüştürmeye zorlamıyoruz
            args[i] = String.valueOf(v);
        }

        return new DeleteCommand(qId(rawTable), where, args);
    }

    /** Tüm satırları sil (WHERE yok) – dikkatli kullanın. */
    public static DeleteCommand buildAll(Class<?> type) {
        if (type == null) throw new IllegalArgumentException("type null olamaz");
        String rawTable = Mapper.getTableName(type);
        return new DeleteCommand(qId(rawTable), null, null);
    }


    public String getTableName() { return tableName; }
    public String getWhereClause() { return whereClause; }
    public String[] getWhereArgs() { return whereArgs; }
}
