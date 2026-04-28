package net.wertygh.jffl.js.plugin;

import net.wertygh.jffl.api.ITransformerHook;
import net.wertygh.jffl.api.ITransformerPlugin;
import net.wertygh.jffl.js.hook.JfflJsHooks;

import java.util.Set;

public class JfflJsPlugin implements ITransformerPlugin {
    @Override
    public Set<String> targetClasses() {
        JfflJsScriptLoader.loadAll();
        return JfflJsHooks.targetClasses();
    }

    @Override
    public byte[] toByte(ITransformerHook hook) throws Exception {
        JfflJsScriptLoader.loadAll();
        return JfflJsHooks.apply(hook);
    }
}