package cz.knav.fedora.client;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

public class DateEvaluator {
    
    private boolean isDateAuthor;

    private static ArrayList<PatternDefinition> patternsDateAuthor = null;
    private static ArrayList<PatternDefinition> patternsDateIssued = null;
    private static YearEvaluation yearEvaluationAuthor;
    private static YearEvaluation yearEvaluationIssued;
    
    private String date;
    private boolean attrApprox;
    
    private int yearCount = 0;
    
    private int ixYearLast;
    
    private String yearLast = null;
    private String yearBeforeLast = null;
    
    public static void init(String dirPath, int yearsAuthor, int yearsIssued) throws Exception {
        if (patternsDateAuthor == null) {
            yearEvaluationAuthor = new YearEvaluation(yearsAuthor);
            yearEvaluationIssued = new YearEvaluation(yearsIssued);
            patternsDateAuthor = (new PatternParser(yearEvaluationAuthor)).parsePatterns(
                    dirPath + "VzoryDatumUmrti.xml");
            patternsDateIssued = (new PatternParser(yearEvaluationIssued)).parsePatterns(
                    dirPath + "VzoryRokVydani.xml");
        }
    }

    public static DateResult evaluate(boolean isDateAuthor, Element element) throws Exception {
        return (new DateEvaluator(isDateAuthor, element)).evaluate();
    }
    
    private DateEvaluator(boolean isDateAuthor, Element element) {
        super();
        this.isDateAuthor = isDateAuthor;
        
        String s = element.getTextContent();
        if (s != null) {
            this.date = s.trim();
        } else {
            this.date = "";
        }

        if (DateResult.getAttrQualifierApproximate(element).equals("")) {
            this.attrApprox = false;
        } else {
            this.attrApprox = true;
        }
    }
    
    private boolean evaluateUserPatterns() throws Exception {
        if (isDateAuthor) {
            return (new PatternsDateEvaluator(patternsDateAuthor, 
                    date, attrApprox)).evaluate();
        } else {
            return (new PatternsDateEvaluator(patternsDateIssued, 
                    date, attrApprox)).evaluate();
        }
    }

    /*
    narozen 1950
    nar. 1950
    1950 nar.
    1950-
    červen 1950
    2.2.1950
    1950
    7/1950
    7.1950
    *1950
    
    01234 ... index pozice
    +1950
    z1950
    zemř. 7.1950
    -1950
    1950 zemř.
    
    1930-2010
    1930 až 2010
    2.2.1850-3.3.1930
    ca1920-ca1990
    nar. 1750-1760 zemř. 1795-1810
    
    nar. 1795-1810
    zemř. 1795-1810
    nar. 1795 zemř. 1810
    */
    private DateResult evaluate() throws Exception {
        DateResult r = new DateResult();
        if (evaluateUserPatterns()) {
            r.setResult(DateResult.YES);
        } else {
            Boolean isYearOk = null;
            date = date.toLowerCase();
            if (date.length() > 2) {
                for (int i = date.length() - 1; i > 1; i--) {
                    if (Character.isDigit(date.charAt(i))
                            && Character.isDigit(date.charAt(i - 1))
                            && Character.isDigit(date.charAt(i - 2))) {
                        if (date.length() == 3) {
                            isYearOk = evaluateYear(true, date);
                        } else {
                            if (i > 2) {
                                if (Character.isDigit(date.charAt(i - 3))) {
                                    if (date.length() == 4) {
                                        isYearOk = evaluateYear(true, date);
                                    } else { 
                                        i = addYear(i, 4);
                                    }
                                } else {
                                    i = addYear(i, 3);
                                }
                            } else {
                                i = addYear(i, 3);
                            }
                        }
                    }
                }
                
                if (isYearOk == null) {
                    if (yearCount > 0) {
                        if (yearCount == 1) {
                            isYearOk = inspectOnlyOneYear();
                        } else {
                            if (Integer.valueOf(yearLast) > Integer.valueOf(yearBeforeLast)) {
                                isYearOk = evaluateYear(false, yearLast);
                            } else {
                                isYearOk = evaluateYear(false, yearBeforeLast);
                            }
                        }
                    }
                }
                
            }
            
            if ((isYearOk == null) || !isYearOk) {
                r.setResult(DateResult.NO);
            } else {
                if (isDateAlmostStandard()) {
                    r.setResult(DateResult.YES);
                } else {
                    r.setResult(DateResult.YES_NONSTANDARD);
                }
            }
        }
        return r;
    }
    
