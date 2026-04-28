package net.wertygh.jffl.js.builder;

import dev.latvian.mods.rhino.Function;
import javassist.*;
import javassist.bytecode.AccessFlag;
import net.wertygh.jffl.js.engine.JfflJsEngine;
import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import net.wertygh.jffl.js.engine.JfflJsFunction;
import net.wertygh.jffl.js.util.*;

import java.util.ArrayList;
import java.util.List;

public class JfflJsMethodBuilder {
    private record Param(String type, String name) {}
    private final JfflJsClassBuilder parent;
    private final boolean constructor;
    private final String name;
    private final List<Param> params = new ArrayList<>();
    private final List<String> throwsTypes = new ArrayList<>();
    private final List<JfflJsAnnotationSpec> annotations = new ArrayList<>();
    private String returnType = "void";
    private String signature;
    private int modifiers = Modifier.PUBLIC;
    private boolean isBridge;
    private boolean isSynthetic;
    
    private JfflJsMethodBuilder(JfflJsClassBuilder parent, boolean constructor, String name) {
        this.parent = parent; this.constructor = constructor; this.name = name;
    }

    static JfflJsMethodBuilder method(JfflJsClassBuilder parent, String name) {
        return new JfflJsMethodBuilder(parent, false, name);
    }
    static JfflJsMethodBuilder constructor(JfflJsClassBuilder parent) {
        return new JfflJsMethodBuilder(parent, true, parent.simpleName()); 
    }
    public JfflJsMethodBuilder modifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }
    public JfflJsMethodBuilder pub() {
        modifiers = (modifiers & ~(Modifier.PRIVATE|Modifier.PROTECTED))|Modifier.PUBLIC;
        return this;
    }
    public JfflJsMethodBuilder priv() {
        modifiers = (modifiers & ~(Modifier.PUBLIC|Modifier.PROTECTED))|Modifier.PRIVATE;
        return this;
    }
    public JfflJsMethodBuilder prot() {
        modifiers = (modifiers & ~(Modifier.PUBLIC|Modifier.PRIVATE))|Modifier.PROTECTED;
        return this;
    }
    public JfflJsMethodBuilder packagePrivate() {
        modifiers &= ~(Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED);
        return this;
    }
    public JfflJsMethodBuilder static_() {
        if (constructor) throw new IllegalStateException("构造函数不能是static");
        modifiers |= Modifier.STATIC;
        return this;
    }
    public JfflJsMethodBuilder abstract_() {
        if (constructor) throw new IllegalStateException("构造函数不能是abstract");
        modifiers |= Modifier.ABSTRACT;
        return this;
    }
    public JfflJsMethodBuilder synchronized_() {
        modifiers |= Modifier.SYNCHRONIZED;
        return this;
    }
    public JfflJsMethodBuilder final_() {modifiers |= Modifier.FINAL; return this;}
    public JfflJsMethodBuilder native_() {modifiers |= Modifier.NATIVE; return this;}
    public JfflJsMethodBuilder varargs() {modifiers |= Modifier.VARARGS; return this;}
    public JfflJsMethodBuilder bridge() {isBridge = true;return this;}
    public JfflJsMethodBuilder synthetic() {isSynthetic = true;return this;}

    public JfflJsMethodBuilder returns(Object type) {
        if (constructor) throw new IllegalStateException("构造函数没有返回类型");
        this.returnType = JfflJsTypes.typeName(type); return this;
    }

    public JfflJsMethodBuilder param(Object type) {
        return param(type, "p" + params.size());
    }
    
    public JfflJsMethodBuilder param(Object type, String name) {
        this.params.add(new Param(
            JfflJsTypes.typeName(type), 
            name == null || name.isBlank() ? "p" + params.size() : name
        ));
        return this;
    }
    
    public JfflJsMethodBuilder params(Object... types) {
        if (types != null) for (Object t : types) param(t);
        return this;
    }
    
    public JfflJsMethodBuilder throws_(String type) {
        throwsTypes.add(type);
        return this;
    }
    
    public JfflJsMethodBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }

    public JfflJsAnnotationBuilder<JfflJsMethodBuilder> annotate(String annotationType) {
        return new JfflJsAnnotationBuilder<>(this, annotationType, annotations::add);
    }

    public JfflJsClassBuilder body(String sourceBody) {
        return buildWithBody(JfflJsTypes.block(sourceBody));
    }

    public JfflJsClassBuilder body(Object builderFunction) {
        JfflJsBodyBuilder b = new JfflJsBodyBuilder();
        if (builderFunction != null) {
            callBody(builderFunction, b);
        }
        return buildWithBody(b.build());
    }

    @Deprecated
    public JfflJsClassBuilder body(Function builderFunction) {
        return body((Object) builderFunction);
    }

    public JfflJsClassBuilder js(Object function) {
        if (constructor) throw new IllegalStateException("构造函数的JS函数体目前有意不支持");
        JfflJsEngine engine = JfflJsEngineHolder.getEngine();
        String id = JfflJsFunctionRegistry.register(function, JfflJsTypes.javaClass(returnType), parent.name() + "." + name);
        String selfExpr = Modifier.isStatic(modifiers) ? "null" : "this";
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) args.append(", ");
            args.append("($w)").append(params.get(i).name());
        }
        String callExpr = "net.wertygh.jffl.js.util.JfflJsFunctionRegistry.call("
                + JfflJsNames.quote(id) + ", " + selfExpr + ", new java.lang.Object[]{" + args + "})";
        return buildWithBody("{" + JfflJsTypes.returnFromObject(returnType, callExpr) + "}");
    }

    @Deprecated
    public JfflJsClassBuilder js(Function function) {
        return js((Object) function);
    }

    public JfflJsClassBuilder noBody() {return buildWithBody(null);}
    
    public JfflJsClassBuilder end() {
        if ((modifiers & (Modifier.ABSTRACT | Modifier.NATIVE)) != 0) return noBody();
        if (constructor) return buildWithBody("{super();}");
        return buildWithBody(JfflJsTypes.defaultReturnBody(returnType));
    }

    private JfflJsClassBuilder buildWithBody(String body) {
        parent.checkOpen();
        try {
            String source = toSource(body);
            if (constructor) {
                CtConstructor c = CtNewConstructor.make(source, parent.ctClass);
                parent.ctClass.addConstructor(c);
                JfflJsAnnotationUtil.apply(
                    parent.ctClass.getClassFile().getConstPool(),
                    c.getMethodInfo().getAttributes(), annotations
                );
            } else {
                CtMethod m = CtNewMethod.make(source, parent.ctClass);
                if (isBridge) m.getMethodInfo().setAccessFlags(
                    m.getMethodInfo().getAccessFlags() | javassist.bytecode.AccessFlag.BRIDGE);
                if (isSynthetic) m.getMethodInfo().setAccessFlags(
                    m.getMethodInfo().getAccessFlags() | javassist.bytecode.AccessFlag.SYNTHETIC);
                if (signature != null && !signature.isBlank()) {
                    m.setGenericSignature(signature);
                }
                parent.ctClass.addMethod(m);
                JfflJsAnnotationUtil.apply(
                    parent.ctClass.getClassFile().getConstPool(),
                    m.getMethodInfo().getAttributes(), annotations
                );
            }
            return parent;
        } catch (Exception e) {
            throw new RuntimeException("无法将方法/构造函数 "+name+" 添加到 "+parent.name(), e);
        }
    }

    private String toSource(String body) {
        StringBuilder sb = new StringBuilder();
        String mods = modifiersToSource(modifiers);
        if (!mods.isEmpty()) sb.append(mods).append(' ');
        if (constructor) sb.append(parent.simpleName());
        else sb.append(returnType).append(' ').append(name);
        sb.append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            Param p = params.get(i);
            sb.append(p.type()).append(' ').append(p.name());
        }
        sb.append(')');
        if (!throwsTypes.isEmpty()) {
            sb.append(" throws ");
            for (int i = 0; i < throwsTypes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(throwsTypes.get(i));
            }
        }
        if (body == null) sb.append(';'); else sb.append(' ').append(body);
        return sb.toString();
    }

    private void callBody(Object body, JfflJsBodyBuilder b) {
        JfflJsEngine engine = JfflJsEngineHolder.getEngine();
        JfflJsFunction func = engine.wrapFunction(body);
        try {
            func.call(null, b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String modifiersToSource(int m) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(m)) sb.append("public ");
        else if (Modifier.isPrivate(m)) sb.append("private ");
        else if (Modifier.isProtected(m)) sb.append("protected ");
        if (Modifier.isStatic(m)) sb.append("static ");
        if (Modifier.isFinal(m)) sb.append("final ");
        if (Modifier.isSynchronized(m)) sb.append("synchronized ");
        if (Modifier.isNative(m)) sb.append("native ");
        if (Modifier.isAbstract(m)) sb.append("abstract ");
        return sb.toString().trim();
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}