package com.example.adbkit.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.adbkit.MyApplication;
import com.example.adbkit.R;
import com.example.adbkit.entities.Todo;
import com.example.adbkit.events.SimpleEventBus;
import com.example.adbkit.events.TodoCreatedEvent;
import com.example.adbkit.events.TodoDeletedEvent;
import com.example.adbkit.events.TodoUpdatedEvent;
import com.example.adbkit.repositories.EventRepository;
import com.example.adbkit.repositories.TodoRepository;
import com.example.adbkit.services.TodoService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import lib.persistence.AndroidFutures;
import lib.persistence.command.manipulation.UpdateSql;
import lib.persistence.command.query.Select;
import lib.persistence.core.CancellableFuture;
import lib.persistence.core.DbResult;
import lib.persistence.core.IDbContext;
import lib.persistence.repository.RepositoryFactory;

public class MainActivity extends AppCompatActivity {

    private TextView logView;
    private ScrollView logScroll;
    private Button startButton, cancelButton, clearButton;

    private SimpleEventBus.Subscription subCreated;
    private SimpleEventBus.Subscription subUpdated;
    private SimpleEventBus.Subscription subDeleted;

    private IDbContext ctx;
    private TodoRepository todoRepo;
    private EventRepository eventRepo;
    private TodoService service;

    @Nullable
    private CancellableFuture<DbResult<String>> cancellableRead;

