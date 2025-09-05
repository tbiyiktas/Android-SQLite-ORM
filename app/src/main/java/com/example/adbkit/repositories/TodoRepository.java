package com.example.adbkit.repositories;

import com.example.adbkit.entities.Todo;

import lib.persistence.GenericRepository;
import lib.persistence.IDbContext;

public class TodoRepository extends GenericRepository<Todo> {

    public TodoRepository(IDbContext context) {
        super(context, Todo.class);
    }


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