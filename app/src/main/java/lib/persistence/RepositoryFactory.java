package lib.persistence;

import android.content.Context;

import com.example.adbkit.MyApplication;

import lib.persistence.repositories.TodoRepository;

public final class RepositoryFactory {
    private RepositoryFactory() {}

    public static TodoRepository getTodoRepository(Context appContext) {
        return new TodoRepository(((MyApplication)appContext.getApplicationContext()).getDbContext());
    }
}
/*
public class RepositoryFactory {

    // DbContext sınıfınızın AppDbContext olduğunu varsayıyoruz
    private static ADbContext adbContextInstance;

    // Uygulama Context'ini sadece bir kez alıp kullanıyoruz
    private static synchronized ADbContext getDbContextInstance(Context context) {
        if (adbContextInstance == null) {
            // AppDbContext, ADbContext'i miras alıyor
            adbContextInstance = new DbContext(context);
        }
        return adbContextInstance;
    }

    // Repository'ler için fabrika metotları
    public static TodoRepository getTodoRepository(Context context) {
        return new TodoRepository(getDbContextInstance(context.getApplicationContext()));
    }
}
*/