    private static <X> CompletableFuture<DbResult<X>> failFuture(DbResult<?> prev) {
        CompletableFuture<DbResult<X>> f = new CompletableFuture<>();
        String msg = prev.isSuccess() ? "Unknown failure" : ((DbResult.Error<?>) prev).getErrorMessage();
        f.completeExceptionally(new RuntimeException(msg));
        return f;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI refs
        startButton = findViewById(R.id.start_db_ops_button);
        cancelButton = findViewById(R.id.cancel_db_read_button);
        clearButton = findViewById(R.id.clear_log_button);
        logView = findViewById(R.id.log_text);
        logScroll = findViewById(R.id.log_scroll);
        logView.setText("ADbKit showcase\n");

        // Deps
        ctx = MyApplication.getDbContext();
        RepositoryFactory repositoryFactory = new RepositoryFactory(ctx);
        todoRepo = repositoryFactory.getTodoRepository();
        eventRepo = repositoryFactory.getEventRepository();
        service = new TodoService(ctx, todoRepo, eventRepo);

        // Events on main
        subCreated = SimpleEventBus.subscribeOnMain(TodoCreatedEvent.class,
                ev -> appendLog("[EVENT] Created  → id=" + ev.todo.id + " title=" + ev.todo.title));
        subUpdated = SimpleEventBus.subscribeOnMain(TodoUpdatedEvent.class,
                ev -> appendLog("[EVENT] Updated  → id=" + ev.todo.id + " completed=" + ev.todo.completed));
        subDeleted = SimpleEventBus.subscribeOnMain(TodoDeletedEvent.class,
                ev -> appendLog("[EVENT] Deleted  → id=" + ev.todo.id));

        // Buttons
        startButton.setOnClickListener(v -> runShowcase());
        cancelButton.setOnClickListener(v -> runCancelableReadDemo());
        clearButton.setOnClickListener(v -> {
            logView.setText("");
            appendLog("🧹 Log cleared");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (subCreated != null) subCreated.close();
        } catch (Throwable ignored) {
        }
        try {
            if (subUpdated != null) subUpdated.close();
        } catch (Throwable ignored) {
        }
        try {
            if (subDeleted != null) subDeleted.close();
        } catch (Throwable ignored) {
        }
        subCreated = subUpdated = subDeleted = null;
    }

    // Showcase: Create → Select → UpdateSql → GetById → Delete
    private void runShowcase() {
        appendLog("⏳ Starting showcase…");

        Todo t = new Todo();
        t.userId = 1;
        t.title = "Buy milk";
        t.completed = false;

        CancellableFuture<DbResult<Todo>> createF = service.createWithAudit(t);

        createF
                .thenCompose(result -> {
                    if (!result.isSuccess()) return failFuture(result);
                    Todo created = result.getData();
                    appendOnMain("[CREATE] id=" + created.id);

                    Select<Todo> sel = Select.from(Todo.class)
                            .columns("id", "title", "completed")
                            .whereEq("userId", 1)
                            .orderBy("id", false)
                            .limit(50);
                    return todoRepo.selectWith(sel);
                })
                .thenCompose(result -> {
                    if (!result.isSuccess()) return failFuture(result);
                    appendOnMain("[SELECT] rows=" + result.getData().size());

                    Select<Todo> last = Select.from(Todo.class)
                            .columns("id", "title", "completed")
                            .orderBy("id", true) // DESC
                            .limit(1);
                    return todoRepo.selectWith(last);
                })
                .thenCompose(result -> {
                    if (!result.isSuccess() || result.getData().isEmpty())
                        return failFuture(result);
                    Todo latest = result.getData().get(0);

                    UpdateSql u = UpdateSql.build(Todo.class)
                            .set("completed", true)
                            .where().Equals("id", latest.id);

                    return todoRepo.updateWith(u)
                            .thenApply(upRes -> {
                                if (upRes.isSuccess())
                                    appendOnMain("[UPDATE] affected=" + upRes.getData());
                                return upRes;
                            });
                })
                .thenCompose(result -> {
                    if (!result.isSuccess()) return failFuture(result);

                    Select<Todo> last = Select.from(Todo.class)
                            .columns("id")
                            .orderBy("id", true)
                            .limit(1);

                    return todoRepo.selectWith(last)
                            .thenCompose(selRes -> {
                                if (!selRes.isSuccess() || selRes.getData().isEmpty())
                                    return failFuture(selRes);
                                int id = selRes.getData().get(0).id;
                                return todoRepo.getById(id)
                                        .thenApply(getRes -> {
                                            if (getRes.isSuccess()) {
                                                Todo got = getRes.getData();
                                                appendOnMain("[GET] id=" + (got == null ? "null" : got.id) +
                                                        " completed=" + (got == null ? "n/a" : got.completed));
                                            }
                                            return getRes;
                                        });
                            });
                })
                .thenCompose(result -> {
                    if (!result.isSuccess() || result.getData() == null) return failFuture(result);
                    Todo toDelete = result.getData();
                    return todoRepo.delete(toDelete)
                            .thenApply(delRes -> {
                                if (delRes.isSuccess()) appendOnMain("[DELETE] ok");
                                return delRes;
                            });
                })
                .whenCompleteAsync((res, ex) -> {
                    if (ex != null) {
                        appendLog("❌ Showcase failed: " + ex.getMessage());
                    } else if (!res.isSuccess()) {
                        appendLog("❌ Showcase failed: " + ((DbResult.Error<?>) res).getErrorMessage());
                    } else {
                        appendLog("✅ Showcase done");
                    }
                }, AndroidFutures.MAIN);
    }

    // helpers

    // Cancelable read demo
    private void runCancelableReadDemo() {
        appendLog("⏳ Starting cancelable read…");
        cancellableRead =
                ctx.runRead(db -> {
                    try {
                        java.util.concurrent.TimeUnit.MILLISECONDS.sleep(600);
                        return new DbResult.Success<>("OK");
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new DbResult.Error<>(ie);
                    }
                });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (cancellableRead != null) {
                cancellableRead.cancelWork();
                appendLog("✖ cancelWork() called");
            }
        }, 100);

        cancellableRead.whenCompleteAsync((r, e) -> {
            if (e != null) {
                appendLog("❌ Read failed/Cancelled: " + e.getClass().getSimpleName());
            } else if (r.isSuccess()) {
                appendLog("✅ Read completed: " + r.getData());
            } else {
                appendLog("❌ Read error: " + ((DbResult.Error<?>) r).getErrorMessage());
            }
            cancellableRead = null;
        }, AndroidFutures.MAIN);
    }

    private void appendOnMain(final String s) {
        AndroidFutures.MAIN.execute(() -> appendLog(s));
    }

    private void appendLog(String s) {
        logView.append(s + "\n");
        // otomatik aşağı kaydır
        logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
