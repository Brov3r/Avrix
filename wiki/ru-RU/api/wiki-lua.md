[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Lua

## 🧩 Lua

### 🚀 Запуск Lua-скриптов

Для запуска пользовательских скриптов из плагина предусмотрено два способа загрузки:

1. **📂 Из локальной директории**  
   Создайте папку со скриптами на диске и загружайте их по пути.
2. **📦 Из JAR-архива**  
   Встройте папку со скриптами непосредственно в архив плагина (`.jar`).

### 🗂️ Структура файлов

Структура одинакова как для локальной папки, так и для содержимого JAR-архива. Точкой входа обычно является `main.lua`.

**Локальная директория:**

```text
lua/
├── libs/
│   ├── library.lua
│   └── ...
├── .../
└── main.lua
```

**Внутри плагина (JAR):***

```
SomePlugin.jar/
└── lua/
    ├── libs/
    │   ├── library.lua
    │   └── ...
    ├── .../
    └── main.lua
```

### ⚙️ Загрузка скриптов

⚠️ **Важно:** Lua-скрипты необходимо загружать строго **после** полной инициализации `LuaManager`.  
Оптимальное место для загрузки — событие старта игры/сервера (например, `OnGameBootEvent`). Попытка загрузки раньше
вызовет ошибки.

Пример реализации загрузки скриптов:

```java
import com.avrix.core.Metadata;
import com.avrix.plugins.Plugin;

// ... другие импорты

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
     * Событие инициализации плагина. Основная логика реализуется именно здесь. 
     * Используется для подписки на события, регистрации команд и прочего.
     * Вызывается до старта игры/сервера
     */
    @Override
    public void onInitialize() {
        LuaExtension.addExposedClass(ExampleExpose.class); // Пример передачи API конкретного класса в Lua скрипты (только публичные поля и методы)
        LuaExtension.addExposedGlobalObject(new ExampleObject()); // Пример регистрации глобальных методов Lua

        // Загрузка Lua скриптов после старта игры/сервера
        EventManager.addListener(new OnGameBootEvent() {
            @Override
            public void handle() {
                /**
                 * Загрузка Lua скриптов из Jar архива плагина.
                 */
                LuaExtension.loadLuaFolderFromJar(pluginFile, "lua", false);

                /**
                 * Пример загрузки одиночных файлов Lua
                 * Если у вас только один файл и без 'require' (т.е. без require пользовательских Lua) достаточно использовать только `loadLua` с передачей абсолютного пути к файлу
                 * Если же скрипт требует через 'require' пользовательские Lua скрипты (как библиотеку, например), то его необходимо зарегистрировать через `registerLuaContent` также с передачей абсолютного пути к файлу/папке
                 */

                LuaExtension.registerLuaContent(Paths.get(".../lua"));
                LuaExtension.loadLua(Paths.get(".../lua/main.lua"), false);


                /**
                 * Загрузка скриптов из папки (рекомендованный способ).
                 * `loadLuaFolder` рекурсивно загружает все Lua скрипты из данной папки с регистрацией (позволяет использовать 'require' к вложенным Lua скриптам данной папки или другим зарегистрированным)
                 */
                LuaExtension.loadLuaFolder(Paths.get(".../lua"), false);

            }
        });
    }

    /**
     * Пример передачи глобального метода в Lua скрипт.
     */
    public class ExampleObject {
        @LuaMethod(
                name = "exampleReturn",
                global = true
        )
        public String exampleReturn(String text) {
            return "Hello " + text;
        }
    }

    /**
     * Пример передачи методов в Lua скрипт
     */
    public static class ExampleExpose {
        public static void exampleVoid(String text) {
            System.out.println(text);
        }

        public static String exampleReturn() {
            System.out.println("Example Return");
        }
    }
}
```

### 📜 Использование в Lua

Пример файла `main.lua`, демонстрирующий работу с загруженными скриптами и Java-API.

```lua
-- 1. Подключение библиотеки из папки libs/
require("libs/library)

print("🚀 Main Lua script!")

-- 2. Вызов глобальной функции, зарегистрированной через addExposedGlobalObject
-- Вывод в консоль: "Print from Global Object: Hello World!"
print("Print from Global Object: " .. exampleReturn("World!"))

-- 3. Вызов статических методов класса, зарегистрированного через addExposedClass
-- Вывод в консоль: "Hello world - Expose!"
ExampleExpose.exampleVoid("Hello world - Expose!")

-- Вывод в консоль: "Example Return"
print(ExampleExpose.exampleReturn())
```