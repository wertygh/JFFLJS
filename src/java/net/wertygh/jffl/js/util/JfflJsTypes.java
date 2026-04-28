package net.wertygh.jffl.js.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import net.wertygh.jffl.js.JfflJs;
import net.wertygh.jffl.js.plugin.JfflJsClassLoader;

import java.lang.reflect.Array;

public class JfflJsTypes {
    public static ClassPool newClassPool() {
        ClassPool pool = new ClassPool(true);
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        if (ctx != null) pool.appendClassPath(new LoaderClassPath(ctx));
        pool.appendClassPath(new LoaderClassPath(JfflJs.class.getClassLoader()));
        pool.appendClassPath(new LoaderClassPath(JfflJsClassLoader.INSTANCE));
        return pool;
    }

    public static String typeName(Object value) {
        if (value == null) throw new IllegalArgumentException("类型不能为空");
        if (value instanceof Class<?> c) return c.getName();
        if (value instanceof CtClass c) return c.getName();
        String s = String.valueOf(value).trim();
        if (s.startsWith("class ")) s = s.substring("class ".length()).trim();
        return s;
    }

    public static CtClass ct(ClassPool pool, Object value) throws NotFoundException {
        return ct(pool, typeName(value));
    }

    public static CtClass ct(ClassPool pool, String type) throws NotFoundException {
        return switch (type) {
            case "void" -> CtClass.voidType;
            case "boolean" -> CtClass.booleanType;
            case "byte" -> CtClass.byteType;
            case "char" -> CtClass.charType;
            case "short" -> CtClass.shortType;
            case "int" -> CtClass.intType;
            case "long" -> CtClass.longType;
            case "float" -> CtClass.floatType;
            case "double" -> CtClass.doubleType;
            default -> pool.get(type);
        };
    }

    public static Class<?> javaClass(String type) {
        try {
            return switch (type) {
                case "void" -> void.class;
                case "boolean" -> boolean.class;
                case "byte" -> byte.class;
                case "char" -> char.class;
                case "short" -> short.class;
                case "int" -> int.class;
                case "long" -> long.class;
                case "float" -> float.class;
                case "double" -> double.class;
                default -> {
                    if (type.endsWith("[]")) yield Array.newInstance(
                            javaClass(type.substring(0, type.length() - 2)), 0
                    ).getClass();
                    ClassLoader ctx = Thread.currentThread().getContextClassLoader();
                    if (ctx != null) {
                        try {
                            yield Class.forName(type, false, ctx);
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                    try {
                        yield Class.forName(type, false, JfflJsClassLoader.INSTANCE);
                    } catch (ClassNotFoundException ignored) {
                        yield Class.forName(type, false, JfflJs.class.getClassLoader());
                    }
                }
            };
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("无法解析 Java 类型: " + type, e);
        }
    }

    public static String defaultReturnBody(String rt) {
        return switch (rt) {
            case "void" -> "{}";
            case "boolean" -> "{return false;}";
            case "byte", "char", "short", "int" -> "{return 0;}";
            case "long" -> "{return 0L;}";
            case "float" -> "{return 0.0F;}";
            case "double" -> "{return 0.0D;}";
            default -> "{return null;}";
        };
    }

    public static String returnFromObject(String rt, String expr) {
        return switch (rt) {
            case "void" -> expr + "; return;";
            case "boolean" -> "return ((java.lang.Boolean)" + expr + ").booleanValue();";
            case "byte" -> "return ((java.lang.Number)" + expr + ").byteValue();";
            case "short" -> "return ((java.lang.Number)" + expr + ").shortValue();";
            case "int" -> "return ((java.lang.Number)" + expr + ").intValue();";
            case "long" -> "return ((java.lang.Number)" + expr + ").longValue();";
            case "float" -> "return ((java.lang.Number)" + expr + ").floatValue();";
            case "double" -> "return ((java.lang.Number)" + expr + ").doubleValue();";
            case "char" -> "return ((java.lang.Character)" + expr + ").charValue();";
            default -> "return (" + rt + ")" + expr + ";";
        };
    }

    public static String block(String src) {
        if (src == null || src.isBlank()) return "{}";
        String s = src.trim();
        return s.startsWith("{") ? s : "{" + s + "}";
    }
}