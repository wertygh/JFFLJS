# JFFLJS – JavaScript Bytecode Manipulation for JFFL

## Introduction

JFFLJS is a JavaScript scripting extension for the [JFFL](https://github.com/wertygh/JFFL-Javassist-Framework-for-Forge-Loader-) bytecode modification framework. It allows developers to dynamically generate Java classes at runtime or apply bytecode patches to existing classes using JavaScript.

## Features

- Provides a chaining builder API to create complete Java classes (fields, methods, constructors, annotations).
- Declarative patching DSL inspired by `@Patch` – inject, redirect, wrap, replace methods.
- Direct manipulation of `CtClass` and raw bytecode from JavaScript.
- Automatically loads all `.js` scripts from the `/kubejs/jffljs_script/` directory.
- Seamless integration with the JFFL transformation pipeline.
- Supports both [GraalJS](https://github.com/oracle/graaljs) and [Rhino](https://github.com/KubeJS-Mods/Rhino) JS engines.

## Installation

1. Install [JFFL](https://www.curseforge.com/minecraft/mc-mods/jffl).
2. Add a JavaScript engine mod: recommended [Graal](https://www.curseforge.com/minecraft/mc-mods/graal/), or [Rhino](https://www.curseforge.com/minecraft/mc-mods/rhino).
3. Place your `.js` scripts into `/kubejs/jffljs_script/`; they will be automatically loaded when the game starts.

## Quick Feature Overview

### Dynamically Generate a Class

```js
// Create a simple utility class
JfflJs.clazz("utils.MathHelper")
    .pub().final_()
    .method("square")
        .pub().static_()
        .returns("int")
        .param("int", "n")
        .body(b => b.return_("n * n"))
    .define();
```

### Apply a Patch to a Minecraft Class

```js
JFFLJS.patch('net.minecraft.client.main.Main')
    .insertBefore('main', '([Ljava/lang/String;)V',
        'System.out.println(\'Hello, JFFLJS(Loaded jffljs_script)\');'
    );
```

### Use a JavaScript Function as the Method Body

```js
JfflJs.clazz("handlers.MyHandler")
    .method("handle")
        .pub()
        .returns("void")
        .js(function(self, args) {
            console.log("Event handled, args: " + args);
        })
    .define();
```

### API Overview

- `JFFLJS.clazz(className)` – Build a new class.
- `JFFLJS.patch(targetClass)` – Declare a set of transformations on an existing class.
- `JFFLJS.transform(targetClass)` – Low‑level access: javassist, raw bytes, operation hooks.
- `JFFLJS.at(value)` – Injection point (HEAD, RETURN, INVOKE, FIELD, etc.).
- `JFFLJS.body()` – Helper to build Javassist source snippets.

For detailed method signatures, please refer to the source code and examples.

### Conditions and Priority

Patches can include conditions and a priority value:

```js
JfflJs.patch("com.example.MyClass")
    .priority(500)
    .classExists("com.some.RequiredModClass")
    .systemProperty("some.config.enabled")
    .inject("myMethod", JfflJs.head(), '{ /* injected code */ }');
```

## Engine Selection

JFFLJS automatically detects the JS engine in the following order:

1. GraalJS (preferred if present)
2. Rhino (chosen when GraalJS is not available)

You can also manually obtain the engine instance via `JfflJsEngineHolder.getEngine()`.

## Important Notes

- All patch actions are executed once, when the target class is first loaded, and are sorted by priority.
- JS callbacks used as patch code run in the JS engine context; avoid blocking operations.
- Classes created via `.clazz()` are placed in the `generated.jffljs.custom` package by default. Use `rawClazz()` for a custom fully‑qualified name.
- For complex modifications, use the `transform()` API to directly manipulate `CtClass` or raw bytes.
- This mod takes security seriously – it will never steal your wallet. Check the scripts located in `/kubejs/jffljs_script/` yourself.

## License

JFFLJS uses the same open‑source license as JFFL.  
See the parent project for details.
