package net.wertygh.jffl.js;

import net.wertygh.jffl.js.builder.*;
import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import net.wertygh.jffl.js.model.JfflJsAt;
import net.wertygh.jffl.js.model.JfflJsSlice;
import net.wertygh.jffl.js.plugin.JfflJsClassLoader;
import net.wertygh.jffl.js.plugin.JfflJsScriptLoader;
import net.wertygh.jffl.js.util.JfflJsNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JfflJs {
    public static final String GENERATED_PREFIX = "generated.jffljs.custom";
    public static final Logger LOGGER = LoggerFactory.getLogger("JFFLJS");

    public static JfflJsClassBuilder clazz(String className) {
        try {
            return JfflJsClassBuilder.create(className, false);
        } catch (Exception e) {
            throw new RuntimeException("未能创建类生成器: " + className, e);
        }
    }

    public static JfflJsClassBuilder rawClazz(String className) {
        try {
            return JfflJsClassBuilder.create(className, true);
        } catch (Exception e) {
            throw new RuntimeException("未能创建原始类生成器: " + className, e);
        }
    }

    public static JfflJsPatchBuilder patch(String targetClass) {
        return new JfflJsPatchBuilder(targetClass);
    }

    public static JfflJsTransformBuilder transform(String targetClass) {
        return new JfflJsTransformBuilder(targetClass);
    }

    public static String name(String className) {
        return JfflJsNames.generatedName(className);
    }

    public static Object load(String className) {
        return JfflJsClassLoader.INSTANCE.getForJS(JfflJsNames.generatedName(className));
    }

    public static JfflJsBodyBuilder body() {
        return new JfflJsBodyBuilder();
    }

    public static JfflJsAt at(String value) {
        return new JfflJsAt(value);
    }

    public static JfflJsAt head() {return at("HEAD");}
    public static JfflJsAt ret() {return at("RETURN");}
    public static JfflJsAt tail() {return at("TAIL");}
    public static JfflJsAt invoke(String target) {return at("INVOKE").target(target);}
    public static JfflJsAt fieldAt(String target) {return at("FIELD").target(target);}
    public static JfflJsAt line(int line) {return at("LINE").line(line);}
    public static JfflJsAt constant() {return at("CONSTANT");}
    public static JfflJsSlice slice() {return new JfflJsSlice();}
    public static void log(Object msg) {LOGGER.info(String.valueOf(msg));}
    public static void warn(Object msg) {LOGGER.warn(String.valueOf(msg));}
    public static void error(Object msg) {LOGGER.error(String.valueOf(msg));}
    public static int hookCount() {return net.wertygh.jffl.js.hook.JfflJsHooks.hookCount();}
    public static int classCount() {return JfflJsClassLoader.INSTANCE.definedCount();}
}