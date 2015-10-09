package cz.knav.fedora.client;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class PatternDefinition {
    
    /*
    <?xml version="1.0" encoding="UTF-8"?>
    <Vzory>
    <VzorDefinice>
    <IAtributApproximate>N</IAtributApproximate>
    <Cislice>C</Cislice>
    <CisliceProVypocet>R</CisliceProVypocet>
    <Vzor>cca CC. - RR. stol.</Vzor>
    <PrevodNaRok>RR*100+150</PrevodNaRok>
    </VzorDefinice>
    <VzorDefinice>
    <IAtributApproximate>N</IAtributApproximate>
    <Cislice>C</Cislice>
    <CisliceProVypocet>R</CisliceProVypocet>
    <Vzor>asi CC. - RR. stol.</Vzor>
    <PrevodNaRok>RR*100+150</PrevodNaRok>
    </VzorDefinice>
    </Vzory>
     */
    
    private boolean iAttrApprox; 
    private char digitChar; 
    private char evalChar; 
    
    private String pattern;
    private String patternToYear;
    
    private YearEvaluation yearEvaluation;
    
    private StringBuffer evalSB = new StringBuffer();

    public PatternDefinition(boolean iAttrApprox, char digitChar,
            char evalChar, String pattern, String patternToYear,
            YearEvaluation yearEvaluation) {
        super();
        this.iAttrApprox = iAttrApprox;
        this.digitChar = digitChar;
        this.evalChar = evalChar;
        this.pattern = pattern;
        this.patternToYear = patternToYear;
        this.yearEvaluation = yearEvaluation;
    }

    public boolean evaluate(String date, boolean attrApprox) throws Exception {
        boolean r = false;
        if (digitChar == evalChar) {
            throw new RuntimeException();
        }
        if (!attrApprox || iAttrApprox) {
            if (date.length() == pattern.length()) {
                r = true;
                int lastIxEvalC = -1;
                for (int i = 0; i < date.length(); i++) {
                    if (date.charAt(i) != pattern.charAt(i)) {
                        if (pattern.charAt(i) == digitChar) {
                            if (!Character.isDigit(date.charAt(i))) {
                                r = false;
                                break;
                            }
                        } else if (pattern.charAt(i) == evalChar) {
                            lastIxEvalC = checkIxOfEvalChar(i, lastIxEvalC);
                            if (Character.isDigit(date.charAt(i))) {
                                evalSB.append(date.charAt(i));
                            } else {
                                r = false;
                                break;
                            }
                        } else {
                            r = false;
                            break;
                        }
                    }
                }
                if (r) {
                   r = evaluateYear(evalSB); 
                }
            }
        }
        return r;
    }
    
    private static int checkIxOfEvalChar(int i, int lastIxEvalC) {
        int r = lastIxEvalC;
        if (r == -1) {
            r = i;
        } else {
            if (i == r + 1) {
                r = i;
            } else {
                throw new RuntimeException();
            }
        }
        return r;
    }
    
    private boolean evaluateYear(StringBuffer y) throws Exception {
        StringBuffer expr = new StringBuffer("");
        int lastIxEvalC = -1;
        for (int i = 0; i < patternToYear.length(); i++) {
            char c = patternToYear.charAt(i); 
            if (c == evalChar) {
                lastIxEvalC = checkIxOfEvalChar(i, lastIxEvalC);
                expr.append(y);
                for (int iE = 1; iE < y.length(); iE++) {
                    i++;
                    if (patternToYear.charAt(i) != evalChar) {
                        throw new RuntimeException();
                    }
                }
            } else {
                expr.append(c);
            }
        }
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        Object o = engine.eval(expr.toString());
        return yearEvaluation.evaluate(((Double) o).intValue());
    }

    /*
    public static void main(String[] args) throws Exception {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String foo = "40+02";
        Object o = engine.eval(foo);
        System.out.println(((Double) o).intValue());
    }
    */
    
}

































