[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Сервисы

## 🔧 Сервисы

`ServiceManager` — это потокобезопасный реестр для регистрации и получения реализаций сервисов по их интерфейсам. Он
позволяет создавать модульную архитектуру плагинов, где компоненты слабо связаны и могут быть легко заменены или
протестированы.

### 🧩 Регистрация сервиса

Чтобы сделать реализацию сервиса доступной для других компонентов, её необходимо зарегистрировать в `ServiceManager`:

```java
import com.avrix.api.services.ServiceManager;

/**
 * Интерфейс сервиса — контракт, который определяет доступные операции
 */
public interface DatabaseService {
    void connect(String connectionString);

    void disconnect();

    boolean isConnected();
}

/**
 * Конкретная реализация сервиса
 */
public class PostgreSQLService implements DatabaseService {
    @Override
    public void connect(String connectionString) {
        // Логика подключения к PostgreSQL
    }

    @Override
    public void disconnect() {
        // Логика отключения
    }

    @Override
    public boolean isConnected() {
        // Проверка статуса соединения
        return true;
    }
}

/**
 * Точка входа плагина, которая обязательно расширяет абстрактный класс Plugin
 */
public class ExamplePlugin extends Plugin {
    /**
     * Конструктор плагина, все аргументы передаются динамически, в ручную указывать ничего не нужно
     *
     * @param metadata   метаданные плагина
     * @param pluginFile файл плагина (файл .jar архива)
     * @param iconURI    URI иконки плагина. Может быть null, если в архиве плагина нет иконки или возникли какие-то проблемы.
     */
    public ExamplePlugin(Metadata metadata, File pluginFile, URI iconURI) {
        super(metadata, pluginFile, iconURI);
    }

    /**
     * Событие инициалиации плагина. Основная логика реализуется именно здесь. 
     * Используетсяд для подписки на события, регистрации команд и прочего.
     * Вызывается до старта игры/сервера
     */
    @Override
    public void onInitialize() {
        ServiceManager.register(DatabaseService.class, new PostgreSQLService());
    }
}
```

### 🛫 Получение зарегистрированного сервиса

Для использования сервиса в любом месте кода достаточно запросить его по интерфейсу:

```java
/**
 * Точка входа плагина, которая обязательно расширяет абстрактный класс Plugin
 */
public class OtherExamplePlugin extends Plugin {
    /**
     * Конструктор плагина, все аргументы передаются динамически, в ручную указывать ничего не нужно
     *
     * @param metadata   метаданные плагина
     * @param pluginFile файл плагина (файл .jar архива)
     * @param iconURI    URI иконки плагина. Может быть null, если в архиве плагина нет иконки или возникли какие-то проблемы.
     */
    public ExamplePlugin(Metadata metadata, File pluginFile, URI iconURI) {
        super(metadata, pluginFile, iconURI);
    }

    /**
     * Событие инициалиации плагина. Основная логика реализуется именно здесь. 
     * Используетсяд для подписки на события, регистрации команд и прочего.
     * Вызывается до старта игры/сервера
     */
    @Override
    public void onInitialize() {
        // Получение сервиса — возвращает Optional для безопасной работы
        Optional<DatabaseService> dbServiceOpt = ServiceManager.getService(DatabaseService.class);

        // Рекомендуемый способ использования:
        dbServiceOpt.ifPresent(service -> {
            service.connect("jdbc:postgresql://localhost:5432/mydb");
            // ... работа с сервисом
        });

        // Или с обработкой отсутствия сервиса:
        DatabaseService service = dbServiceOpt.orElseThrow(() ->
                new IllegalStateException("DatabaseService not registered")
        );
    }
}
```

### 🧹 Отмена регистрации сервиса

Если необходимо удалить сервис из реестра (например, принудительная замена реализации или выгрузка плагина):

```java
/**
 * Точка входа плагина, которая обязательно расширяет абстрактный класс Plugin
 */
public class OtherExamplePlugin extends Plugin {
    /**
     * Конструктор плагина, все аргументы передаются динамически, в ручную указывать ничего не нужно
     *
     * @param metadata   метаданные плагина
     * @param pluginFile файл плагина (файл .jar архива)
     * @param iconURI    URI иконки плагина. Может быть null, если в архиве плагина нет иконки или возникли какие-то проблемы.
     */
    public ExamplePlugin(Metadata metadata, File pluginFile, URI iconURI) {
        super(metadata, pluginFile, iconURI);
    }

    /**
     * Событие инициалиации плагина. Основная логика реализуется именно здесь. 
     * Используетсяд для подписки на события, регистрации команд и прочего.
     * Вызывается до старта игры/сервера
     */
    @Override
    public void onInitialize() {
        // Безопасно: если сервис не был зарегистрирован, исключение не выбрасывается
        ServiceManager.unregister(DatabaseService.class);
    }
}
```