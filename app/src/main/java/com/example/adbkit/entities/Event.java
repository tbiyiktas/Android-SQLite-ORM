package com.example.adbkit.entities;

import lib.persistence.annotations.DbColumnAnnotation;
import lib.persistence.annotations.DbConverterAnnotation;
import lib.persistence.annotations.DbTableAnnotation;

@DbTableAnnotation(name = "events")
public class Event {
    @DbColumnAnnotation(ordinal = 1, isPrimaryKey = true, isIdentity = true)
    private int id;

    @DbColumnAnnotation(ordinal = 2, name = "event_type")
    @DbConverterAnnotation(converter = EventTypeConverter.class)
    private EventType type;

    @DbColumnAnnotation(ordinal = 3, name = "event_message")
    private String message;

    @DbColumnAnnotation(ordinal = 4, name = "created_at")
    @DbConverterAnnotation(converter = DateMillisConverter.class)
    private java.util.Date createdAt;

    public Event(){

    }
    public Event(EventType type, String message){
        this.type = type;
        this.message = message;
        this.createdAt = java.util.Date.from(new java.util.Date().toInstant());
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}