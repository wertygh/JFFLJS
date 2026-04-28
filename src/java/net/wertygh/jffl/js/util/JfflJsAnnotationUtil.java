package net.wertygh.jffl.js.util;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;
import java.util.List;

public class JfflJsAnnotationUtil {
    public static void apply(ConstPool cp, List<AttributeInfo> attrs, List<JfflJsAnnotationSpec> specs) {
        if (specs == null || specs.isEmpty()) return;
        AnnotationsAttribute visible = null, invisible = null;
        for (AttributeInfo a : attrs) {
            if (AnnotationsAttribute.visibleTag.equals(a.getName()) 
                    && a instanceof AnnotationsAttribute aa) visible = aa;
            if (AnnotationsAttribute.invisibleTag.equals(a.getName()) 
                    && a instanceof AnnotationsAttribute aa) invisible = aa;
        }
        for (JfflJsAnnotationSpec s : specs) {
            if (s.visible) {
                if (visible == null) {
                    visible = new AnnotationsAttribute(cp,AnnotationsAttribute.visibleTag);
                    attrs.add(visible);
                }
                visible.addAnnotation(s.toAnnotation(cp));
            } else {
                if (invisible == null) {
                    invisible = new AnnotationsAttribute(cp, AnnotationsAttribute.invisibleTag);
                    attrs.add(invisible);
                }
                invisible.addAnnotation(s.toAnnotation(cp));
            }
        }
    }
}
