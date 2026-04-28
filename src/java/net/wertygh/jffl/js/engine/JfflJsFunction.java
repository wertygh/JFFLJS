package net.wertygh.jffl.js.engine;

@FunctionalInterface
public interface JfflJsFunction {
    Object call(Object self, Object... args) throws Exception;
}