package lib.persistence.repositories;

import lib.persistence.ADbContext;
import lib.persistence.GenericRepository;
import lib.persistence.domain.entities.Todo;
import lib.persistence.DbContext;

public class TodoRepository extends GenericRepository<Todo> {

    public TodoRepository(ADbContext context) {
        super(context, Todo.class);
    }

}