    /*
    dle standardu - dateAuthor: RRRR-RRRR
    
    dle standardu - dateIssued:
    
    1. Pro nejvyssi urovne - titul periodika a titul monografie
    
    <dateIssued>
    datum  vydání  předlohy,  nutno  zaznamenat  v případě  titulu
     roky  v nichž  časopis  vycházel  (např. 1900‐1939),  přebírat  ve
    formě,  jak  je  zapsáno  v hodnotě  pole  v  katalogu
    odpovídá  hodnotě  z katalogizačního  záznamu,  pole  260,  podpole  „c“
    
    2. Pro rocnik periodika
    
    <dateIssued>
    datum  vydání  předlohy,  v případě  ročníku rok,  případně  rozsah
    let,  kdy  vyšel
    ‐ RRRR  – pokud  víme  rok
    ‐ RRRR‐RRRR  – rozsah  let
    - atribut "qualifier" - možnost  dalšího  upřesnění,  hodnota
    „approximate“ pro data, kde nevíme přesný údaj
    
    3. Pro číslo periodika a přílohu
    
    <dateIssued>
    datum  vydání  předlohy,  v případě  čísla  datum  dne,  kdy  vyšlo;
    musí  vyjádřit  den,  měsíc  a  rok,  dle  toho  jaké  údaje  jsou  k
    dispozici;
    nutno  zapsat  v následujících  podobách:
    ‐ DD.MM.RRRR – pokud  víme  den,  měsíc  i rok  vydání
    ‐ MM.RRRR  – pokud  víme  jen  měsíc  a  rok vydání
    ‐ RRRR – pokud  víme  pouze  rok
    ‐ DD.‐DD.MM.RRRR – vydání  pro  více  dní
    - MM.‐MM.RRRR – vydání  pro  více  měsíců
    - atribut - qualifier  – možnost  dalšího  upřesnění,  hodnota
    „approximate“  pro  data,  kde  nevíme  přesný  údaj
          
    ----------------------
    jiný typ "pomlček":
    DD.MM.RRRR – pokud víme den, měsíc i rok vydání
    RRRR – pokud víme pouze rok
    MM.RRRR – pokud víme jen měsíc a rok vydání
    DD.-DD.MM.RRRR – vydání pro více dní
    MM.-MM.RRRR – vydání pro více měsíců
    ----------------------
    */
    private boolean isDateAlmostStandard() {
        boolean r = false;
        if (!attrApprox) {
            String minusesAndSimilar = "[−‒–—―‐-]{0,5} {0,2}";
            if (isDateAuthor) {
                r = Pattern.compile(
                        "^\\d\\d\\d\\d {0,2}" + minusesAndSimilar
                        + "\\d\\d\\d\\d$").matcher(date).find();
            } else {
                String d0m0yyyy = 
                        "((\\d?\\d\\. {0,2})?"+
                        "\\d?\\d\\. {0,2})?"+
                        "\\d\\d\\d\\d {0,2}";
                r = Pattern.compile(
                        "^(" + d0m0yyyy +
                            "(" + minusesAndSimilar + d0m0yyyy + 
                        ")?)$|^"+
                        "(" + 
                            "(\\d?\\d\\. {0,2})?"+
                            "\\d?\\d\\. {0,2}"+
                            minusesAndSimilar + d0m0yyyy + 
                        ")$").matcher(date).find();
                /*
                String[] aS = new String[] {
                        "2000",
                        "2000-2010",
                        "21.01.2000",
                        "01.2000",
                        "21. - 29.01.2000",
                        "01.-02.2000",
                        "21.01.–29.01.2000",
                        "21.1.2000  —29.01.2000",
                        "21.1.2000 -—--- 2000",
                        "01.01.99",    //ne
                        "01.01.99 - 20.01.99",//ne
                };
                for (int i = 0; i < aS.length; i++) {
                    Matcher m = patternIssued.matcher(aS[i]);
                    if (m.find()) {
                        System.out.println("ano " + aS[i]);
                    } else {
                        System.out.println("ne " + aS[i]);
                    }
                }
                */
            }
        }
        return r;
    }

