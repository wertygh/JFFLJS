## 简介

JFFLJS是[JFFL](https://github.com/wertygh/JFFL-Javassist-Framework-for-Forge-Loader-)字节码修改框架的JavaScript脚本扩展。
允许开发者在运行时使用JavaScript动态生成Java类或对现有类进行字节码补丁。
[English](https://github.com/wertygh/JFFLJS/blob/main/README_English.md)

## 功能特性

 - 提供链式构建器API创建完整的Java类（字段、方法、构造函数、注解）。
 - 仿照@Patch注解的声明式补丁DSL – 注入、重定向、包装、替换方法。
 - 可在JS中直接操作CtClass和原始字节码。
 - 自动加载kubejs/jffljs_script/目录下的所有.js脚本。
 - 无缝集成JFFL变换管线。
 - 支持[GraalJS](https://github.com/oracle/graaljs)和[Rhino](https://github.com/KubeJS-Mods/Rhino)两种JS引擎。

## 安装

1. 安装[JFFL](https://www.curseforge.com/minecraft/mc-mods/jffl)。
2. 添加JavaScript引擎模组：推荐[Graal](https://www.curseforge.com/minecraft/mc-mods/graal/)，或者[Rhino](https://www.curseforge.com/minecraft/mc-mods/rhino)。
3. 将你的.js脚本放入/kubejs/jffljs_script/目录，启动游戏时会自动加载。

## 功能快速概览

 - **动态生成类**

```js
// 创建一个简单的工具类
JfflJs.clazz("utils.MathHelper")
    .pub().final_()
    .method("square")
        .pub().static_()
        .returns("int")
        .param("int", "n")
        .body(b => b.return_("n * n"))
    .define();
```

 - **对Minecraft类应用补丁**

```js
JFFLJS.patch('net.minecraft.client.main.Main')
    .insertBefore('main', '([Ljava/lang/String;)V',
        'System.out.println(\'Hello, JFFLJS(Loaded jffljs_script)\');'
    );
```

 - **使用JavaScript函数作为方法体**

```js
JfflJs.clazz("handlers.MyHandler")
    .method("handle")
        .pub()
        .returns("void")
        .js(function(self, args) {
            console.log("事件处理，参数：" + args);
        })
    .define();
```

 - **API概述**

 - JFFLJS.clazz(类名) – 构建一个新类。
 - JFFLJS.patch(目标类) – 对一个现有类声明一组变换。
 - JFFLJS.transform(目标类) – 底层访问：javassist、原始字节、操作钩子。
 - JFFLJS.at(值) – 注入点（HEAD、RETURN、INVOKE、FIELD等）。
 - JFFLJS.body() – 辅助构建Javassist源码片段。

详细方法签名请参阅源码及示例。

 - **条件与优先级**

补丁可以附带条件和优先级：

```js
JfflJs.patch("com.example.MyClass")
    .priority(500)
    .classExists("com.some.RequiredModClass")
    .systemProperty("some.config.enabled")
    .inject("myMethod", JfflJs.head(), '{ /* 注入代码 */ }');
```

## 引擎选择

JFFLJS按以下顺序自动检测JS引擎：

1. GraalJS（两者存在时优先）
2. Rhino（GraalJS不存在时选择）

也可以手动通过JfflJsEngineHolder.getEngine()获取引擎实例。

## 注意事项

 - 所有补丁动作在对应类首次加载时执行一次，按优先级排序。
 - 作为补丁的JS回调会在JS引擎上下文中执行，应避免阻塞操作。
 - 通过.clazz()创建的类默认放置在generated.jffljs.custom包下，如需自定义完整类名请使用rawClazz()。
 - 复杂修改可使用transform() API直接操作CtClass或原始字节。
 - 本模组非常重视安全性，绝不会偷走你的钱包——请到kubejs/jffljs_script/下检查本模组的脚本。

## 协议许可

JFFLJS与JFFL采用相同的开源协议。
详情请查看父项目。
