[Home](../../wiki-language.md) > [Documentation](../wiki-main.md) > Mixins

## ⚡ Bytecode Transformation & Mixins

Avrix implements its class modification subsystem on top of the lightweight,
high-performance [ClassTransform](https://github.com/Lenni0451/ClassTransform) library. For an in-depth understanding of
transformation internals, refer to the [official ClassTransform Wiki](https://github.com/Lenni0451/ClassTransform/wiki).

---

### Supported Annotation Formats

Avrix provides developers with full flexibility in choosing mixin syntax:

1. **Native ClassTransform Annotations** (`@CTransformer`, `@CInject`, `@COverwrite`, `@CRedirect`):
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

Mixins are registered when their owning plugin is loaded. If another plugin can load the target class earlier, declare
`loadBefore` so the mixin-providing plugin is loaded first. Avoid loading target classes during `onInitialize` unless
the load order guarantees that the required mixin is already registered.

---

### Learning Resources

The principles of writing mixins in Avrix are similar to the FabricMC ecosystem:

* [Introduction to Mixins (FabricMC Wiki)](https://fabricmc.net/wiki/tutorial:mixin_introduction) — Core injection
  points (`HEAD`, `RETURN`, `INVOKE`).
* [SpongePowered Mixin Documentation](https://github.com/SpongePowered/Mixin/wiki) — Comprehensive guide on field
  accessors and local variable manipulation.