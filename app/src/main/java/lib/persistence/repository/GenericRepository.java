// lib/persistence/GenericRepository.java
package lib.persistence.repository;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
import lib.persistence.core.CancellableFuture;
import lib.persistence.core.DbResult;
import lib.persistence.core.IDbContext;
import lib.persistence.core.IDbContext.DbWork;
import lib.persistence.mapping.DbColumn;
import lib.persistence.mapping.Mapper;

public abstract class GenericRepository<T> {
    protected final IDbContext dbContext;
    protected final Class<T> type;

    protected GenericRepository(IDbContext ctx, Class<T> type) {
        this.dbContext = ctx;
        this.type = type;
    }

    // --- küçük yardımcılar (overload belirsizliğini engeller) ---
    private <R> CancellableFuture<DbResult<R>> runWrite(DbWork<R> work) {
        return dbContext.runDbOperation(work, true);
    }
    private <R> CancellableFuture<DbResult<R>> runRead(DbWork<R> work) {
        return dbContext.runDbOperation(work, false);
    }

    // CREATE
    public CancellableFuture<DbResult<T>> insert(T entity) {
        return runWrite((DbWork<T>) db -> {
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
                    if (ft == int.class || ft == Integer.class) f.set(entity, (int) rowId);
                    else if (ft == long.class || ft == Long.class) f.set(entity, rowId);
                    else f.set(entity, rowId); // fallback
                    break;
                }
            }
            return new DbResult.Success<>(entity);
        });
    }

    // UPDATE (PK’lere göre)
    public CancellableFuture<DbResult<T>> update(T entity) {
        return runWrite((DbWork<T>) db -> {
            UpdateCommand cmd = UpdateCommand.build(entity);
            int n = db.update(cmd.getTableName(), cmd.getValues(), cmd.getWhereClause(), cmd.getWhereArgs());
            if (n <= 0) throw new Exception("Update affected 0 rows");
            return new DbResult.Success<>(entity);
        });
    }

    // UPDATE: UpdateSql builder ile (set/where)
    public CancellableFuture<DbResult<Integer>> updateWith(UpdateSql sql) {
        return runWrite((DbWork<Integer>) db -> {
            int n = db.update(sql.getTableName(), sql.getContentValues(), sql.getWhereClause(), sql.getWhereArgs());
            return new DbResult.Success<>(n);
        });
    }

    // DELETE (entity’nin PK değeriyle, bileşik PK destekli)
    public CancellableFuture<DbResult<T>> delete(T entity) {
        return runWrite((DbWork<T>) db -> {
            DeleteCommand cmd = DeleteCommand.build(entity);
            int n = db.delete(cmd.getTableName(), cmd.getWhereClause(), cmd.getWhereArgs());
            if (n <= 0) throw new Exception("Delete affected 0 rows");
            return new DbResult.Success<>(entity);
        });
    }

    // DELETE: doğrudan PK değer(ler)i ile
    public CancellableFuture<DbResult<Integer>> deleteById(Object... primaryKeyValues) {
        return runWrite((DbWork<Integer>) db -> {
            DeleteCommand cmd = DeleteCommand.build(type, primaryKeyValues);
            int n = db.delete(cmd.getTableName(), cmd.getWhereClause(), cmd.getWhereArgs());
            return new DbResult.Success<>(n);
        });
    }

    // DELETE: DeleteSql builder ile (koşullu silme)
    public CancellableFuture<DbResult<Integer>> deleteWhere(DeleteSql sql) {
        return runWrite((DbWork<Integer>) db -> {
            int n = db.delete(sql.getTableName(), sql.getWhereClause(), sql.getWhereArgs());
            return new DbResult.Success<>(n);
        });
    }

    // READ: getById (tek PK varsayımı – GetQuery güvenli & quoted)
    public CancellableFuture<DbResult<T>> getById(Object id) {
        return runRead((DbWork<T>) db -> {
            GetQuery q = GetQuery.build(type, id);
            try (Cursor c = db.rawQuery(q.getQuery(), q.getArgs())) {
                if (c.moveToFirst()) {
                    T obj = Mapper.cursorToObject(c, type);
                    return new DbResult.Success<>(obj);
                } else {
                    return new DbResult.Success<>(null);
                }
            }
        });
    }

    // READ: hepsi
    public CancellableFuture<DbResult<ArrayList<T>>> selectAll() {
        return selectWith(Select.from(type));
    }

    // READ: Select builder ile
    public CancellableFuture<DbResult<ArrayList<T>>> selectWith(Select<T> builder) {
        SelectQuery<T> q = builder.compile();
        return runRead((DbWork<ArrayList<T>>) db -> {
            ArrayList<T> list = new ArrayList<>();
            try (Cursor c = db.rawQuery(q.getSql(), q.getArgs())) {
                while (c.moveToNext()) list.add(q.getRowMapperOrDefault().apply(c));
            }
            return new DbResult.Success<>(list);
        });
    }

    // Opsiyonel: ham sorgu
    public CancellableFuture<DbResult<ArrayList<HashMap<String, String>>>> rawQuery(String sql, String[] args) {
        return runRead((DbWork<ArrayList<HashMap<String, String>>>) db -> {
            ArrayList<HashMap<String, String>> rows = new ArrayList<>();
            try (Cursor c = db.rawQuery(sql, args)) {
                String[] names = c.getColumnNames();
                while (c.moveToNext()) {
                    HashMap<String, String> row = new HashMap<>();
                    for (String col : names) {
                        int idx = c.getColumnIndex(col);
                        row.put(col, c.isNull(idx) ? null : c.getString(idx));
                    }
                    rows.add(row);
                }
            }
            return new DbResult.Success<>(rows);
        });
    }

    // --- Tx-aware iç yardımcılar (Transactions.run içinde kullanmak için) ---
    protected T insertTx(SQLiteDatabase db, T entity) throws Exception {
        var cmd = lib.persistence.command.manipulation.InsertCommand.build(entity);
        long rowId = db.insert(cmd.getTableName(), null, cmd.getContentValues());
        if (rowId == -1) throw new Exception("Insert failed");
        var cols = Mapper.classToDbColumns(type);
        for (var c : cols) {
            if (c.isPrimaryKey() && c.isIdentity()) {
                var f = Mapper.findField(type, c.getFieldName());
                f.setAccessible(true);
                Class<?> ft = f.getType();
                if (ft == int.class || ft == Integer.class) f.set(entity, (int) rowId);
                else if (ft == long.class || ft == Long.class) f.set(entity, rowId);
                break;
            }
        }
        return entity;
    }

    protected T updateTx(SQLiteDatabase db, T entity) throws Exception {
        var cmd = lib.persistence.command.manipulation.UpdateCommand.build(entity);
        int n = db.update(cmd.getTableName(), cmd.getValues(), cmd.getWhereClause(), cmd.getWhereArgs());
        if (n <= 0) throw new Exception("Update affected 0 rows");
        return entity;
    }

    protected int deleteTx(SQLiteDatabase db, T entity) throws Exception {
        var cmd = lib.persistence.command.manipulation.DeleteCommand.build(entity);
        int n = db.delete(cmd.getTableName(), cmd.getWhereClause(), cmd.getWhereArgs());
        if (n <= 0) throw new Exception("Delete affected 0 rows");
        return n;
    }
}
