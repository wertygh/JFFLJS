package net.wertygh.jffl.js.hook;

import net.wertygh.jffl.api.ITransformerHook;
import net.wertygh.jffl.js.engine.JfflJsEngine;
import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import net.wertygh.jffl.js.engine.JfflJsFunction;
import net.wertygh.jffl.js.util.JfflJsNames;
import net.wertygh.jffl.js.util.JfflJsTypes;
import org.slf4j.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class JfflJsHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger(JfflJsHooks.class);
    private static final List<HookEntry> HOOKS = new ArrayList<>();
    private static final AtomicLong NEXT = new AtomicLong();

    private enum Kind {JAVASSIST, BYTES, OPERATION}

    @FunctionalInterface
    public interface TransformOperation {
        void apply(JfflJsTransformClass cls) throws Exception;
    }

    private record HookEntry(String target, int priority, long order, Kind kind, Object fn, TransformOperation op) {}

    public static synchronized void registerJavassist(String target, int priority, Object fn) {
        HOOKS.add(new HookEntry(target, priority, NEXT.getAndIncrement(), Kind.JAVASSIST, fn, null));
    }

    public static synchronized void registerBytes(String target, int priority, Object fn) {
        HOOKS.add(new HookEntry(target, priority, NEXT.getAndIncrement(), Kind.BYTES, fn, null));
    }

    public static synchronized void registerOperation(String target, int priority, TransformOperation op) {
        HOOKS.add(new HookEntry(target, priority, NEXT.getAndIncrement(), Kind.OPERATION, null, op));
    }

    public static synchronized Set<String> targetClasses() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (HookEntry h : HOOKS) out.add(h.target);
        return Set.copyOf(out);
    }

    public static synchronized int hookCount() {
        return HOOKS.size();
    }

    public static byte[] apply(ITransformerHook hook) throws Exception {
        List<HookEntry> selected;
        synchronized (JfflJsHooks.class) {
            selected = HOOKS.stream()
                    .filter(h -> JfflJsNames.matchesTarget(h.target, hook.getClassName()))
                    .sorted(Comparator.comparingInt(HookEntry::priority).thenComparingLong(HookEntry::order))
                    .toList();
        }
        if (selected.isEmpty()) return hook.getBytes();
        byte[] cur = hook.getBytes();
        for (HookEntry e : selected) {
            cur = switch (e.kind) {
                case JAVASSIST -> applyJavassist(e, hook, cur);
                case BYTES -> applyBytes(e, hook, cur);
                case OPERATION -> applyOperation(e, hook, cur);
            };
        }
        return cur;
    }

    private static byte[] applyJavassist(HookEntry e, ITransformerHook hook, byte[] cur) throws Exception {
        javassist.ClassPool pool = JfflJsTypes.newClassPool();
        javassist.CtClass ct = null;
        try {
            ct = pool.makeClass(new ByteArrayInputStream(cur));
            JfflJsTransformClass cls = new JfflJsTransformClass(pool, ct);
            JfflJsEngine engine = JfflJsEngineHolder.getEngine();
            JfflJsFunction func = engine.wrapFunction(e.fn);
            engine.enterContext();
            try {
                func.call(null, cls, hook);
            } finally {
                engine.exitContext();
            }
            return ct.toBytecode();
        } catch (Throwable t) {
            LOGGER.error("JFFL钩子失败, {}", hook.getClassName(), t);
            return cur;
        } finally {
            if (ct != null) ct.detach();
        }
    }

    private static byte[] applyBytes(HookEntry e, ITransformerHook hook, byte[] cur) {
        JfflJsMutableHook wrapped = new JfflJsMutableHook(hook, cur);
        JfflJsEngine engine = JfflJsEngineHolder.getEngine();
        engine.enterContext();
        try {
            JfflJsFunction func = engine.wrapFunction(e.fn);
            Object result = func.call(null, wrapped);
            if (result instanceof byte[] bytes) return bytes;
            return cur;
        } catch (Throwable t) {
            LOGGER.error("字节钩子对{}失败", hook.getClassName(), t);
            return cur;
        } finally {
            engine.exitContext();
        }
    }

    private static byte[] applyOperation(HookEntry e, ITransformerHook hook, byte[] cur) throws Exception {
        javassist.ClassPool pool = JfflJsTypes.newClassPool();
        javassist.CtClass ct = null;
        try {
            ct = pool.makeClass(new ByteArrayInputStream(cur));
            JfflJsTransformClass cls = new JfflJsTransformClass(pool, ct);
            e.op.apply(cls);
            return ct.toBytecode();
        } catch (Throwable t) {
            LOGGER.error("对{}的钩子操作失败", hook.getClassName(), t);
            return cur;
        } finally {
            if (ct != null) ct.detach();
        }
    }
}