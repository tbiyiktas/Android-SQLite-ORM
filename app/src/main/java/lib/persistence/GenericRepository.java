// lib/persistence/GenericRepository.java
package lib.persistence;

import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import lib.persistence.command.manipulation.DeleteCommand;
import lib.persistence.command.manipulation.DeleteSql;
import lib.persistence.command.manipulation.InsertCommand;
import lib.persistence.command.manipulation.UpdateCommand;
import lib.persistence.command.manipulation.UpdateSql;
import lib.persistence.command.query.GetQuery;
import lib.persistence.command.query.Select;
import lib.persistence.command.query.SelectQuery;
import lib.persistence.profile.DbColumn;
import lib.persistence.profile.Mapper;

public abstract class GenericRepository<T> {
    protected final IDbContext  dbContext;
    protected final Class<T> type;

    protected GenericRepository(IDbContext ctx, Class<T> type) {
        this.dbContext = ctx;
        this.type = type;
    }

    // CREATE
    public void insert(T entity, DbCallback<T> cb) {
        dbContext.runDbOperation(db -> {
            InsertCommand cmd = InsertCommand.build(entity);
            long rowId = db.insert(cmd.getTableName(), null, cmd.getContentValues());
            if (rowId == -1) throw new Exception("Insert failed");

            // Identity PK'yi geri yaz
            ArrayList<DbColumn> cols = Mapper.classToDbColumns(type);
            for (DbColumn c : cols) {
                if (c.isPrimaryKey() && c.isIdentity()) {
                    Field f = Mapper.findField(type, c.getFieldName());
                    f.setAccessible(true);
                    Class<?> ft = f.getType();
                    if (ft == int.class || ft == Integer.class)      f.set(entity, (int) rowId);
                    else if (ft == long.class || ft == Long.class)   f.set(entity, rowId);
                    else                                             f.set(entity, rowId); // fallback
                    break;
                }
            }
            return new DbResult.Success<>(entity);
        }, cb, true);
    }

    // UPDATE (PK’lere göre)
    public void update(T entity, DbCallback<T> cb) {
        dbContext.runDbOperation(db -> {
            UpdateCommand cmd = UpdateCommand.build(entity);
            int n = db.update(cmd.getTableName(), cmd.getValues(), cmd.getWhereClause(), cmd.getWhereArgs());
            if (n <= 0) throw new Exception("Update affected 0 rows");
            return new DbResult.Success<>(entity);
        }, cb, true);
    }

    // UPDATE: UpdateSql builder ile (set/where)
    public void updateWith(UpdateSql sql, DbCallback<Integer> cb) {
        dbContext.runDbOperation(db -> {
            int n = db.update(sql.getTableName(), sql.getContentValues(), sql.getWhereClause(), sql.getWhereArgs());
            return new DbResult.Success<>(n);
        }, cb, true);
    }

    // DELETE (entity’nin PK değeriyle, bileşik PK destekli)
    public void delete(T entity, DbCallback<T> cb) {
        dbContext.runDbOperation(db -> {
            DeleteCommand cmd = DeleteCommand.build(entity);
            int n = db.delete(cmd.getTableName(), cmd.getWhereClause(), cmd.getWhereArgs());
            if (n <= 0) throw new Exception("Delete affected 0 rows");
            return new DbResult.Success<>(entity);
        }, cb, true);
    }

    // DELETE: doğrudan PK değer(ler)i ile
    public void deleteById(DbCallback<Integer> cb, Object... primaryKeyValues) {
        dbContext.runDbOperation(db -> {
            DeleteCommand cmd = DeleteCommand.build(type, primaryKeyValues);
            int n = db.delete(cmd.getTableName(), cmd.getWhereClause(), cmd.getWhereArgs());
            return new DbResult.Success<>(n);
        }, cb, true);
    }

    // DELETE: DeleteSql builder ile (koşullu silme)
    public void deleteWhere(DeleteSql sql, DbCallback<Integer> cb) {
        dbContext.runDbOperation(db -> {
            int n = db.delete(sql.getTableName(), sql.getWhereClause(), sql.getWhereArgs());
            return new DbResult.Success<>(n);
        }, cb, true);
    }

    // READ: getById (tek PK varsayımı – GetQuery güvenli & quoted)
    public void getById(Object id, DbCallback<T> cb) {
        dbContext.runDbOperation(db -> {
            GetQuery q = GetQuery.build(type, id);
            try (Cursor c = db.rawQuery(q.getQuery(), q.getArgs())) {
                if (c.moveToFirst()) {
                    T obj = Mapper.cursorToObject(c, type);
                    return new DbResult.Success<>(obj);
                } else {
                    return new DbResult.Success<>(null);
                }
            }
        }, cb, false);
    }

    // READ: hepsi
    public void selectAll(DbCallback<ArrayList<T>> cb) {
        selectWith(Select.from(type), cb);
    }

    // READ: Select builder ile
    public void selectWith(Select<T> builder, DbCallback<ArrayList<T>> cb) {
        SelectQuery<T> q = builder.compile();
        dbContext.runDbOperation(db -> {
            ArrayList<T> list = new ArrayList<>();
            try (Cursor c = db.rawQuery(q.getSql(), q.getArgs())) {
                while (c.moveToNext()) list.add(q.getRowMapperOrDefault().apply(c));
            }
            return new DbResult.Success<>(list);
        }, cb, false);
    }

    // Opsiyonel: ham sorgu
    public void rawQuery(String sql, String[] args, DbCallback<ArrayList<HashMap<String,String>>> cb) {
        dbContext.runDbOperation(db -> {
            ArrayList<HashMap<String,String>> rows = new ArrayList<>();
            try (Cursor c = db.rawQuery(sql, args)) {
                String[] names = c.getColumnNames();
                while (c.moveToNext()) {
                    HashMap<String,String> row = new HashMap<>();
                    for (String col : names) {
                        int idx = c.getColumnIndex(col);
                        row.put(col, c.isNull(idx) ? null : c.getString(idx));
                    }
                    rows.add(row);
                }
            }
            return new DbResult.Success<>(rows);
        }, cb, false);
    }
}
