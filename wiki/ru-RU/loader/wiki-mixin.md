[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Миксины

## ⚡ Трансформация байт-кода и Миксины

В Avrix система модификации классов реализована на базе легковесной и быстрой
библиотеки [ClassTransform](https://github.com/Lenni0451/ClassTransform). Для подробного изучения внутренней архитектуры
трансформаций рекомендуем ознакомиться
с [официальной Wiki ClassTransform](https://github.com/Lenni0451/ClassTransform/wiki).

---

### Поддерживаемые форматы аннотаций

Avrix предоставляет разработчикам свободу выбора синтаксиса миксинов:

1. **Нативные аннотации ClassTransform** (`@CTransformer`, `@CInject`, `@COverwrite`, `@CRedirect`):
   Рекомендуются для новых разработчиков, не имеющих опыта работы с Minecraft.
2. **Стандартные аннотации SpongePowered Mixin** (`@Mixin`, `@Inject`, `@Overwrite`, `@Redirect`):
   Поддерживаются благодаря встроенному в ядро Avrix препроцессору `MixinsTranslator`. Если вы ранее писали моды для
   FabricMC или Sponge, ваш привычный код будет работать без изменений.

---

### Подключение зависимостей в `build.gradle`

Чтобы использовать аннотации миксинов в коде плагина, добавьте соответствующие зависимости в блок `dependencies`:

```groovy
dependencies {
    // Ядро ClassTransform (базовые аннотации трансформации)
    compileOnly 'net.lenni0451.classtransform:core:1.15.1'

    // Заглушки аннотаций SpongePowered Mixin (если вы используете синтаксис @Mixin)
    compileOnly 'net.lenni0451.classtransform:mixinsdummy:1.15.1'
}
```

---

### Регистрация миксинов в плагине

Чтобы загрузчик применил ваши классы трансформации, перечислите их в файле `metadata.yml`:

```yaml
mixins:
  - "com.example.myplugin.mixin.PlayerMixin"
  - "com.example.myplugin.mixin.InventoryMixin"
```

Avrix гарантирует, что все указанные миксины регистрируются в `KnotClassLoader` до момента загрузки и инициализации
целевых игровых классов Project Zomboid.

---

### Обучающие материалы

Принципы написания миксинов в Avrix полностью идентичны экосистеме FabricMC:

* [Введение в Mixin (FabricMC Wiki)](https://fabricmc.net/wiki/tutorial:mixin_introduction) — базовые концепции точек
  внедрения (`HEAD`, `RETURN`, `INVOKE`).
* [Документация SpongePowered Mixin](https://github.com/SpongePowered/Mixin/wiki) — подробный разбор манипуляций с
  полями и локальными переменными.