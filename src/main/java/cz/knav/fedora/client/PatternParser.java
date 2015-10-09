package cz.knav.fedora.client;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PatternParser {
    
    private YearEvaluation yearEvaluation;
        
    public PatternParser(YearEvaluation yearEvaluation) {
        super();
        this.yearEvaluation = yearEvaluation;
    }

    public ArrayList<PatternDefinition> parsePatterns(String filePath) throws Exception {
        ArrayList<PatternDefinition> r = new ArrayList<PatternDefinition>();        
        File fXmlFile = new File(filePath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile); 
        NodeList nList = doc.getElementsByTagName("VzorDefinice");
        for (int i = 0; i < nList.getLength(); i++) {
            Element node = (Element) nList.item(i);
            boolean iAttrApprox;
            String iAttrApproxStr = node.getElementsByTagName(
                    "IAtributApproximate").item(0).getTextContent();
            if (iAttrApproxStr.equalsIgnoreCase("A")) {
                iAttrApprox = true;
            } else {
                iAttrApprox = false;
            }
            r.add(new PatternDefinition(iAttrApprox,
                    getChar(node, "Cislice"),
                    getChar(node, "CisliceProVypocet"),
                    node.getElementsByTagName(
                            "Vzor").item(0).getTextContent(),
                    node.getElementsByTagName(
                            "PrevodNaRok").item(0).getTextContent(),
                    yearEvaluation));
        }
        return r;
    }
    
    private static char getChar(Element node, String tagName) {
        String s = node.getElementsByTagName(tagName).item(0).getTextContent();
        if (s.length() != 1) {
            throw new RuntimeException();
        }
        return s.charAt(0);
    }

}











