    /*
    private static boolean isMinusOrSimilar(char c) {
        return ((c == '−')
            || (c == '‒')
            || (c == '–')
            || (c == '—')
            || (c == '―')
            || (c == '‐')
            || (c == '-'));
    }
    */
    
    private int getIncrementForYearOfBirth() {
        if (isDateAuthor) {
            return 150;
        } else {
            return 0;
        }
    }
    
    private boolean inspectOnlyOneYear() {
        if (isDateAuthor) {
            if ((date.indexOf("z") > -1 && date.indexOf("oz") < 0)
                    || date.indexOf("u") > -1
                    || date.indexOf("ú") > -1
                    || date.indexOf("ů") > -1
                    || date.indexOf("s") > -1
                    || date.indexOf("+") > -1
                    || date.indexOf("#") > -1
                    /*
                    vypustka, tecka, minus a ruzne druhy pomlcek a spojovniku: 
                    System.out.println("…" + (int)'…');
                    System.out.println("." + (int)'.');
                    System.out.println("−" + (int)'−');
                    System.out.println("‒" + (int)'‒');
                    System.out.println("–" + (int)'–');
                    System.out.println("—" + (int)'—');
                    System.out.println("―" + (int)'―');
                    System.out.println("‐" + (int)'‐');
                    System.out.println("-" + (int)'-');
                    ----------------
                    …8230
                    .46
                    −8722
                    ‒8210
                    –8211
                    —8212
                    ―8213
                    ‐8208
                    -45
                     */
                    || isNearBeforeYearLast("…")
                    || isNearBeforeYearLast("..")
                    || isNearBeforeYearLast("−")
                    || isNearBeforeYearLast("‒")
                    || isNearBeforeYearLast("–")
                    || isNearBeforeYearLast("—")
                    || isNearBeforeYearLast("―")
                    || isNearBeforeYearLast("‐")
                    || isNearBeforeYearLast("-")
                    ) {
                return evaluateYear(false, yearLast);
            } else {
                return evaluateYear(true, yearLast);
            }
        } else {
            return evaluateYear(false, yearLast);
        }
    }

    //yearLength 3 or 4
    private int addYear(int i, int yearLength) {
        yearCount++;
        if (yearLast == null) {
            ixYearLast = (i - yearLength) + 1;
            yearLast = date.substring(ixYearLast, i + 1);
        } else {
            if (yearBeforeLast == null) {
                yearBeforeLast = date.substring((i - yearLength) + 1, i + 1);
            }
        }
        return (i - yearLength);
    }
    
    private boolean evaluateYear(boolean birth, String year) {
        int y = Integer.valueOf(year);
        
        if (birth) {
            y = y + getIncrementForYearOfBirth();
        }
        
        if (isDateAuthor) {
            return yearEvaluationAuthor.evaluate(y);
        } else {
            return yearEvaluationIssued.evaluate(y);
        }
    }
    
    private boolean isNearBeforeYearLast(String s) {
        return date.indexOf(s) == ixYearLast - s.length() - 2
                || date.indexOf(s) == ixYearLast - s.length() - 3
                || date.indexOf(s) == ixYearLast - s.length() - 4;
    }
    
}





















































