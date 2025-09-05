package com.example.adbkit.entities;

import lib.persistence.converters.TypeConverter;

public final class EventTypeConverter implements TypeConverter<EventType, String> {
    @Override public String toDatabaseValue(EventType v) { return v == null ? null : v.name(); }
    @Override public EventType fromDatabaseValue(String s) { return s == null ? null : EventType.valueOf(s); }
    @Override public String sqliteType() { return "TEXT"; }
}