package cz.knav.fedora.client;

import org.w3c.dom.Element;

public class DateResult {
    
    public static final int NO = 1; 
    public static final int YES_NONSTANDARD = 2; 
    public static final int YES = 3; 
    
    int result = NO;
    
    public static String getAttrQualifierApproximate(Element element) {
        String r = "";
        String attrName = "qualifier";
        if (element.hasAttribute(attrName)) {
            final String attrValApproximate = "approximate";
            String attrVal = element.getAttribute(attrName);
            if (attrVal.equalsIgnoreCase(attrValApproximate)) {
                r = attrValApproximate + " ";
            }
        }
        return r;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
    
}
