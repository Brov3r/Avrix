[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Сервисы

## 🔧 Сервисы

`ServiceManager` — это централизованный потокобезопасный реестр (Service Locator) загрузчика Avrix, предназначенный для регистрации, поиска и взаимодействия сервисов по их типам и интерфейсам.

Он обеспечивает слабую связность (Loose Coupling) между плагинами: один плагин может предоставить API (интерфейс) и зарегистрировать его реализацию, а другие плагины — использовать сервис без прямой зависимости от конкретных классов реализации.

---

### 🧱 Встроенные сервисы

Загрузчик регистрирует следующие контракты при старте:

| Контракт          | Реализация                                                    |
|-------------------|---------------------------------------------------------------|
| `KnotClassLoader` | Плоский трансформирующий загрузчик классов, в котором работает рантайм |
| `LoaderProvider`  | Имя, версия, авторы, лицензия и контакты загрузчика           |
| `GameProvider`    | Активный игровой провайдер клиента или выделенного сервера    |
| `PluginManager`   | Активный менеджер плагинов                                    |

Эти сервисы принадлежат загрузчику. Никогда не отменяйте их регистрацию и не заменяйте их. Все четыре сервиса доступны
к моменту вызова `onInitialize` у плагинов. При закрытии runtime загрузчик сам снимает свои регистрации; сервисы,
принадлежащие плагинам, должны сниматься их плагинами в `onShutdown`. Если плагин использует сервис, который
регистрирует другой плагин, объявите зависимость или `loadAfter`, чтобы плагин-поставщик инициализировался первым.

---

### 🧩 Регистрация сервиса

Для регистрации сервиса в `ServiceManager` используется метод `register(Class<T> serviceType, T implementation)`. На один тип контракта регистрируется ровно один экземпляр сервиса.

```java
package com.example.database;

import com.avrix.core.ServiceManager;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Service interface - public contract.
 */
public interface DatabaseService {
    void connect(String url);
    void disconnect();
    boolean isConnected();
}

/**
 * Service implementation.
 */
public class PostgreSQLService implements DatabaseService {
    @Override
    public void connect(String url) {
        throw new UnsupportedOperationException("Connect implementation is omitted from this example.");
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException("Disconnect implementation is omitted from this example.");
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}

/**
 * Entrypoint of the service provider plugin.
 */
public class DatabasePlugin implements Plugin {

    @Override
    public void onInitialize(PluginData pluginData) {
        ServiceManager.register(DatabaseService.class, new PostgreSQLService());
    }
}
```

> [!WARNING]
> Если сервис для указанного класса уже был зарегистрирован ранее, метод `ServiceManager.register` выбросит `IllegalStateException`. Это защищает среду от случайных коллизий реализаций.

Регистрируйте сервисы в `onInitialize`, а отменяйте регистрацию только тех контрактов, которыми владеет ваш плагин, в
`onShutdown`:

```java
@Override
public void onShutdown() {
    ServiceManager.unregister(DatabaseService.class);
}
```

Загрузчик вызывает callback'и завершения плагинов в обратном порядке загрузки через `PluginManager.close()`.

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
    // Use this only when the presence of the service changes your plugin's behavior.
}
```

---

### 🧹 Отмена регистрации сервиса

Если необходимо удалить привязку сервиса во время работы (например, в тестах или при замене реализации, которой вы
владеете):

```java
boolean removed = ServiceManager.unregister(DatabaseService.class);
```

> [!WARNING]
> `ServiceManager` — глобальный реестр. Не удаляйте сервисы загрузчика или других плагинов, если не управляете их
> жизненным циклом.