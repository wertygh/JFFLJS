package net.wertygh.jffl.js.model;

public class JfflJsSlice {
    public JfflJsAt from = new JfflJsAt("HEAD");
    public JfflJsAt to = new JfflJsAt("TAIL");
    public String id = "";

    public JfflJsSlice from(JfflJsAt from) {
        this.from = from == null ? new JfflJsAt("HEAD") : from;
        return this;
    }
    
    public JfflJsSlice to(JfflJsAt to) {
        this.to = to == null ? new JfflJsAt("TAIL") : to;
        return this;
    }
    
    public JfflJsSlice id(String id) {
        this.id = id == null ? "" : id;
        return this;
    }
}