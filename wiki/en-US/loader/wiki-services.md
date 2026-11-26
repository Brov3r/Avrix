[Home](../../wiki-language.md) > [Documentation](../wiki-main.md) > Services

## 🔧 Services

`ServiceManager` is the centralized, thread-safe service registry (Service Locator) of the Avrix loader, designed for
registering, discovering, and interacting with services via their types and interfaces.

It enforces loose coupling between plugins: one plugin can declare an API (interface) and register its implementation,
while other plugins consume the service without direct dependencies on the underlying implementation classes.

---

### 🧱 Built-in Services

The loader registers these contracts during bootstrap:

| Contract          | Implementation                                        |
|-------------------|-------------------------------------------------------|
| `KnotClassLoader` | Flat transforming classloader hosting game and plugin classes |
| `LoaderProvider`  | Loader name, version, authors, license, and contacts  |
| `GameProvider`    | Active client or dedicated server game provider       |
| `PluginManager`   | Active plugin manager                                 |

These services are loader-owned. Never unregister or replace them. All four are available by the time plugin
`onInitialize` callbacks run. The loader removes its own service registrations when the runtime closes; plugin-owned
services should be unregistered by their plugins in `onShutdown`. A plugin that consumes a service registered by another
plugin should declare a dependency or a `loadAfter` entry so the provider plugin is initialized first.

---

### 🧩 Registering a Service

To register a service in `ServiceManager`, use the `register(Class<T> serviceType, T implementation)` method. Exactly
one service instance is registered per contract type.

```java
package com.example.database;

import com.avrix.core.ServiceManager;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Service interface — public contract.
 */
public interface DatabaseService {
    void connect(String url);

    void disconnect();

    boolean isConnected();
}

/**
 * Service implementation.
 */
public class PostgreSQLService implements DatabaseService {
    @Override
    public void connect(String url) {
        throw new UnsupportedOperationException("Connect implementation is omitted from this example.");
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException("Disconnect implementation is omitted from this example.");
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}

/**
 * Entrypoint of the service provider plugin.
 */
public class DatabasePlugin implements Plugin {

    @Override
    public void onInitialize(PluginData pluginData) {
        ServiceManager.register(DatabaseService.class, new PostgreSQLService());
    }
}
```

> [!WARNING]
> If a service for the specified class is already registered, `ServiceManager.register` throws an
`IllegalStateException`. This safeguards the runtime against unintended implementation collisions.

Register services from `onInitialize` and unregister only the contracts your plugin owns from `onShutdown`:

```java
@Override
public void onShutdown() {
    ServiceManager.unregister(DatabaseService.class);
}
```

The loader invokes plugin shutdown callbacks in reverse load order through `PluginManager.close()`.

---

### 🛫 Resolving a Service

You can resolve a registered service in two ways depending on whether the dependency is mandatory:

#### 1. Mandatory Service (`get`)

Use this when your plugin strictly requires the service to operate. Throws an `IllegalStateException` if the service is
missing:

```java
package com.example.consumer;

import com.avrix.core.ServiceManager;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;
import com.example.database.DatabaseService;

public class ConsumerPlugin implements Plugin {

    @Override
    public void onInitialize(PluginData pluginData) {
        DatabaseService dbService = ServiceManager.get(DatabaseService.class);
        dbService.connect("jdbc:postgresql://localhost:5432/zomboid");
    }
}
```

#### 2. Optional Service (`find`)

Use this when your plugin functions independently and provides optional third-party integrations:

```java
package com.example.consumer;

import com.avrix.core.ServiceManager;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;
import com.example.database.DatabaseService;

public class OptionalConsumerPlugin implements Plugin {

    @Override
    public void onInitialize(PluginData pluginData) {
        ServiceManager.find(DatabaseService.class).ifPresent(db -> {
            db.connect("jdbc:postgresql://localhost:5432/zomboid");
        });
    }
}
```

---

### 🔍 Checking Service Availability

To check if a service type is registered without retrieving an instance, use the `contains` method:

```java
if (ServiceManager.contains(DatabaseService.class)) {
    // Use this only when the presence of the service changes your plugin's behavior.
}
```

---

### 🧹 Unregistering a Service

If you need to remove a service binding at runtime (for example, in tests or when replacing an implementation you own):

```java
boolean removed = ServiceManager.unregister(DatabaseService.class);
```

> [!WARNING]
> `ServiceManager` is a global registry. Do not unregister services owned by the loader or by other plugins unless you
> control their lifecycle.

---

### 🔎 Plugin Lookup and Version Checks

`PluginManager` exposes convenience lookups for integrations:

- `findPlugin(String id)` returns the loaded plugin or core component; empty when the ID is not loaded.
- `findLoadResult(String id)` returns the most recent recorded result (`LOADED`, `FAILED`, or `SKIPPED`); empty when no
  attempt was recorded.
- `isLoaded(String id)` is the boolean form of `findPlugin(id)`.
- `isLoaded(String id, String versionConstraint)` checks the declared version against a dependency-style SemVer
  constraint. A missing plugin returns `false`; an invalid constraint throws `IllegalArgumentException`.
- `getUserPlugins()` returns only plugins backed by a physical JAR, sorted by plugin ID.

```java
PluginManager plugins = ServiceManager.get(PluginManager.class);

if (plugins.isLoaded("core-api", ">=2.1.0")) {
    // A compatible provider is loaded.
}

plugins.findPlugin("optional-plugin").ifPresent(plugin ->
        LOGGER.debug("Using optional plugin {}", plugin.metadata().version()));

for (PluginData plugin : plugins.getUserPlugins()) {
    LOGGER.debug("User plugin: {} {}", plugin.id(), plugin.metadata().version());
}
```

---

### 🧭 Classpath Conflicts

`PluginManager.getClasspathConflicts()` returns immutable `ClasspathConflict` entries sorted by resource path:

```java
ClasspathConflict(
        String resourcePath,
        List<String> pluginIds,
        String winnerPluginId
)
```

Detection runs while plugin archives are attached. Avrix keeps first-wins classpath behavior: the earliest attached
archive wins, and later copies are never merged. `pluginIds` is in discovery order. Classes delegated to the parent class
loader (`java.*`, `org.slf4j.*`, `com.avrix.*`, ASM/Mixin, and other loader libraries) are excluded, as are
infrastructure entries such as `module-info.class` and `META-INF/*`.

The report is diagnostic only: conflicts do not fail loading. The loader writes a bounded `WARN` summary with the first
10 entries plus the remaining count. The report is cleared at the start of a new `loadPlugins()` run; direct
`loadPlugin(...)` calls add to the current report.
