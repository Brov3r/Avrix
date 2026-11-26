[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Миксины

## ⚡ Трансформация байт-кода и Миксины

Avrix использует среду SpongePowered Mixin, поддерживаемую [FabricMC](https://github.com/FabricMC/Mixin), и включает
[MixinExtras](https://github.com/LlamaLad7/MixinExtras). Классы миксинов регистрируются из метаданных плагина; файл
`.mixins.json` не нужен.

---

### Поддерживаемые аннотации

Миксины плагинов используют стандартные аннотации SpongePowered Mixin (`@Mixin`, `@Inject`, `@Overwrite`,
`@Redirect`, `@Accessor` и другие). Аннотации MixinExtras, например `@WrapOperation`, `@ModifyExpressionValue`,
`@Local` и `@Share`, также поддерживаются.

---

### Подключение зависимостей в `build.gradle`

Чтобы компилировать аннотации миксинов в плагине, подключите зависимости как compile-only:

```groovy
dependencies {
    // Предоставляются загрузчиком во время выполнения.
    compileOnly 'net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7'
    compileOnly 'io.github.llamalad7:mixinextras-common:0.5.5'
}
```

Не включайте эти библиотеки в JAR плагина. Загрузчик предоставляет их сам и направляет их пакеты в единственную
runtime-копию.

---

### Регистрация миксинов в плагине

В манифесте схемы 1 перечислите классы миксинов в файле `metadata.yml`:

```yaml
mixins:
  - "com.example.myplugin.mixin.PlayerMixin"
  - "com.example.myplugin.mixin.InventoryMixin"
```

Элементы списка — это полные имена классов миксинов (FQCN), а не пути к файлам конфигурации. Каждый класс должен
иметь аннотацию `@Mixin` хотя бы с одной целью:

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
        // Пользовательская логика.
    }
}
```

Держите миксины в отдельном пакете, в котором нет целевых классов. Mixin считает все классы пакета миксинов частью
этого пакета, поэтому смешивать цели и миксины в одном пакете нельзя.

В схеме 2 (Avrix 2.6.0+) миксины распределяются по сторонам:

```yaml
schema: 2
mixins:
  common:
    - "com.example.myplugin.mixin.CommonMixin"
  client:
    - "com.example.myplugin.mixin.client.ClientMixin"
  server:
    - "com.example.myplugin.mixin.server.ServerMixin"
```

`common` применяется на обеих сторонах, `client` и `server` — только в соответствующем окружении. Если активное
окружение — `both` или `auto`, регистрируется только `common`. Плоский список `mixins: [...]` из схемы 1 по-прежнему
принимается и означает `mixins.common`. Во время выполнения `Metadata.mixinsFor(Environment)` вычисляет `common`
плюс активную сторону, а `mixins()` остаётся legacy-списком общих миксинов.

---

### Регистрация и жизненный цикл

Все JAR-файлы плагинов подключаются, а все активные миксины регистрируются одним пакетом подготовки — до запуска
первого инициализатора плагинов. В этот пакет входят `mixins.common` и подходящий список `client` или `server`.
Поэтому `onInitialize` может загружать целевые классы, не опасаясь гонки с регистрацией миксинов другого плагина.
`loadBefore` / `loadAfter` управляют порядком плагинов и, следовательно, порядком применения нескольких
конфигураций миксинов к одной цели. Инициализаторы плагинов запускаются после готовности всего пакета, а не сразу
после миксинов своего плагина.

Списки миксинов для конкретной стороны — это маршрутизация метаданных, а не фазы `preLaunch` из Fabric.
Регистрируется только список активной стороны; отдельной фазы запуска миксинов клиента или сервера нет.

Объявления каждого плагина проверяются до регистрации. Объявление отклоняется, если класс отсутствует, не имеет
аннотации `@Mixin`, не объявляет цели или находится в одном пакете с целью. Если ошибка регистрации могла оставить
частичное состояние, запуск прерывается даже без `--strict`. Ошибки entrypoint после регистрации любых миксинов тоже
прерывают запуск: уже зарегистрированные или применённые трансформации нельзя безопасно откатить.

Сгенерированные конфигурации следуют режиму строгости загрузчика. В strict-режиме неудачный миксин обязателен и
прерывает загрузку классов; без `--strict` класс загружается без неудавшейся трансформации, а ошибка записывается
в лог.

Ресурсы конфигураций миксинов создаются в памяти из `metadata.yml`; загрузчик не записывает JSON-файлы миксинов на
диск. Multi-release JAR поддерживаются для обычной загрузки классов, целей миксинов и wildcard-поиска классов.

---

### Обучающие материалы

* [Fabric Mixin](https://github.com/FabricMC/Mixin) — поддерживаемая среда Mixin, используемая Avrix.
* [MixinExtras](https://github.com/LlamaLad7/MixinExtras) — дополнительные инъекторы и синтаксический сахар.
* [Введение в Mixin (FabricMC Wiki)](https://fabricmc.net/wiki/tutorial:mixin_introduction) — базовые точки
  внедрения (`HEAD`, `RETURN`, `INVOKE`).
* [Документация SpongePowered Mixin](https://github.com/SpongePowered/Mixin/wiki) — аксессоры полей, работа с
  локальными переменными и продвинутые точки внедрения.
