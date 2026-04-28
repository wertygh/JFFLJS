package net.wertygh.jffl.js.hook;

import net.wertygh.jffl.api.ITransformerHook;

class JfflJsMutableHook implements ITransformerHook {
    private final ITransformerHook d;
    private final byte[] b;
    
    JfflJsMutableHook(ITransformerHook d, byte[] b) {this.d = d;this.b = b;}
    public String getClassName() {return d.getClassName();}
    public String getInternalName() {return d.getInternalName();}
    public byte[] getBytes() {return b;}
    public byte[] getOriginalBytes() {return d.getOriginalBytes();}
    public boolean isPatched() {return d.getOriginalBytes() != b;}
}