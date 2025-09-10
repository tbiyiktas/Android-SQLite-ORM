package lib.persistence.repository;

import android.content.Context;

import com.example.adbkit.repositories.EventRepository;
import com.example.adbkit.repositories.TodoRepository;

import lib.persistence.core.IDbContext;

public final class RepositoryFactory {
    private final IDbContext dbContext;
    private TodoRepository todoRepository;
    private EventRepository eventRepository;

    public RepositoryFactory(IDbContext dbContext) {
        this.dbContext = dbContext;
    }

    public synchronized TodoRepository getTodoRepository() {
        if (todoRepository == null) {
            todoRepository = new TodoRepository(dbContext);
        }
        return todoRepository;
    }

    public synchronized EventRepository getEventRepository() {
        if (eventRepository == null) {
            eventRepository = new EventRepository(dbContext);
        }
        return eventRepository;
    }
}
