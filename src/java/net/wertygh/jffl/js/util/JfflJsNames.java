package net.wertygh.jffl.js.util;

import net.wertygh.jffl.js.JfflJs;

public class JfflJsNames {
    public static String generatedName(String className) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("类名不能为空");
        }
        String n = className.trim().replace('/', '.');
        return n.startsWith(JfflJs.GENERATED_PREFIX + ".") ? n : JfflJs.GENERATED_PREFIX + "." + n;
    }

    public static String normalizeTarget(String className) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("目标类不能为空");
        }
        return className.trim().replace('/', '.');
    }

    public static boolean matchesTarget(String target, String actual) {
        if (target == null || actual == null) return false;
        return target.replace('/', '.').equals(actual.replace('/', '.'))
                || target.replace('.', '/').equals(actual.replace('.', '/'));
    }

    public static String simpleName(String fqcn) {
        int i = fqcn.lastIndexOf('.');
        return i < 0 ? fqcn : fqcn.substring(i + 1);
    }

    public static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}