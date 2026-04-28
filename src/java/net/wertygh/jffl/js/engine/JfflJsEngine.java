package net.wertygh.jffl.js.engine;

import java.nio.file.Path;

public interface JfflJsEngine {
    void initialize() throws Exception;
    void executeScript(Path script) throws Exception;
    void enterContext();
    void exitContext();
    Object getCurrentContext();
    JfflJsFunction wrapFunction(Object nativeCallable);
    Object callFunction(JfflJsFunction function, Object self, Object[] args) throws Exception;
    Object wrapJavaObject(Object javaObject);
    Object wrapJavaClass(Class<?> clazz);
    Object loadJavaClass(String className) throws Exception;
    Object getGlobalBindings();
    String name();
}