package com.example.adbkit.entities;

public class TodoCreatedEvent {
    public final Todo todo;
    public TodoCreatedEvent(Todo todo) {
        this.todo = todo;
    }
}
