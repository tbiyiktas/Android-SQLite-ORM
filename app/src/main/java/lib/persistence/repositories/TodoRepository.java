package lib.persistence.repositories;


import android.content.Context;
import lib.persistence.domain.entities.Todo;
import lib.persistence.DbContext;
import lib.persistence.DbCallback;
import lib.persistence.DbResult;
import lib.persistence.command.query.SelectQuery;
import java.util.ArrayList;

public class TodoRepository {
    private final DbContext dbContext;

    public TodoRepository(Context context) {
        this.dbContext = new DbContext(context);
    }

    public void create(Todo todo, DbCallback<Todo> callback) {
        dbContext.insert(todo, callback);
    }

    public void getAll(DbCallback<ArrayList<Todo>> callback) {
        SelectQuery selectQuery = SelectQuery.build(Todo.class);
        dbContext.select(selectQuery, callback);
    }

    public void getById(int id, DbCallback<Todo> callback) {
        dbContext.get(Todo.class, id, callback);
    }

    public void update(Todo todo, DbCallback<Todo> callback) {
        dbContext.update(todo, callback);
    }

    public void delete(Todo todo, DbCallback<Todo> callback) {
        dbContext.delete(todo, callback);
    }
}