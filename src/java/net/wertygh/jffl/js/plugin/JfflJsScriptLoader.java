package net.wertygh.jffl.js.plugin;

import net.wertygh.jffl.js.JfflJs;
import net.wertygh.jffl.js.engine.JfflJsEngine;
import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class JfflJsScriptLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(JfflJsScriptLoader.class);
    public static final Path SCRIPT_DIR = Path.of("kubejs", "jffljs_script");
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    public static void loadAll() {
        if (!LOADED.compareAndSet(false, true)) return;
        try {
            Files.createDirectories(SCRIPT_DIR);
        } catch (IOException e) {
            LOGGER.error("无法创建 {}", SCRIPT_DIR.toAbsolutePath(), e);
            return;
        }

        List<Path> scripts;
        try (Stream<Path> s = Files.walk(SCRIPT_DIR)) {
            scripts = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".js"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            LOGGER.error("无法列出 JFFLJS 脚本", e);
            return;
        }

        if (scripts.isEmpty()) {
            LOGGER.info("{} 中未找到 .js 文件", SCRIPT_DIR.toAbsolutePath());
            return;
        }

        JfflJsEngine engine = JfflJsEngineHolder.getEngine();
        for (Path script : scripts) {
            try {
                engine.executeScript(script);
            } catch (Exception e) {
                LOGGER.error("脚本执行失败: {}", script, e);
            }
        }
        LOGGER.info("JFFLJS: 已加载 {} 个脚本, {} 个类, {} 个钩子",
                scripts.size(), JfflJs.classCount(), JfflJs.hookCount());
    }
}