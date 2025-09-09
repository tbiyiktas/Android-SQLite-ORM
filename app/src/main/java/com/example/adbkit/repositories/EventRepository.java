package com.example.adbkit.repositories;

import android.database.sqlite.SQLiteDatabase;

import com.example.adbkit.entities.Event;

import lib.persistence.repository.GenericRepository;
import lib.persistence.core.IDbContext;

public class EventRepository extends GenericRepository<Event> {
    public EventRepository(IDbContext context) {
        super(context, Event.class);
    }

    public Event insertTx(SQLiteDatabase db, Event e) throws Exception { return super.insertTx(db, e); }
    public Event updateTx(SQLiteDatabase db, Event e) throws Exception { return super.updateTx(db, e); }
    public int   deleteTx(SQLiteDatabase db, Event e) throws Exception { return super.deleteTx(db, e); }
}
