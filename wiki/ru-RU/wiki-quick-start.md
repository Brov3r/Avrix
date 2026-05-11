[Главная](../wiki-language.md) > [Документация](wiki-main.md) > Быстрый старт

## 🏃Быстрый старт (Запуск Avrix)

Во многих аспектах Avrix очень схож с [Fabric MC](https://fabricmc.net/) для Minecraft. Если вы имеет опыт разработки
модификаций на Fabric, то с Avrix у вас не возникнет существенных проблем.

Для быстрого запуска игры с Avrix достаточно следовать этим простым шагам:

1) Скачать актуальную версию Avrix с раширением `.jar` со страницы [Release ](https://github.com/Brov3r/Avrix/releases)
   на GitHub
2) Переместить скачанный `.jar` файл в корневую папку с игрой/серверов. (Например,
   `C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid`)
3) Открыть командную строку (CMD) и прописать команду запуска:

   ```bash
   java "-Djdk.attach.allowAttachSelf=true" -XX:+EnableDynamicAgentLoading -jar ./Avrix-Loader-X.X.X.jar
   ```

   где:

   Avrix-Loader-X.X.X.jar - название скачанного `.jar` файла.
4) **Опционально:** для упрощения запуска вы можете создать `.sh` или `.bat` файл с данной командой внутри.

> [!WARNING]
> Обратите внимание! Аргументы **-Djdk.attach.allowAttachSelf=true** и **-XX:+EnableDynamicAgentLoading** обязательны! В
> противном случае миксины не будут трансформировать игровые файлы.

## 💻 Быстрый старт (Разработка плагинов)

Чтобы начать разработку плагинов, вам необходимо привести структуру своего проекта к следующему минимальному виду:

```
ExamplePlugin/
├── .gitignore
├── README.md
├── build.gradle
├── settings.gradle
├── libs/
│   ├── library.jar
│   └── ...
└── src/
    ├── main/
    │   ├── java/com/example/exampleplugin/
    │   │   ├── ExamplePlugin.java      # Точка входа (extends Plugin)
    │   │   ├── ExampleMixin.java       # Точка входа для миксина
    │   │   └── ...      
    │   └── resources/
    │       ├── metadata.yml            # Метаданные плагина
    │       ├── icon.png                # Иконка (опционально)
    │       └── ...
    └── test/
        └── java/com/example/exampleplugin/
            └── ...
```

### build.gradle

Минимальный работоспособный вид build.gradle:

```gradle
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '9.4.1'
}

group = 'com.example'
version = '1.0.0'

// Название jar архива
def baseArchiveName = "Example Plugin"

// Место, где будет собран готовый jar архив
def buildPath = System.getenv('BUILD_PATH') ?: 'build'

repositories {
    mavenCentral()
}

dependencies {
	// Загрузка всех .jar библиотек из папки libs. 
	// Для примера указано 'compileOnly', при необходимости вы можете изменить на 'implementation' и/или подключать каждую библиотеку отдельно
	// Обратите внимание: projectzomboid.jar (игровые классы), avrix-loader-x.x.x.jar (загрузчик) и avrix-api-x.x.x.jar (API для Project Zomboid) необходимо подключать исключительно через 'compileOnly'!
    compileOnly(fileTree(dir: 'libs', include: '*.jar'))

    // Тестирование
    testImplementation platform('org.junit:junit-bom:6.0.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.assertj:assertj-core:3.27.7'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}

processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
	// Опционально. Замена версии в метаданных динамически через ключ.
    filesMatching('**/metadata.yml') {
        filter { String line -> line.replace('%PLUGIN_VERSION%', version.toString()) }
    }
}

// Основное задание для Gradle, через которое происходит сборка jar архива плагина
shadowJar {
    archiveBaseName.set(baseArchiveName)
    archiveClassifier.set('')
    destinationDirectory.set(file(buildPath))

    manifest {
        attributes(
                'Implementation-Title': "${rootProject.name.capitalize()}",
                'Implementation-Version': version
        )
    }
}
```

### metadata.yml

Минимальный работоспособный вид metadata.yml:

```yaml
schema: 1                                # Идентификатор шаблона метаданных, как правило не меняется
name: "Example Plugin"                    # Название плагина
description: "Project Zomboid Plugin"    # Описание плагина
id: "example-id"                        # Уникальный идентификатор плагина
version: %PLUGIN_VERSION%                # Версия плагина, указывается вручную или через ключ (реализация в build.gradle)
environment: "*"                        # Окружение плагина (Может быть "*" (или "Both"), "Server", "Client)
authors: # Авторы плагина
  - "YourName"
licence: "MIT"                            # Лицензия плагина
contacts: # Реквизиты для связи и прочие контакты
  - "https://github.com/YourName/YourPlugin"
  - "yourname@gmail.com"
dependencies: # Зависимости плагина (ключ - ID плагина, значение - условие версии по SemVer)
  avrix-loader: ">=2.0.0"
  project-zomboid: "~42.17"
entrypoint: "..."                        # (Опционально) Путь к классу плагина, например, com.example.exampleplugin.ExamplePlugin
mixins:
  - "..."                                # (Опционально) Путь к классу миксина, например, com.example.exampleplugin.ExampleMixin
```

### ExamplePlugin.java (Точка входа)

В текущей реализации Avrix в каждом плагине может быть только одна точка входа, либо не быть вовсе (в таком случае
плагин загружается как библиотека). Примерный вид реализации точки входа:

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

    }
}

```

### ExampleMixin.java (Пример реализации миксина)

Подробнее про миксины - [здесь](./loader/wiki-mixin.md).

Пример реальной реализации миксинов, для внедрения своего вызова в метод 'LuaEventManager.triggerEvent(String)'

```java
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;

@CTransformer(value = LuaEventManager.class)
public class ExampleMixin {
    @CInject(
            method = "triggerEvent(Ljava/lang/String;)V",
            target = @CTarget("HEAD")
    )
    private static void injecEvent0(String event) {
        EventManager.invoke(event);
    }
}
```