# Android SQLite ORM

> A lightweight, annotation-driven ORM for Android that turns raw SQLite into clean, safe, and testable code — with zero magic and full control when you need it.

[![Android](https://img.shields.io/badge/Android-%2B9-3DDC84)](#)
[![SQLite](https://img.shields.io/badge/SQLite-3.x-044a64)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-black.svg)](#license)
[![Status](https://img.shields.io/badge/Stability-Production%20Ready-blue)](#)

**Why this library?**
You get the ergonomics of an ORM without losing the transparency of SQL. Everything is plain Java, strongly-typed, and optimized for Android (WAL, PRAGMA tuning, bounded executors, reflection caches).

### Why not Room?
| Use case | Room | Android SQLite ORM |
|---|---|---|
| Full SQL control | Limited (abstractions) | ✅ Direct, fluent, parameterized |
| Lightweight | Medium | ✅ Very light, zero extra deps |
| WAL/PRAGMA tuning | Manual | ✅ Built-in hooks |
---

## Table of Contents

* [Highlights](#highlights)
* [Quick Start](#quick-start)

  * [1) Define your model](#1-define-your-model)
  * [2) Create your DbContext](#2-create-your-dbcontext)
  * [3) Use a repository](#3-use-a-repository)
* [Fluent SQL Builders](#fluent-sql-builders)

  * [Select](#select)
  * [UpdateSql](#updatesql)
  * [DeleteSql](#deletesql)
* [Single-Record Helpers](#single-record-helpers)
* [Custom Type Converters (NEW)](#custom-type-converters-new)

  * [When to use](#when-to-use)
  * [Implement a converter](#implement-a-converter)
  * [Annotate your field](#annotate-your-field)
  * [How it works under the hood](#how-it-works-under-the-hood)
  * [Examples](#examples)
  * [Migrations & schema notes](#migrations--schema-notes)
  * [FAQ for converters](#faq-for-converters)
* [Simple Event Bus (NEW)](#simple-event-bus-new)
* [Async & Threading](#async--threading)
* [Migrations](#migrations)
* [Tuning & Pragmas](#tuning--pragmas)
* [Error & Result Model](#error--result-model)
* [ProGuard / R8](#proguard--r8)
* [FAQ](#faq)
* [Key Components & Features](#key-components--features)
* [Architectural Considerations](#architectural-considerations)
* [Contributing](#contributing)
* [License](#license)

---

## Highlights

* **Annotation Mapping**: Map entities with `@DbTableAnnotation` and `@DbColumnAnnotation` (PK, identity, nullability, ordinal).
* **Custom Type Converters (NEW)**: Store non‑primitive/complex types using `@DbConverterAnnotation` + `TypeConverter<F, D>`. Full round‑trip on **INSERT/UPDATE/DELETE/SELECT**.
* **Simple Event Bus (NEW)**: Lightweight, zero‑dep, in‑process pub/sub for domain events (e.g., fire `TodoCreatedEvent` → handle in one place).
* **Generic Repository**: Reusable `GenericRepository<T>` handles async CRUD in a thread-safe way.
* **Fluent Query Builders**: Type-aware `Select`, `UpdateSql`, `DeleteSql` compile to parameterized SQL with escaped identifiers.
* **Safe by Default**: Parameter binding everywhere. Identifiers backticked. Null-safe APIs (e.g., `whereNull` / `whereNotNull`).
* **Android-Ready Concurrency**: Separate read/write executors, main-thread callbacks, bounded queues for backpressure.
* **Performant**: Column/metadata caches; WAL and PRAGMAs; minimal allocations during hot paths.
* **Migration Support**: Incremental, versioned steps with transaction wrapping.

---

## Quick Start

> Add the module to your project (via source or your preferred dependency strategy). The library is pure Java and has no external runtime deps.

### 1) Define your model

```java
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.annotations.DbColumnAnnotation;

@DbTableAnnotation(name = "todos") // name is required by Mapper
public class Todo {
    @DbColumnAnnotation(ordinal = 1, isPrimaryKey = true, isIdentity = true, isNullable = false)
    public int id;

    @DbColumnAnnotation(ordinal = 2, isNullable = false)
    public int userId;

    @DbColumnAnnotation(ordinal = 3, isNullable = false)
    public String title;

    @DbColumnAnnotation(ordinal = 4)
    public boolean completed;

    @Override
    public String toString() {
        return "Todo{id=" + id + ", userId=" + userId + ", title='" + title + "', completed=" + completed + "}";
    }
}
```

### 2) Create your DbContext

```java
// app/src/main/java/com/example/adbkit/DbContext.java
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import lib.persistence.ADbContext;
import lib.persistence.command.definition.CreateTableCommand;
import lib.persistence.command.definition.CreateIndexCommand;
import lib.persistence.command.definition.DropTableCommand;

import com.example.adbkit.entities.Todo;

public class DbContext extends ADbContext {
    private static final String DB_NAME = "local.db";
    private static final int VERSION = 5;

    public DbContext(Context context) { super(context, DB_NAME, VERSION); }

    @Override
    protected void onCreateSchema(SQLiteDatabase db) {
        db.execSQL(CreateTableCommand.build(Todo.class).getQuery());
        db.execSQL(CreateIndexCommand.build(Todo.class, "idx_todos_title", false, "title").getQuery());
    }

    @Override
    protected void onUpgradeSchema(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DropTableCommand.build(Todo.class).getQuery());
        onCreateSchema(db);
    }
}
```

### 3) Use a repository

```java
// A simple repository specialized for Todo
import lib.persistence.GenericRepository;
import lib.persistence.IDbContext;
import com.example.adbkit.entities.Todo;

public class TodoRepository extends GenericRepository<Todo> {
    public TodoRepository(IDbContext context) { super(context, Todo.class); }
}
```

#### Create → Read → Update → Delete (Async)

```java
DbContext db = new DbContext(getApplicationContext());
TodoRepository todos = new TodoRepository(db);

// CREATE
Todo t = new Todo();
t.userId = 1;
t.title = "Buy groceries";
t.completed = false;

todos.insert(t, result -> {
    if (result.isSuccess()) {
        // identity PK filled back (e.g., t.id)
        // READ ALL
        todos.selectAll(listRes -> {
            if (listRes.isSuccess()) {
                // UPDATE (toggle completed)
                Todo first = listRes.getData().get(0);
                first.completed = true;
                todos.update(first, upRes -> {
                    // READ BY ID
                    todos.getById(first.id, getRes -> {
                        // DELETE
                        todos.delete(first, delRes -> {
                            // done
                        });
                    });
                });
            }
        });
    }
});
```

---

## Fluent SQL Builders

### Select

```java
import lib.persistence.command.query.Select;

Select<Todo> builder = Select.from(Todo.class)
    .columns("id", "title", "completed")
    .whereEq("userId", 1)
    .and("`completed` = ?", false)
    .orderBy("id", false) // ASC
    .limit(50);

todos.selectWith(builder, res -> {
    if (res.isSuccess()) {
        // List<Todo> ready
    }
});
```

#### Aggregates & expressions

```java
Select<Todo> q = Select.from(Todo.class)
    .count("*", "total")
    .whereLike("title", "%book%");
```

### UpdateSql

```java
import lib.persistence.command.manipulation.UpdateSql;

UpdateSql u = UpdateSql.build(Todo.class)
    .set("completed", true)
    .where().Equals("id", 42);

todos.updateWith(u, rowsRes -> {
    // rowsRes.getData() → affected rows
});
```

### DeleteSql

```java
import lib.persistence.command.manipulation.DeleteSql;

DeleteSql d = DeleteSql.build(Todo.class)
    .where().Equals("completed", true)
    .and().Equals("userId", 1);

todos.deleteWhere(d, rowsRes -> {
    // rowsRes.getData() → affected rows
});
```

---

## Single-Record Helpers

```java
todos.getById(123, res -> {
    if (res.isSuccess()) {
        Todo item = res.getData(); // may be null if not found
    }
});
```

### Delete by primary key

```java
todos.deleteById(rowsRes -> {
    // ...
}, 123);
```

---
## Custom Type Converters (NEW)

Many apps need to persist **enums, dates, JSON objects, UUIDs** or any domain‑specific value object. With custom converters you can declare **how to serialize** your field to a SQLite‑friendly type and **how to deserialize** it back.

### When to use

Use converters whenever a field is not directly representable as a primitive SQLite column, or when you want a custom storage representation (e.g., `Date` as epoch millis instead of ISO `TEXT`).

### Implement a converter

Create a class that implements `TypeConverter<F, D>` where:

* `F` = field type in your model (e.g., `EventType`, `Date`)
* `D` = database column type (e.g., `String`, `Long`, `byte[]`)

```java
import lib.persistence.converters.TypeConverter;

public final class EventTypeConverter implements TypeConverter<EventType, String> {
    @Override public String toDatabaseValue(EventType v) { return v == null ? null : v.name(); }
    @Override public EventType fromDatabaseValue(String s) { return s == null ? null : EventType.valueOf(s); }
    @Override public String sqliteType() { return "TEXT"; } // column type in DDL
}

public final class DateMillisConverter implements TypeConverter<java.util.Date, Long> {
    @Override public Long toDatabaseValue(java.util.Date v) { return v == null ? null : v.getTime(); }
    @Override public java.util.Date fromDatabaseValue(Long t) { return t == null ? null : new java.util.Date(t); }
    @Override public String sqliteType() { return "INTEGER"; }
}
```

> `sqliteType()` informs the table generator which SQLite type to emit in `CREATE TABLE`.

### Annotate your field

Attach a converter to a field with `@DbConverterAnnotation`:

```java
import lib.persistence.annotations.*;

@DbTableAnnotation(name = "events")
public class Event {
    @DbColumnAnnotation(ordinal = 1, isPrimaryKey = true, isIdentity = true)
    int id;

    @DbColumnAnnotation(ordinal = 2, name = "event_type")
    @DbConverterAnnotation(converter = EventTypeConverter.class)
    EventType type;

    @DbColumnAnnotation(ordinal = 3, name = "event_message")
    String message;

    @DbColumnAnnotation(ordinal = 4, name = "created_at")
    @DbConverterAnnotation(converter = DateMillisConverter.class)
    java.util.Date createdAt;

    public Event() {}
}
```

### How it works under the hood

* **DDL generation**: `CreateTableCommand` inspects fields; if a field has a converter, its `sqliteType()` determines the column type.
* **INSERT/UPDATE**: `Mapper.objectToContentValues` calls your converter’s `toDatabaseValue(...)` before writing into `ContentValues`.
* **SELECT**: `Mapper.cursorToObject` fetches the raw DB value and calls `fromDatabaseValue(...)` to set the field.
* **DELETE/UPDATE WHERE (PK)**: If a **primary key** field is annotated with a converter, the key is transformed with `toDatabaseValue(...)` before binding to the `WHERE` clause.
* **Caching**: Converters are cached internally for performance; you don’t have to register them manually.

### Examples

**1) Enum ↔ TEXT**

```java
@DbConverterAnnotation(converter = EventTypeConverter.class)
EventType type; // stored as TEXT (e.g., "CREATED")
```

**2) Date ↔ INTEGER (epoch millis)**

```java
@DbConverterAnnotation(converter = DateMillisConverter.class)
Date createdAt; // stored as INTEGER
```

**3) JSON ↔ TEXT**

```java
public final class AddressJsonConverter implements TypeConverter<Address, String> {
    @Override public String toDatabaseValue(Address v) { return v == null ? null : v.toJson(); }
    @Override public Address fromDatabaseValue(String s) { return s == null ? null : Address.fromJson(s); }
    @Override public String sqliteType() { return "TEXT"; }
}
```
### Migrations & schema notes

* Changing a field to use a converter **may change the column type** (e.g., `TEXT` → `INTEGER`). Provide a migration step if your table already exists.
* Identity integer PKs remain `INTEGER PRIMARY KEY AUTOINCREMENT` and are **not** updated via SET.
* Ensure `ordinal` values are unique per entity and add a **no‑arg constructor** for reflection.

### FAQ for converters

* **Do I need to register converters?**  No. Attach with `@DbConverterAnnotation` on fields. Instances are cached internally.
* **Are converters used in custom `Select` builders?**  Builders bind your literal arguments. Mapping back to objects always goes through `Mapper.cursorToObject`, so fields with converters are correctly materialized.
* **Composite PKs?**  Supported. If a PK field has a converter, it’s applied to the `WHERE` binding.

---

# Simple Event Bus (NEW)

A tiny, zero‑dependency pub/sub helper to emit domain events from repositories/UI and handle them in one place. Great for logging/auditing (e.g., store `Event` rows when a `Todo` is created/updated/deleted).

### API

```java
public final class SimpleEventBus {
  // Register a single consumer per event type
  public static <T> void subscribe(Class<T> eventType, java.util.function.Consumer<T> listener);
  public static <T> void post(T event); // Synchronous, in‑process
}
```

> Current implementation keeps **one subscriber per event class** (a simple `Map<Class<?>, Consumer<?>>`). If you need fan‑out (N subscribers), replace the `Consumer<?>` with a `List<Consumer<?>>` — the public API can stay the same.

### Built‑in sample events

```java
// app/src/main/java/com/example/adbkit/events
class TodoCreatedEvent { public final Todo todo; /* ctor */ }
class TodoUpdatedEvent { public final Todo todo; /* ctor */ }
class TodoDeletedEvent { public final Todo todo; /* ctor */ }
```

### Wiring example (MainActivity)

```java
// Subscribe once (e.g., in onCreate)
SimpleEventBus.subscribe(TodoCreatedEvent.class, e -> {
    Event ev = new Event(EventType.CREATED, "Yeni Todo: " + e.todo.id + " title: " + e.todo.title);
    eventRepository.insert(ev, r -> { /* handle DbResult */ });
});

// Post from your repository callbacks
SimpleEventBus.post(new TodoCreatedEvent(createdTodo));
```

This example persists an `Event` row with `EventType` and message. `EventType` is stored as `TEXT` via `EventTypeConverter`; `created_at` as epoch millis via `DateMillisConverter`.

### Notes

* Calls are **synchronous** on the caller thread; keep handlers light and push heavy work to your existing executors.
* The bus is package‑private and simple by design. For cross‑module or sticky events, bring your favorite event lib.

---

## Async & Threading

* **Reads**: bounded read pool (default 4 threads).
* **Writes**: single-thread executor with serialized transactions.
* **Callbacks**: always posted to main thread.

#### Global settings

```java
import lib.persistence.DbContextConfig;

DbContextConfig cfg = new DbContextConfig();
cfg.readThreads = 4;
cfg.enableWAL = true;
cfg.pragmaSynchronous = "NORMAL";
cfg.pragmaBusyTimeoutMs = 10_000;

DbContextConfig.apply(cfg);
```

---

## Migrations

```java
new MigrationStep() {
    public int from() { return 1; }
    public int to()   { return 2; }
    public void apply(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE `todos` ADD COLUMN `notes` TEXT");
    }
};
```

Use versioned steps or simple recreate for demos:

```java
new MigrationStep() {
  public int from() { return 4; }
  public int to()   { return 5; }
  public void apply(SQLiteDatabase db) {
    db.execSQL(CreateTableCommand.build(Event.class).getQuery());
  }
};
```

---

## Tuning & Pragmas

* `PRAGMA foreign_keys = ON`
* `journal_mode = WAL`
* `synchronous = NORMAL`
* `busy_timeout = 10000 ms`

Custom PRAGMAs can be added in `onConfigureExtra(SQLiteDatabase db)`.

---

## Error & Result Model

```java
todos.insert(item, result -> {
    if (result.isSuccess()) {
        Todo saved = result.getData();
    } else {
        Exception ex = ((DbResult.Error<Todo>) result).getException();
        String message = ((DbResult.Error<Todo>) result).getErrorMessage();
    }
});
```

---

## ProGuard / R8

```pro
-keep class com.example.adbkit.entities.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    public <init>();
}
```

---

## FAQ

**Q: Do I need to write raw SQL?**
A: Only if you want to. Builders cover most cases; raw SQL is still available via `rawQuery`.

**Q: How are booleans stored?**
A: As `INTEGER` (0/1). Dates as ISO strings when using `LocalDate`/`LocalDateTime`. Dates can also be stored via converters (e.g., epoch millis with `DateMillisConverter`), or as text if you provide a different converter.

**Q: Composite primary keys?**
A: Fully supported in `DeleteCommand`, `UpdateCommand`, and builder APIs.

---

## Key Components & Features

* **`IDbContext` / `ADbContext`**: DB connection, schema lifecycle (`onCreateSchema`, `onUpgradeSchema`), PRAGMAs, async `runDbOperation`.
* **`DbContextConfig`**: Global threading + WAL + PRAGMAs.
* **`Mapper`**: Reflection-based mapping for `Cursor`/`ContentValues`, with schema caches. Supports primitives, `LocalDate`, `LocalDateTime`, `Enum`.
* **Annotations**: `@DbTableAnnotation(name = "...")`, `@DbColumnAnnotation(...)` (PK, identity, nullability, ordinal).
* **Command Builders**:

  * DDL: `CreateTableCommand`, `DropTableCommand`, `CreateIndexCommand`, `DropIndexCommand`.
  * DML: `InsertCommand`, `UpdateCommand`, `DeleteCommand`.
  * Queries: `Select<T>`, `GetQuery`, `UpdateSql`, `DeleteSql`.
* **`SqlNames`**: Safe identifier quoting with backticks.
* **`DbResult<T>` & `DbCallback<T>`**: Success/error wrappers.
* **`GenericRepository<T>`**: Generic CRUD + query helpers.

---

## Architectural Considerations

* **Repository Pattern**: Clean separation of data access.
* **Builder Pattern**: Fluent, readable SQL builders.
* **Command Pattern (implicit)**: DDL/DML encapsulated as objects.
* **DI-Friendly**: `IDbContext` makes testing/injection straightforward.
* **Separation of Concerns**: Annotations, profile (schema/meta), commands, core context, repositories.
* **Testability**: Clear seams for unit and instrumented tests.

---

## Contributing

Issues and PRs are welcome!
Please include:

* A concise problem statement
* Minimal repro
* Tests if possible

---

## License

This project is licensed under the MIT License.

---

### Bonus: One-file example

```java
DbContext db = new DbContext(getApplicationContext());
TodoRepository repo = new TodoRepository(db);

Todo todo = new Todo();
todo.userId = 7;
todo.title = "Write a dazzling README";
todo.completed = false;

repo.insert(todo, ins -> {
    if (!ins.isSuccess()) return;

    repo.selectAll(all -> {
        if (!all.isSuccess() || all.getData().isEmpty()) return;

        Todo first = all.getData().get(0);
        first.completed = true;

        repo.update(first, up -> {
            repo.getById(first.id, get -> {
                repo.delete(first, del -> {
                    // All done
                });
            });
        });
    });
});
```
