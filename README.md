# Android SQLite ORM

> A lightweight, annotation‑driven ORM for Android that turns raw SQLite into clean, safe, and testable code — with zero magic and full control when you need it.

[![Android](https://img.shields.io/badge/Android-%2B9-3DDC84)](#)
[![SQLite](https://img.shields.io/badge/SQLite-3.x-044a64)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-black.svg)](#license)
[![Status](https://img.shields.io/badge/Stability-Production%20Ready-blue)](#)

---

## Why this library?

You get the ergonomics of an ORM **without losing SQL**. Everything is plain Java, strongly‑typed, and optimized for Android (WAL, PRAGMA tuning, bounded executors, reflection caches). Builders compile to **parameterized SQL** with quoted identifiers.

### Why not Room?

| Use case          | Room    | Android SQLite ORM              |
| ----------------- | ------- | ------------------------------- |
| Full SQL control  | Limited | ✅ Direct, fluent, parameterized |
| Lightweight       | Medium  | ✅ Very light, zero extra deps   |
| WAL/PRAGMA tuning | Manual  | ✅ Built‑in hooks                |

---

## What’s new (major changes)

* **`CancellableFuture<T>`** replaces callbacks: chainable, cancel‑aware `CompletableFuture` subclass.
* **`IDbContext` / `ADbContext`** expose `runDbOperation(...)` overloads (read/write routing, bounded pools, optional transactions).
* **Transactions helper**: `Transactions.run(ctx, work)` executes a **single SQLite transaction** and returns `CancellableFuture<DbResult<R>>`.
* **After‑commit events**: collect with `AfterCommitEvents.add(e)`, publish once the tx commits.
* **`SimpleEventBus`**: tiny, in‑process pub/sub with **main‑thread delivery** helpers and `Subscription` tokens.

## Highlights

* **Annotation Mapping**: Map entities with `@DbTableAnnotation` and `@DbColumnAnnotation` (PK, identity, nullability, ordinal).
* **Custom Type Converters (NEW)**: Store non‑primitive/complex types using `@DbConverterAnnotation` + `TypeConverter<F, D>`. Full round‑trip on **INSERT/UPDATE/DELETE/SELECT**.
* **Simple Event Bus (NEW)**: Lightweight, zero‑dep, in‑process pub/sub for domain events (e.g., fire `TodoCreatedEvent` → handle in one place).
* **Generic Repository**: Reusable `GenericRepository<T>` handles async CRUD in a thread‑safe way.
* **Fluent Query Builders**: Type‑aware `Select`, `UpdateSql`, `DeleteSql` compile to parameterized SQL with escaped identifiers.
* **Safe by Default**: Parameter binding everywhere. Identifiers backticked. Null‑safe APIs (e.g., `whereNull` / `whereNotNull`).
* **Android‑Ready Concurrency**: Separate read/write executors, main‑thread callbacks, bounded queues for backpressure.
* **Performant**: Column/metadata caches; WAL and PRAGMAs; minimal allocations during hot paths.
* **Migration Support**: Incremental, versioned steps with transaction wrapping.
* **Cancellable futures & UI delivery (NEW)**: Chainable `CancellableFuture<T>` with `cancelWork()`; easy UI posting via `AndroidFutures.MAIN`.
* **Transactions & after‑commit events (NEW)**: `Transactions.run(...)` for atomic writes; domain events collected and dispatched after a successful commit via `AfterCommitEvents`.
* **API 36 & Java 21 ready (NEW)**: Robolectric 4.16+ support for Android 14/15 (API 34/36) and GitHub Actions workflow pinned to Java 21.

---

## Table of Contents

* [Quick Start](#quick-start)

  * [1) Define your model](#1-define-your-model)
  * [2) Create your DbContext](#2-create-your-dbcontext)
  * [3) Repository](#3-repository)
  * [4) End‑to‑end demo](#4-end-to-end-demo)
* [Fluent SQL Builders](#fluent-sql-builders)

  * [Select](#select)
  * [UpdateSql](#updatesql)
  * [DeleteSql](#deletesql)
* [Single‑record helpers](#single-record-helpers)
* [Custom type converters](#custom-type-converters)
* [Transactions & After‑commit events](#transactions--after-commit-events)
* [Simple Event Bus](#simple-event-bus)
* [Tuning & Pragmas](#tuning--pragmas)
* [ProGuard / R8](#proguard--r8)
* [License](#license)

---

## Quick Start

### 1) Define your model

```java
import lib.persistence.mapping.annotations.DbTableAnnotation;
import lib.persistence.mapping.annotations.DbColumnAnnotation;

@DbTableAnnotation(name = "todos")
public class Todo {
    @DbColumnAnnotation(ordinal = 1, isPrimaryKey = true, isIdentity = true, isNullable = false)
    public int id;

    @DbColumnAnnotation(ordinal = 2, isNullable = false)
    public int userId;

    @DbColumnAnnotation(ordinal = 3, isNullable = false)
    public String title;

    @DbColumnAnnotation(ordinal = 4)
    public boolean completed;
}
```

### 2) Create your DbContext

```java
// com/example/adbkit/DbContext.java
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import lib.persistence.core.ADbContext;
import lib.persistence.command.definition.CreateTableCommand;
import lib.persistence.command.definition.CreateIndexCommand;
import com.example.adbkit.entities.Todo;

public class DbContext extends ADbContext {
    private static final String DB_NAME = "local.db";
    private static final int VERSION = 5;

    public DbContext(Context context) { super(context, DB_NAME, VERSION); }

    @Override protected void onCreateSchema(SQLiteDatabase db) {
        db.execSQL(CreateTableCommand.build(Todo.class).getQuery());
        db.execSQL(CreateIndexCommand.build(Todo.class, "idx_todos_title", false, new String[]{"title"}).getQuery());
    }

    @Override protected void onUpgradeSchema(SQLiteDatabase db, int oldV, int newV) {
        // apply migrations... 
        // (see Migrations.java)
        onCreateSchema(db);
    }
}
```

### 3) Repository

```java
// lib/persistence/repository/GenericRepository<T>
// com/example/adbkit/repositories/TodoRepository.java
import lib.persistence.repository.GenericRepository;
import lib.persistence.core.IDbContext;
import com.example.adbkit.entities.Todo;

public class TodoRepository extends GenericRepository<Todo> {
    public TodoRepository(IDbContext ctx) { super(ctx, Todo.class); }
}
```

### 4) End‑to‑end demo (create → select → update → get → delete)

```java
// In an Activity (or anywhere you have access to a Context)
Context appCtx = getApplicationContext();

// Obtain the singleton DB context provided by your Application class
IDbContext db = MyApplication.getDbContext();

// Get repositories via the factory (uses the same DB context)
TodoRepository repo = RepositoryFactory.getTodoRepository(appCtx);

// Optional: domain service that wraps a transaction + after-commit events
EventRepository eventRepo = RepositoryFactory.getEventRepository(appCtx);

TodoService service = new TodoService(db, repo, eventRepo);

// 1) Create (with audit + after-commit event via the service)
Todo todo = new Todo();

todo.userId = 1; 
todo.title = "Buy milk"; 
todo.completed = false;

service.createWithAudit(todo)
    // 2) Read (all)
    .thenCompose(ins -> repo.selectAll())

    // 3) Update first row (toggle completed)
    .thenCompose(list -> {
        if (!list.isSuccess() || list.getData().isEmpty())
            return CompletableFuture.failedFuture(new RuntimeException("no rows"));
        Todo first = list.getData().get(0);
        first.completed = true;
        return repo.update(first)
                   // 4) Get by id to verify
                   .thenCompose(up -> repo.getById(first.id))
                   // 5) Delete
                   .thenCompose(got -> repo.delete(first));
    })

    // UI thread delivery helper (AndroidFutures.MAIN)
    .whenCompleteAsync((res, ex) -> {
        if (ex != null) {
            // show error
        } else if (!res.isSuccess()) {
            // show DbResult.Error
        } else {
            // done
        }
    }, lib.persistence.AndroidFutures.MAIN);
```

---

## Fluent SQL Builders

### Select

```java
import lib.persistence.command.query.Select;

Select<Todo> q = Select.from(Todo.class)
    .columns("id", "title", "completed")
    .whereEq("userId", 1)
    .orderBy("id", false) // ASC
    .limit(50);

repo.selectWith(q)
    .thenAccept(res -> { /* handle result */ });
```

### UpdateSql

```java
import lib.persistence.command.manipulation.UpdateSql;

UpdateSql u = UpdateSql.build(Todo.class)
    .set("completed", true)
    .where().Equals("id", 42);

repo.updateWith(u).thenAccept(rows -> { /* affected rows */ });
```

### DeleteSql

```java
import lib.persistence.command.manipulation.DeleteSql;

DeleteSql d = DeleteSql.build(Todo.class)
    .where().Equals("completed", true)
    .and().Equals("userId", 1);

repo.deleteWhere(d).thenAccept(rows -> { /* affected rows */ });
```

---

## Single‑record helpers

```java
repo.getById(123).thenAccept(res -> {
    if (res.isSuccess()) {
        Todo item = res.getData(); // may be null if not found
    }
});

// Delete by primary key
repo.deleteById(123).thenAccept(rows -> { /* ... */ });
```

---

## Custom type converters

Use `@DbConverterAnnotation` to map complex field types to SQLite primitives and back.

```java
// Example: Enum <-> TEXT
import lib.persistence.mapping.converters.TypeConverter;

public final class EventTypeConverter 
    implements TypeConverter<EventType, String> {

  @Override 
  public String toDatabaseValue(EventType v) { 
    return v == null ? null : v.name(); 
  }

  @Override 
  public EventType fromDatabaseValue(String s) { 
    return s == null ? null : EventType.valueOf(s);
  }

  @Override 
  public String sqliteType() {
     return "TEXT";
  }
}
```

```java
// Field usage
@DbConverterAnnotation(converter = EventTypeConverter.class)
EventType type; // stored as TEXT
```

Converters are respected in DDL generation, INSERT/UPDATE (via `Mapper.objectToContentValues`), and SELECT materialization.

---

## Transactions & After‑commit events

Execute multiple commands in **one transaction** and publish domain events **after** commit.

```java
import lib.persistence.tx.Transactions;
import lib.persistence.tx.AfterCommitEvents;

Transactions.run(db, sqlite -> {

    Todo saved = repo.insertTx(sqlite, todo);   // repository tx‑helpers

    eventRepo.insertTx(sqlite, new Event(EventType.CREATED, "Created #" + saved.id));

    AfterCommitEvents.add(new TodoCreatedEvent(saved));

    return new lib.persistence.core.DbResult.Success<>(saved);
});
```

`Transactions.run(...)` returns `CancellableFuture<DbResult<R>>`. If the tx fails, events are **not** dispatched.

---

## Simple Event Bus

A tiny, zero‑dep pub/sub. You can subscribe on the main thread and receive a `Subscription` that auto‑unsubscribes when closed.

```java
// Subscribe on main
AndroidFutures.MAIN.execute(() -> {
    
  SimpleEventBus.Subscription sub =
      SimpleEventBus.subscribeOnMain(
          TodoCreatedEvent.class,
          e -> {/* update UI */}
      );
  // Keep `sub` and call `sub.close()` when appropriate.
});

// Post anywhere (synchronous)
SimpleEventBus.post(new TodoCreatedEvent(todo));
```

---

## Tuning & Pragmas

* `PRAGMA foreign_keys = ON`
* `journal_mode = WAL`
* `synchronous = NORMAL`
* `busy_timeout = 10000 ms`

Custom PRAGMAs can be added in `onConfigureExtra(SQLiteDatabase db)`.

---

## ProGuard / R8

```proguard
-keep class ** { *; }
-keep class lib.persistence.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * { public <init>(); }
```

---

## License

MIT
