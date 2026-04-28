package net.wertygh.jffl.js.plugin;

import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JfflJsClassLoader extends ClassLoader {
    public static final JfflJsClassLoader INSTANCE = new JfflJsClassLoader(JfflJsClassLoader.class.getClassLoader());
    private static final Logger LOGGER = LoggerFactory.getLogger(JfflJsClassLoader.class);
    private final Map<String, Class<?>> defined = new ConcurrentHashMap<>();

    private JfflJsClassLoader(ClassLoader parent) {
        super(parent);
    }

    public synchronized Class<?> define(String name, byte[] bytes) {
        Class<?> old = defined.get(name);
        if (old != null) {
            return old;
        }
        ProtectionDomain pd = JfflJsClassLoader.class.getProtectionDomain();
        Class<?> c = defineClass(name, bytes, 0, bytes.length, pd);
        resolveClass(c);
        defined.put(name, c);
        LOGGER.debug("JFFLJS defined class {}", name);
        return c;
    }

    public Object defineForJS(String name, byte[] bytes) {
        return wrapForJS(define(name, bytes));
    }

    public Object getForJS(String name) {
        Class<?> c = defined.get(name);
        return c == null ? null : wrapForJS(c);
    }

    public int definedCount() {
        return defined.size();
    }

    private Object wrapForJS(Class<?> clazz) {
        return JfflJsEngineHolder.getEngine().wrapJavaClass(clazz);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> c = defined.get(name);
        return c != null ? c : super.findClass(name);
    }
}
