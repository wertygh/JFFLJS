package net.wertygh.jffl.js.builder;

import dev.latvian.mods.rhino.Function;
import javassist.*;
import javassist.bytecode.ClassFile;
import net.wertygh.jffl.js.engine.JfflJsEngine;
import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import net.wertygh.jffl.js.engine.JfflJsFunction;
import net.wertygh.jffl.js.plugin.JfflJsClassLoader;
import net.wertygh.jffl.js.util.*;

import java.util.ArrayList;
import java.util.List;

public class JfflJsClassBuilder {
    private final String className;
    private final ClassPool pool;
    final CtClass ctClass;
    private final List<JfflJsAnnotationSpec> annotations = new ArrayList<>();
    private boolean closed = false;

    private JfflJsClassBuilder(String className, boolean rawName) throws Exception {
        this.className = rawName ? className.trim().replace('/', '.') : JfflJsNames.generatedName(className);
        this.pool = JfflJsTypes.newClassPool();
        this.ctClass = pool.makeClass(this.className);
        this.ctClass.setModifiers(Modifier.PUBLIC);
    }

    public static JfflJsClassBuilder create(String className, boolean rawName) throws Exception {
        return new JfflJsClassBuilder(className, rawName);
    }
    
    public String name() {return className;}

    public JfflJsClassBuilder modifiers(int modifiers) {
        checkOpen(); ctClass.setModifiers(modifiers); return this;
    }
    
    public JfflJsClassBuilder pub() {return access(Modifier.PUBLIC);}
    public JfflJsClassBuilder packagePrivate() {return access(0);}
    
    public JfflJsClassBuilder final_() {
        checkOpen(); 
        ctClass.setModifiers((ctClass.getModifiers() & ~Modifier.ABSTRACT) | Modifier.FINAL); 
        return this;
    }
    
    public JfflJsClassBuilder abstract_() {
        checkOpen();
        ctClass.setModifiers((ctClass.getModifiers() & ~Modifier.FINAL) | Modifier.ABSTRACT);
        return this;
    }
    
    public JfflJsClassBuilder interface_() {
        checkOpen();
        ctClass.setModifiers(Modifier.PUBLIC | Modifier.INTERFACE | Modifier.ABSTRACT);
        return this;
    }

    public JfflJsClassBuilder access(int access) {
        checkOpen();
        int m = ctClass.getModifiers();
        m &= ~(Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED);
        m |= access;
        ctClass.setModifiers(m);
        return this;
    }

    public JfflJsClassBuilder version(int majorVersion) {
        checkOpen();
        ctClass.getClassFile().setMajorVersion(majorVersion);
        return this;
    }
    
    public JfflJsClassBuilder signature(String signature) {
        checkOpen();
        ctClass.setGenericSignature(signature);
        return this;
    }

    public JfflJsAnnotationBuilder<JfflJsClassBuilder> annotate(String annotationType) {
        checkOpen();
        return new JfflJsAnnotationBuilder<>(this, annotationType, annotations::add);
    }

    public JfflJsClassBuilder extendsClass(Object superClass) {
        checkOpen();
        try {
            ctClass.setSuperclass(JfflJsTypes.ct(pool, superClass)); return this;
        } catch (Exception e) {
            throw new RuntimeException("不能将"+className+"的超类设置为"+superClass, e);
        }
    }

    public JfflJsClassBuilder implement(Object interfaceClass) {
        checkOpen();
        try {
            ctClass.addInterface(JfflJsTypes.ct(pool, interfaceClass)); return this;
        } catch (Exception e) {
            throw new RuntimeException("无法将接口"+interfaceClass+"添加到"+className, e);
        }
    }

    public JfflJsClassBuilder implements_(Object... interfaces) {
        if (interfaces != null) for (Object i : interfaces) implement(i);
        return this;
    }

    public JfflJsFieldBuilder field(String name, Object type) {
        checkOpen();
        return new JfflJsFieldBuilder(this, name, JfflJsTypes.typeName(type));
    }

    public JfflJsClassBuilder constField(String name, Object type, String initializer) {
        return field(name, type).pub().static_().final_().init(initializer).end();
    }

    public JfflJsPropertyBuilder property(String name, Object type) {
        checkOpen();
        return new JfflJsPropertyBuilder(this, name, JfflJsTypes.typeName(type));
    }

