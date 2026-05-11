[Главная](../../wiki-language.md) > [Документация](../wiki-main.md) > Миксины

## ⚡Миксины

В Avrix реализована система миксинов на базе библиотеки [ClassTransform](https://github.com/Lenni0451/ClassTransform). Рекомендуем ознакомиться с [Wiki](https://github.com/Lenni0451/ClassTransform/wiki) этой библиотеки.

Если вы только начинаете изучать миксины для Avrix и не знакомы с Fabric MC лучшим решением будет использовать аннотации от **ClassTransform**. Однако, поскольку данная библиотека позволяет использовать аннотации [Mixin](https://github.com/SpongePowered/Mixin/) и [ExtraMixin](https://github.com/LlamaLad7/MixinExtras), вы можете использовать и их: 

```gradle
// ...
dependencies {
	compileOnly 'net.lenni0451.classtransform:mixinsdummy:x.x.x'
}
// ...
```

В рамках данной документации не будет показано как писать классы-миксины, для этого уже существуют некоторые [туториалы для Fabric MC](https://wiki.fabricmc.net/tutorial:mixin_introduction), которые в общем случае не отличаются в разработке для Avrix.
