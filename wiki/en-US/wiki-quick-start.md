[Home](../wiki-language.md) > [Documentation](wiki-main.md) > Quick Start

## 🏃 Quick Start (Launching Avrix)

In many aspects, Avrix is architecturally similar to [FabricMC](https://fabricmc.net/) for Minecraft. If you have
experience with Fabric mod development, working with Avrix will feel immediately familiar.

To launch Project Zomboid with the Avrix loader:

1. ⬇️ Download the latest `Avrix-Loader-<version>.jar` from the GitHub
   [Releases](https://github.com/Brov3r/Avrix/releases) page.
2. 📁 Place the downloaded `.jar` file into the root directory of your Project Zomboid client or dedicated server (for
   example, `C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid`).
3. 💡 Launch the game via your terminal (CMD, PowerShell, or Bash):

```bash
java -jar ./Avrix-Loader-<version>.jar
```

*To launch a dedicated server without Steam integration, append the `-nosteam` argument:*

```bash
java -jar ./Avrix-Loader-<version>.jar -nosteam
```

4. ⚙️ **Optional:** Create a `.bat` (Windows) or `.sh` (Linux) launch script containing this command for quick
   execution.

### Launch Options

JVM options must be placed before `-jar`, while loader and game arguments go after the JAR name:

```bash
# Detailed loader logs (TRACE, DEBUG, INFO, WARN, ERROR)
java -Dconsole.level=DEBUG -jar ./Avrix-Loader-<version>.jar

# Launch an installation located outside the current directory
java -Davrix.game.dir="C:\Path\To\ProjectZomboid" -jar ./Avrix-Loader-<version>.jar

# Keep the original Project Zomboid console output
java -jar ./Avrix-Loader-<version>.jar --no-redirect-log

# Abort startup when any plugin fails to load
java -jar ./Avrix-Loader-<version>.jar --strict
```

The game directory can also be provided through the `AVRIX_GAME_DIR` environment variable (or `ZOMBOID_DIR`), or
through the `-Dzomboid.dir` JVM property. Avrix detects the client or dedicated server from installation metadata and
files: the `mainClass` in `ProjectZomboid64.json` takes priority, followed by launch scripts (`StartServer*`,
`start-server.sh`), platform marker files, the `java/` and `natives/` layout, Steam app manifests, and finally the
installation directory name. No environment flag is required, but `-Davrix.environment=client|server` (or
`AVRIX_ENVIRONMENT`) can be used as an explicit override.

Avrix writes a log file to `logs/log_<timestamp>.log` in the working directory. The console threshold is controlled by
`-Dconsole.level`, while the file appender records `DEBUG` and above.

---

## 🚫 Disabling Plugins

To disable a plugin without deleting it from the plugins directory, create `plugins/disabled.txt`.

Each non-blank line contains either the plugin `id` or the plugin archive name. Matching is case-insensitive. Lines
starting with `#` and inline comments are ignored.

```text
# plugins/disabled.txt
my-plugin
old-mod.jar
```

Disabled plugins are skipped and reported with the `SKIPPED` status. Use `--strict` to make plugin load failures abort
startup instead.

---

## 🧪 Running from an IDE

The Gradle build provides two development run tasks:

```bash
./gradlew runClient
./gradlew runServer
```

The tasks auto-detect common Steam installation paths on Windows, Linux, and macOS. Override the location when the game
is installed elsewhere:

```bash
# Gradle properties
./gradlew runClient -PpzClientDir="C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid"
./gradlew runServer -PpzServerDir="C:\Program Files (x86)\Steam\steamapps\common\Project Zomboid Dedicated Server"

# Environment variables
PZ_CLIENT_DIR=/path/to/ProjectZomboid ./gradlew runClient
PZ_SERVER_DIR=/path/to/ProjectZomboidDedicatedServer ./gradlew runServer
```

`runServer` keeps standard input attached so server commands can be entered from the terminal. On macOS, `runClient`
also applies `-XstartOnFirstThread`, which is required by the client windowing stack.

---

## 💻 Quick Start (Plugin Development)

Recommended minimal project structure for an Avrix plugin:

```text
ExamplePlugin/
├── .gitignore
├── README.md
├── build.gradle
├── settings.gradle
├── libs/
│   ├── projectzomboid.jar            # Game classes
│   └── Avrix-Loader-<version>.jar    # Loader runtime and plugin API
└── src/
    ├── main/
    │   ├── java/com/example/exampleplugin/
    │   │   ├── ExamplePlugin.java      # Entrypoint (implements Plugin)
    │   │   ├── ExampleMixin.java       # Bytecode transformer (Mixin)
    │   │   └── ...
    │   └── resources/
    │       ├── metadata.yml            # Plugin metadata manifest
    │       ├── icon.png                # Plugin icon (optional)
    │       └── ...
    └── test/
        └── java/com/example/exampleplugin/
            └── ...
```

The loader JAR also contains the public plugin API, so a separate `avrix-api` dependency is not required.

---

### `build.gradle`

Minimal production-ready build configuration based on Gradle Groovy DSL and the ShadowJar plugin:

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
    // Compile-only dependencies: provided by the game and loader at runtime, so they are not shaded.
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

Metadata manifest located at `src/main/resources/metadata.yml`:

```yaml
schema: 1                                # Metadata schema specification version
id: "example-plugin"                     # Unique plugin identifier
name: "Example Plugin"                   # Human-readable display name
version: "%PLUGIN_VERSION%"              # SemVer plugin version (templated via Gradle)
description: "Plugin for Project Zomboid" # Brief description
environment: "*"                         # Target environment: "*", "both", "server", or "client"
license: "MIT"                           # License identifier (e.g., MIT, Apache-2.0, PROPRIETARY)
authors:
  - "YourName"
contacts:
  - "https://github.com/YourName/ExamplePlugin"
  - "contact@example.com"
# Add version constraints only when your plugin requires a specific API or game build.
dependencies:
  avrix-loader: "*"
  project-zomboid: "*"
loadBefore: # Load before these plugin IDs; use "*" to load before all plugins when possible.
  - "*"
loadAfter: # Load after these plugin IDs.
  - "plugin-id"
entrypoint: "com.example.exampleplugin.ExamplePlugin" # (Optional) FQCN of the entrypoint class
mixins:
  - "com.example.exampleplugin.ExampleMixin"          # (Optional) List of mixin class FQCNs
conflicts: # (Optional) Plugin IDs declared incompatible with this plugin
  - "legacy-plugin"
```

`schema` is required and must be `1`, `version` must be a valid SemVer string, and the identifiers `avrix-loader` and
`project-zomboid` are reserved for the loader and game provider. Always quote `version` and dependency constraints
(`"1.10.0"`, `">=1.2.0"`) so YAML does not coerce them into numbers. The `dependencies` section must be a map of plugin
identifiers to constraints; list syntax is not supported. Use a dependency or `loadAfter` entry when your plugin relies
on another plugin being initialized first. Unknown identifiers in `loadBefore` / `loadAfter` are reported as warnings
because the referenced plugin may be optional. To require a minimum loader or game version, declare a constraint on
`avrix-loader` or `project-zomboid` respectively. The `mixins` entries are fully qualified mixin class names, not
resource paths. Plugin IDs in `metadata.yml` must match `[a-z0-9][a-z0-9._-]{0,63}`; uppercase or space-containing IDs
are rejected. The `conflicts` field declares plugin IDs that are incompatible with this plugin. A single declaration is
sufficient, identifiers are matched case-insensitively, and conflicts with plugins that are not installed are ignored.
In normal mode a detected conflict is logged as a warning; under `--strict` it aborts load order resolution. For game
provider descriptors, `entrypoint` means the game's main class rather than a `Plugin` implementation; loader
descriptors leave it empty. Parser dispatch is keyed by `schema`, so a future schema version can be added without
changing existing version-1 manifests.

---

### `ExamplePlugin.java` (Entrypoint)

Each plugin can define a main entrypoint implementing the `Plugin` interface (or omit it entirely if the plugin serves
solely as a library or mixin pack):

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

### Plugin Lifecycle

`Plugin` exposes two lifecycle callbacks:

- `onInitialize(PluginData)` is invoked once, after the plugin JAR is attached and its mixins are registered but before
  the game starts. Acquire resources and register services here.
- `onShutdown()` is invoked when the loader shuts down. Loaded plugins are shut down in reverse load order, and the
  default implementation does nothing.

```java
@Override
public void onInitialize(PluginData pluginData) {
    // Acquire resources and register services.
}

@Override
public void onShutdown() {
    // Release resources owned by this plugin.
}
```

If your plugin registers a service, unregister it in `onShutdown()`; never unregister services owned by the loader or by
other plugins. Loader integrations can inspect the most recent attempt through `PluginManager.getLoadResults()`, which
returns immutable `PluginLoadResult` entries with `LOADED`, `FAILED`, or `SKIPPED` status. `PluginManager.close()` shuts
down loaded plugins in reverse order and is called automatically by the loader on JVM shutdown.

---

### `ExampleMixin.java` (Bytecode Transformation)

Avrix supports bytecode transformations via ClassTransform as well as translation of standard SpongePowered Mixin
annotations:

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

The `mixins` entries are fully qualified transformer class names, not configuration file paths. Registration happens
while the owning plugin is prepared, before its entrypoint runs.

---

## 📊 Performance Benchmark

A lightweight comparative benchmark is available without JMH or additional dependencies:

```bash
./gradlew benchmark
```

It measures dependency resolution, metadata-only plugin loading, realistic plugin loading with entrypoints and JAR
resources, and class loading with mixin transformation. Use the results to compare changes on the same machine; they are
not absolute performance guarantees. The `benchmark` task is intentionally not part of `check`.
