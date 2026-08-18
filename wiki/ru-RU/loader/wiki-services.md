[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Сервисы

## 🔧 Сервисы

`ServiceManager` — это централизованный потокобезопасный реестр (Service Locator) загрузчика Avrix, предназначенный для регистрации, поиска и взаимодействия сервисов по их типам и интерфейсам.

Он обеспечивает слабую связность (Loose Coupling) между плагинами: один плагин может предоставить API (интерфейс) и зарегистрировать его реализацию, а другие плагины — использовать сервис без прямой зависимости от конкретных классов реализации.

---

### 🧩 Регистрация сервиса

Для регистрации сервиса в `ServiceManager` используется метод `register(Class<T> serviceType, T implementation)`. На один тип контракта регистрируется ровно один экземпляр сервиса.

```java
package com.example.database;

import com.avrix.core.ServiceManager;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Интерфейс сервиса — публичный контракт
 */
public interface DatabaseService {
    void connect(String url);
    void disconnect();
    boolean isConnected();
}

/**
 * Реализация сервиса
 */
public class PostgreSQLService implements DatabaseService {
    @Override
    public void connect(String url) {
        // Логика подключения
    }

    @Override
    public void disconnect() {
        // Логика отключения
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}

/**
 * Точка входа плагина-поставщика сервиса
 */
public class DatabasePlugin implements Plugin {

    @Override
    public void onInitialize(PluginData pluginData) {
        // Регистрация реализации под интерфейсом контракта
        ServiceManager.register(DatabaseService.class, new PostgreSQLService());
    }
}
```

> [!WARNING]
> Если сервис для указанного класса уже был зарегистрирован ранее, метод `ServiceManager.register` выбросит `IllegalStateException`. Это защищает среду от случайных коллизий реализаций.

---

### 🛫 Получение сервиса

Получить зарегистрированный сервис можно двумя способами в зависимости от того, является ли зависимость обязательной:

#### 1. Обязательный сервис (`get`)
Если вашему плагину сервис критически необходим для работы. Выбрасывает `IllegalStateException`, если сервис не найден:

```java
package com.example.consumer;

import com.avrix.core.ServiceManager;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;
import com.example.database.DatabaseService;

public class ConsumerPlugin implements Plugin {

    @Override
    public void onInitialize(PluginData pluginData) {
        // Прямое получение (выбросит исключение, если сервис отсутствует)
        DatabaseService dbService = ServiceManager.get(DatabaseService.class);
        dbService.connect("jdbc:postgresql://localhost:5432/zomboid");
    }
}
```

#### 2. Опциональный сервис (`find`)
Если ваш плагин может работать без стороннего сервиса и лишь добавляет опциональные интеграции:

```java
package com.example.consumer;

import com.avrix.core.ServiceManager;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;
import com.example.database.DatabaseService;

public class OptionalConsumerPlugin implements Plugin {

    @Override
    public void onInitialize(PluginData pluginData) {
        // Безопасный поиск через Optional
        ServiceManager.find(DatabaseService.class).ifPresent(db -> {
            db.connect("jdbc:postgresql://localhost:5432/zomboid");
        });
    }
}
```

---

### 🔍 Проверка наличия сервиса

Для быстрой проверки регистрации типа без извлечения экземпляра используется метод `contains`:

```java
if (ServiceManager.contains(DatabaseService.class)) {
    // Сервис доступен
}
```

---

### 🧹 Отмена регистрации сервиса

Если необходимо удалить сервис из реестра (например, при завершении работы или выгрузке модуля):

```java
// Возвращает true, если сервис был найден и успешно удален
boolean removed = ServiceManager.unregister(DatabaseService.class);
```