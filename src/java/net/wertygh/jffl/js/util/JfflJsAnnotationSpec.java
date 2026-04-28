package net.wertygh.jffl.js.util;

import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JfflJsAnnotationSpec {
    final String type;
    public boolean visible = true;
    public final Map<String, Value> values = new LinkedHashMap<>();

    public JfflJsAnnotationSpec(String type) {this.type = JfflJsTypes.typeName(type);}

    Annotation toAnnotation(ConstPool cp) {
        Annotation a = new Annotation(type, cp);
        for (Map.Entry<String, Value> e : values.entrySet()) a.addMemberValue(e.getKey(), e.getValue().toMemberValue(cp));
        return a;
    }

    public interface Value {MemberValue toMemberValue(ConstPool cp);}

    public record RawValue(Object value) implements Value {
        public MemberValue toMemberValue(ConstPool cp) {
            if (value instanceof Boolean b) return new BooleanMemberValue(b, cp);
            if (value instanceof Byte b) return new ByteMemberValue(b, cp);
            if (value instanceof Character c) return new CharMemberValue(c, cp);
            if (value instanceof Short s) return new ShortMemberValue(s, cp);
            if (value instanceof Integer i) return new IntegerMemberValue(cp, i);
            if (value instanceof Long l) return new LongMemberValue(l, cp);
            if (value instanceof Float f) return new FloatMemberValue(f, cp);
            if (value instanceof Double d) return new DoubleMemberValue(d, cp);
            return new StringMemberValue(String.valueOf(value), cp);
        }
    }

    public record ClassValue(String className) implements Value {
        public MemberValue toMemberValue(ConstPool cp) {return new ClassMemberValue(className, cp);}
    }

    public record EnumValue(String enumClass, String enumName) implements Value {
        public MemberValue toMemberValue(ConstPool cp) {
            EnumMemberValue e = new EnumMemberValue(cp);
            e.setType(enumClass); e.setValue(enumName); return e;
        }
    }

    public record AnnotationValue(JfflJsAnnotationSpec spec) implements Value {
        public MemberValue toMemberValue(ConstPool cp) {return new AnnotationMemberValue(spec.toAnnotation(cp), cp);}
    }

    public record ArrayValue(Value[] values) implements Value {
        public MemberValue toMemberValue(ConstPool cp) {
            ArrayMemberValue a = new ArrayMemberValue(cp);
            MemberValue[] mv = new MemberValue[values.length];
            for (int i = 0; i < values.length; i++) mv[i] = values[i].toMemberValue(cp);
            a.setValue(mv);
            return a;
        }
    }
}
