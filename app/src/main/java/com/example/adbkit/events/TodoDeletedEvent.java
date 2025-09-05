package com.example.adbkit.events;

import com.example.adbkit.entities.Todo;

public class TodoDeletedEvent {
    public final Todo todo;
    public TodoDeletedEvent(Todo todo) {
        this.todo = todo;
    }
}
