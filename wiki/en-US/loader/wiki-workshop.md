[Home](../../wiki-language.md) > [Documentation](../wiki-main.md) > Workshop

## 🛠️ Workshop Mods & Java Plugins

Avrix supports two delivery models:

1. **Manual loader** — download `Avrix-Loader-<version>.jar` and launch it with `java -jar`.
2. **Workshop loader** — subscribe to the Avrix Workshop item and start the normal game launcher with a
   `-pzexeconfig` profile.

The Workshop model is intended for Steam users. It keeps the loader, its launch profiles, and plugin mods updatable
through Steam. The manual model remains available for dedicated servers, non-Steam installations, and development.

---

### 📦 Loader Workshop Item

The Avrix Workshop item contains:

```text
mods/Avrix/
├── 42/
│   ├── media/...
│   └── mod.info
└── libs/
    ├── Avrix-Loader.jar
    ├── Avrix-windows.json
    └── Avrix-linux.json
```

`Avrix-Loader.jar` is the same shaded loader used by `java -jar`. The JSON files are launcher profiles read by the
Project Zomboid native launcher. They are not game assets and are intentionally stored next to the loader JAR.

After subscribing, add the profile for your platform to **Steam → Project Zomboid → Properties → Launch Options**.

**Windows (Normal Launch):**

```text
-pzexeconfig ..\..\workshop\content\108600\3833849848\mods\Avrix\libs\Avrix-windows.json
```

**Linux:**

```text
-pzexeconfig ../../../workshop/content/108600/3833849848/mods/Avrix/libs/Avrix-linux.json
```

