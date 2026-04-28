package net.wertygh.jffl.js.util;

import net.wertygh.jffl.js.engine.JfflJsEngine;
import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import net.wertygh.jffl.js.engine.JfflJsFunction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JfflJsFunctionRegistry {
    private static final ConcurrentHashMap<String, JfflJsFunction> FUNCTIONS = new ConcurrentHashMap<>();

    public static String register(Object nativeFn, Class<?> returnType, String debug) {
        JfflJsEngine engine = JfflJsEngineHolder.getEngine();
        JfflJsFunction func = engine.wrapFunction(nativeFn);
        String id = debug + "#" + UUID.randomUUID();
        FUNCTIONS.put(id, func);
        return id;
    }

    public static String registerObject(Object nativeFn, String debug) {
        return register(nativeFn, Object.class, debug);
    }

    public static Object call(String id, Object self, Object[] args) {
        JfflJsFunction fn = FUNCTIONS.get(id);
        if (fn == null) {
            throw new IllegalArgumentException("未知函数ID: " + id);
        }
        try {
            return fn.call(self, args);
        } catch (Exception e) {
            throw new RuntimeException("函数调用失败: " + id, e);
        }
    }
}