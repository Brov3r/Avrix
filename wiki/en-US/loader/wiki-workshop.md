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
> `-pzexeconfig`.

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
archives) remain a load-order error.

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
- `prompt` on a dedicated server is downgraded to `deny-new` because there is no Lua UI.

Policy behavior:

| Policy | Behavior |
|---|---|
| `prompt` | Unknown/changed JARs are not loaded. A single Lua batch dialog appears on the main menu. |
| `deny-new` | Unknown/changed JARs are silently skipped. No UI. |
| `allow-all` | Every active mod JAR loads without approval. Development only. |

Approval files are stored in the game directory:

```text
<Project Zomboid>/avrix/plugin-approvals.txt
<Project Zomboid>/avrix/plugin-pending.txt
```

The Project Zomboid Lua sandbox can only access files inside the game cache directory, so Avrix writes the mirror and
reads the decision hand-off there:

```text
<Zomboid cache>/Lua/avrix/plugin-pending.txt
<Zomboid cache>/Lua/avrix/plugin-decisions.txt
```

`<Zomboid cache>` is `~/Zomboid` by default and follows the game `-cachedir=` launch argument when present.

The authoritative approval store and pending list remain in the game directory. Lua only reads the pending mirror and
writes a decision hand-off file; Avrix applies it on the next launch.

Approval flow:

1. Avrix scans active `jarPlugins`, hashes each JAR with SHA-256, and does not load unknown/changed archives.
2. The Lua mod shows **one** batch dialog with the pending entries.
3. Click a plugin to cycle **skip → allow → deny → skip**. `Allow all` and `Deny all` fill every row, `Apply` saves
   the rows that have a decision, and `Later` closes the dialog without saving.
4. Rows left on **skip** are not saved and remain pending, so the dialog asks about them again on the next launch.
5. Approved hashes load without another prompt. A changed JAR hash requires approval again.

Explicit **deny** decisions are remembered for that exact JAR hash. To ask again, delete
`<Project Zomboid>/avrix/plugin-approvals.txt` (or the corresponding line) and restart.

`plugins/` remains trusted and is not part of this flow. `jarPlugins` mods should declare `require=\Avrix` so the
Workshop mod and its Lua approval UI are enabled.

### ⚠️ Security

Java plugins have unrestricted access to the system. Avrix is not a sandbox. Install Java plugins only from sources
you trust and review the source code when it is available.

Treat every active mod that declares `jarPlugins` as executable code.

The Lua approval dialog is a consent and visibility step, not a security boundary. Any enabled Lua mod can read the
pending mirror and write the decision hand-off file, because all mods share the same Lua sandbox. The dialog also does
not protect against a mod that ships its own Java plugin: a malicious Lua mod can alter the hand-off while the game is
running. Review mods before enabling them, and use `plugins/` only for fully trusted code.

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
