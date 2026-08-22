[Главная](../wiki-language.md) > [Документация](wiki-main.md) > Быстрый старт

## 🏃 Быстрый старт (Запуск Avrix)

Во многих аспектах Avrix схож с [FabricMC](https://fabricmc.net/) для Minecraft. Если у вас есть опыт разработки
модификаций под Fabric, освоить Avrix будет максимально просто.

Для запуска Project Zomboid с загрузчиком Avrix выполните следующие шаги:

1. ⬇️ Скачайте актуальный релиз `Avrix-Loader-X.X.X.jar` со
   страницы [Releases](https://github.com/Brov3r/Avrix/releases) на GitHub.
2. 📁 Поместите скачанный `.jar` файл в корневую папку клиента или сервера Project Zomboid (например,
   `C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid`).
3. 💡 Запустите игру через терминал (CMD / PowerShell / Bash):

```bash
java -jar ./Avrix-Loader-2.1.0.jar
```

*Для запуска выделенного сервера без интеграции Steam добавьте аргумент `-nosteam`:*

```bash
java -jar ./Avrix-Loader-2.1.0.jar -nosteam
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
│   ├── projectzomboid.jar      # Игровые классы
│   ├── Avrix-Loader-2.1.0.jar  # Ядро загрузчика
│   └── avrix-api-1.0.0.jar     # API (опционально)
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
    // Внешние runtime-библиотеки плагина (будут упакованы в ShadowJar)
    // Зависимости компиляции: ядро игры и загрузчик (НЕ упаковываются в итоговый JAR)
    compileOnly(fileTree(dir: 'libs', include: '*.jar'))
    compileOnly 'net.lenni0451.classtransform:core:1.15.1'

    // Тестирование
    testImplementation platform('org.junit:junit-bom:5.12.0')
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
dependencies:
  avrix-loader: ">=2.1.0"
  project-zomboid: ">=42.20.0"
loadBefore: # Загрузка перед каким либо плагином (список идентификаторов).
  - "*" # Можно указать "*", тогда загрузка, по возможности, будет происходить перед (либо после, если в loadAfter) всеми плагинами       
loadAfter: # Загрузка после какого-либо плагина (список идентификаторов)
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
     * Invoked during the loader initialization phase after classpath binding
     * and mixin bytecode transformations, but before the game starts.
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