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
миксинов, а также при неизвестном значении `environment` в metadata. Без `--strict` неизвестное значение `environment`
только предупреждает и трактуется как `both`; неизвестные имена сторон внутри `entrypoints` или `mixins` отклоняются
даже без `--strict`, поэтому манифест с неизвестной стороной не загружается. Остальные предупреждения metadata
ограничены неизвестными ключами верхнего уровня, дубликатами в `loadBefore`/`loadAfter` и пустыми элементами списков.
Дубликаты YAML-ключей всегда отклоняются; если проверку дубликатов нельзя включить, разбор завершается ошибкой.

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
}

dependencies {
    // Compile-only: provided by the game and loader at runtime, so they are not shaded.
    compileOnly(fileTree(dir: 'libs', include: '*.jar'))
    compileOnly 'net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7'
    compileOnly 'io.github.llamalad7:mixinextras-common:0.5.5'

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

`schema` обязателен и выбирает формат манифеста: схема 1 использует `1`, схема 2 — `2` (см. ниже). `version`
должен быть корректной SemVer-строкой, а идентификаторы `avrix-loader` и `project-zomboid` зарезервированы за
загрузчиком и игровым провайдером. Всегда заключайте `version` и ограничения зависимостей в кавычки
(`"1.10.0"`, `">=1.2.0"`), чтобы YAML не преобразовал их в числа. Секция
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
загрузчика поле пустое. Разбор манифеста выбирается по `schema`: поддерживаются только схемы `1` и `2`, любое другое
значение отклоняется.

### Очистка отображаемого текста

Поля metadata, предназначенные для показа, очищаются перед выводом в логи, approval UI и консоль. `name`, `authors`,
`contacts` и `license` приводятся к однострочному тексту, а `description` сохраняет переводы строк и табуляцию;
последовательности CRLF и одиночный CR нормализуются в LF. Управляющие символы ISO (включая ESC), управляющие символы
двунаправленного текста и разделители строк/абзацев Unicode удаляются или заменяются пробелами. Значения `name` и
`author`, прочитанные из `mod.info`, очищаются так же. Поэтому approval UI и консольные запросы показывают очищенные
имена, авторов и названия источников.

Идентификаторы и машиночитаемые значения не очищаются: `id`, `version`, entrypoints, миксины, хэши SHA-256 и пути
файловой системы сохраняются без изменений и проверяются отдельно. Mod ID, прочитанный из `mod.info`, показывается в
консольном и оконном approval frontend через безопасное display-представление (управляющие символы, bidi и ESC
удаляются), а identity-значение `modId` для approval store, протокола и поиска `previousHash` не меняется. Plugin ID
уже проходит валидацию, поэтому повторно не очищается.

> [!NOTE]
> Очистка предназначена для корректного отображения, а не является границей безопасности. Плагины по-прежнему
> выполняют произвольный Java-код.

---

### `ExamplePlugin.java` (Точка входа)

Плагин схемы 1 может иметь одну точку входа, реализующую интерфейс `Plugin` (либо не иметь ее вовсе, если плагин
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
     * @param pluginData plugin metadata, archive access, and the handler snapshot
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

- `onInitialize(PluginData)` вызывается один раз для каждого активного экземпляра entrypoint'а — после подключения
  всех JAR-файлов плагинов и регистрации всех активных миксинов, но до запуска игры. Callback получает снапшот
  `PluginData` с обработчиками плагина (см. ниже). Здесь следует захватывать ресурсы и регистрировать сервисы.
- `onShutdown()` вызывается при завершении загрузчика в порядке, строго обратном инициализации. Реализация по
  умолчанию ничего не делает. Он также вызывается, если `onInitialize()` завершился ошибкой после создания
  экземпляра: очистка должна учитывать частично инициализированные поля и освобождать уже захваченные ресурсы.

Для пакета плагинов инициализация идёт так:

1. Сначала регистрируются все активные миксины: `mixins.common` вместе с подходящим списком `client` или `server`
   либо плоский список схемы 1.
2. Плагины инициализируются в разрешённом топологическом порядке (`dependencies`, `loadAfter`, `loadBefore`).
3. Внутри одного плагина классы `entrypoints.main` идут в порядке объявления, затем активный список стороны
   (`client` на клиенте, `server` на сервере).

Завершение идёт строго в обратном порядке: для каждого плагина в обратном топологическом порядке сначала
entrypoint'ы активной стороны в обратном порядке объявления, затем `main` в обратном порядке объявления.

`PluginData` хранит неизменяемый снапшот обработчиков (`Plugin`). Во время `onInitialize` `instances()` содержит
уже созданные обработчики, включая текущий (текущий добавляется до вызова его callback'а); снапшот фиксируется в
момент callback'а, поэтому более ранний callback не видит обработчики, созданные позже. После завершения загрузки сохранённый дескриптор содержит все
активные обработчики. `instance()` возвращает первый успешно созданный обработчик или `null`; `getPluginInstance()`
— то же значение в виде `Optional<Plugin>`. Оба метода дают legacy-представление одного обработчика и для второго
и последующих entrypoint'ов не обязаны указывать на текущий. Список всех обработчиков возвращает только
`instances()`; используйте его для плагинов с несколькими entrypoint'ами. У плагинов без entrypoint'ов (только
библиотека или миксины) снапшот пуст, и первого обработчика нет.

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

### Каталог данных плагина

`PluginData.getDataDirectory()` возвращает записываемый каталог, закреплённый за плагином: `<pluginsDir>/<plugin-id>`.
Путь абсолютный и нормализованный. Каталог создаётся лениво при первом вызове, а не при подключении архива, поэтому
плагин, который его не запрашивает, ничего не создаёт. Все entrypoint'ы одного плагина получают один и тот же путь.
Для synthetic- и core-дескрипторов возвращается `Optional.empty()`. Если создать каталог не удалось, метод выбрасывает
`UncheckedIOException`.

```java
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Override
public void onInitialize(PluginData pluginData) {
    Path stateFile = pluginData.getDataDirectory()
            .orElseThrow(() -> new IllegalStateException("Plugin data directory is unavailable"))
            .resolve("state.txt");

    try {
        Files.writeString(stateFile, "enabled=true");
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to write " + stateFile, e);
    }
}
```

`<pluginsDir>` — это настроенный каталог плагинов, а не корень игры.

---

### Метаданные с учётом стороны (schema 2)

Схема 2 позволяет одному JAR объявить разные entrypoint'ы и миксины для клиента и выделенного сервера. Схема 1 не
меняется: существующий манифест сохраняет единственный `entrypoint`, плоский список `mixins` и прежнее поведение.

```yaml
schema: 2
id: "example-dual-side"
name: "Example Dual-Side Plugin"
version: "%PLUGIN_VERSION%"
description: "Shared, client-only, and server-only code in one plugin"
environment: "*"                         # Фильтрует плагин целиком: "*"/"both", "client" или "server"

entrypoints:
  main:
    - "com.example.exampleplugin.ExamplePlugin"
  client:
    - "com.example.exampleplugin.client.ExampleClientPlugin"
  server:
    - "com.example.exampleplugin.server.ExampleServerPlugin"

mixins:
  common:
    - "com.example.exampleplugin.mixin.ExampleCommonMixin"
  client:
    - "com.example.exampleplugin.mixin.client.ExampleClientMixin"
  server:
    - "com.example.exampleplugin.mixin.server.ExampleServerMixin"
```

- Каждый список может содержать несколько реализаций `Plugin`; они инициализируются в порядке объявления.
- `entrypoints.main` и `mixins.common` активны на обеих сторонах. Списки `client` и `server` активны только в
  соответствующем окружении.
- Верхнеуровневый `environment` по-прежнему фильтрует плагин целиком. Например, `environment: "server"` не даёт
  плагину загрузиться на клиенте, поэтому его `entrypoints.client` и `mixins.client` никогда не выполняются.
- Если активное окружение — `both` или `auto`, активны только объявления `main`/`common`; списки `client` и
  `server` пропускаются.
- Схема 2 также принимает поля схемы 1 как сокращения: одиночный `entrypoint` — это один элемент
  `entrypoints.main`, а список `mixins` — это `mixins.common`.
- Одновременное объявление `entrypoint` и `entrypoints` недопустимо.
- Неизвестные имена сторон внутри `entrypoints` или `mixins` отклоняются: для entrypoints допустимы только
  `main`/`client`/`server`, для mixins — `common`/`client`/`server`.
- Повторяющиеся имена классов пропускаются; используется первое вхождение. Каждый entrypoint должен реализовывать
  `Plugin` и иметь доступный конструктор без аргументов. Элементы `mixins` остаются FQCN, а не путями к ресурсам.

Во время выполнения `Metadata.entrypointsFor(Environment)` и `Metadata.mixinsFor(Environment)` вычисляют
эффективные списки: main/common плюс активную сторону. Legacy-аксессоры `entrypoint()` и `mixins()` сохраняют
представление схемы 1: `entrypoint()` возвращает первый основной entrypoint, а `mixins()` — общий список
(`mixins.common`); для схемы 1 это исходные одиночный entrypoint и плоский список.

Классы миксинов используют стандартный шаблон `@Mixin` из раздела
[Трансформация байт-кода и миксины](./loader/wiki-mixin.md). `ExamplePlugin` повторяет шаблон, показанный выше. Два
класса для конкретных сторон выглядят так:

```java
package com.example.exampleplugin.client;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entrypoint declared under {@code entrypoints.client}.
 */
public final class ExampleClientPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleClientPlugin.class);

    /**
     * Initializes client-only features.
     *
     * @param pluginData plugin metadata and archive access
     */
    @Override
    public void onInitialize(PluginData pluginData) {
        LOGGER.info("Client-only initialization for [{}]", pluginData.id());
    }
}
```

```java
package com.example.exampleplugin.server;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side entrypoint declared under {@code entrypoints.server}.
 */
public final class ExampleServerPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleServerPlugin.class);

    /**
     * Initializes server-only features.
     *
     * @param pluginData plugin metadata and archive access
     */
    @Override
    public void onInitialize(PluginData pluginData) {
        LOGGER.info("Server-only initialization for [{}]", pluginData.id());
    }
}
```

С `loadAfter: ["core"]`, `entrypoints.main: [A, B]` и `entrypoints.client: [C]` клиентский запуск инициализирует
`core`, затем A, B и C; при завершении вызываются C, B и A в обратном порядке. Полные правила пакетной
инициализации — в разделе [Жизненный цикл плагина](#жизненный-цикл-плагина).

Эти списки лишь распределяют код по сторонам в рамках одного пакета инициализации. Это не фазы entrypoint'ов
`preLaunch` из Fabric: Avrix не добавляет `preLaunch` или пользовательские типы entrypoint'ов, а клиентские
объявления не выполняются в отдельной более поздней фазе. В одном процессе активна только одна сторона.

> [!NOTE]
> Схема 2 поддерживается начиная с Avrix 2.6.0.

---

### `ExampleMixin.java` (Трансформация байт-кода)

Avrix использует среду SpongePowered Mixin, поддерживаемую FabricMC, и включает MixinExtras. Классы миксинов плагина
используют стандартные аннотации Mixin:

```java
package com.example.exampleplugin.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для {@code zombie.Lua.LuaEventManager}.
 */
@Mixin(targets = "zombie.Lua.LuaEventManager")
public class LuaEventManagerMixin {

    /**
     * Вставляет логику в начало (HEAD) метода triggerEvent(String).
     *
     * @param eventName имя события Lua
     * @param callback  колбэк инъекции
     */
    @Inject(method = "triggerEvent(Ljava/lang/String;)V", at = @At("HEAD"))
    private void onTriggerEvent(String eventName, CallbackInfo callback) {
        System.out.printf("Intercepted Lua event trigger: [%s]%n", eventName);
    }
}
```

Держите миксины в отдельном пакете, в котором нет целевых классов. Это стандартное требование Mixin. Аннотации
MixinExtras, например `@WrapOperation` и `@ModifyExpressionValue`, также доступны.

Элементы `mixins` — это полные имена классов миксинов (FQCN), а не пути к файлам конфигурации. Регистрация
выполняется при пакетной подготовке, до запуска любого entrypoint.

---

## 📊 Бенчмарк производительности

В проект встроен лёгкий сравнительный бенчмарк без JMH и дополнительных зависимостей:

```bash
./gradlew benchmark
```

Он измеряет разрешение зависимостей, загрузку плагинов только с метаданными, загрузку реалистичных плагинов с
entrypoint и ресурсами JAR, а также загрузку классов с применением миксинов. Сравнивайте результаты на одной машине;
это не абсолютные гарантии производительности. Задача `benchmark` намеренно не входит в `check`.
