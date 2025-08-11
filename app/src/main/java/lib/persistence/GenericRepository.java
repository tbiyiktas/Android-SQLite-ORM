package lib.persistence;

import java.util.ArrayList;
import java.util.List;

import lib.persistence.command.query.SelectQuery;

public abstract class GenericRepository<T> {

    private final ADbContext dbContext;
    private final Class<T> type;

    public GenericRepository(ADbContext dbContext, Class<T> type) {
        this.dbContext = dbContext;
        this.type = type;
    }

    public void insert(T object, DbCallback<T> callback) {
        dbContext.insert(object, callback);
    }

    public void insertAll(List<T> objects, DbCallback<ArrayList<T>> callback) {
        dbContext.insertAll(objects, callback);
    }

    public void update(T object, DbCallback<T> callback) {
        dbContext.update(object, callback);
    }

    public void delete(T object, DbCallback<T> callback) {
        dbContext.delete(object, callback);
    }

    public void getById(Object id, DbCallback<T> callback) {
        dbContext.get(type, id, callback);
    }

    public void selectAll(DbCallback<ArrayList<T>> callback) {
        SelectQuery command = SelectQuery.build(type);
        dbContext.select(command, callback);
    }

    public void selectWhere(String whereClause, String[] whereArgs, DbCallback<ArrayList<T>> callback) {
        SelectQuery command = SelectQuery.build(type);
        command.addRawWhereClause(whereClause, whereArgs);
        dbContext.select(command, callback);
    }


    // Diğer özel sorgu metotları buraya eklenebilir.
}