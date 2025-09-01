package com.example.adbkit;


import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.adbkit.DbContext; // Gerçek DbContext implementasyonunuz
import com.example.adbkit.entities.Todo;
import com.example.adbkit.repositories.TodoRepository;

import lib.persistence.DbCallback;
import lib.persistence.DbResult;
import lib.persistence.IDbContext; // IDbContext kullanın
import lib.persistence.command.query.Select;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TodoRepositoryTest {

    private IDbContext dbContext; // IDbContext kullanın
    private TodoRepository todoRepository;
    private Context appContext;

    @Before
    public void setUp() throws Exception {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        appContext.deleteDatabase("repo_test.db"); // Her test için temiz DB
        dbContext = new DbContext(appContext, "repo_test.db", 1); // Gerçek DbContext
        todoRepository = new TodoRepository(dbContext);

        // Şemanın oluştuğundan emin olmak için bir kere writable db alalım.
        // Bu normalde dbContext instance'ı ilk defa kullanıldığında olur.
        ((DbContext)dbContext).getWritableDatabase().close(); // Cast gerekebilir
    }

    @After
    public void tearDown() throws Exception {
        dbContext.close();
        appContext.deleteDatabase("repo_test.db");
    }

    @Test
    public void insertAndGetById_shouldWorkCorrectly() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2); // insert + getById
        final Todo newTodo = new Todo();
        newTodo.userId = 10;
        newTodo.title = "Test Insert";
        newTodo.completed = false;

        final int[] insertedId = {-1};

        todoRepository.insert(newTodo, result -> {
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertTrue(result.getData().id > 0); // Identity PK atanmalı
            insertedId[0] = result.getData().id;
            Assert.assertEquals("Test Insert", result.getData().title);
            latch.countDown();

            // Şimdi getById ile oku
            todoRepository.getById(insertedId[0], getResult -> {
                assertTrue(getResult.isSuccess());
                assertNotNull(getResult.getData());
                Assert.assertEquals(insertedId[0], getResult.getData().id);
                Assert.assertEquals("Test Insert", getResult.getData().title);
                assertFalse(getResult.getData().completed);
                latch.countDown();
            });
        });

        assertTrue("Database operations did not complete in time", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void update_shouldModifyEntity() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3); // insert + update + get
        final Todo todo = new Todo();
        todo.userId = 20;
        todo.title = "To Be Updated";
        todo.completed = false;

        final int[] currentId = new int[1];

        todoRepository.insert(todo, insertResult -> {
            assertTrue(insertResult.isSuccess());
            currentId[0] = insertResult.getData().id;
            latch.countDown();

            Todo toUpdate = insertResult.getData();
            toUpdate.title = "Updated Title";
            toUpdate.completed = true;

            todoRepository.update(toUpdate, updateResult -> {
                assertTrue(updateResult.isSuccess());
                Assert.assertEquals("Updated Title", updateResult.getData().title);
                latch.countDown();

                todoRepository.getById(currentId[0], getResult -> {
                    assertTrue(getResult.isSuccess());
                    assertNotNull(getResult.getData());
                    Assert.assertEquals("Updated Title", getResult.getData().title);
                    assertTrue(getResult.getData().completed);
                    latch.countDown();
                });
            });
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void delete_shouldRemoveEntity() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3); // insert + delete + get(null)
        final Todo todo = new Todo();
        todo.userId = 30;
        todo.title = "To Be Deleted";

        final int[] currentId = new int[1];

        todoRepository.insert(todo, insertResult -> {
            assertTrue(insertResult.isSuccess());
            currentId[0] = insertResult.getData().id;
            latch.countDown();

            todoRepository.delete(insertResult.getData(), deleteResult -> {
                assertTrue(deleteResult.isSuccess());
                latch.countDown();

                todoRepository.getById(currentId[0], getResult -> {
                    assertTrue(getResult.isSuccess()); // İşlem başarılı
                    assertNull(getResult.getData());   // Ama data null gelmeli
                    latch.countDown();
                });
            });
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void selectAll_andSelectWith_shouldReturnCorrectData() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);

        Todo todo1 = new Todo(); todo1.userId = 40; todo1.title = "Item A"; todo1.completed = false;
        Todo todo2 = new Todo(); todo2.userId = 40; todo2.title = "Item B"; todo2.completed = true;
        Todo todo3 = new Todo(); todo3.userId = 41; todo3.title = "Item C"; todo3.completed = false;

        todoRepository.insert(todo1, r1 -> {
            todoRepository.insert(todo2, r2 -> {
                todoRepository.insert(todo3, r3 -> {
                    latch.countDown(); // All inserts done

                    // Select All
                    todoRepository.selectAll(selectAllResult -> {
                        assertTrue(selectAllResult.isSuccess());
                        Assert.assertEquals(3, selectAllResult.getData().size());
                        latch.countDown();

                        // Select With
                        Select<Todo> builder = Select.from(Todo.class)
                                .whereEq("userId", 40)
                                .whereEq("completed", false); // Item A
                        todoRepository.selectWith(builder, selectWithResult -> {
                            assertTrue(selectWithResult.isSuccess());
                            Assert.assertEquals(1, selectWithResult.getData().size());
                            Assert.assertEquals("Item A", selectWithResult.getData().get(0).title);
                            latch.countDown();
                        });
                    });
                });
            });
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    // UpdateWith ve DeleteWhere için benzer testler yazılabilir.
}
