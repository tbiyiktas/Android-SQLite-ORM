package com.example.adbkit.entities;

import java.util.Date;

import lib.persistence.converters.TypeConverter;

public final class DateMillisConverter implements TypeConverter<Date, Long> {
    @Override public Long toDatabaseValue(Date v) { return v == null ? null : v.getTime(); }
    @Override public Date fromDatabaseValue(Long t) { return t == null ? null : new Date(t); }
    @Override public String sqliteType() { return "INTEGER"; }
}