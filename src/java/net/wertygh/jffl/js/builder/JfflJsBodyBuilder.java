package net.wertygh.jffl.js.builder;

import dev.latvian.mods.rhino.Function;
import net.wertygh.jffl.js.engine.JfflJsEngine;
import net.wertygh.jffl.js.engine.JfflJsEngineHolder;
import net.wertygh.jffl.js.engine.JfflJsFunction;

public class JfflJsBodyBuilder {
    private final StringBuilder out = new StringBuilder();
    private int indent = 1;

    public String build() {
        return "{\n" + out + "}";
    }

    @Override
    public String toString() {
        return build();
    }

    public JfflJsBodyBuilder raw(String source) {
        if (source == null || source.isBlank()) return this;
        pad();
        out.append(source).append('\n');
        return this;
    }

    public JfflJsBodyBuilder line(String source) {
        return raw(source);
    }

    public JfflJsBodyBuilder comment(String text) {
        return raw("// " + String.valueOf(text).replace("\n", "\n// "));
    }

    public JfflJsBodyBuilder stmt(String statement) {
        if (statement == null || statement.isBlank()) return this;
        String s = statement.trim();
        if (!s.endsWith(";") && !s.endsWith("}") && !s.endsWith(":")) s += ";";
        return raw(s);
    }

    public JfflJsBodyBuilder local(String type, String name) {
        return stmt(type + " " + name);
    }

    public JfflJsBodyBuilder local(String type, String name, String init) {
        return (init == null || init.isBlank()) ? local(type, name) : stmt(type + " " + name + " = " + init);
    }

    public JfflJsBodyBuilder inc(String varName) {return stmt(varName + "++");}
    public JfflJsBodyBuilder dec(String varName) {return stmt(varName + "--");}
    public JfflJsBodyBuilder call(String expression) {return stmt(expression);}
    public JfflJsBodyBuilder return_() {return stmt("return");}
    public JfflJsBodyBuilder throw_(String expression) {return stmt("throw "+expression);}
    public JfflJsBodyBuilder break_() {return stmt("break");}
    public JfflJsBodyBuilder continue_() {return stmt("continue");}

    public JfflJsBodyBuilder assign(String left, String right) {
        return stmt(left + " = " + right);
    }

    public JfflJsBodyBuilder return_(String expression) {
        return (expression == null || expression.isBlank()) ? return_() : stmt("return " + expression);
    }

    public JfflJsBodyBuilder block(String header, Object body) {
        begin(header);
        if (body != null) {
            callBody(body);
        }
        end();
        return this;
    }

    @Deprecated
    public JfflJsBodyBuilder block(String header, Function body) {
        return block(header, (Object) body);
    }

    public JfflJsBodyBuilder if_(String condition, Object body) {
        return block("if (" + condition + ")", body);
    }

    @Deprecated
    public JfflJsBodyBuilder if_(String condition, Function body) {
        return if_(condition, (Object) body);
    }

    public JfflJsBodyBuilder elseIf_(String condition, Object body) {
        pad();out.append("else if (").append(condition).append(") {\n");
        indent++;callBody(body);indent--;
        pad();out.append("}\n");
        return this;
    }

    @Deprecated
    public JfflJsBodyBuilder elseIf_(String condition, Function body) {
        return elseIf_(condition, (Object) body);
    }

    public JfflJsBodyBuilder else_(Object body) {
        pad();out.append("else {\n");indent++;callBody(body);indent--;
        pad();out.append("}\n");
        return this;
    }

    @Deprecated
    public JfflJsBodyBuilder else_(Function body) {
        return else_((Object) body);
    }

    public JfflJsBodyBuilder for_(String init, String condition, String update, Object body) {
        return block("for (" + safe(init) + "; " + safe(condition) + "; " + safe(update) + ")", body);
    }

    @Deprecated
    public JfflJsBodyBuilder for_(String init, String condition, String update, Function body) {
        return for_(init, condition, update, (Object) body);
    }

    public JfflJsBodyBuilder forEach(String type, String var, String iterable, Object body) {
        return block("for (" + type + " " + var + " : " + iterable + ")", body);
    }

    @Deprecated
    public JfflJsBodyBuilder forEach(String type, String var, String iterable, Function body) {
        return forEach(type, var, iterable, (Object) body);
    }

    public JfflJsBodyBuilder while_(String condition, Object body) {
        return block("while (" + condition + ")", body);
    }

    @Deprecated
    public JfflJsBodyBuilder while_(String condition, Function body) {
        return while_(condition, (Object) body);
    }

    public JfflJsBodyBuilder doWhile(String condition, Object body) {
        pad();out.append("do {\n");indent++;callBody(body);indent--;
        pad();out.append("} while (").append(condition).append(");\n");
        return this;
    }

    @Deprecated
    public JfflJsBodyBuilder doWhile(String condition, Function body) {
        return doWhile(condition, (Object) body);
    }

    public JfflJsBodyBuilder tryCatch(Object tryBody, String exceptionType, String name, Object catchBody) {
        pad();out.append("try {\n");indent++;callBody(tryBody);indent--;
        pad();
        out.append("} catch (")
          .append(exceptionType==null||exceptionType.isBlank()?"java.lang.Throwable":exceptionType)
          .append(' ').append(name==null||name.isBlank() ? "t" : name).append(") {\n");
        indent++;callBody(catchBody);indent--;
        pad();out.append("}\n");
        return this;
    }

    public JfflJsBodyBuilder tryFinally(Object tryBody, Object finallyBody) {
        pad();out.append("try {\n");indent++;callBody(tryBody);indent--;
        pad();out.append("} finally {\n");indent++;callBody(finallyBody);indent--;
        pad();out.append("}\n");
        return this;
    }

    public JfflJsBodyBuilder synchronized_(String lockExpr, Object body) {
        return block("synchronized (" + lockExpr + ")", body);
    }

    public JfflJsBodyBuilder switch_(String expression, Object body) {
        return block("switch (" + expression + ")", body);
    }

    public JfflJsBodyBuilder case_(String label) {
        indent--;pad();out.append("case ").append(label).append(":\n");indent++;
        return this;
    }

    public JfflJsBodyBuilder default_() {
        indent--;pad();out.append("default:\n");indent++;
        return this;
    }

    public JfflJsBodyBuilder begin(String header) {
        pad();out.append(header).append(" {\n");indent++;
        return this;
    }

    public JfflJsBodyBuilder end() {
        indent--;pad();out.append("}\n");
        return this;
    }

    private void callBody(Object body) {
        if (body == null) return;
        JfflJsEngine engine = JfflJsEngineHolder.getEngine();
        JfflJsFunction func = engine.wrapFunction(body);
        try {
            func.call(null, this);
        } catch (Exception e) {
            throw new RuntimeException("调用主体生成器lambda时出错", e);
        }
    }

    private void pad() {
        for (int i = 0; i < indent; i++) out.append("    ");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}