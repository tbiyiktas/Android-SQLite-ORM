// com/example/adbkit/repositories/TodoRepositoryIntegrationTest.java
package com.example.adbkit.repositories;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;

import com.example.adbkit.MyApplication;
import com.example.adbkit.entities.Todo;
import com.example.adbkit.events.SimpleEventBus;
import com.example.adbkit.events.TodoCreatedEvent;
import com.example.adbkit.events.TodoDeletedEvent;
import com.example.adbkit.events.TodoUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import lib.persistence.core.DbResult;
import lib.persistence.core.IDbContext;
import lib.persistence.repository.RepositoryFactory;
import testutil.UiTestHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(application = MyApplication.class)
public class TodoRepositoryIntegrationTest {

    private TodoRepository repo;
    private int createdCount, updatedCount, deletedCount;

    @Before public void setup() {
        // Robolectric'te Application'ın init edilmesini garanti et
        MyApplication app = (MyApplication) ApplicationProvider.getApplicationContext();

        // Instance-scope factory: testte hangi context'i istiyorsan onu ver
        IDbContext db = MyApplication.getDbContext();
        RepositoryFactory repositoryFactory = new RepositoryFactory(db);
        repo = repositoryFactory.getTodoRepository();

        createdCount = updatedCount = deletedCount = 0;
        SimpleEventBus.subscribe(TodoCreatedEvent.class, e -> createdCount++);
        SimpleEventBus.subscribe(TodoUpdatedEvent.class, e -> updatedCount++);
        SimpleEventBus.subscribe(TodoDeletedEvent.class, e -> deletedCount++);
    }

    @Test public void crud_flow_emitsEventsOnUiThread() {
        // INSERT
        Todo t = new Todo(); t.title = "A"; t.userId = 1; t.completed = false;
        DbResult<Todo> ins = repo.insert(t).join();
        assertTrue(ins.isSuccess());
        assertTrue(ins.getData().id > 0);
        UiTestHelpers.flushUi(); // AfterCommitEvents + UI publish

        // UPDATE
        t = ins.getData(); t.completed = true;
        DbResult<Todo> up = repo.update(t).join();
        assertTrue(up.isSuccess());
        UiTestHelpers.flushUi();

        // DELETE
        DbResult<Todo> del = repo.delete(t).join();
        assertTrue(del.isSuccess());
        UiTestHelpers.flushUi();

        assertEquals(1, createdCount);
        assertEquals(1, updatedCount);
        assertEquals(1, deletedCount);
    }
}
