package lib.persistence.repositories;

import android.database.Cursor;

import java.util.ArrayList;

import lib.persistence.ADbContext;
import lib.persistence.DbCallback;
import lib.persistence.DbResult;
import lib.persistence.GenericRepository;
import lib.persistence.command.query.SelectQuery;
import lib.persistence.domain.entities.Todo;
import lib.persistence.DbContext;
import lib.persistence.profile.Mapper;

public class TodoRepository extends GenericRepository<Todo> {

    public TodoRepository(DbContext context) {
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