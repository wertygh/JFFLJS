package net.wertygh.jffl.js.builder;

import javassist.CtField;
import javassist.Modifier;
import net.wertygh.jffl.js.util.JfflJsAnnotationSpec;
import net.wertygh.jffl.js.util.JfflJsAnnotationUtil;
import net.wertygh.jffl.js.util.JfflJsTypes;

import java.util.ArrayList;
import java.util.List;

public class JfflJsFieldBuilder {
    private final JfflJsClassBuilder parent;
    private final String name;
    private final String type;
    private final List<JfflJsAnnotationSpec> annotations = new ArrayList<>();
    private int modifiers = Modifier.PRIVATE;
    private String initializer;
    private String signature;
    private boolean built = false;

    JfflJsFieldBuilder(JfflJsClassBuilder parent, String name, String type) {
        this.parent = parent; this.name = name; this.type = type;
    }

    public JfflJsFieldBuilder modifiers(int modifiers) {
        this.modifiers = modifiers; return this;
    }
    public JfflJsFieldBuilder pub() {
        modifiers = (modifiers & ~(Modifier.PRIVATE|Modifier.PROTECTED))|Modifier.PUBLIC;
        return this;
    }
    public JfflJsFieldBuilder priv() {
        modifiers = (modifiers & ~(Modifier.PUBLIC|Modifier.PROTECTED))|Modifier.PRIVATE;
        return this;
    }
    public JfflJsFieldBuilder prot() {
        modifiers = (modifiers & ~(Modifier.PUBLIC|Modifier.PRIVATE))|Modifier.PROTECTED;
        return this;
    }
    public JfflJsFieldBuilder packagePrivate() {
        modifiers &= ~(Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED);
        return this;
    }
    public JfflJsFieldBuilder init(String expression) {
        this.initializer = expression;
        return this;
    }
    public JfflJsFieldBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }
    public JfflJsFieldBuilder static_() {modifiers |= Modifier.STATIC;return this;}
    public JfflJsFieldBuilder final_() {modifiers |= Modifier.FINAL;return this;}
    public JfflJsFieldBuilder volatile_() {modifiers |= Modifier.VOLATILE;return this;}
    public JfflJsFieldBuilder transient_() {modifiers |= Modifier.TRANSIENT;return this;}


    public JfflJsAnnotationBuilder<JfflJsFieldBuilder> annotate(String annotationType) {
        return new JfflJsAnnotationBuilder<>(this, annotationType, annotations::add);
    }

    public JfflJsClassBuilder getter() {
        end();
        return parent.rawMethod(
            "public " + type + " get" + cap(name) + "() {return this." + name + ";}"
        ); 
    }
    
    public JfflJsClassBuilder setter() {
        end();
        return parent.rawMethod(
            "public void set" + cap(name) +"(" + type + " value) {this."+name+" = value;}"
        );
    }
    
    public JfflJsClassBuilder getterSetter() {
        end();
        parent.rawMethod(
            "public " + type + " get" + cap(name) + "() {return this." + name + ";}"
        );
        parent.rawMethod(
            "public void set" + cap(name) +"(" + type + " value) {this."+name+" = value;}"
        );
        return parent;
    }

    public JfflJsClassBuilder end() {
        if (built) return parent;
        parent.checkOpen();
        try {
            CtField f = new CtField(JfflJsTypes.ct(parent.pool(), type), name, parent.ctClass);
            f.setModifiers(modifiers);
            if (signature != null && !signature.isBlank()) f.setGenericSignature(signature);
            if (initializer == null || initializer.isBlank()) parent.ctClass.addField(f);
            else parent.ctClass.addField(f, CtField.Initializer.byExpr(initializer));
            JfflJsAnnotationUtil.apply(
                parent.ctClass.getClassFile().getConstPool(),
                f.getFieldInfo().getAttributes(), annotations
            );
            built = true;
            return parent;
        } catch (Exception e) {
            throw new RuntimeException("无法添加字段"+name, e);
        }
    }

    private static String cap(String s) {
        return s == null||s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1);
    }
}
