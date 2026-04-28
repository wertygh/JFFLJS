package net.wertygh.jffl.js.builder;

import net.wertygh.jffl.js.JfflJs;
import net.wertygh.jffl.js.hook.JfflJsCondition;
import net.wertygh.jffl.js.hook.JfflJsHooks;
import net.wertygh.jffl.js.hook.JfflJsTransformClass;
import net.wertygh.jffl.js.model.JfflJsAt;
import net.wertygh.jffl.js.model.JfflJsSlice;
import net.wertygh.jffl.js.util.JfflJsNames;

import java.util.ArrayList;
import java.util.List;

public class JfflJsPatchBuilder {
    private final String targetClass;
    private int priority = 1000;
    private boolean optional = false;
    private final List<JfflJsCondition> conditions = new ArrayList<>();

    public JfflJsPatchBuilder(String targetClass) {
        this.targetClass = JfflJsNames.normalizeTarget(targetClass);
    }
    
    public String target() {
        return targetClass;
    }
    
    public int priority() {
        return priority;
    }
    
    public JfflJsPatchBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public JfflJsPatchBuilder optional() {
        this.optional = true;
        return this;
    }

    public JfflJsPatchBuilder classExists(String className) {
        conditions.add(cls -> {
            try {
                cls.pool().get(className);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        });
        return this;
    }

    public JfflJsPatchBuilder notClassExists(String className) {
        conditions.add(cls -> {
            try {
                cls.pool().get(className);
                return false;
            } catch (Throwable ignored) {
                return true;
            }
        });
        return this;
    }

    public JfflJsPatchBuilder systemProperty(String key) {
        return systemProperty(key, "");
    }

    public JfflJsPatchBuilder systemProperty(String key, String expect) {
        conditions.add(cls -> {
            String val = System.getProperty(key, "");
            return expect == null || expect.isEmpty() ? !val.isEmpty() : expect.equals(val);
        });
        return this;
    }

    public JfflJsPatchBuilder envVar(String key) {
        return envVar(key, "");
    }

    public JfflJsPatchBuilder envVar(String key, String expect) {
        conditions.add(cls -> {
            String val = System.getenv(key);
            if (val == null) val = "";
            return expect == null || expect.isEmpty() ? !val.isEmpty() : expect.equals(val);
        });
        return this;
    }
    public JfflJsPatchBuilder methodExists(String methodName) {
        conditions.add(cls -> cls.hasMethod(methodName));
        return this;
    }
    public JfflJsPatchBuilder condition(JfflJsCondition condition) {
        conditions.add(condition);
        return this;
    }
    public JfflJsPatchBuilder dumpClass() {
        return dumpClass(".jffl-dump");
    }
    public JfflJsPatchBuilder dumpClass(String dir) {
        register(cls -> cls.dump(dir));
        return this;
    }
    public JfflJsPatchBuilder addInterface(String iface) {
        register(cls -> cls.addInterface(iface));
        return this;
    }
    public JfflJsPatchBuilder addField(String source) {
        register(cls -> cls.addField(source));
        return this;
    }
    public JfflJsPatchBuilder addField(String name, String type, String initializer, int access) {
        register(cls -> cls.addField(name, type, initializer, access));
        return this;
    }
    public JfflJsPatchBuilder patchStateField(String name, String type, String initializer, int access) {
        return addField(name, type, initializer, access == 0 ? javassist.Modifier.PRIVATE | javassist.Modifier.STATIC : access);
    }
    public JfflJsPatchBuilder accessor(String target) {
        return accessor(target, false, "", false);
    }
    public JfflJsPatchBuilder accessor(String target, boolean invoker, String desc, boolean optional) {
        register(cls -> cls.accessor(target, invoker, desc, optional));
        return this;
    }
    public JfflJsPatchBuilder shadow(String target) {
        return shadow(target, false, "", false);
    }
    public JfflJsPatchBuilder shadow(String target, boolean invoker, String desc, boolean optional) {
        register(cls -> cls.accessor(target, invoker, desc, optional));
        return this;
    }
    public JfflJsPatchBuilder insertBefore(String method, String source) {
        return insertBefore(method, "", source);
    }
    public JfflJsPatchBuilder insertBefore(String method, String desc, String source) {
        register(cls -> cls.insertBefore(method, desc, source));
        return this;
    }
    public JfflJsPatchBuilder insertAfter(String method, String source) {
        return insertAfter(method, "", source);
    }
    public JfflJsPatchBuilder insertAfter(String method, String desc, String source) {
        register(cls -> cls.insertAfter(method, desc, source));
        return this;
    }
    public JfflJsPatchBuilder replaceMethod(String method, String source) {
        return replaceMethod(method, "", source);
    }
    public JfflJsPatchBuilder replaceMethod(String method, String desc, String source) {
        register(cls -> cls.setBody(method, desc, source));
        return this;
    }
    public JfflJsPatchBuilder addMethod(String source) {
        register(cls -> cls.addMethod(source));
        return this;
    }
    public JfflJsPatchBuilder inject(String method, JfflJsAt at, String source) {
        return inject(method, "", at, null, source);
    }
    public JfflJsPatchBuilder inject(String method, String desc, JfflJsAt at, JfflJsSlice slice, String source) {
        register(cls -> cls.inject(method, desc, at, slice, source));
        return this;
    }
    public JfflJsPatchBuilder redirect(String method, JfflJsAt at, String source) {
        return redirect(method, "", at, null, source);
    }
    public JfflJsPatchBuilder redirect(String method, String desc, JfflJsAt at, JfflJsSlice slice, String source) {
        register(cls -> cls.redirect(method, desc, at, slice, source));
        return this;
    }
    public JfflJsPatchBuilder wrapOperation(String method, JfflJsAt at, String source) {
        return wrapOperation(method, "", at, null, source);
    }
    public JfflJsPatchBuilder wrapOperation(String method, String desc, JfflJsAt at, JfflJsSlice slice, String source) {
        register(cls -> cls.wrapOperation(method, desc, at, slice, source));
        return this;
    }
    public JfflJsPatchBuilder modifyConstant(String method, JfflJsAt constant, String source) {
        return modifyConstant(method, "", constant, null, source);
    }
    public JfflJsPatchBuilder modifyConstant(String method, String desc, JfflJsAt constant, JfflJsSlice slice, String source) {
        register(cls -> cls.inject(method, desc, constant == null ? JfflJs.constant() : constant, slice, source));
        return this;
    }
    public JfflJsPatchBuilder modifyReturnValue(String method, String source) {
        return modifyReturnValue(method, "", source);
    }
    public JfflJsPatchBuilder modifyReturnValue(String method, String desc, String source) {
        register(cls -> cls.modifyReturnValue(method, desc, source));
        return this;
    }
    public JfflJsPatchBuilder insertAtLine(String method, int line, String source) {
        return insertAtLine(method, "", line, source);
    }
    public JfflJsPatchBuilder insertAtLine(String method, String desc, int line, String source) {
        register(cls -> cls.insertAtLine(method, desc, line, source));
        return this;
    }
    public JfflJsPatchBuilder wrapTryCatch(String method, String source) {
        return wrapTryCatch(method, "", "java.lang.Throwable", source);
    }
    public JfflJsPatchBuilder wrapTryCatch(String method, String desc, String exceptionType, String source) {
        register(cls -> cls.wrapTryCatch(method, desc, exceptionType, source));
        return this;
    }
    public JfflJsPatchBuilder addConstructorCode(String position, String source) {
        return addConstructorCode("", position, source);
    }
    public JfflJsPatchBuilder addConstructorCode(String desc, String position, String source) {
        register(cls -> cls.addConstructorCode(desc, position, source));
        return this;
    }
    public JfflJsPatchBuilder injectStaticInit(String position, String source) {
        register(cls -> cls.injectStaticInit(position, source));
        return this;
    }
    public JfflJsPatchBuilder cloneMethod(String method) {
        return cloneMethod(method, "", "");
    }
    public JfflJsPatchBuilder cloneMethod(String method, String desc, String cloneName) {
        register(cls -> cls.cloneMethod(method, desc, cloneName));
        return this;
    }
    public JfflJsPatchBuilder wrapMethod(String method, String source) {
        return wrapMethod(method, "", source);
    }
    public JfflJsPatchBuilder wrapMethod(String method, String desc, String source) {
        register(cls -> cls.wrapMethod(method, desc, source));
        return this;
    }
    public JfflJsPatchBuilder modifyArg(String method, String target, int index, String expr) {
        return modifyArg(method, "", target, index, -1, expr);
    }
    public JfflJsPatchBuilder modifyArg(String method, String desc, String target, int index, int ordinal, String expr) {
        register(cls -> cls.modifyArg(method, desc, target, index, ordinal, expr));
        return this;
    }
    public JfflJsPatchBuilder modifyVariable(String method, String name, String expr) {
        return modifyVariable(method, "", name, -1, -1, JfflJs.head(), expr);
    }
    public JfflJsPatchBuilder modifyVariable(String method, String desc, String name, int index, int line, JfflJsAt at, String expr) {
        register(cls -> cls.modifyVariable(method, desc, name, index, line, at, expr));
        return this;
    }
    public JfflJsPatchBuilder instrumentNewExpr(String method, String target, int ordinal, String source) {
        register(cls -> cls.instrumentNewExpr(method, "", target, ordinal, source));
        return this;
    }
    public JfflJsPatchBuilder instrumentCast(String method, String target, int ordinal, String source) {
        register(cls -> cls.instrumentCast(method, "", target, ordinal, source));
        return this;
    }
    public JfflJsPatchBuilder instrumentInstanceof(String method, String target, int ordinal, String source) {
        register(cls -> cls.instrumentInstanceof(method, "", target, ordinal, source));
        return this;
    }
    public JfflJsPatchBuilder instrumentHandler(String method, String exceptionType, int ordinal, String source) {
        register(cls -> cls.instrumentHandler(method, "", exceptionType, ordinal, source));
        return this;
    }
    public JfflJsPatchBuilder instrumentFieldAccess(String method, String target, String accessType, int ordinal, String source) {
        register(cls -> cls.instrumentFieldAccess(method, "", target, accessType, ordinal, source));
        return this;
    }
    public JfflJsPatchBuilder instrumentConstructorCall(String method, String target, int ordinal, String source) {
        register(cls -> cls.instrumentConstructorCall(method, "", target, ordinal, source));
        return this;
    }

    private void register(JfflJsHooks.TransformOperation operation) {
        JfflJsHooks.registerOperation(targetClass, priority, cls -> {
            if (!conditionsPass(cls)) return;
            try {
                operation.apply(cls);
            } catch (Throwable t) {
                if (!optional) throw t;
                JfflJs.warn("Optional JFFLJS patch operation failed on " + targetClass + ": " + t);
            }
        });
    }

    private boolean conditionsPass(JfflJsTransformClass cls) throws Exception {
        for (JfflJsCondition c : conditions) if (!c.test(cls)) return false;
        return true;
    }
}