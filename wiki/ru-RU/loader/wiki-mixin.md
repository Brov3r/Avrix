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
   FabricMC или Sponge, многие существующие объявления можно переиспользовать; проверьте, что используемые аннотации и
   возможности поддерживаются транслятором.

---

### Подключение зависимостей в `build.gradle`

Чтобы использовать аннотации миксинов в коде плагина, добавьте соответствующие зависимости в блок `dependencies`:

```groovy
dependencies {
    // Compile-only: provided by the loader at runtime.
    compileOnly 'net.lenni0451.classtransform:core:1.15.1'

    // Only needed when using @Mixin syntax.
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

Элементы списка — это полные имена классов трансформации (FQCN), а не пути к файлам конфигурации.

Миксины регистрируются при загрузке плагина-владельца. Если другой плагин может загрузить целевой класс раньше,
укажите `loadBefore`, чтобы плагин с миксином загрузился первым. Не загружайте целевые классы в `onInitialize`, если
порядок загрузки не гарантирует, что нужный миксин уже зарегистрирован.

---

### Обучающие материалы

Принципы написания миксинов в Avrix схожи с экосистемой FabricMC:

* [Введение в Mixin (FabricMC Wiki)](https://fabricmc.net/wiki/tutorial:mixin_introduction) — базовые концепции точек
  внедрения (`HEAD`, `RETURN`, `INVOKE`).
* [Документация SpongePowered Mixin](https://github.com/SpongePowered/Mixin/wiki) — подробный разбор манипуляций с
  полями и локальными переменными.