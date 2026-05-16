[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Конфиги

## ⚙️ Конфиги

`ConfigManager` — это утилитарный класс для работы с конфигурационными файлами формата YAML. Он предоставляет удобный API для создания, загрузки, сохранения и редактирования конфигов, поддерживая как файловую систему, так и извлечение ресурсов из JAR-архивов.

🎯 Основные возможности
- Загрузка конфигов из файловой системы или JAR с автоматическим извлечением
- Приоритет пользовательских конфигов (файл на диске переопределяет ресурс в JAR)
- Get-or-create паттерн: loadOrCreate() для лёгкой инициализации
- Читаемый YAML-вывод с настраиваемым стилем (BLOCK / FLOW)
- Защита от path traversal и изоляция work directory для безопасности
- Потокобезопасное чтение; запись требует внешней синхронизации

> [!WARNING]
> **Важно**: Все YAML-файлы должны быть в кодировке UTF-8. Комментарии в YAML игнорируются при загрузке.

### 🧩 Создание конфигурации

Для программного создания конфига используйте метод create():

```java
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
        try {
            Path workDir = ConfigManager.getWorkDir(pluginFile); // Получение рабочей директории - '<папки с игрой>/plugins/<название папки плагина>/'
            ConfigurationNode config = ConfigManager.create();

            config.node("server", "host").set("localhost");
            config.node("server", "port").set(8080);
            config.node("features", "auth").set(true);
            config.node("players", "max").set(100);

            ConfigManager.save(config, workDir.resolve("config.yml")); // сохранение по пути - '<папки с игрой>/plugins/<название папки плагина>/config.yml'
        } catch (Exception e) {
			// Обработка ошибок
        }
    }
}
```

Результат:

```yaml
# config.yml
server:
    host: localhost
    port: 8080
features:
    auth: true
players:
    max: 100
```

### 🎨 Настройка стиля вывода

```java
// BLOCK: человекочитаемый, с отступами (по умолчанию)
ConfigurationNode blockConfig = ConfigManager.create(
    ConfigurationOptions.defaults(), 
    NodeStyle.BLOCK
);

// FLOW: компактный, в одну строку
ConfigurationNode flowConfig = ConfigManager.create(
    ConfigurationOptions.defaults(), 
    NodeStyle.FLOW
);

// Стиль применяется только при save(), не хранится в узле
ConfigManager.save(blockConfig, Path.of("readable.yml"), NodeStyle.BLOCK);
ConfigManager.save(flowConfig, Path.of("compact.yml"), NodeStyle.FLOW);
```

Результат:

```yaml
# readable.yml (BLOCK)
server:
  host: localhost
  port: 8080

# compact.yml (FLOW)
{server: {host: localhost, port: 8080}}
```

### 📥 Загрузка конфигурации

Из файловой системы

```java
// Простая загрузка существующего файла (любой путь в файловой системе)
ConfigurationNode config = ConfigManager.load(Path.of("config.yml"));

// Path workDir = ConfigManager.getWorkDir(pluginFile);
// ConfigurationNode config = ConfigManager.load(workDir.resolve("config.yml")); // Загрузка из '<папки с игрой>/plugins/<название папки плагина>/config.yml'


// Чтение значений
String host = config.node("server", "host").getString(); // Если пустой - вернет null
int port = config.node("server", "port").getInt(8080); // со значение по умолчанию
```
> [!WARNING]
> ⚠️ Файл должен существовать. Если нет — будет выброшено ConfigurateException.

Из JAR-архива (с авто-извлечением)

```java
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
        try {
            // Загрузка дефолтного конфига (config.yml из корня JAR)
            ConfigurationNode defaultCfg = ConfigManager.loadDefault(pluginFile); // Если файл существует в рабочей папки (work dir), приоритет будет отдан ему
            String appName = defaultCfg.node("app", "name").getString();
            
            // Загрузка вложенного конфига из подпапки в JAR
            ConfigurationNode dbCfg = ConfigManager.load(pluginFile, "database/config.yml"); // Если файл существует в рабочей папки (work dir), приоритет будет отдан ему
            String dbUrl = dbCfg.node("connection", "url").getString();
            
        } catch (IOException e) {
            // Обработка ошибок
        }
    }
}
```

Как работает извлечение:
1. Проверяется workDir: <jar-dir>/<sanitized-jar-name>/
2️. Если конфиг уже есть на диске → загружается он (пользовательские правки!)
3️. Если нет → ищется в JAR, извлекается в workDir с сохранением структуры, затем загружается
4️. Если в JAR нет → FileNotFoundException

> [!NOTE]
> ⚠️ Пути нормализуются, попытки выхода за пределы workDir (../../../) блокируются.

`loadOrCreate()` идеально для инициализации пользовательских конфигов:

```java
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
	try {
        // Загружает существующий файл ИЛИ создаёт новый пустой узел
        ConfigurationNode userCfg = ConfigManager.loadOrCreate(
            pluginFile, 
            Path.of("user-settings.yml")
        );
        
        // Если файл новый — узел будет виртуальным (пустым)
        if (userCfg.node("theme").virtual()) {
            // Устанавливаем дефолтные значения
            userCfg.node("theme").set("dark");
            userCfg.node("language").set("ru");
            userCfg.node("notifications").set(true);
            
            // Сохраняем на диск
            Path workDir = ConfigManager.getWorkDir(pluginFile);
            ConfigManager.save(userCfg, workDir.resolve("user-settings.yml"));
        }
        
        // Читаем настройки (с дефолтами для безопасности)
        String theme = userCfg.node("theme").getString("light");
        
    } catch (IOException e) {
        // Обработка ошибок
    }
}
```

### 💾 Сохранение конфигурации

```java
// Сохранение с дефолтным стилем (BLOCK)
ConfigManager.save(config, Path.of("output/config.yml"));

// Сохранение с явным указанием стиля
ConfigManager.save(config, Path.of("compact.yml"), NodeStyle.FLOW);

// Сохранение в workDir плагина
Path workDir = ConfigManager.getWorkDir(pluginFile);
ConfigManager.save(config, workDir.resolve("plugin-config.yml"));
```

> [!NOTE]
> ✅ Родительские директории создаются автоматически. Существующие файлы перезаписываются.


### ✏️ Чтение и запись значений

Базовые типы

```java
ConfigurationNode node = ConfigManager.load(Path.of("config.yml"));

// Чтение с дефолтными значениями (рекомендуется!)
String name = node.node("app", "name").getString("MyApp");
int port = node.node("server", "port").getInt(8080);
boolean debug = node.node("flags", "debug").getBoolean(false);
double timeout = node.node("network", "timeout").getDouble(30.0);

// Чтение списков
List<String> admins = node.node("admins").getList(String.class, List.of());

// Запись значений
node.node("app", "version").set("2.1.0");
node.node("features", "beta").set(true);
node.node("limits", "rates").setList(String.class, List.of("100", "500", "1000"));
```

Работа с вложенными структурами

```yaml
# config.yml
database:
  primary:
    host: localhost
    port: 5432
    credentials:
      user: admin
      password: secret
  replica:
    host: backup.local
    port: 5432
```

```java
// Чтение глубоко вложенных значений
String dbUser = config.node("database", "primary", "credentials", "user").getString();

// Запись с созданием всей цепочки узлов
config.node("database", "replica", "credentials", "password").set("newpass");

// Проверка наличия ключа
boolean hasReplica = !config.node("database", "replica").virtual();
```

### 🗂️ Работа с вложенными путями в JAR

**ConfigManager** корректно обрабатывает вложенные структуры при извлечении из JAR:

```java
// JAR содержит: resources/modules/auth/config.yml

// Извлечёт в: <workDir>/modules/auth/config.yml
ConfigurationNode authCfg = ConfigManager.load(pluginJar, "modules/auth/config.yml");

// Или создание нового с сохранением структуры
ConfigurationNode newCfg = ConfigManager.loadOrCreate(
    pluginJar, 
    Path.of("modules/cache/config.yml")
);
// Файл будет создан в workDir/modules/cache/config.yml
```

> [!NOTE]
> 💡 Совет: Всегда используйте .getString("default-text") вместо .getString() для защиты от null при отсутствии ключа.

