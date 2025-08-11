package com.example.adbkit;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

import lib.persistence.DbCallback;
import lib.persistence.DbResult;
import lib.persistence.domain.entities.Todo;
import lib.persistence.repositories.TodoRepository;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TodoRepository todoRepository;

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_main.xml dosyasını varsayıyoruz

        // Veritabanı Repository'sini başlatma
        todoRepository = new TodoRepository(getApplicationContext());

        // Örnek bir işlem başlatmak için bir buton tanımlayalım
        Button startDbOpsButton = findViewById(R.id.start_db_ops_button);
        if (startDbOpsButton != null) {
            startDbOpsButton.setOnClickListener(v -> startDbOperations());
        }
    }

    private void startDbOperations() {
        Log.d(TAG, "Veritabanı işlemleri başlatılıyor...");

        // CREATE: Yeni bir Todo oluşturma
        createTodo();
    }

    // Asenkron CREATE işlemi
    private void createTodo() {
        Todo newTodo = new Todo();
        newTodo.userId = 1;
        newTodo.title = "Alışveriş yap";
        newTodo.completed = false;

        Log.d(TAG, "Yeni bir Todo oluşturuluyor...");
        todoRepository.create(newTodo, new DbCallback<Todo>() {
            @Override
            public void onResult(DbResult<Todo> result) {
                if (result instanceof DbResult.Success) {
                    Todo createdTodo = ((DbResult.Success<Todo>) result).getData();
                    Log.d(TAG, "CREATE - Başarılı: " + createdTodo);

                    // İşlem başarılıysa, bir sonraki işleme geçelim
                    readAllTodos();
                } else {
                    Exception e = ((DbResult.Error) result).getException();
                    Log.e(TAG, "CREATE - Hata: " + e.getMessage());
                }
            }
        });
    }

    // Asenkron READ işlemi (Tüm Todoları oku)
    private void readAllTodos() {
        Log.d(TAG, "Tüm Todolar okunuyor...");
        todoRepository.getAll(new DbCallback<ArrayList<Todo>>() {
            @Override
            public void onResult(DbResult<ArrayList<Todo>> result) {
                if (result.isSuccess()) {
                    ArrayList<Todo> todos = ((DbResult.Success<ArrayList<Todo>>) result).getData();
                    Log.d(TAG, "READ ALL - Başarılı, bulunan Todo sayısı: " + todos.size());
                    for (Todo todo : todos) {
                        Log.d(TAG, "READ ALL - Todo: " + todo);
                    }

                    // Bir sonraki adıma geçelim: güncelleme
                    if (!todos.isEmpty()) {
                        Todo firstTodo = todos.get(0);
                        updateTodo(firstTodo);
                    }
                } else {
                    Exception e = ((DbResult.Error) result).getException();
                    Log.e(TAG, "READ ALL - Hata: " + e.getMessage());
                }
            }
        });
    }

    // Asenkron UPDATE işlemi
    private void updateTodo(Todo todoToUpdate) {
        // İlk Todonun tamamlanma durumunu değiştirelim
        todoToUpdate.completed = true;
        Log.d(TAG, "Todo güncelleniyor: " + todoToUpdate);
        todoRepository.update(todoToUpdate, new DbCallback<Todo>() {
            @Override
            public void onResult(DbResult<Todo> result) {
                if (result instanceof DbResult.Success) {
                    Todo updatedTodo = ((DbResult.Success<Todo>) result).getData();
                    Log.d(TAG, "UPDATE - Başarılı: " + updatedTodo);

                    // Bir sonraki adıma geçelim: okuma
                    readUpdatedTodo(updatedTodo.id);
                } else {
                    Exception e = ((DbResult.Error) result).getException();
                    Log.e(TAG, "UPDATE - Hata: " + e.getMessage());
                }
            }
        });
    }

    // Asenkron READ işlemi (Güncellenen Todoyu oku)
    private void readUpdatedTodo(int todoId) {
        Log.d(TAG, "ID " + todoId + " olan Todo okunuyor...");
        todoRepository.getById(todoId, new DbCallback<Todo>() {
            @Override
            public void onResult(DbResult<Todo> result) {
                if (result instanceof DbResult.Success) {
                    Todo foundTodo = ((DbResult.Success<Todo>) result).getData();
                    if (foundTodo != null) {
                        Log.d(TAG, "READ BY ID - Başarılı: " + foundTodo);
                        // Bir sonraki adıma geçelim: silme
                        deleteTodo(foundTodo);
                    } else {
                        Log.d(TAG, "READ BY ID - Hata: Todo bulunamadı.");
                    }
                } else {
                    Exception e = ((DbResult.Error) result).getException();
                    Log.e(TAG, "READ BY ID - Hata: " + e.getMessage());
                }
            }
        });
    }

    // Asenkron DELETE işlemi
    private void deleteTodo(Todo todoToDelete) {
        Log.d(TAG, "Todo siliniyor: " + todoToDelete.id);
        todoRepository.delete(todoToDelete, new DbCallback<Todo>() {
            @Override
            public void onResult(DbResult<Todo> result) {
                if (result instanceof DbResult.Success) {
                    Log.d(TAG, "DELETE - Başarılı: Todo silindi.");
                    // Son kontrol için tüm todoları tekrar okuyalım
                    readAllTodos();
                } else {
                    Exception e = ((DbResult.Error) result).getException();
                    Log.e(TAG, "DELETE - Hata: " + e.getMessage());
                }
            }
        });
    }

    /*
    *
 // Ham SQL sorgusu
String sql = "SELECT p.name AS personName, t.title AS todoTitle FROM persons p INNER JOIN todos t ON p.id = t.personId WHERE p.age > 30";

// Geri çağırma metodu ile sonucu yakalama
dbContext.rawQuery(sql, null, new DbCallback<ArrayList<HashMap<String, String>>>() {
    @Override
    public void onResult(DbResult<ArrayList<HashMap<String, String>>> result) {
        if (result instanceof DbResult.Success) {
            ArrayList<HashMap<String, String>> rows = result.Data();
            for (HashMap<String, String> row : rows) {
                String personName = row.get("personName");
                String todoTitle = row.get("todoTitle");
                // ... veriyi işleme
            }
        } else {
            // ... hata yönetimi
        }
    }
});
 * */
}