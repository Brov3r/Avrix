[Home](../../wiki-language.md) > [Documentation](../wiki-main.md) > Services

## 🔧 Services

`ServiceManager` is the centralized, thread-safe service registry (Service Locator) of the Avrix loader, designed for
registering, discovering, and interacting with services via their types and interfaces.

It enforces loose coupling between plugins: one plugin can declare an API (interface) and register its implementation,
while other plugins consume the service without direct dependencies on the underlying implementation classes.

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