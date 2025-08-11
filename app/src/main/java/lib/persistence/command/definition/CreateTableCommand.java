package lib.persistence.command.definition;


import java.util.ArrayList;

import lib.persistence.profile.DbColumn;
import lib.persistence.profile.Mapper;
import java.util.stream.Collectors;

public class CreateTableCommand {
    private Class<?> type;
    private String tableName;
    private String query;

    private CreateTableCommand() {
    }

    public static CreateTableCommand build(Class<?> type) {
        CreateTableCommand command = new CreateTableCommand();
        command.type = type;
        command.tableName = Mapper.getTableName(type);

        ArrayList<DbColumn> columns = Mapper.classToDbColumns(type);

        // Her bir sütun için SQL tanımını oluşturur ve listeye ekler
        ArrayList<String> columnDefinitions = columns.stream()
                .map(column -> {
                    StringBuilder columnDef = new StringBuilder();
                    columnDef.append(column.getColumnName()).append(" ").append(column.getDataType());
                    if (column.isPrimaryKey()) {
                        columnDef.append(" PRIMARY KEY");
                        // Sadece primary key ise AUTOINCREMENT eklenir
                        if (column.isIdentity()) {
                            columnDef.append(" AUTOINCREMENT");
                        }
                    }
                    if (!column.isNullable()) {
                        columnDef.append(" NOT NULL");
                    }
                    return columnDef.toString();
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Tüm sütun tanımlarını birleştirir
        String columnsString = String.join(", ", columnDefinitions);

        // Nihai sorguyu oluşturur
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("CREATE TABLE IF NOT EXISTS ")
                .append(command.tableName).append(" (")
                .append(columnsString)
                .append(");");

        command.query = queryBuilder.toString();
        return command;
    }

    public Class<?> getType() {
        return type;
    }

    public String getTableName() {
        return tableName;
    }

    public String getQuery() {
        return query;
    }
}

/*
public class CreateTableCommand {
    private Class<?> type;
    private String tableName;
    private String query;

    private CreateTableCommand() {
    }

    public static CreateTableCommand build(Class<?> type) {
        CreateTableCommand command = new CreateTableCommand();
        command.type = type;
        command.tableName = Mapper.getTableName(type);

        ArrayList<DbColumn> columns = Mapper.classToDbColumns(type);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("CREATE TABLE IF NOT EXISTS ")
                .append(command.tableName).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            DbColumn column = columns.get(i);
            queryBuilder.append(column.getColumnName()).append(" ").append(column.getDataType());

            if (column.isPrimaryKey()) {
                queryBuilder.append(" PRIMARY KEY");
            }
            if (column.isIdentity()) {
                queryBuilder.append(" AUTOINCREMENT");
            }
            if (!column.isNullable()) {
                queryBuilder.append(" NOT NULL");
            }

            if (i < columns.size() - 1) {
                queryBuilder.append(", ");
            }
        }
        queryBuilder.append(");");

        command.query = queryBuilder.toString();
        return command;
    }

    public Class<?> getType() {
        return type;
    }

    public String getTableName() {
        return tableName;
    }

    public String getQuery() {
        return query;
    }
}
*/