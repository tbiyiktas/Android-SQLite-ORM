package lib.persistence.profile;


public class DbColumn {
    private final int ordinal;
    private final String fieldName;
    private final String columnName;
    private final DbDataType dataType;
    private final boolean primaryKey;
    private final boolean identity;
    private final boolean nullable;


    public DbColumn(int ordinal,
                    String fieldName,
                    String columnName,
                    DbDataType dataType,
                    boolean primaryKey,
                    boolean identity,
                    boolean nullable) {
        this.ordinal = ordinal;
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.dataType = dataType;
        this.primaryKey = primaryKey;
        this.identity = identity;
        this.nullable = nullable;
    }


    public int getOrdinal() { return ordinal; }
    public String getFieldName() { return fieldName; }
    public String getColumnName() { return columnName; }
    public DbDataType getDataType() { return dataType; }
    public boolean isPrimaryKey() { return primaryKey; }
    public boolean isIdentity() { return identity; }
    public boolean isNullable() { return nullable; }
}