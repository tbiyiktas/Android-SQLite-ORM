package lib.persistence.profile;

public class DbColumn {
    private int ordinal;
    private String fieldName;
    private String columnName;
    private String dataType;
    private boolean isPrimaryKey;
    private boolean isIdentity;
    private boolean isNullable;

    public DbColumn(int ordinal, String fieldName, String columnName, String dataType, boolean isPrimaryKey, boolean isIdentity, boolean isNullable) {
        this.ordinal = ordinal;
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.dataType = dataType;
        this.isPrimaryKey = isPrimaryKey;
        this.isIdentity = isIdentity;
        this.isNullable = isNullable;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean isIdentity() {
        return isIdentity;
    }

    public boolean isNullable() {
        return isNullable;
    }
}