package net.wertygh.jffl.js.model;

public class JfflJsAt {
    public final String value;
    public String target = "";
    public int line = -1;
    public String stringValue = " ";
    public int intValue = Integer.MIN_VALUE;
    public double doubleValue = Double.NaN;
    public int ordinal = -1;
    public String shift = "BEFORE";

    public JfflJsAt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("@At值不能为空");
        }
        this.value = value.trim().toUpperCase();
    }

    public JfflJsAt target(String target) {this.target=target==null?"":target;return this;}
    public JfflJsAt line(int line) {this.line = line;return this;}
    public JfflJsAt stringValue(String v) {this.stringValue = v==null?" ":v;return this;}
    public JfflJsAt intValue(int v) {this.intValue = v;return this;}
    public JfflJsAt doubleValue(double v) {this.doubleValue = v;return this;}
    public JfflJsAt ordinal(int ordinal) {this.ordinal = ordinal;return this;}
    public JfflJsAt before() {this.shift = "BEFORE";return this;}
    public JfflJsAt after() {this.shift = "AFTER";return this;}
}
