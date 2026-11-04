[Home](../wiki-language.md) > Documentation

# 📄 Documentation

This documentation is up to date for the following versions:

- 🧟 **Project Zomboid:** `42.21.0+`
- ⚡ **Avrix Loader:** current release (see [Releases](https://github.com/Brov3r/Avrix/releases))
- ☕ **Java Runtime:** `JDK 25+`

> [!IMPORTANT]
> Developing plugins and using the standalone `java -jar` loader requires **Java Development Kit (JDK) 25 or higher**.
> Older Java versions are not supported. Steam Workshop users use the Java 25 runtime bundled with Project Zomboid and
> do not need a separate JDK.

---

### 🤓 Terminology

Key concepts used across this documentation:

- **Plugin (Mod):** A user-created software module in `.jar` format containing logic, assets, or mixins to extend game
  features.
- **Mixin:** An on-the-fly bytecode transformation mechanism that alters target game classes (Project Zomboid) during
  class loading without modifying original game files.
- **KnotClassLoader:** A transforming classloader that hosts game classes, loader runtime, and plugins in a single flat
  domain. JVM internals, logging, and loader-owned libraries are delegated to the parent classloader.
- **ServiceManager:** A thread-safe, type-safe service registry (Service Locator) that binds interface contracts to
  runtime implementations.
- **Workshop Mod:** A Project Zomboid mod distributed through Steam Workshop. A Workshop mod can declare Avrix Java
  plugins with the `jarPlugins` key in `mod.info`.

---

### 📁 Documentation Sections

- [🏃 Quick Start (Launching & Development)](wiki-quick-start.md)
- [🛠️ Workshop Mods & Java Plugins](./loader/wiki-workshop.md)
- [⚡ Bytecode Transformation & Mixins](./loader/wiki-mixin.md)
- [🔧 Service Architecture & ServiceManager](./loader/wiki-services.md)