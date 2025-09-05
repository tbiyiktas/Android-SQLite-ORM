package com.example.adbkit.events;

import com.example.adbkit.entities.Todo;

public class TodoUpdatedEvent {
    public final Todo todo;
    public TodoUpdatedEvent(Todo todo) {
        this.todo = todo;
    }
}