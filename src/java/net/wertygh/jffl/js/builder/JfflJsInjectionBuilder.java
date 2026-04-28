package net.wertygh.jffl.js.builder;

import dev.latvian.mods.rhino.Function;
import net.wertygh.jffl.js.hook.JfflJsHooks;
import net.wertygh.jffl.js.util.JfflJsFunctionRegistry;
import net.wertygh.jffl.js.util.JfflJsNames;

public class JfflJsInjectionBuilder {
    public enum At {HEAD, RETURN, FINALLY}
    private final JfflJsPatchBuilder parent;
    private final String method, desc;
    private At at = At.HEAD;

    JfflJsInjectionBuilder(JfflJsPatchBuilder p, String m, String d) {
        parent = p; method = m; desc = d;
    }

    public JfflJsInjectionBuilder atHead() {at = At.HEAD; return this;}
    public JfflJsInjectionBuilder atReturn() {at = At.RETURN; return this;}
    public JfflJsInjectionBuilder atFinally() {at = At.FINALLY; return this;}

    public JfflJsPatchBuilder body(String source) {
        JfflJsHooks.registerOperation(parent.target(), parent.priority(), cls -> {
            if (at == At.HEAD) {
                if (desc == null) cls.insertBefore(method, source);
                else cls.insertBefore(method, desc, source);
            } else if (at == At.RETURN) {
                if (desc == null) cls.insertAfter(method, source);
                else cls.insertAfter(method, desc, source);
            } else {
                if (desc == null) cls.insertFinally(method, source);
                else cls.insertFinally(method, desc, source);
            }
        });
        return parent;
    }

    public JfflJsPatchBuilder js(Object fn) {
        String id = JfflJsFunctionRegistry.registerObject(fn, parent.target() + "." + method + " inject");
        JfflJsHooks.registerOperation(parent.target(), parent.priority(), cls -> {
            if (at == At.HEAD) {
                if (desc == null) cls.insertBeforeJs(method, id);
                else cls.insertBeforeJs(method, desc, id);
            } else if (at == At.RETURN) {
                if (desc == null) cls.insertAfterJs(method, id);
                else cls.insertAfterJs(method, desc, id);
            } else {
                if (desc == null) cls.insertFinallyJs(method, id);
                else cls.insertFinallyJs(method, desc, id);
            }
        });
        return parent;
    }

    @Deprecated
    public JfflJsPatchBuilder js(Function fn) {
        return js((Object) fn);
    }

    public JfflJsPatchBuilder log(String message) {
        return body("net.wertygh.jffl.js.JfflJs.log(" + JfflJsNames.quote(message) + ");");
    }
}