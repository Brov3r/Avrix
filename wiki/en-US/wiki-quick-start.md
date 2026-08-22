[Home](../wiki-language.md) > [Documentation](wiki-main.md) > Quick Start

## 🏃 Quick Start (Launching Avrix)

In many aspects, Avrix is architecturally similar to [FabricMC](https://fabricmc.net/) for Minecraft. If you have
experience with Fabric mod development, working with Avrix will feel immediately familiar.

To launch Project Zomboid with the Avrix loader:

1. ⬇️ Download the latest `Avrix-Loader-X.X.X.jar` from the GitHub [Releases](https://github.com/Brov3r/Avrix/releases)
   page.
2. 📁 Place the downloaded `.jar` file into the root directory of your Project Zomboid client or dedicated server (for
   example, `C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid`).
3. 💡 Launch the game via your terminal (CMD, PowerShell, or Bash):

```bash
java -jar ./Avrix-Loader-2.1.0.jar
```

*To launch a dedicated server without Steam integration, append the `-nosteam` argument:*

```bash
java -jar ./Avrix-Loader-2.1.0.jar -nosteam
```

4. ⚙️ **Optional:** Create a `.bat` (Windows) or `.sh` (Linux) launch script containing this command for quick
   execution.

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
│   ├── projectzomboid.jar      # Game classes
│   ├── Avrix-Loader-2.1.0.jar  # Loader core runtime
│   └── avrix-api-1.0.0.jar     # Loader API (optional)
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
    // Compile-only dependencies: game core and loader runtime (NOT shaded into final JAR)
    compileOnly(fileTree(dir: 'libs', include: '*.jar'))
    compileOnly 'net.lenni0451.classtransform:core:1.15.1'

    // Testing
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

Metadata manifest located at `src/main/resources/metadata.yml`:

```yaml
schema: 1                                # Metadata schema specification version
id: "example-plugin"                     # Unique alphanumeric plugin identifier
name: "Example Plugin"                   # Human-readable display name
version: "%PLUGIN_VERSION%"              # Plugin version (templated via Gradle)
description: "Plugin for Project Zomboid" # Brief description
environment: "*"                         # Target environment: "*", "both", "server", or "client"
license: "MIT"                           # License identifier (e.g., MIT, Apache-2.0, PROPRIETARY)
authors:
  - "YourName"
contacts:
  - "https://github.com/YourName/ExamplePlugin"
  - "contact@example.com"
dependencies:
  avrix-loader: ">=2.1.0"
  project-zomboid: ">=42.20.0"
loadBefore: # Loading before any plugin (list of identifiers).
  - "*"                                # You can specify “*”, in which case the loading will, if possible, occur before (or after, if in loadAfter) all plugins.
loadAfter: # Loading after some plugin (list of identifiers)
  - "plugin-id"
entrypoint: "com.example.exampleplugin.ExamplePlugin" # (Optional) FQCN of the entrypoint class
mixins:
  - "com.example.exampleplugin.ExampleMixin"          # (Optional) List of mixin class FQCNs
```

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
     * Invoked during the loader initialization phase after classpath binding
     * and mixin bytecode transformations, but before the game starts.
     *
     * @param pluginData container containing active metadata, jar path, and icon
     */
    @Override
    public void onInitialize(PluginData pluginData) {
        LOGGER.info("Example Plugin [{}] v{} initialized successfully!",
                pluginData.getId(),
                pluginData.getMetadata().version());
    }
}
```

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