package com.example.adbkit.events;

import com.example.adbkit.entities.Todo;

public class TodoCreatedEvent {
    public final Todo todo;
    public TodoCreatedEvent(Todo todo) {
        this.todo = todo;
    }
}
