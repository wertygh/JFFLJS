package net.wertygh.jffl.js.hook;

@FunctionalInterface
public interface JfflJsCondition {
    boolean test(JfflJsTransformClass cls) throws Exception;
}
