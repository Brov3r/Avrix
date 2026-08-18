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
        // Connection logic
    }

    @Override
    public void disconnect() {
        // Disconnection logic
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
        // Registering the implementation under the interface contract
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
        // Direct resolution (throws an exception if the service is not found)
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
        // Safe lookup via Optional
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
if(ServiceManager.contains(DatabaseService .class)){
        // Service is available
        }
```

---

### 🧹 Unregistering a Service

If you need to remove a service from the registry (for example, during plugin teardown or implementation replacement):

```java
// Returns true if the service was present and successfully removed
boolean removed = ServiceManager.unregister(DatabaseService.class);
```