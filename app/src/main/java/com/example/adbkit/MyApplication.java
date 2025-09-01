package com.example.adbkit;

import android.app.Application;

import lib.persistence.IDbContext;

public class MyApplication extends Application {
    private volatile IDbContext dbContextInstance; // lazy

    @Override
    public void onCreate() {
        super.onCreate();
        dbContextInstance = null; // lazy init
    }

    /** Lazy + thread-safe */
    public synchronized IDbContext getDbContext () {
        if (dbContextInstance == null) {
            dbContextInstance = new DbContext(getApplicationContext());
        }
        return dbContextInstance;
    }
}
