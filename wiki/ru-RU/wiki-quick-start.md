[Главная](../wiki-language.md) > [Документация](wiki-main.md) > Быстрый старт

## 🏃 Быстрый старт (Запуск Avrix)

Во многих аспектах Avrix схож с [FabricMC](https://fabricmc.net/) для Minecraft. Если у вас есть опыт разработки
модификаций под Fabric, освоить Avrix будет максимально просто.

Для запуска Project Zomboid с загрузчиком Avrix выполните следующие шаги:

1. ⬇️ Скачайте актуальный релиз `Avrix-Loader-<version>.jar` со
   страницы [Releases](https://github.com/Brov3r/Avrix/releases) на GitHub.
2. 📁 Поместите скачанный `.jar` файл в корневую папку клиента или сервера Project Zomboid (например,
   `C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid`).
3. 💡 Запустите игру через терминал (CMD / PowerShell / Bash):

```bash
java -jar ./Avrix-Loader-<version>.jar
```

*Для запуска выделенного сервера без интеграции Steam добавьте аргумент `-nosteam`:*

```bash
java -jar ./Avrix-Loader-<version>.jar -nosteam
```

4. ⚙️ **Опционально:** для удобства создайте скрипт запуска `.bat` (Windows) или `.sh` (Linux) с этой командой.

### Параметры запуска

JVM-параметры указываются до `-jar`, а аргументы загрузчика и игры — после имени JAR:

```bash
# Подробные логи загрузчика (TRACE, DEBUG, INFO, WARN, ERROR)
java -Dconsole.level=DEBUG -jar ./Avrix-Loader-<version>.jar

# Запуск установки, расположенной вне текущего каталога
java -Davrix.game.dir="C:\Path\To\ProjectZomboid" -jar ./Avrix-Loader-<version>.jar

# Сохранить оригинальный вывод консоли Project Zomboid
java -jar ./Avrix-Loader-<version>.jar --no-redirect-log

# Прерывать запуск при ошибке загрузки любого плагина
java -jar ./Avrix-Loader-<version>.jar --strict
```

Каталог игры также можно задать переменной окружения `AVRIX_GAME_DIR` (или `ZOMBOID_DIR`) либо JVM-свойством
`-Dzomboid.dir`. Avrix определяет клиент или выделенный сервер только по файлам установки: `StartServer64.*`,
каталогам `java/` и `natives/`, исполняемым файлам клиента или `ProjectZomboid64.json`. Флаг окружения не требуется.

Avrix записывает лог в `logs/log_<timestamp>.log` в рабочем каталоге. Порог вывода в консоль задаётся через
`-Dconsole.level`, а файловый appender сохраняет сообщения уровня `DEBUG` и выше.

---

## 🚫 Отключение плагинов

Чтобы отключить плагин, не удаляя его из папки `plugins/`, создайте `plugins/disabled.txt`.

В каждой непустой строке указывается `id` плагина или имя JAR-файла. Регистр не важен. Строки, начинающиеся с `#`, и
inline-комментарии игнорируются.

```text
# plugins/disabled.txt
my-plugin
old-mod.jar
```

Отключённые плагины пропускаются и попадают в отчёт со статусом `SKIPPED`. Флаг `--strict` заставляет загрузчик
прерывать запуск при ошибке загрузки плагина.

---

## 🧪 Запуск из IDE

В Gradle-сборке предусмотрены две задачи для разработки:

```bash
./gradlew runClient
./gradlew runServer
```

Задачи автоматически определяют типовые пути установки Steam в Windows, Linux и macOS. Если игра установлена в другом
месте, переопределите путь:

```bash
# Свойства Gradle
./gradlew runClient -PpzClientDir="C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid"
./gradlew runServer -PpzServerDir="C:\Program Files (x86)\Steam\steamapps\common\Project Zomboid Dedicated Server"

# Переменные окружения
PZ_CLIENT_DIR=/path/to/ProjectZomboid ./gradlew runClient
PZ_SERVER_DIR=/path/to/ProjectZomboidDedicatedServer ./gradlew runServer
```

`runServer` оставляет стандартный ввод подключённым, чтобы команды сервера можно было вводить из терминала. В macOS
`runClient` также добавляет `-XstartOnFirstThread`, который требуется оконной подсистеме клиента.

---

## 💻 Быстрый старт (Разработка плагинов)

Минимальная рекомендуемая структура проекта плагина для Avrix:

```text
ExamplePlugin/
├── .gitignore
├── README.md
├── build.gradle
├── settings.gradle
├── libs/
│   ├── projectzomboid.jar            # Игровые классы
│   └── Avrix-Loader-<version>.jar    # Загрузчик и API плагинов
└── src/
    ├── main/
    │   ├── java/com/example/exampleplugin/
    │   │   ├── ExamplePlugin.java      # Точка входа (implements Plugin)
    │   │   ├── ExampleMixin.java       # Класс трансформации (Миксин)
    │   │   └── ...
    │   └── resources/
    │       ├── metadata.yml            # Метаданные плагина
    │       ├── icon.png                # Иконка (опционально)
    │       └── ...
    └── test/
        └── java/com/example/exampleplugin/
            └── ...
```

JAR загрузчика также содержит публичный API плагинов, поэтому отдельная зависимость `avrix-api` не требуется.

---

### `build.gradle`

Минимальная production-ready конфигурация сборки на базе Gradle Groovy DSL и плагина ShadowJar:

```groovy
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '9.6.1'
}

group = 'com.example'
version = '1.0.0'

def baseArchiveName = "ExamplePlugin"
def buildPath = System.getenv('BUILD_PATH') ?: layout.buildDirectory.get().asFile

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = 'https://maven.lenni0451.net/everything' }
}

dependencies {
    // Compile-only: provided by the game and loader at runtime, so they are not shaded.
    compileOnly(fileTree(dir: 'libs', include: '*.jar'))
    compileOnly 'net.lenni0451.classtransform:core:1.15.1'

    testImplementation platform('org.junit:junit-bom:6.1.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.assertj:assertj-core:3.27.7'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}

processResources {
    filteringCharset = 'UTF-8'
    filesMatching('**/metadata.yml') {
        filter { String line -> line.replace('%PLUGIN_VERSION%', project.version.toString()) }
    }
}

shadowJar {
    archiveBaseName.set(baseArchiveName)
    archiveClassifier.set('')
    destinationDirectory.set(file(buildPath))

    mergeServiceFiles()

    manifest {
        attributes(
                'Implementation-Title': baseArchiveName,
                'Implementation-Version': project.version.toString(),
                'Implementation-Vendor': 'ExampleAuthor'
        )
    }
}
```

---

### `metadata.yml`

Манифест метаданных, размещаемый в папке `src/main/resources/metadata.yml`:

```yaml
schema: 1                                # Версия спецификации схемы метаданных
id: "example-plugin"                     # Уникальный ID плагина
name: "Example Plugin"                   # Человекочитаемое имя
version: "%PLUGIN_VERSION%"              # Версия плагина в формате SemVer (подставляется через Gradle)
description: "Plugin for Project Zomboid" # Краткое описание
environment: "*"                         # Целевое окружение: "*", "both", "server" или "client"
license: "MIT"                           # Лицензия (например, MIT, Apache-2.0, PROPRIETARY)
authors:
  - "YourName"
contacts:
  - "https://github.com/YourName/ExamplePlugin"
  - "contact@example.com"
# Указывайте ограничения версий только если плагину нужен конкретный API или сборка игры.
dependencies:
  avrix-loader: "*"
  project-zomboid: "*"
loadBefore: # Загрузка перед указанными ID; "*" — перед всеми плагинами, когда это возможно.
  - "*"
loadAfter: # Загрузка после указанных ID.
  - "plugin-id"
entrypoint: "com.example.exampleplugin.ExamplePlugin" # (Опционально) Полный FQCN точки входа
mixins:
  - "com.example.exampleplugin.ExampleMixin"          # (Опционально) Список классов миксинов
conflicts: # (Опционально) ID плагинов, несовместимых с этим плагином
  - "legacy-plugin"
```

`schema` обязателен и должен быть равен `1`, `version` должен быть корректной SemVer-строкой, а идентификаторы
`avrix-loader` и `project-zomboid` зарезервированы за загрузчиком и игровым провайдером. Всегда заключайте `version`
и ограничения зависимостей в кавычки (`"1.10.0"`, `">=1.2.0"`), чтобы YAML не преобразовал их в числа. Секция
`dependencies` должна быть map'ой «ID плагина → ограничение»; списочный синтаксис не поддерживается. Если плагин
зависит от инициализации другого плагина, укажите зависимость или `loadAfter`. Неизвестные идентификаторы в
`loadBefore` / `loadAfter` выводятся как предупреждения, потому что указанный плагин может быть опциональным. Чтобы
потребовать минимальную версию загрузчика или игры, объявите ограничение для `avrix-loader` или `project-zomboid`
соответственно. Элементы `mixins` — это полные имена классов миксинов (FQCN), а не пути к ресурсам. ID плагинов в
`metadata.yml` должны соответствовать `[a-z0-9][a-z0-9._-]{0,63}`; ID с заглавными буквами или пробелами
отклоняются. Поле `conflicts` объявляет ID плагинов, несовместимых с этим плагином. Достаточно объявления с одной
стороны, ID сравниваются без учёта регистра, а конфликты с неустановленными плагинами игнорируются. В обычном режиме
обнаруженный конфликт выводится как предупреждение; под `--strict` разрешение порядка загрузки прерывается. Для
descriptor'а игрового провайдера `entrypoint` означает главный класс игры, а не реализацию `Plugin`; у descriptor'а
загрузчика поле пустое. Разбор манифеста выбирается по `schema`, поэтому будущую версию схемы можно добавить, не
меняя существующие манифесты версии 1.

---

### `ExamplePlugin.java` (Точка входа)

Каждый плагин может иметь одну точку входа, реализующую интерфейс `Plugin` (либо не иметь ее вовсе, если плагин
выполняет роль чистой библиотеки или набора миксинов):

```java
package com.example.exampleplugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for the example Avrix plugin.
 */
public class ExamplePlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamplePlugin.class);

    /**
     * Invoked after the plugin JAR is attached and its mixins are registered,
     * but before the game starts.
     *
     * @param pluginData container containing active metadata, jar path, and icon
     */
    @Override
    public void onInitialize(PluginData pluginData) {
        LOGGER.info("Example Plugin [{}] v{} initialized successfully!",
                pluginData.id(),
                pluginData.metadata().version());
    }
}
```

### Жизненный цикл плагина

`Plugin` предоставляет два callback'а жизненного цикла:

- `onInitialize(PluginData)` вызывается один раз после подключения JAR плагина и регистрации его миксинов, но до
  запуска игры. Здесь следует захватывать ресурсы и регистрировать сервисы.
- `onShutdown()` вызывается при завершении загрузчика. Загруженные плагины останавливаются в обратном порядке загрузки;
  реализация по умолчанию ничего не делает.

```java
@Override
public void onInitialize(PluginData pluginData) {
    // Захват ресурсов и регистрация сервисов.
}

@Override
public void onShutdown() {
    // Освобождение ресурсов, принадлежащих плагину.
}
```

Если плагин регистрирует сервис, отмените регистрацию в `onShutdown()`; никогда не удаляйте сервисы загрузчика или
других плагинов. Интеграции могут получить результаты последней попытки через `PluginManager.getLoadResults()` —
неизменяемый список `PluginLoadResult` со статусами `LOADED`, `FAILED` и `SKIPPED`. `PluginManager.close()` завершает
загруженные плагины в обратном порядке и вызывается загрузчиком автоматически при остановке JVM.

---

### `ExampleMixin.java` (Трансформация байт-кода)

Avrix поддерживает трансформации через ClassTransform, а также трансляцию стандартных аннотаций SpongePowered Mixin:

```java
package com.example.exampleplugin;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;

/**
 * Bytecode transformer targeting {@code zombie.Lua.LuaEventManager}.
 */
@CTransformer(name = "zombie.Lua.LuaEventManager")
public class ExampleMixin {
    /**
     * Injects custom logic at the start (HEAD) of triggerEvent(String).
     *
     * @param eventName name of the triggered Lua event
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;)V",
            target = @CTarget("HEAD")
    )
    private static void injectEvent0(String eventName) {
        System.out.printf("Intercepted Lua event trigger: [%s]%n", eventName);
    }
}
```

Элементы `mixins` — это полные имена классов трансформации (FQCN), а не пути к файлам конфигурации. Регистрация
выполняется при подготовке плагина-владельца, до запуска его entrypoint.

---

## 📊 Бенчмарк производительности

В проект встроен лёгкий сравнительный бенчмарк без JMH и дополнительных зависимостей:

```bash
./gradlew benchmark
```

Он измеряет разрешение зависимостей, загрузку плагинов только с метаданными, загрузку реалистичных плагинов с
entrypoint и ресурсами JAR, а также загрузку классов с применением миксинов. Сравнивайте результаты на одной машине;
это не абсолютные гарантии производительности. Задача `benchmark` намеренно не входит в `check`.
