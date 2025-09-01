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
  * [2) Create your-dbcontext](#2-create-your-dbcontext)
  * [3) Use a repository](#3-use-a-repository)
* [Fluent SQL Builders](#fluent-sql-builders)

  * [Select](#select)
  * [UpdateSql](#updatesql)
  * [DeleteSql](#deletesql)
* [Single-Record Helpers](#single-record-helpers)
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
    private static final int VERSION = 3;

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
A: As `INTEGER` (0/1). Dates as ISO strings when using `LocalDate`/`LocalDateTime`.

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
