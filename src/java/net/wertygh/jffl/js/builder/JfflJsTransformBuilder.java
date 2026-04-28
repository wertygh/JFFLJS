package net.wertygh.jffl.js.builder;

import dev.latvian.mods.rhino.Function;
import net.wertygh.jffl.js.hook.JfflJsHooks;
import net.wertygh.jffl.js.util.JfflJsNames;

public class JfflJsTransformBuilder {
    private final String targetClass;
    private int priority = 1000;

    public JfflJsTransformBuilder(String targetClass) {
        this.targetClass = JfflJsNames.normalizeTarget(targetClass);
    }

    public JfflJsTransformBuilder priority(int p) {
        priority = p;
        return this;
    }

    public JfflJsTransformBuilder javassist(Object fn) {
        JfflJsHooks.registerJavassist(targetClass, priority, fn);
        return this;
    }

    @Deprecated
    public JfflJsTransformBuilder javassist(Function fn) {
        return javassist((Object) fn);
    }

    public JfflJsTransformBuilder bytes(Object fn) {
        JfflJsHooks.registerBytes(targetClass, priority, fn);
        return this;
    }

    @Deprecated
    public JfflJsTransformBuilder bytes(Function fn) {
        return bytes((Object) fn);
    }

    public JfflJsTransformBuilder op(JfflJsHooks.TransformOperation op) {
        JfflJsHooks.registerOperation(targetClass, priority, op);
        return this;
    }

    public JfflJsTransformBuilder addField(String src) {
        return op(cls -> cls.addField(src));
    }

    public JfflJsTransformBuilder addMethod(String src) {
        return op(cls -> cls.addMethod(src));
    }

    public JfflJsTransformBuilder addConstructor(String src) {
        return op(cls -> cls.addConstructor(src));
    }

    public JfflJsTransformBuilder insertBefore(String method, String src) {
        return op(cls -> cls.insertBefore(method, src));
    }

    public JfflJsTransformBuilder insertBefore(String method, String desc, String src) {
        return op(cls -> cls.insertBefore(method, desc, src));
    }

    public JfflJsTransformBuilder insertAfter(String method, String src) {
        return op(cls -> cls.insertAfter(method, src));
    }

    public JfflJsTransformBuilder insertAfter(String method, String desc, String src) {
        return op(cls -> cls.insertAfter(method, desc, src));
    }

    public JfflJsTransformBuilder setBody(String method, String src) {
        return op(cls -> cls.setBody(method, src));
    }

    public JfflJsTransformBuilder setBody(String method, String desc, String src) {
        return op(cls -> cls.setBody(method, desc, src));
    }
}