[Home](../wiki-language.md) > Documentation

# 📄 Documentation

This documentation is up to date for the following versions:

- 🧟 **Project Zomboid:** `42.21.0+`
- ⚡ **Avrix Loader:** `2.3.x`
- ☕ **Java Runtime:** `JDK 25+`

> [!IMPORTANT]
> Running and developing plugins for Avrix requires **Java Development Kit (JDK) 25 or higher**. Older Java versions are
> not supported.

---

### 🤓 Terminology

Key concepts used across this documentation:

- **Plugin (Mod):** A user-created software module in `.jar` format containing logic, assets, or mixins to extend game
  features.
- **Mixin:** An on-the-fly bytecode transformation mechanism that alters target game classes (Project Zomboid) during
  class loading without modifying original game files.
- **KnotClassLoader:** A transforming classloader that hosts game classes, loader runtime, and plugins in a single flat
  domain. JVM internals and logging/loader infrastructure are delegated to the parent classloader.
- **ServiceManager:** A thread-safe, type-safe service registry (Service Locator) that binds interface contracts to
  runtime implementations.

---

### 📁 Documentation Sections

- [🏃 Quick Start (Launching & Development)](wiki-quick-start.md)
- [⚡ Bytecode Transformation & Mixins](./loader/wiki-mixin.md)
- [🔧 Service Architecture & ServiceManager](./loader/wiki-services.md)