package net.wertygh.jffl.js.engine;

import org.slf4j.*;

public class JfflJsEngineHolder {
    private static final Logger LOG = LoggerFactory.getLogger(JfflJsEngineHolder.class);
    private static final JfflJsEngine ENGINE;
    static {
        ENGINE = detect();
        try {
            ENGINE.initialize();
        } catch (Exception e) {
            throw new RuntimeException("无法初始化JavaScript引擎", e);
        }
    }

    private static JfflJsEngine detect() {
        try {
            Class.forName("graal.graalvm.polyglot.Context");
            LOG.info("使用GraalJS");
            return new GraalJsEngine();
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("dev.latvian.mods.rhino.Context");
            LOG.info("使用Rhino引擎, 你最好还是去用GraalJS");
            return new RhinoJsEngine();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("缺少类, 请安装Graal或Rhino模组");
        }
    }

    public static JfflJsEngine getEngine() {
        return ENGINE;
    }
}