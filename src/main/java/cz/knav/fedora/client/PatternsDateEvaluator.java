package cz.knav.fedora.client;

import java.util.ArrayList;

public class PatternsDateEvaluator {
    
    private ArrayList<PatternDefinition> patterns = new ArrayList<PatternDefinition>();
    
    private String date;
    private boolean attrApprox;
    
    public PatternsDateEvaluator(ArrayList<PatternDefinition> patterns, 
            String date, boolean attrApprox) {
        super();
        this.patterns = patterns;
        this.date = date;
        this.attrApprox = attrApprox;
    }
    
    public boolean evaluate() throws Exception {
        boolean r = false;
        for (PatternDefinition p : patterns) {
            if (p.evaluate(date, attrApprox)) {
                r = true;
                break;
            }
            
        }
        return r;
    }

}
