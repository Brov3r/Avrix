[Главная](../wiki-language.md) > [Документация](wiki-main.md) > Быстрый старт

## 🏃 Быстрый старт (Запуск Avrix)

Во многих аспектах Avrix схож с [FabricMC](https://fabricmc.net/) для Minecraft. Если у вас есть опыт разработки
модификаций под Fabric, освоить Avrix будет максимально просто.

### 🎮 Пользователи Steam (рекомендуется)

Рекомендуемый способ установки для Steam — Workshop-мод Avrix.

1. Подпишитесь на Workshop-мод Avrix.
2. Откройте **Steam → Project Zomboid → Свойства → Параметры запуска** и добавьте профиль для вашей платформы:

   **Windows (Normal Launch):**

   ```text
   -pzexeconfig ..\..\workshop\content\108600\3833849848\mods\Avrix\libs\Avrix-windows.json
   ```

   **Linux:**

   ```text
   -pzexeconfig ../../../workshop/content/108600/3833849848/mods/Avrix/libs/Avrix-linux.json
   ```

   ID Workshop-мода Avrix — `3833849848`
   ([Steam Workshop](https://steamcommunity.com/sharedfiles/filedetails/?id=3833849848)).

3. Запустите Project Zomboid через **Normal Launch**.
4. Включите Workshop-моды с Avrix-плагинами в игровом меню **Mods**. Avrix загружает `jarPlugins` только из
   активных модов. Классическая папка `plugins/` при этом продолжает работать независимо.

Java-плагины из Workshop-модов требуют подтверждения. На клиенте при политике по умолчанию `prompt` Avrix показывает
одно Java-окно перед загрузкой плагинов. На выделенном сервере по умолчанию используется `deny-new`; при наличии
интерактивной консоли можно использовать `prompt`, если задать `-Davrix.plugin.policy=prompt`. Снятая галочка
означает запрет; нажмите `Apply`, чтобы загрузить одобренные плагины в этом же запуске. Включите `Remember for future launches`, чтобы сохранить выбор для будущих
запусков. Подробности о хранилище решений и значениях `avrix.plugin.policy` — в
[Workshop-модах и Java-плагинах](./loader/wiki-workshop.md).

> [!IMPORTANT]
> Workshop-профиль работает через обычный нативный лаунчер. Он не работает с Alternate Launch
> (`ProjectZomboid64.bat` / `start-server.sh`). Поддержка macOS экспериментальная и не проверена, в том числе при
> ручном запуске через `java -jar`. Примеры не гарантируют работу игры на macOS.

Подробности о формате `jarPlugins`, правилах активных модов и диагностике —
в [Workshop-модах и Java-плагинах](./loader/wiki-workshop.md).

### 🖥️ Ручной запуск (java -jar / выделенный сервер)

Этот способ предназначен для выделенных серверов, не-Steam установок и разработки. Workshop-мод для него не нужен.

1. ⬇️ Скачайте актуальный релиз `Avrix-Loader-<version>.jar` со
   страницы [Releases](https://github.com/Brov3r/Avrix/releases) на GitHub.
2. 📁 Поместите скачанный `.jar` файл в корневую папку клиента или сервера Project Zomboid (например,
   `C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid`).
3. 💡 Перейдите в корневую папку игры и запустите её через терминал (CMD / PowerShell / Bash):

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

# Явно выбрать установку (рабочий каталог JVM уже должен быть её корнем)
java -Davrix.game.dir="C:\Path\To\ProjectZomboid" -jar ./Avrix-Loader-<version>.jar

# Сохранить оригинальный вывод консоли Project Zomboid
java -jar ./Avrix-Loader-<version>.jar --no-redirect-log

# Прерывать запуск при ошибке загрузки плагина или трансформации миксина
java -jar ./Avrix-Loader-<version>.jar --strict
```

В PowerShell смените каталог командой `Set-Location "C:\Path\To\ProjectZomboid"`, в CMD —
`cd /d "C:\Path\To\ProjectZomboid"`. На macOS используйте каталог с файлами игры внутри application bundle.
Свойства `avrix.game.dir` и `user.dir` не меняют рабочую директорию процесса. В IDE задайте её в настройках запуска.
Относительный каталог плагинов разрешается относительно корня выбранной игры.

`--strict` прерывает запуск при ошибках загрузки плагинов, разрешения зависимостей/конфликтов и трансформации
миксинов, а также при неизвестном значении `environment` в metadata. Неизвестные ключи, дубликаты в
`loadBefore`/`loadAfter` и пустые элементы списков остаются предупреждениями. Дубликаты YAML-ключей всегда
отклоняются; если проверку дубликатов нельзя включить, разбор завершается ошибкой.

Объявления миксинов проверяются до регистрации. Ошибка регистрации, которая могла оставить частичное состояние
трансформера, или ошибка entrypoint после регистрации любых миксинов прерывает запуск даже без `--strict`:
безопасно откатить такие изменения нельзя. Фатальные ошибки JVM, например `VirtualMachineError`, не считаются
обычными ошибками плагинов.

Каталог игры также можно задать переменной окружения `AVRIX_GAME_DIR` (или `ZOMBOID_DIR`) либо JVM-свойством
`-Dzomboid.dir`. Avrix определяет клиент или выделенный сервер по метаданным и файлам установки: приоритет имеет
`mainClass` в `ProjectZomboid64.json`, затем launch-скрипты (`StartServer*`, `start-server.sh`), платформенные
маркеры, раскладка `java/` и `natives/`, Steam app manifest и, в последнюю очередь, имя каталога. Флаг окружения
не требуется, но можно явно задать `-Davrix.environment=client|server` (или `AVRIX_ENVIRONMENT`).

Avrix записывает отдельный ротируемый лог `logs/avrix-<timestamp>.log` на каждый запуск. В рамках запуска файлы
ротируются по 10 MB и хранятся 7 дней, а общий размер логов ограничен 1 GB; файлы предыдущих запусков удаляются по
тем же лимитам возраста и размера при старте. Каталог логов можно переопределить через `-Davrix.log.dir=...`.
Порог вывода в консоль задаётся через `-Dconsole.level`, а файловый appender сохраняет сообщения уровня `DEBUG` и
выше.

---

## 🚫 Отключение плагинов

Чтобы отключить плагин, не удаляя его из папки `plugins/`, создайте `plugins/disabled.txt`.

В каждой непустой строке указывается `id` плагина или имя JAR-файла. Регистр не важен. Строки, начинающиеся с `#`, и
inline-комментарии игнорируются. Файл читается как UTF-8; BOM в начале файла игнорируется.

```text
# plugins/disabled.txt
my-plugin
old-mod.jar
```

Отключённые плагины пропускаются и попадают в отчёт со статусом `SKIPPED`. Флаг `--strict` заставляет загрузчик
прерывать запуск при ошибке загрузки плагина. Этот же список действует и для плагинов, найденных в активных
Workshop- или локальных модах через `jarPlugins`.

---

## 🧪 Запуск из IDE

В Gradle-сборке предусмотрены две задачи для разработки:

```bash
./gradlew runClient
./gradlew runServer
```

Задачи ищут типовые пути установки Steam в Windows и Linux; поиск путей macOS экспериментальный.
Запуск игры на macOS не проверен. Если игра установлена в другом месте, переопределите путь:

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

`./gradlew check` включает тесты, проверку Javadoc и `smokeTest` собранного fat JAR и дочернего процесса подтверждения
в headless-режиме. CI выполняется на Windows и Linux. Эти проверки не подтверждают запуск реальной игры или работу на macOS.

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

Чтобы распространять готовый плагин через Steam Workshop, см.
[Workshop-моды и Java-плагины](./loader/wiki-workshop.md).

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
стороны, но все ID, включая ссылки в `conflicts`, должны быть в каноническом нижнем регистре; ссылки с заглавными
буквами отклоняются. Конфликты с неустановленными плагинами игнорируются. В обычном режиме обнаруженный конфликт
выводится как предупреждение; под `--strict` разрешение порядка загрузки прерывается. Некритичные предупреждения
metadata выводятся, но не прерывают разбор; ошибки schema, ID, версии, entrypoint, mixin и структуры зависимостей
всегда являются фатальными. Для
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

- `onInitialize(PluginData)` вызывается один раз после подключения всех JAR-файлов плагинов и регистрации всех
  миксинов, но до запуска игры. Переданный `PluginData` уже содержит инициализированный экземпляр плагина. Здесь
  следует захватывать ресурсы и регистрировать сервисы.
- `onShutdown()` вызывается при завершении загрузчика. Загруженные плагины останавливаются в обратном порядке загрузки;
  реализация по умолчанию ничего не делает. Он также вызывается, если `onInitialize()` завершился ошибкой после
  создания экземпляра: очистка должна учитывать частично инициализированные поля и освобождать уже захваченные ресурсы.

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
