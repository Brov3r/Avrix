[Home](../../wiki-language.md) > [Documentation](../wiki-main.md) > Mixins

## ⚡ Bytecode Transformation & Mixins

Avrix uses the SpongePowered Mixin runtime maintained by [FabricMC](https://github.com/FabricMC/Mixin) and bundles
[MixinExtras](https://github.com/LlamaLad7/MixinExtras). Mixin classes are registered from plugin metadata; no
`.mixins.json` file is needed.

---

### Supported Annotations

Plugin mixins use the standard SpongePowered Mixin annotations (`@Mixin`, `@Inject`, `@Overwrite`, `@Redirect`,
`@Accessor`, and others). MixinExtras annotations such as `@WrapOperation`, `@ModifyExpressionValue`, `@Local`, and
`@Share` are supported as well.

---

### Dependency Configuration in `build.gradle`

To compile mixin annotations in your plugin, declare the dependencies as compile-only:

```groovy
dependencies {
    // Provided by the loader at runtime.
    compileOnly 'net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7'
    compileOnly 'io.github.llamalad7:mixinextras-common:0.5.5'
}
```

Do not shade these libraries into your plugin. The loader provides them and delegates their packages to a single
runtime copy.

---

### Registering Mixins in a Plugin

List mixin classes in your `metadata.yml` manifest:

```yaml
mixins:
  - "com.example.myplugin.mixin.PlayerMixin"
  - "com.example.myplugin.mixin.InventoryMixin"
```

The entries are fully qualified mixin class names, not configuration file paths. Each class must carry a `@Mixin`
annotation with at least one target:

```java
package com.example.myplugin.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "zombie.characters.IsoPlayer")
public class PlayerMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(CallbackInfo callback) {
        // Custom logic.
    }
}
```

Keep mixins in a dedicated package that contains no target classes. Mixin treats every class in a mixin package as
part of that package, so mixing targets and mixins in one package is invalid.

---

### Registration and Lifecycle

All plugin archives are attached and all declared mixins are registered during the preparation phase, before any
plugin entrypoint runs. A plugin's `onInitialize` can therefore load target classes without racing another plugin's
mixin registration. `loadBefore` / `loadAfter` control preparation order and therefore the order in which multiple
mixin configurations are applied to the same target.

Each plugin's declarations are checked before registration. A declaration is rejected when the class is missing, is
not annotated with `@Mixin`, has no target, or shares its package with a target. If registration may have left
partial state, startup aborts even in lenient mode. Entrypoint failures after any mixins are registered are also fatal
to startup, because already registered or applied transformations cannot be safely rolled back.

Generated configurations follow the loader's strictness. In strict mode a failed mixin is required and aborts class
loading; without `--strict` the affected class is loaded without the failed transformation and the error is logged.

Mixin configuration resources are generated in memory from `metadata.yml`; the loader never writes mixin JSON files
to disk. Multi-release JARs are supported for normal class loading, mixin targets, and wildcard class discovery.

---

### Learning Resources

* [Fabric Mixin](https://github.com/FabricMC/Mixin) — maintained Mixin runtime used by Avrix.
* [MixinExtras](https://github.com/LlamaLad7/MixinExtras) — additional injectors and sugar.
* [Introduction to Mixins (FabricMC Wiki)](https://fabricmc.net/wiki/tutorial:mixin_introduction) — core injection
  points (`HEAD`, `RETURN`, `INVOKE`).
* [SpongePowered Mixin Documentation](https://github.com/SpongePowered/Mixin/wiki) — field accessors, local variable
  manipulation, and advanced injection points.