The Avrix Workshop item ID is `3833849848`
([Steam Workshop](https://steamcommunity.com/sharedfiles/filedetails/?id=3833849848)).

`-pzexeconfig` is a launcher argument. If you add JVM arguments, keep them before the `--` separator used by Project
Zomboid.

> [!IMPORTANT]
> The Workshop profile uses the **normal native launcher**. It does not work with the Alternate Launch path
> (`ProjectZomboid64.bat` on Windows or `start-server.sh` on Linux). Use the manual `java -jar` model there.
>
> macOS is not verified yet. The macOS launcher is `JavaAppLauncher` inside `Project Zomboid.app`; it may not support
> `-pzexeconfig`. Manual `java -jar` launches on macOS are also experimental and unverified.

---

### 🧩 Plugin Mod Structure

A Java plugin is distributed as a normal Project Zomboid mod. Put the Avrix plugin JAR inside the mod's version
directory and declare it in `mod.info`.

```text
MyJavaPluginMod/
├── 42/
│   ├── mod.info
│   └── media/java/MyPlugin.jar
└── common/                  # optional
```

The JAR is a regular Avrix plugin and must contain `metadata.yml`:

```text
MyPlugin.jar
├── metadata.yml
├── com/example/myplugin/MyPlugin.class
└── com/example/myplugin/MyMixin.class
```

The plugin can use the same `entrypoint`, `mixins`, dependencies, environment, and version constraints as a plugin
installed in `plugins/`.

---

### 📝 `mod.info` and `jarPlugins`

Declare one or more plugin archives with the `jarPlugins` key:

```ini
name=My Java Plugin
id=MyJavaPluginMod
require=\Avrix
author=YourName
versionMin=42.21.0
jarPlugins=media/java/MyPlugin.jar, media/java/MyPlugin-extra.jar
```

`require=\Avrix` declares the Avrix Workshop mod as a Project Zomboid dependency. It does not start Java by itself —
the loader still needs the `-pzexeconfig` launch profile — but it makes the Mods menu enforce the dependency and
allows the Avrix status/warning Lua to load. Plugin mods should include it.

Rules:

- The key is case-insensitive.
- Multiple paths can be separated by a comma or a semicolon.
- Multiple `jarPlugins=` lines are accumulated.
- Paths are relative to the directory containing `mod.info`.
- If `mod.info` is in `common` and the JAR is in a version directory (`42`, `42.21`, ...), Avrix also checks that
  version directory.
- A version-specific `mod.info` with `jarPlugins` overrides `common` jar declarations. `common` remains the fallback
  when the selected version does not declare any `jarPlugins` entries.
- Avrix selects the highest version directory at or below the running game version. If every directory is newer, it
  uses the nearest newer directory. If the game version is unknown, it uses the newest directory.
- Absolute paths, drive/UNC paths, root-relative paths, and **any** `..` segment are rejected. Every `jarPlugins`
  path must stay inside the mod directory.
- Symlinks or junctions that resolve outside the mod directory are rejected.
- Every JAR must exist and be a valid Avrix plugin. Non-Avrix JARs are reported as failed plugins.
- The mod must be **active** in Project Zomboid. Java from disabled/inactive mods is not loaded.
- Enable or disable a mod with `jarPlugins` and restart the game. The active mod list is read when the JVM starts, so
  changes made in the current session apply on the next launch.
- With `--strict`, a missing or invalid `jarPlugins` entry aborts startup. Without `--strict`, it is logged and
  skipped.

---

### ✅ Active Mods Only

Avrix loads `jarPlugins` archives only from mods that the game or server actually enabled.

**Client** — `%USERPROFILE%\Zomboid\mods\default.txt`, inside the `mods` block:

```text
mods
{
    mod = MyJavaPluginMod,
}
```

**Dedicated server** — `%USERPROFILE%\Zomboid\Server\<servername>.ini`:

```ini
Mods=MyJavaPluginMod;OtherMod
```

The server name is read from `-servername`. If the argument is absent, Avrix uses `servertest`.

If no active mod list is found, Avrix does not load any `jarPlugins` archives. The manual `plugins/` directory is
independent and is still scanned.

---

### 🔌 `plugins/` Compatibility

The classic directory remains fully supported:

```text
plugins/
├── MyPlugin.jar
└── disabled.txt
```

Both sources are merged, deduplicated by canonical path, and sorted by file name. `plugins/disabled.txt` can disable
a plugin by its `id` or by its archive name, regardless of whether the plugin came from `plugins/` or a mod.

If the same plugin ID exists in `plugins/` and in an active mod, the **mod archive wins** and the archive from
`plugins/` is ignored with a warning. Duplicate IDs within the same source (two `plugins/` archives or two mod
archives) are reported as load-order failures; `--strict` turns them into a startup abort.

---

### 🖥️ Dedicated Server

For a dedicated server, prefer the manual `java -jar` model or a server launch script:

```bash
java -jar ./Avrix-Loader-<version>.jar -nosteam
```

If you use Workshop mods on a server:

1. Enable the mods in `Server/<servername>.ini` (`Mods=...`).
2. Start the server with the matching `-servername` argument.
3. Make sure Workshop content for app `108600` is available in the server's Steam library.

Avrix reads the active server mods from the server INI, so plugins from inactive server mods are not started.

---

### 🔐 Plugin Approval

Archives from the trusted `plugins/` directory load without approval. Archives discovered through `jarPlugins` from
active Workshop or local mods are subject to the approval policy.

Configure the policy with a JVM property:

```text
-Davrix.plugin.policy=prompt
-Davrix.plugin.policy=deny-new
-Davrix.plugin.policy=allow-all
```

Defaults:

- client: `prompt`;
- dedicated server: `deny-new`;
- `prompt` on a dedicated server is downgraded to `deny-new` only when the selected frontend cannot ask interactively.
  A console frontend counts as interactive, so `prompt` remains active when it is available.

Policy behavior:

| Policy | Behavior |
|---|---|
| `prompt` | Unknown/changed JARs are not loaded. An interactive approval frontend runs before plugin loading. |
| `deny-new` | Unknown/changed JARs are silently skipped. No UI. |
| `allow-all` | Every active mod JAR loads without approval. Development only. |

The approval flow is owned by the Project Zomboid provider, not by the Lua mod. The Swing window runs in a separate JVM
because the game process is headless; the console frontend prompts in the same process. The batch is shown once per
launch when at least one archive needs a decision:

1. Avrix scans active `jarPlugins`, hashes each JAR with SHA-256, and does not load unknown/changed archives.
2. The approval frontend lists every pending archive with its source, size, and SHA-256; the Swing window also shows
   the modification time.
3. In the Swing window, unchecked rows are denied; press **Apply** to apply all rows or **Cancel** to skip everything.
   The console frontend asks for each archive separately.
4. Decisions are applied immediately; approved plugins load in the same launch.
5. Enable **Remember for future launches** to keep the same decisions on later launches. Otherwise they apply to the
   current session only.
6. Approved hashes load without another prompt. A changed JAR hash requires approval again.

One approval file is created per installation, inside the game or dedicated-server root:

```text
<game-root>/.avrix/plugin-approvals.txt
```

It contains exact SHA-256 decisions and is not writable through the stock Project Zomboid Lua file APIs.

To ask again, delete `<game-root>/.avrix/plugin-approvals.txt` (or the corresponding line) and restart.

Optional frontend settings:

```text
-Davrix.approval.frontend=auto|swing|console|none
-Davrix.approval.timeout=600
```

`auto` uses the Swing window on a client. On a dedicated server it prefers the interactive console, falls back to the
Swing window when AWT is available, and otherwise denies unknown archives. `console` prompts on standard input and
supports piped input; `none` always denies. The timeout is in seconds, defaults to `600`, and is clamped to `30`–`3600`.

`plugins/` remains trusted and is not part of this flow. Plugin mods should declare `require=\Avrix` in `mod.info`:
it keeps the Avrix Workshop item as a Project Zomboid dependency and enables the inactive-loader warning from the
Workshop mod. The approval flow itself does not depend on this declaration.

### ⚠️ Security

Java plugins have unrestricted access to the system. Avrix is not a sandbox. Install Java plugins only from sources
you trust and review the source code when it is available.

Treat every active mod that declares `jarPlugins` as executable code.

The approval flow is a consent and visibility step, not a sandbox. Decisions are collected outside the Lua sandbox,
so an enabled Lua mod cannot silently approve its own Java plugin. There are no signatures: SHA-256 proves that the
approved bytes are the loaded bytes, not who authored them. Mod names and source metadata are shown as unverified
context. A malicious Java plugin has the same access as any other Java code once approved.

Do not run Avrix and ZombieBuddy on the same game launch. Both frameworks transform game classes, and combining them
can produce conflicts that are difficult to diagnose.

---

### 🔍 Troubleshooting

- **The game starts without Avrix** — check that the `-pzexeconfig` line points to `libs/Avrix-windows.json` or
  `libs/Avrix-linux.json`, and that you use Normal Launch.
- **A plugin does not load** — verify that its mod is enabled in the Mods menu (client) or `Mods=` (server), that
  `jarPlugins` points to an existing JAR, and that the JAR contains `metadata.yml`.
- **A mod was disabled but its Java still feels active** — remove it from `default.txt` / `Mods=`, or add its plugin
  ID or JAR name to `plugins/disabled.txt`.
- **The in-game warning says Avrix is not active** — the Workshop mod is enabled, but the `-pzexeconfig` profile is
  missing or incorrect.
