package net.wertygh.jffl.js.builder;

import dev.latvian.mods.rhino.Function;
import net.wertygh.jffl.js.hook.JfflJsHooks;
import net.wertygh.jffl.js.util.JfflJsFunctionRegistry;
import net.wertygh.jffl.js.util.JfflJsNames;

public class JfflJsInvokeBuilder {
    private final JfflJsPatchBuilder parent;
    private final String method, desc, target;
    private int ordinal = -1;

    JfflJsInvokeBuilder(JfflJsPatchBuilder p, String m, String d, String t) {
        parent = p; method = m; desc = d; target = t;
    }

    public JfflJsInvokeBuilder ordinal(int o) {ordinal = o; return this;}

    public JfflJsPatchBuilder before(String src) {
        JfflJsHooks.registerOperation(parent.target(), parent.priority(), cls -> {
            if (desc == null) cls.insertBeforeInvoke(method, target, ordinal, src);
            else cls.insertBeforeInvoke(method, desc, target, ordinal, src);
        });
        return parent;
    }

    public JfflJsPatchBuilder after(String src) {
        JfflJsHooks.registerOperation(parent.target(), parent.priority(), cls -> {
            if (desc == null) cls.insertAfterInvoke(method, target, ordinal, src);
            else cls.insertAfterInvoke(method, desc, target, ordinal, src);
        });
        return parent;
    }

    public JfflJsPatchBuilder beforeJs(Object fn) {
        String id = JfflJsFunctionRegistry.registerObject(fn, parent.target() + "." + method + " invoke-before");
        return before("net.wertygh.jffl.js.util.JfflJsFunctionRegistry.call(" + JfflJsNames.quote(id) + ", $0, $args);");
    }

    @Deprecated
    public JfflJsPatchBuilder beforeJs(Function fn) {
        return beforeJs((Object) fn);
    }
}