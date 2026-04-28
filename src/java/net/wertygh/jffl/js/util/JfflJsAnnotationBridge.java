package net.wertygh.jffl.js.util;

import net.wertygh.jffl.api.annotation.At;
import net.wertygh.jffl.api.annotation.Slice;
import net.wertygh.jffl.js.model.JfflJsAt;
import net.wertygh.jffl.js.model.JfflJsSlice;

import java.lang.reflect.Proxy;

public class JfflJsAnnotationBridge {
    public static At at(JfflJsAt spec) {
        JfflJsAt s = spec == null ? new JfflJsAt("HEAD") : spec;
        return (At) Proxy.newProxyInstance(At.class.getClassLoader(), new Class<?>[]{At.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "value" -> At.Value.valueOf(s.value);
                    case "target" -> s.target;
                    case "line" -> s.line;
                    case "stringValue" -> s.stringValue;
                    case "intValue" -> s.intValue;
                    case "doubleValue" -> s.doubleValue;
                    case "ordinal" -> s.ordinal;
                    case "shift" -> At.Shift.valueOf(s.shift);
                    case "annotationType" -> At.class;
                    case "toString" -> "@JfflJsAt(" + s.value + ")";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultFor(method.getReturnType());
                });
    }

    public static Slice slice(JfflJsSlice spec) {
        JfflJsSlice s = spec == null ? new JfflJsSlice() : spec;
        return (Slice) Proxy.newProxyInstance(Slice.class.getClassLoader(), new Class<?>[]{Slice.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "from" -> at(s.from);
                    case "to" -> at(s.to);
                    case "id" -> s.id;
                    case "annotationType" -> Slice.class;
                    case "toString" -> "@JfflJsSlice(" + s.id + ")";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultFor(method.getReturnType());
                });
    }

    private static Object defaultFor(Class<?> rt) {
        if (rt == boolean.class) return false;
        if (rt == byte.class) return (byte)0;
        if (rt == short.class) return (short)0;
        if (rt == int.class) return 0;
        if (rt == long.class) return 0L;
        if (rt == float.class) return 0f;
        if (rt == double.class) return 0d;
        if (rt == char.class) return (char)0;
        return null;
    }
          }
