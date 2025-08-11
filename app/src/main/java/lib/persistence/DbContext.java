package lib.persistence;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import lib.persistence.domain.entities.Person;
import lib.persistence.domain.entities.Todo;
import lib.persistence.command.definition.CreateIndexCommand;
import lib.persistence.command.definition.CreateTableCommand;
import lib.persistence.command.definition.DropIndexCommand;
import lib.persistence.command.definition.DropTableCommand;

public class DbContext extends ADbContext {
    private static final String dbName = "local.db";
    private static final int version = 1;

    public DbContext(Context context) {
        super(context, dbName, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // SQLiteOpenHelper zaten thread-safe olduğu için ekstra synchronized bloğuna gerek yoktur.
        // Bu metod, veritabanı ilk kez oluşturulduğunda çalışır.
            sqLiteDatabase.execSQL(CreateTableCommand.build(Person.class).getQuery());
            sqLiteDatabase.execSQL(CreateTableCommand.build(Todo.class).getQuery());
            sqLiteDatabase.execSQL(CreateIndexCommand.build(Person.class, "idxPersonId", false, "id").getQuery());

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // SQLiteOpenHelper zaten thread-safe olduğu için ekstra synchronized bloğuna gerek yoktur.
        // Bu metod, veritabanı versiyonu yükseltildiğinde çalışır.

        // Bu basit bir upgrade stratejisidir (tüm tabloları silip yeniden oluşturur).
        // Gerçek projelerde veri kaybını önlemek için daha gelişmiş migration adımları uygulanmalıdır.
            sqLiteDatabase.execSQL(DropIndexCommand.build("idxPersonId").getQuery());
            sqLiteDatabase.execSQL(DropTableCommand.build(Person.class).getQuery());
            sqLiteDatabase.execSQL(DropTableCommand.build(Todo.class).getQuery());

        onCreate(sqLiteDatabase);
    }
}