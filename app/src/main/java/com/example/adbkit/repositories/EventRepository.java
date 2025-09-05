package com.example.adbkit.repositories;

import com.example.adbkit.entities.Event;

import lib.persistence.GenericRepository;
import lib.persistence.IDbContext;

public class EventRepository extends GenericRepository<Event> {
    public EventRepository(IDbContext context) {
        super(context, Event.class);
    }
}
