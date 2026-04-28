package net.wertygh.jffl.js.engine;

import dev.latvian.mods.rhino.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.wertygh.jffl.js.JfflJs;
import net.wertygh.jffl.js.JfflJsConsole;

public class RhinoJsEngine implements JfflJsEngine {
    private ScriptableObject globalScope;
    private final ThreadLocal<Context> threadCtx = new ThreadLocal<>();

    @Override
    public void initialize() {
        Context cx = Context.enter();
        try {
            globalScope = cx.initStandardObjects();
            ScriptableObject.putProperty(globalScope, "JFFLJS",
                    new NativeJavaClass(cx, globalScope, JfflJs.class), cx);
            ScriptableObject.putProperty(globalScope, "console",
                    new JfflJsConsole(), cx);
            ScriptableObject.putProperty(globalScope, "Java",
                    new JavaHelper(), cx);
        } finally {}
    }

    @Override
    public void executeScript(Path script) throws Exception {
        Context cx = Context.enter();
        try {
            String src = Files.readString(script);
            cx.evaluateString(globalScope, src, script.toString(), 1, null);
        } finally {}
    }

    @Override
    public void enterContext() {
        if (threadCtx.get() == null) {
            threadCtx.set(Context.enter());
        }
    }

    @Override
    public void exitContext() {
        threadCtx.remove();
    }

    @Override
    public Object getCurrentContext() {
        Context cx = threadCtx.get();
        if (cx == null) {
            cx = Context.enter();
            threadCtx.set(cx);
        }
        return cx;
    }

    @Override
    public JfflJsFunction wrapFunction(Object nativeCallable) {
        if (!(nativeCallable instanceof Function f)) {
            throw new IllegalArgumentException("不是Rhino功能");
        }
        return (self, args) -> {
            Context cx = getContextForCall();
            Object[] actual = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                actual[i] = Context.javaToJS(cx, args[i], globalScope);
            }
            Object result = f.call(cx, globalScope, globalScope, actual);
            return result == Undefined.instance ? null : result;
        };
    }

    @Override
    public Object callFunction(JfflJsFunction function, Object self, Object[] args) throws Exception {
        return function.call(self, args);
    }

    @Override
    public Object wrapJavaObject(Object javaObject) {
        Context cx = getContextForCall();
        return Context.javaToJS(cx, javaObject, globalScope);
    }
    
    @Override
    public Object wrapJavaClass(Class<?> clazz) {
        Context cx = getContextForCall();
        return new NativeJavaClass(cx, globalScope, clazz);
    }
    
    @Override
    public Object loadJavaClass(String className) throws Exception {
        Class<?> clazz = Class.forName(className, false, JfflJs.class.getClassLoader());
        Context cx = getContextForCall();
        return new NativeJavaClass(cx, globalScope, clazz);
    }

    @Override
    public Object getGlobalBindings() {
        return globalScope;
    }

    @Override
    public String name() {
        return "Rhino (dev.latvian.mods.rhino)";
    }

    private Context getContextForCall() {
        Context cx = (Context) getCurrentContext();
        if (cx == null) {
            cx = Context.enter();
        }
        return cx;
    }

    public class JavaHelper {
        public Object loadClass(String className) {
            try {
                return loadJavaClass(className);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}