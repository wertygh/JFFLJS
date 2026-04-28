package net.wertygh.jffl.js.hook;

import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.expr.*;
import net.wertygh.jffl.engine.InjectionPoints;
import net.wertygh.jffl.js.JfflJs;
import net.wertygh.jffl.js.model.JfflJsAt;
import net.wertygh.jffl.js.model.JfflJsSlice;
import net.wertygh.jffl.js.util.JfflJsAnnotationBridge;
import net.wertygh.jffl.js.util.JfflJsTypes;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

public class JfflJsTransformClass {
    private final ClassPool pool;
    private final CtClass ctClass;

    JfflJsTransformClass(ClassPool p, CtClass c) {pool = p; ctClass = c;}

    public String name() {return ctClass.getName();}
    public CtClass raw() {return ctClass;}
    public ClassPool pool() {return pool;}

    public JfflJsTransformClass dump(String dir) throws Exception {
        byte[] bytes = ctClass.toBytecode();
        if (ctClass.isFrozen()) ctClass.defrost();
        Path root = Path.of(dir == null || dir.isBlank() ? ".jffl-dump" : dir);
        File out = root.resolve(ctClass.getName().replace('.', '/') + ".class").toFile();
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(out)) {fos.write(bytes);}
        return this;
    }

    public JfflJsTransformClass addField(String src) throws CannotCompileException {
        ctClass.addField(CtField.make(src, ctClass));
        return this;
    }
    
    public JfflJsTransformClass addField(String name, String type, String initializer, int access) throws Exception {
        if (hasField(name)) return this;
        CtField f = new CtField(JfflJsTypes.ct(pool, type), name, ctClass);
        f.setModifiers(access == 0 ? Modifier.PRIVATE : access);
        if (initializer == null || initializer.isBlank()) ctClass.addField(f);
        else ctClass.addField(f, CtField.Initializer.byExpr(initializer));
        return this;
    }
    
    public JfflJsTransformClass addMethod(String src) throws CannotCompileException {
        ctClass.addMethod(CtNewMethod.make(src, ctClass)); return this;
    }
    
    public JfflJsTransformClass addConstructor(String src) throws CannotCompileException{ 
        ctClass.addConstructor(CtNewConstructor.make(src, ctClass)); return this;
    }
    
    public JfflJsTransformClass setSuperclass(Object t) throws Exception {
        ctClass.setSuperclass(JfflJsTypes.ct(pool, t)); return this;
    }
    
    public JfflJsTransformClass addInterface(Object t) throws Exception {
        CtClass iface = JfflJsTypes.ct(pool, t);
        for (CtClass old : ctClass.getInterfaces()) if (old.getName().equals(iface.getName())) return this;
        ctClass.addInterface(iface);
        return this;
    }

    public boolean hasMethod(String n) {
        for(CtMethod m:ctClass.getDeclaredMethods())if(m.getName().equals(n))return true;
        return false;
    }
    public boolean hasField(String n) {
        for(CtField f:ctClass.getDeclaredFields())if(f.getName().equals(n))return true; 
        return false;
    }

    public JfflJsTransformClass accessor(String target, boolean invoker, String desc, boolean optional) throws Exception {
        try {
            if (invoker) {
                CtMethod priv = method(target, desc);
                String bridgeName = "jffl$invoke$" + target;
                if (hasMethod(bridgeName)) return this;
                boolean isStatic = Modifier.isStatic(priv.getModifiers());
                StringBuilder sb = new StringBuilder("public ");
                if (isStatic) sb.append("static ");
                sb.append(priv.getReturnType().getName())
                  .append(' ').append(bridgeName).append('(');
                CtClass[] params = priv.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(params[i].getName()).append(" p").append(i);
                }
                sb.append(") {");
                if (priv.getReturnType() != CtClass.voidType) sb.append("return ");
                if (isStatic) sb.append(ctClass.getName()).append('.');
                sb.append(target).append('(');
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("p").append(i);
                }
                sb.append(");}");
                addMethod(sb.toString());
            } else {
                CtField f = ctClass.getDeclaredField(target);
                String getName = "jffl$get$" + target;
                String setName = "jffl$set$" + target;
                if (!hasMethod(getName)) addMethod(
                    "public " + f.getType().getName() + " " + getName
                    + "() {return this." + target + ";}"
                );
                if (!hasMethod(setName)) addMethod(
                    "public void " + setName + "("
                    + f.getType().getName() + " v) {this." + target + " = v;}"
                );
            }
        } catch (NotFoundException e) {
            if (!optional) throw e;
        }
        return this;
    }

    public JfflJsTransformClass insertBefore(String m, String src) throws Exception {
        return insertBefore(m, "", src);
    }
    public JfflJsTransformClass insertBefore(String m, String d, String src) throws Exception {
        method(m, d).insertBefore(block(src)); return this;
    }
    public JfflJsTransformClass insertAfter(String m, String src) throws Exception {
        return insertAfter(m, "", src);
    }
    public JfflJsTransformClass insertAfter(String m, String d, String src) throws Exception {
        method(m, d).insertAfter(block(src)); return this;
    }
    public JfflJsTransformClass setBody(String m, String src) throws Exception {
        return setBody(m, "", src);
    }
    public JfflJsTransformClass setBody(String m, String d, String src) throws Exception {
        method(m, d).setBody(block(src)); return this;
    }

    public JfflJsTransformClass inject(String m, String d, JfflJsAt at, JfflJsSlice slice, String src) throws Exception {
        InjectionPoints.apply(method(m, d), JfflJsAnnotationBridge.at(at), block(src), JfflJsAnnotationBridge.slice(slice));
        return this;
    }

    public JfflJsTransformClass redirect(String m, String d, JfflJsAt at, JfflJsSlice slice, String src) throws Exception {
        InjectionPoints.applyRedirect(method(m, d), JfflJsAnnotationBridge.at(at), block(src), JfflJsAnnotationBridge.slice(slice));
        return this;
    }

    public JfflJsTransformClass wrapOperation(String m, String d, JfflJsAt at, JfflJsSlice slice, String src) throws Exception {
        return redirect(m, d, at, slice, src);
    }

    public JfflJsTransformClass modifyReturnValue(String m, String d, String src) throws Exception {
        method(m, d).insertAfter(block(src), false);
        return this;
    }

    public JfflJsTransformClass insertAtLine(String m, String d, int line, String src) throws Exception {
        method(m, d).insertAt(line, block(src));
        return this;
    }
    
    public JfflJsTransformClass wrapTryCatch(String m, String d, String exceptionType, String src) throws Exception {
        method(m, d).addCatch(block(src), pool.get(exceptionType == null || exceptionType.isBlank() ? "java.lang.Throwable" : exceptionType));
        return this;
    }
    
    public JfflJsTransformClass insertBeforeJs(String m, String functionId) throws Exception {
        return insertBeforeJs(m, "", functionId);
    }
    
    public JfflJsTransformClass insertBeforeJs(String m, String d, String functionId) throws Exception {
        String callExpr = buildJsCallExpr(functionId);
        return insertBefore(m, d, callExpr);
    }
    
    public JfflJsTransformClass insertAfterJs(String m, String functionId) throws Exception {
        return insertAfterJs(m, "", functionId);
    }
    
    public JfflJsTransformClass insertAfterJs(String m, String d, String functionId) throws Exception {
        String callExpr = buildJsCallExpr(functionId);
        return insertAfter(m, d, callExpr);
    }
    
    public JfflJsTransformClass insertFinallyJs(String m, String functionId) throws Exception {
        return insertFinallyJs(m, "", functionId);
    }
    
    public JfflJsTransformClass insertFinallyJs(String m, String d, String functionId) throws Exception {
        String callExpr = buildJsCallExpr(functionId);
        CtMethod target = method(m, d);
        target.insertAfter(block(callExpr), true);
        return this;
    }
    
    public JfflJsTransformClass setBodyJs(String m, String functionId) throws Exception {
        return setBodyJs(m, "", functionId);
    }
    
    public JfflJsTransformClass setBodyJs(String m, String d, String functionId) throws Exception {
        CtMethod target = method(m, d);
        boolean isStatic = Modifier.isStatic(target.getModifiers());
        String selfExpr = isStatic ? "null" : "$0";
        StringBuilder sb = new StringBuilder("{");
        CtClass[] paramTypes = target.getParameterTypes();
        if (paramTypes.length > 0) {
            sb.append("Object[] jffl_args = new Object[").append(paramTypes.length).append("];");
            for (int i = 0; i < paramTypes.length; i++) {
                sb.append("jffl_args[").append(i).append("] = ($w)$").append(i + 1).append(";");
            }
        } else {
            sb.append("Object[] jffl_args = new Object[0];");
        }
        sb.append("return ($w) net.wertygh.jffl.js.JfflJsFunctionRegistry.call(");
        sb.append('"').append(functionId).append('"');
        sb.append(", ").append(selfExpr).append(", jffl_args);");
        sb.append("}");
        target.setBody(sb.toString());
        return this;
    }
    
    private String buildJsCallExpr(String functionId) {
        return "net.wertygh.jffl.js.JfflJsFunctionRegistry.call(\"" + functionId + "\", $0, $args);";
    }
    
    public JfflJsTransformClass addConstructorCode(String desc, String position, String src) throws Exception {
        CtConstructor[] ctors = (desc == null || desc.isBlank()) ? ctClass.getDeclaredConstructors() : new CtConstructor[]{ ctClass.getConstructor(desc) };
        String p = position == null ? "AFTER" : position.trim().toUpperCase();
        for (CtConstructor ctor : ctors) {
            if ("BEFORE".equals(p) || "BEFORE_SUPER".equals(p)) ctor.insertBeforeBody(block(src));
            else ctor.insertAfter(block(src));
        }
        return this;
    }

    public JfflJsTransformClass injectStaticInit(String position, String src) throws Exception {
        CtConstructor clinit = ctClass.getClassInitializer();
        if (clinit == null) clinit = ctClass.makeClassInitializer();
        if ("BEFORE".equalsIgnoreCase(position)) clinit.insertBefore(block(src));
        else clinit.insertAfter(block(src));
        return this;
    }

    public JfflJsTransformClass cloneMethod(String m, String d, String cloneName) throws Exception {
        CtMethod target = method(m, d);
        String finalName = cloneName == null || cloneName.isBlank() ? "jffljs$original$" + m : cloneName;
        if (!hasMethod(finalName)) {
            CtMethod clone = CtNewMethod.copy(target, finalName, ctClass, null);
            clone.setModifiers(clone.getModifiers() | AccessFlag.SYNTHETIC);
            ctClass.addMethod(clone);
        }
        return this;
    }

    public JfflJsTransformClass wrapMethod(String m, String d, String src) throws Exception {
        cloneMethod(m, d, "jffljs$original$" + m);
        method(m, d).setBody(block(src));
        return this;
    }
    
    public JfflJsTransformClass insertBeforeInvoke(String m, String target, int ordinal, String src) throws Exception {
        return insertBeforeInvoke(m, "", target, ordinal, src);
    }
    
    public JfflJsTransformClass insertBeforeInvoke(String m, String d, String target, int ordinal, String src) throws Exception {
        CtMethod ctMethod = method(m, d);
        int[] seen = {0};
        ctMethod.instrument(new ExprEditor() {
            @Override public void edit(MethodCall mc) throws CannotCompileException {
                if (!matchesInvoke(mc, target)) return;
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                mc.replace("{" + src + " $_ = $proceed($$);}");
            }
        });
        return this;
    }
    
    public JfflJsTransformClass insertAfterInvoke(String m, String target, int ordinal, String src) throws Exception {
        return insertAfterInvoke(m, "", target, ordinal, src);
    }
    
    public JfflJsTransformClass insertAfterInvoke(String m, String d, String target, int ordinal, String src) throws Exception {
        CtMethod ctMethod = method(m, d);
        int[] seen = {0};
        ctMethod.instrument(new ExprEditor() {
            @Override public void edit(MethodCall mc) throws CannotCompileException {
                if (!matchesInvoke(mc, target)) return;
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                mc.replace("{$_ = $proceed($$);" + src + "}");
            }
        });
        return this;
    }
    
    public JfflJsTransformClass modifyArg(String m, String d, String target, int index, int ordinal, String expr) throws Exception {
        CtMethod method = method(m, d);
        final int[] seen = {0};
        method.instrument(new ExprEditor() {
            @Override public void edit(MethodCall call) throws CannotCompileException {
                if (!matchesInvoke(call, target)) return;
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                try {
                    int count = call.getMethod().getParameterTypes().length;
                    if (index < 0 || index >= count) {
                        throw new CannotCompileException("ModifyArg索引超出范围: " + index);
                    }
                    StringBuilder args = new StringBuilder();
                    for (int i = 0; i < count; i++) {
                        if (i > 0) args.append(", ");
                        args.append(i == index ? "(" + expr + ")" : "$" + (i + 1));
                    }
                    String repl = call.getMethod().getReturnType() == CtClass.voidType 
                            ? "{$proceed(" + args + ");}" 
                            : "{$_ = $proceed(" + args + ");}";
                    call.replace(repl);
                } catch (NotFoundException e) {
                    throw new CannotCompileException(e);
                }
            }
        });
        return this;
    }

    public JfflJsTransformClass modifyVariable(String m, String d, String name, int index, int line, JfflJsAt at, String expr) throws Exception {
        String assignment = (name != null && !name.isBlank()) ? name + " = (" + expr + ");" : expr + ";";
        if (line > 0) return insertAtLine(m, d, line, assignment);
        return inject(m, d, at == null ? JfflJs.head() : at, null, assignment);
    }

    public JfflJsTransformClass instrumentNewExpr(String m, String d, String target, int ordinal, String src) throws Exception {
        CtMethod cm = method(m, d); final int[] seen = {0};
        cm.instrument(new ExprEditor() {
            @Override public void edit(NewExpr e) throws CannotCompileException {
                if (target != null && !target.isBlank() && !target.equals(e.getClassName())) return;
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                e.replace(block(src));
            }
        });
        return this;
    }

    public JfflJsTransformClass instrumentCast(String m, String d, String target, int ordinal, String src) throws Exception {
        CtMethod cm = method(m, d); final int[] seen = {0};
        cm.instrument(new ExprEditor() {
            @Override public void edit(Cast c) throws CannotCompileException {
                try { if (target != null && !target.isBlank() && !target.equals(c.getType().getName())) return; }
                catch (NotFoundException ignored) { return; }
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                c.replace(block(src));
            }
        });
        return this;
    }

    public JfflJsTransformClass instrumentInstanceof(String m, String d, String target, int ordinal, String src) throws Exception {
        CtMethod cm = method(m, d); final int[] seen = {0};
        cm.instrument(new ExprEditor() {
            @Override public void edit(Instanceof i) throws CannotCompileException {
                try { if (target != null && !target.isBlank() && !target.equals(i.getType().getName())) return; }
                catch (NotFoundException ignored) { return; }
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                i.replace(block(src));
            }
        });
        return this;
    }

    public JfflJsTransformClass instrumentHandler(String m, String d, String exceptionType, int ordinal, String src) throws Exception {
        CtMethod cm = method(m, d); final int[] seen = {0};
        cm.instrument(new ExprEditor() {
            @Override public void edit(Handler h) throws CannotCompileException {
                try {
                    if (exceptionType != null && !exceptionType.isBlank()) {
                        CtClass caught = h.getType();
                        if (caught == null || !caught.getName().equals(exceptionType)) return;
                    }
                } catch (NotFoundException ignored) { return; }
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                h.insertBefore(block(src));
            }
        });
        return this;
    }

    public JfflJsTransformClass instrumentFieldAccess(String m, String d, String target, String accessType, int ordinal, String src) throws Exception {
        CtMethod cm = method(m, d); final int[] seen = {0};
        String mode = accessType == null ? "BOTH" : accessType.trim().toUpperCase();
        cm.instrument(new ExprEditor() {
            @Override public void edit(FieldAccess fa) throws CannotCompileException {
                if ("READ".equals(mode) && fa.isWriter()) return;
                if ("WRITE".equals(mode) && fa.isReader()) return;
                if (target != null && !target.isBlank()) {
                    String full = fa.getClassName() + "." + fa.getFieldName();
                    if (!target.equals(fa.getFieldName()) && !target.equals(full)) return;
                }
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                fa.replace(block(src));
            }
        });
        return this;
    }

    public JfflJsTransformClass instrumentConstructorCall(String m, String d, String target, int ordinal, String src) throws Exception {
        CtMethod cm = method(m, d); final int[] seen = {0};
        cm.instrument(new ExprEditor() {
            @Override public void edit(ConstructorCall c) throws CannotCompileException {
                if (target != null && !target.isBlank() && !target.equals(c.getClassName())) return;
                int idx = seen[0]++;
                if (ordinal >= 0 && idx != ordinal) return;
                c.replace(block(src));
            }
        });
        return this;
    }

    public CtMethod method(String name) throws NotFoundException {return ctClass.getDeclaredMethod(name);}
    public CtMethod method(String name, String desc) throws NotFoundException {
        if (desc == null || desc.isBlank()) return method(name);
        for (CtMethod m : ctClass.getDeclaredMethods(name)) {
            if (desc.equals(m.getSignature()) || desc.equals(m.getMethodInfo().getDescriptor())) return m;
        }
        throw new NotFoundException("找不到到方法: " + ctClass.getName() + "." + name + desc);
    }

    private static String block(String s) {return JfflJsTypes.block(s);}
    private static boolean matchesInvoke(MethodCall call, String target) {
        if (target == null || target.isBlank()) return true;
        String full = call.getClassName() + "." + call.getMethodName();
        return target.equals(call.getMethodName()) || target.equals(full) || target.equals(full.replace('.', '/'));
    }
    
    public JfflJsTransformClass insertFinally(String m, String src) throws Exception {
        return insertFinally(m, "", src);
    }
    
    public JfflJsTransformClass insertFinally(String m, String d, String src) throws Exception {
        method(m, d).insertAfter(block(src), true);
        return this;
    }
}
