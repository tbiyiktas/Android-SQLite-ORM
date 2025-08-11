# Android SQLite ORM

## 📚 Giriş
Android SQLite ORM, uygulamalarınızda veritabanı işlemlerini daha kolay, güvenli ve temiz bir şekilde yapmanızı sağlayan hafif ve esnek bir Object-Relational Mapping (ORM) kütüphanesidir. Geleneksel SQLite işlemlerindeki boilerplate kodları azaltır, veriye nesne tabanlı bir yaklaşımla erişmenize olanak tanır.

## ✨ Temel Özellikler
* **Anotasyon Tabanlı Eşleme:** `@DbTableAnnotation` ve `@DbColumnAnnotation` ile model sınıflarınızı doğrudan tablo/sütunlara eşleyin.
* **Generik Repository Deseni:** Tüm modeller için generic CRUD (Create, Read, Update, Delete) operasyonları sunar.
* **Güvenli ve Esnek Sorgu Oluşturma:** Builder deseniyle tasarlanmış `SelectQuery` ile zincirleme API (fluent) üzerinden parametreli, güvenli sorgular oluşturun.
* **Asenkron İşlemler:** Callback tabanlı sonuç modeli (`DbCallback` / `DbResult`) ile veritabanı işlemlerini arka planda, thread-safe bir şekilde yürütün.
* **Clean Code İlkeleri:** SRP, DRY ve katmanlı mimari gözetilerek tasarlanmıştır.

> **Not:** Bazı özelliklerin kapsamı sürümlere göre değişebilir. Ayrıntılar için sürüm notlarına bakın.

## 🚀 Başlangıç

### 1. Bağımlılığı Ekleyin
Projeye eklemek için `build.gradle` (app) dosyanıza aşağıdaki bağımlılığı ekleyin.

```groovy
// build.gradle (app)
dependencies {
    implementation 'com.github.tbiyiktas:Android-SQLite-ORM:1.0.0' // Versiyonu güncelleyin
}
```
### 2. Modelinizi Tanımlayın
@DbTableAnnotation ve @DbColumnAnnotation ile model sınıfınızı oluşturun.
```java
// app/src/main/java/com/example/model/Todo.java
@DbTableAnnotation(name = "todos")
public class Todo {

    @DbColumnAnnotation(name = "id", primaryKey = true, autoIncrement = true)
    private long id;

    @DbColumnAnnotation(name = "title")
    private String title;

    @DbColumnAnnotation(name = "is_completed")
    private boolean isCompleted;

    public Todo() { }

    public Todo(String title, boolean isCompleted) {
        this.title = title;
        this.isCompleted = isCompleted;
    }

    // getters & setters...
}
```
### 3. DbContext ve Repository’yi Tanımlayın
ADbContext’ten türeyerek veritabanı bağlantısını yönetin; GenericRepository<T>’den türeyerek modelinize özel repository yazın.

```java
// app/src/main/java/com/example/database/AppDb.java
public class AppDb extends ADbContext {
    public static final String DATABASE_NAME = "app_db";
    public static final int DATABASE_VERSION = 1;

    public AppDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tablo oluşturma
        db.execSQL("CREATE TABLE todos (" +
                   "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   "title TEXT, " +
                   "is_completed INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Basit örnek: drop & recreate (gelişmiş migration için bkz. Yol Haritası)
        db.execSQL("DROP TABLE IF EXISTS todos");
        onCreate(db);
    }
}
```

```java
// app/src/main/java/com/example/repository/TodoRepository.java
public class TodoRepository extends GenericRepository<Todo> {
    public TodoRepository(ADbContext dbContext) {
        super(dbContext, Todo.class);
    }
}
```
### 4. Kullanım
Repository üzerinden CRUD ve sorgu işlemlerini gerçekleştirin.

```java
// Örnek kullanım (ör. bir Activity/Fragment içinde)
AppDb dbContext = new AppDb(this);
TodoRepository todoRepository = new TodoRepository(dbContext);

// Yeni kayıt ekleme
Todo newTodo = new Todo("Ödevi tamamla", false);
todoRepository.insert(newTodo, result -> {
    if (result.isSuccess()) {
        System.out.println("Kayıt eklendi: " + newTodo.getId());
    } else {
        System.err.println("Hata: " + result.getError());
    }
});

// Koşullu sorgu (ör. tamamlanmamışlar) ve sayfalama
todoRepository.selectWhere(
    "is_completed = ?", new String[]{"0"},
    new DbCallback<ArrayList<Todo>>() {
        @Override
        public void onResult(DbResult<ArrayList<Todo>> result) {
            if (result.isSuccess()) {
                System.out.println("Bulunan kayıt: " + result.getData().size());
            } else {
                System.err.println("Hata: " + result.getError());
            }
        }
    }
);
```
<b>İpucu:</b> Sorgularda parametre bağlama (? + args[]) kullanarak SQL enjeksiyon risklerini azaltın.

## 🏛 Mimari ve Tasarım
### Desenler
Repository Deseni: Veri erişim mantığı soyutlanır; üst katmanlar yalnızca arayüzle konuşur.

#### Builder Deseni: SelectQuery ile esnek ve zincirleme sorgu oluşturma.

#### Command Deseni: DDL/DML işlemleri (Insert, Update, Delete, Select) nesneleştirilir.

#### Dependency Injection: Bağımlılıklar kurucu metotlar üzerinden enjekte edilebilir.

### Clean Code İlkeleri
#### SRP: Anotasyonlar, eşleme (mapping) ve komut/sorgu sınıfları ayrı sorumluluklara sahip.

#### DRY: Ortak SQL üretimi ve hata/sunum modelleri yeniden kullanılır.

### Test Edilebilirlik: Repository/Context arayüzleri üzerinden birim test imkânı.

## 🔐 Güvenlik ve Performans İpuçları
#### Parametreli Sorgular: WHERE name = ? ve argüman dizisi kullanın.

####### Kaynak Yönetimi: Cursor/statement’lar için try-with-resources kullanın.

#### Önbellek: Reflection tabanlı eşlemelerde meta-veri cache’i ile maliyeti düşürün.

#### ProGuard/R8: Anotasyon’lu entity alanları ve varsayılan ctor’lar için keep kuralları ekleyin.

## 📄 Lisans
Bu proje MIT lisansı ile dağıtılmaktadır. Ayrıntılar için LICENSE dosyasına bakınız.
