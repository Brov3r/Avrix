[Home](../../wiki-language.md) > [Documentation](../wiki-main.md) > Mixins

## ⚡ Bytecode Transformation & Mixins

Avrix implements its class modification subsystem on top of the lightweight,
high-performance [ClassTransform](https://github.com/Lenni0451/ClassTransform) library. For an in-depth understanding of
transformation internals, refer to the [official ClassTransform Wiki](https://github.com/Lenni0451/ClassTransform/wiki).

---

### Supported Annotation Formats

Avrix provides developers with full flexibility in choosing mixin syntax:

1. **Native ClassTransform Annotations** (`@CTransformer`, `@CInject`, `@COverride`, `@CRedirect`):
   Recommended for developers starting fresh without prior Minecraft modding experience.
2. **Standard SpongePowered Mixin Annotations** (`@Mixin`, `@Inject`, `@Overwrite`, `@Redirect`):
   Supported via the built-in `MixinsTranslator` preprocessor inside the Avrix core. If you have previously built mods
   for FabricMC or Sponge, many existing mixin declarations can be reused; verify that the annotations and features you
   use are supported by the translator.

---

### Dependency Configuration in `build.gradle`

To use mixin annotations in your plugin codebase, declare the dependencies in the `dependencies` block:

```groovy
dependencies {
    // Compile-only: provided by the loader at runtime.
    compileOnly 'net.lenni0451.classtransform:core:1.15.1'

    // Only needed when using @Mixin syntax.
    compileOnly 'net.lenni0451.classtransform:mixinsdummy:1.15.1'
}
```

---

### Registering Mixins in a Plugin

To instruct the loader to apply your transformer classes, list them in your `metadata.yml` manifest:

```yaml
mixins:
  - "com.example.myplugin.mixin.PlayerMixin"
  - "com.example.myplugin.mixin.InventoryMixin"
```

The entries are fully qualified transformer class names, not configuration file paths.

All plugin archives are attached and all declared mixins are registered during the preparation phase, before any
plugin entrypoint runs. A plugin's `onInitialize` can therefore load target classes without racing another plugin's
mixin registration. `loadBefore` / `loadAfter` still control preparation order and therefore the order in which
multiple mixins are applied to the same target.

Each plugin's declarations are checked before registration. If registration may have left partial state, startup
aborts even in lenient mode. Entrypoint failures after any mixins are registered are also fatal to startup, because
already registered or applied transformations cannot be safely rolled back.

Multi-release JARs are supported for normal class loading, mixin targets, and wildcard class discovery. In strict mode
a mixin transformation failure aborts startup; without `--strict` the affected class is loaded without the failed
transformation and the error is logged.

---

### Learning Resources

The principles of writing mixins in Avrix are similar to the FabricMC ecosystem:

* [Introduction to Mixins (FabricMC Wiki)](https://fabricmc.net/wiki/tutorial:mixin_introduction) — Core injection
  points (`HEAD`, `RETURN`, `INVOKE`).
* [SpongePowered Mixin Documentation](https://github.com/SpongePowered/Mixin/wiki) — Comprehensive guide on field
  accessors and local variable manipulation.