    public JfflJsClassBuilder defaultConstructor() {
        checkOpen();
        try {
            CtConstructor c = CtNewConstructor.defaultConstructor(ctClass);
            ctClass.addConstructor(c);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("无法向" + className + "添加默认构造函数", e);
        }
    }

    public JfflJsMethodBuilder constructor(Object... paramTypes) {
        checkOpen();
        return JfflJsMethodBuilder.constructor(this).params(paramTypes);
    }
    
    public JfflJsMethodBuilder method(String name) {
        checkOpen();
        return JfflJsMethodBuilder.method(this, name);
    }
    
    public JfflJsMethodBuilder main() {
        return method("main").pub().static_().returns("void").param("java.lang.String[]", "args");
    }

    public JfflJsClassBuilder staticInit(String source) {
        checkOpen();
        try {
            CtConstructor c = ctClass.getClassInitializer();
            if (c == null) c = ctClass.makeClassInitializer();
            c.insertAfter(JfflJsTypes.block(source));
            return this;
        } catch (Exception e) {
            throw new RuntimeException("无法向" + className + "添加静态初始化器", e);
        }
    }
    
    public JfflJsClassBuilder staticInit(Object body) {
        checkOpen();
        JfflJsBodyBuilder b = new JfflJsBodyBuilder();
        if (body != null) {
            callBody(body, b);
        }
        return staticInit(b.build());
    }
    
    @Deprecated
    public JfflJsClassBuilder staticInit(Function body) {
        return staticInit((Object) body);
    }

    public JfflJsClassBuilder initializer(String source) {
        checkOpen();
        try {
            for (CtConstructor c : ctClass.getDeclaredConstructors()) {
                c.insertBeforeBody(JfflJsTypes.block(source));
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException("无法向" + className + "添加实例初始化器", e);
        }
    }
    
    public JfflJsClassBuilder initializer(Object body) {
        checkOpen();
        JfflJsBodyBuilder b = new JfflJsBodyBuilder();
        if (body != null) {
            callBody(body, b);
        }
        return initializer(b.build());
    }
    
    @Deprecated
    public JfflJsClassBuilder initializer(Function body) {
        return initializer((Object) body);
    }
    
    public JfflJsClassBuilder rawMethod(String source) {
        checkOpen();
        try {
            ctClass.addMethod(CtNewMethod.make(source, ctClass));
            return this;
        } catch (Exception e) {
            throw new RuntimeException("无法向" + className + "添加原始方法: " + source, e);
        }
    }

    public JfflJsClassBuilder rawConstructor(String source) {
        checkOpen();
        try {
            ctClass.addConstructor(CtNewConstructor.make(source, ctClass));
            return this;
        } catch (Exception e) {
            throw new RuntimeException("无法将原始构造函数添加到"+className + ":" + source, e);
        }
    }

    public JfflJsClassBuilder rawField(String source) {
        checkOpen();
        try {
            ctClass.addField(CtField.make(source, ctClass));
            return this;
        } catch (Exception e) {
            throw new RuntimeException("无法向" + className + "添加原始字段: " + source, e);
        }
    }

    public Object define() {
        checkOpen();
        try {
            applyAnnotations();
            byte[] bytes = ctClass.toBytecode();
            closed = true;
            return JfflJsClassLoader.INSTANCE.defineForJS(className, bytes);
        } catch (Exception e) {
            throw new RuntimeException("无法定义JFFLJS类 " + className, e);
        } finally {
            ctClass.detach();
        }
    }

    private void applyAnnotations() {
        ClassFile cf = ctClass.getClassFile();
        JfflJsAnnotationUtil.apply(cf.getConstPool(), cf.getAttributes(), annotations);
    }

    private void callBody(Object body, JfflJsBodyBuilder builder) {
        JfflJsEngine engine = JfflJsEngineHolder.getEngine();
        JfflJsFunction func = engine.wrapFunction(body);
        try {
            func.call(null, builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ClassPool pool() {return pool;}
    String simpleName() {return JfflJsNames.simpleName(className);}
    
    void checkOpen() {
        if (closed) throw new IllegalStateException("类构建器已定义: " + className);
    }
    
    public String toString() {return "JfflJsClassBuilder(" + className + ")";}
}