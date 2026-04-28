package net.wertygh.jffl.js.builder;

import javassist.Modifier;

public class JfflJsPropertyBuilder {
    private final JfflJsClassBuilder parent;
    private final String name;
    private final String type;
    private int fieldAccess = Modifier.PRIVATE;
    private boolean getter = true;
    private boolean setter = true;
    private String initializer;

    JfflJsPropertyBuilder(JfflJsClassBuilder parent, String name, String type) {
        this.parent = parent; this.name = name; this.type = type;
    }

    public JfflJsPropertyBuilder fieldAccess(int access) {this.fieldAccess = access; return this;}
    public JfflJsPropertyBuilder init(String expr) {this.initializer = expr; return this;}
    public JfflJsPropertyBuilder readonly() {this.setter = false; return this;}
    public JfflJsPropertyBuilder writeonly() {this.getter = false; return this;}
    public JfflJsPropertyBuilder noGetter() {this.getter = false; return this;}
    public JfflJsPropertyBuilder noSetter() {this.setter = false; return this;}

    public JfflJsClassBuilder end() {
        parent.field(name, type).modifiers(fieldAccess).init(initializer).end();
        String c = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        if (getter) parent.rawMethod(
            "public " + type + " get" + c + "() {return this." + name + ";}"
        );
        if (setter) parent.rawMethod(
            "public void set" + c + "(" + type + " value) {this." + name + " = value;}"
        );
        return parent;
    }
}
