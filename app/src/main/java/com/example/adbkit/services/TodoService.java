// com/example/adbkit/services/TodoService.java
package com.example.adbkit.services;

import com.example.adbkit.entities.Event;
import com.example.adbkit.entities.EventType;
import com.example.adbkit.entities.Todo;
import com.example.adbkit.events.TodoCreatedEvent;
import com.example.adbkit.repositories.EventRepository;
import com.example.adbkit.repositories.TodoRepository;

import lib.persistence.core.CancellableFuture;
import lib.persistence.core.DbResult;
import lib.persistence.core.IDbContext;
import lib.persistence.tx.AfterCommitEvents;

public final class TodoService {
    private final IDbContext ctx;
    private final TodoRepository todos;
    private final EventRepository events;

    public TodoService(IDbContext ctx, TodoRepository todos, EventRepository events) {
        this.ctx = ctx;
        this.todos = todos;
        this.events = events;
    }

    public CancellableFuture<DbResult<Todo>> createWithAudit(Todo t) {
        return ctx.runWrite(db -> {
            Todo saved = todos.insertTx(db, t);
            events.insertTx(db, new Event(
                    EventType.CREATED,
                    "Todo " + saved.id + " : " + saved.title
            ));
            // Commit sonrası publish edilecek outbox event
            AfterCommitEvents.add(new TodoCreatedEvent(saved));
            return new DbResult.Success<>(saved);
        });
    }
}
