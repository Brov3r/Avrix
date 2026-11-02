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
id: "example-plugin"                     # Уникальный строковый ID плагина
name: "Example Plugin"                   # Человекочитаемое имя
version: "%PLUGIN_VERSION%"              # Версия плагина (подставляется через Gradle)
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
```

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