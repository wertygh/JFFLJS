package net.wertygh.jffl.js.builder;

import net.wertygh.jffl.js.util.JfflJsAnnotationSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class JfflJsAnnotationBuilder<P> {
    private final P parent;
    private final Consumer<JfflJsAnnotationSpec> sink;
    final JfflJsAnnotationSpec spec;

    JfflJsAnnotationBuilder(P parent, String type, Consumer<JfflJsAnnotationSpec> sink) {
        this.parent = parent; this.sink = sink; this.spec = new JfflJsAnnotationSpec(type);
    }

    public JfflJsAnnotationBuilder<P> visible() {spec.visible = true;return this;}
    public JfflJsAnnotationBuilder<P> invisible() {spec.visible = false;return this;}
    public JfflJsAnnotationBuilder<P> value(String name, Object value) {
        spec.values.put(name, new JfflJsAnnotationSpec.RawValue(value));
        return this;
    }
    public JfflJsAnnotationBuilder<P> value(Object value) {
        return value("value", value);
    }
    public JfflJsAnnotationBuilder<P> string(String name, String value) {
        return value(name, value);
    }
    public JfflJsAnnotationBuilder<P> bool(String name, boolean value) {
        return value(name, value);
    }
    public JfflJsAnnotationBuilder<P> int_(String name, int value) {
        return value(name, value);
    }
    public JfflJsAnnotationBuilder<P> long_(String name, long value) {
        return value(name, value);
    }
    public JfflJsAnnotationBuilder<P> float_(String name, float value) {
        return value(name, value);
    }
    public JfflJsAnnotationBuilder<P> double_(String name, double value) {
        return value(name, value);
    }

    public JfflJsAnnotationBuilder<P> classValue(String name, String className) {
        spec.values.put(name, new JfflJsAnnotationSpec.ClassValue(className)); return this;
    }

    public JfflJsAnnotationBuilder<P> enumValue(String name, String enumClass, String enumName) {
        spec.values.put(name, new JfflJsAnnotationSpec.EnumValue(enumClass, enumName)); return this;
    }

    public JfflJsAnnotationBuilder<P> annotation(String name, JfflJsAnnotationBuilder<?> nested) {
        spec.values.put(name, new JfflJsAnnotationSpec.AnnotationValue(nested.spec));
        return this;
    }

    public JfflJsAnnotationBuilder<P> array(String name, Object... values) {
        List<JfflJsAnnotationSpec.Value> list = new ArrayList<>();
        if (values != null) for (Object v : values) {
            if (v instanceof JfflJsAnnotationBuilder<?> b) list.add(
                new JfflJsAnnotationSpec.AnnotationValue(b.spec)
            );
            else list.add(new JfflJsAnnotationSpec.RawValue(v));
        }
        spec.values.put(name, new JfflJsAnnotationSpec.ArrayValue(
            list.toArray(new JfflJsAnnotationSpec.Value[0])
        ));
        return this;
    }

    public P end() {sink.accept(spec); return parent;}
}

