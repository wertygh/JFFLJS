package net.wertygh.jffl.js.builder;

import dev.latvian.mods.rhino.Function;
import net.wertygh.jffl.js.hook.JfflJsHooks;
import net.wertygh.jffl.js.util.JfflJsFunctionRegistry;

public class JfflJsReplaceBuilder {
    private final JfflJsPatchBuilder parent;
    private final String method, desc;

    JfflJsReplaceBuilder(JfflJsPatchBuilder p, String m, String d) {
        parent = p; method = m; desc = d;
    }

    public JfflJsPatchBuilder body(String source) {
        JfflJsHooks.registerOperation(parent.target(), parent.priority(), cls -> {
            if (desc == null) cls.setBody(method, source);
            else cls.setBody(method, desc, source);
        });
        return parent;
    }

    public JfflJsPatchBuilder js(Object fn) {
        String id = JfflJsFunctionRegistry.registerObject(fn, parent.target() + "." + method + " replace");
        JfflJsHooks.registerOperation(parent.target(), parent.priority(), cls -> {
            if (desc == null) cls.setBodyJs(method, id);
            else cls.setBodyJs(method, desc, id);
        });
        return parent;
    }

    @Deprecated
    public JfflJsPatchBuilder js(Function fn) {
        return js((Object) fn);
    }
}