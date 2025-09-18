package com.example.adbkit.repositories;

import android.database.sqlite.SQLiteDatabase;

import com.example.adbkit.entities.Todo;

import lib.persistence.repository.GenericRepository;
import lib.persistence.core.IDbContext;

import lib.concurrent.CancellableFuture;
import lib.persistence.core.DbResult;
import lib.concurrent.AndroidFutures;
import com.example.adbkit.events.SimpleEventBus;
import com.example.adbkit.events.TodoCreatedEvent;
import com.example.adbkit.events.TodoUpdatedEvent;
import com.example.adbkit.events.TodoDeletedEvent;

public class TodoRepository extends GenericRepository<Todo> {

    public TodoRepository(IDbContext context) {
        super(context, Todo.class);
    }

    @Override
    public CancellableFuture<DbResult<Todo>> insert(Todo entity) {
        CancellableFuture<DbResult<Todo>> f = super.insert(entity);
        // Başarılıysa UI thread’te event publish et
        AndroidFutures.thenAcceptOnMain(f, res -> {
            if (res.isSuccess() && res.getData() != null) {
                SimpleEventBus.post(new TodoCreatedEvent(res.getData()));
            }
        });
        return f;
    }

    @Override
    public CancellableFuture<DbResult<Todo>> update(Todo entity) {
        CancellableFuture<DbResult<Todo>> f = super.update(entity);
        AndroidFutures.thenAcceptOnMain(f, res -> {
            if (res.isSuccess() && res.getData() != null) {
                SimpleEventBus.post(new TodoUpdatedEvent(res.getData()));
            }
        });
        return f;
    }

    @Override
    public CancellableFuture<DbResult<Todo>> delete(Todo entity) {
        CancellableFuture<DbResult<Todo>> f = super.delete(entity);
        AndroidFutures.thenAcceptOnMain(f, res -> {
            if (res.isSuccess() && res.getData() != null) {
                SimpleEventBus.post(new TodoDeletedEvent(res.getData()));
            }
        });
        return f;
    }

    // Tx-forwarders (service için)
    public Todo insertTx(SQLiteDatabase db, Todo t) throws Exception { return super.insertTx(db, t); }
    public Todo updateTx(SQLiteDatabase db, Todo t) throws Exception { return super.updateTx(db, t); }
    public int  deleteTx(SQLiteDatabase db, Todo t) throws Exception { return super.deleteTx(db, t); }
//    // Uygulamaya özel bir sorgu metodu
//    public void findCompletedTodosPaginated(int page, int pageSize, DbCallback<ArrayList<Todo>> callback) {
//
//        // Kapsamlı SelectQuery'yi oluşturuyoruz
//        SelectQuery<Todo> query = SelectQuery.build(Todo.class)
//                .where()
//                .Equals("is_completed", true)
//                .orderBy("id")
//                .limit((page - 1) * pageSize, pageSize);
//
//        // Oluşturulan sorguyu generic bir metot aracılığıyla çalıştırıyoruz
//        // Bu metot, ADbContext'e erişerek asenkron işlemi başlatır.
//        dbContext.runDbOperation((db) -> {
//            ArrayList<Todo> items = new ArrayList<>();
//            try (Cursor cursor = db.rawQuery(query.getQuery(), query.getWhereArgs())) {
//                if (cursor.moveToFirst()) {
//                    do {
//                        // T item = Mapper.cursorToObject(cursor, query.getType());
//                        // items.add(item);
//                        // ...
//                    } while (cursor.moveToNext());
//                }
//            }
//            return new DbResult.Success<>(items);
//        }, callback, false);
//    }
}