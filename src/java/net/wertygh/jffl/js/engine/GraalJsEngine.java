package net.wertygh.jffl.js.engine;

import graal.graalvm.polyglot.Context;
import graal.graalvm.polyglot.Engine;
import graal.graalvm.polyglot.HostAccess;
import graal.graalvm.polyglot.Value;
import java.nio.file.Files;
import java.nio.file.Path;
import net.wertygh.jffl.js.JfflJs;
import net.wertygh.jffl.js.JfflJsConsole;

public class GraalJsEngine implements JfflJsEngine {
    private Context context;
    private Value globalBindings;

    @Override
    public void initialize() {
        Engine sharedEngine = Engine.newBuilder()
            .allowExperimentalOptions(true)
            .option("js.ecmascript-version", "2024")
            .option("js.nashorn-compat", "true")
            .option("js.foreign-object-prototype", "true")
            .build();
        
        context = Context.newBuilder("js")
            .engine(sharedEngine)
            .allowAllAccess(true)
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup(s -> true)
            .hostClassLoader(JfflJs.class.getClassLoader())
            .allowNativeAccess(true)
            .allowExperimentalOptions(true)
            .build();
        context.eval("js", """
            Java.loadClass = Java.type
            Java.class = Java.type("java.lang.Class").forName("java.lang.Class")
            Java.class.forName = Java.type("java.lang.Class").forName
        """);
        context.eval("js", "JFFLJS = Java.type('"+JfflJs.class.getName()+"')");
        context.eval("js", "console = new (Java.type('"+JfflJsConsole.class.getName()+"'))()");
    }

    @Override
    public void executeScript(Path script) throws Exception {
        String src = Files.readString(script);
        context.eval("js", src);
    }

    @Override
    public void enterContext() {}

    @Override
    public void exitContext() {}

    @Override
    public Object getCurrentContext() {
        return context;
    }
    
    @Override
    public Object wrapJavaClass(Class<?> clazz) {
        return Value.asValue(clazz);
    }
    
    @Override
    public JfflJsFunction wrapFunction(Object nativeCallable) {
        if (nativeCallable instanceof Value v && v.canExecute()) {
            return (self, args) -> {
                Value[] jsArgs = new Value[args.length];
                for (int i = 0; i < args.length; i++) {
                    jsArgs[i] = Value.asValue(args[i]);
                }
                return v.execute(jsArgs).as(Object.class);
            };
        }
        if (nativeCallable instanceof java.util.function.Function) {
            Value v = Value.asValue(nativeCallable);
            return (self, args) -> {
                Value[] jsArgs = new Value[args.length];
                for (int i = 0; i < args.length; i++) {
                    jsArgs[i] = Value.asValue(args[i]);
                }
                return v.execute(jsArgs).as(Object.class);
            };
        }
        throw new IllegalArgumentException("不是GraalJS可调用的");
    }

    @Override
    public Object callFunction(JfflJsFunction function, Object self, Object[] args) throws Exception {
        return function.call(self, args);
    }

    @Override
    public Object wrapJavaObject(Object javaObject) {
        return Value.asValue(javaObject);
    }

    @Override
    public Object loadJavaClass(String className) throws Exception {
        return context.eval("js", "Java.type('" + className + "')");
    }

    @Override
    public Object getGlobalBindings() {
        return globalBindings;
    }

    @Override
    public String name() {
        return "GraalJS";
    }
}
