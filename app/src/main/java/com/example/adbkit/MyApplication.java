package com.example.adbkit;

import android.app.Application;

import lib.persistence.DbContext;

public class MyApplication extends Application {
    private volatile DbContext dbContext; // lazy

    @Override
    public void onCreate() {
        super.onCreate();
        dbContext = null; // lazy init
    }

    /** Lazy + thread-safe */
    public synchronized DbContext getDbContext () {
        if (dbContext == null) {
            dbContext = new DbContext(getApplicationContext());
        }
        return dbContext;
    }
}
