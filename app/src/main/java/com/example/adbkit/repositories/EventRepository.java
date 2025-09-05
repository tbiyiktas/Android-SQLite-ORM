package com.example.adbkit.entities;

import lib.persistence.GenericRepository;
import lib.persistence.IDbContext;

public class EventRepository extends GenericRepository<Event> {
    public EventRepository(IDbContext context) {
        super(context, Event.class);
    }
}
