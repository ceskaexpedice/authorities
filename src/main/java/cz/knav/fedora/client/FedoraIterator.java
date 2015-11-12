package cz.knav.fedora.client;

import static com.yourmediashelf.fedora.client.FedoraClient.getDatastreamDissemination;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.inject.Inject;
import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import com.yourmediashelf.fedora.client.request.FedoraRequest;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import com.yourmediashelf.fedora.client.response.RiSearchResponse;

import cz.incad.kramerius.processes.annotations.DefaultParameterValue;
import cz.incad.kramerius.processes.annotations.ParameterName;
import cz.incad.kramerius.processes.annotations.Process;
import cz.incad.kramerius.processes.impl.ProcessStarter;
import cz.incad.kramerius.service.impl.PolicyServiceImpl;
import cz.incad.kramerius.utils.conf.KConfiguration;

public final class FedoraIterator {

    public static final Logger LOGGER = Logger.getLogger(FedoraIterator.class.getName());

    public static final String NO = "ne";
    public static final String YES = "ano";

    //private static final int OBJECTS_LIMIT = 100;
    private static final int NOT_FOUND = 404;

    private static int yearsAuthor = 70;
    private static int yearsIssued = 50;

    private static String CAN_RUN = "SpusteniPovoleno";
    private static String IS_UPDATE = "Zpristupnovat";

    private String dirPath;
    private String dirPathOutput;
    
    private boolean canRun;
    private boolean isUpdate;
    private Date modifiedBefore;
    private String persistentUrlBeginning;

    private static final String DSID = "BIBLIO_MODS";
    
    private boolean links = false; //probably will not use this
    
    private PrintStream outputYes = null;
    private PrintStream outputYesNonstandard = null;
    private PrintStream outputNo = null;
    private PrintStream output = null;
    private PrintStream outputLinks = null;
    
    private long lineNumber = 0;
    private long outputNumber = 0;
    
    private FedoraClient fedora;
    //private String token;
    private long objectsCountAll = 0;
    private long objectsCountSelected = 0;
    
    private String model;
    private Document doc;
    private XPath xpath;
    private String pid;
    private NodeList dateIssuedNL;
    private boolean ignore;
    private boolean areDatesAlmostStandard;
    
    @Inject
    KConfiguration configuration;
    
    /*
    <process>
        <id>fedoraiterator</id>
        <description>Zp��stup�ov�n� d�l</description>
        <mainClass>cz.knav.fedora.client.FedoraIterator</mainClass>
        <standardOs>lrOut</standardOs>
        <errOs>lrErr</errOs>
        <parameters>C:/Zpristupnovani/</parameters>
    </process>
    */
    ///*
    public static void main(String[] args) throws Exception {
        process(args[0]/*, args[1], args[2], args[3], args[4]*/);
    }
    //*/

    private static String[] getParams(String dirPathParam) throws Exception {
        String[] r = new String[5];
        
        File fXmlFile = new File(dirPathParam + "Parametry.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile); 
        
        Element spusteniPovoleno = ((Element) doc.getElementsByTagName(CAN_RUN).item(0));
        r[0] = spusteniPovoleno.getTextContent();
        r[1] = ((Element) doc.getElementsByTagName(IS_UPDATE).item(0)).getTextContent();
        r[2] = ((Element) doc.getElementsByTagName("ZmenenePred").item(0)).getTextContent();
        r[3] = ((Element) doc.getElementsByTagName("LetAutori").item(0)).getTextContent();
        r[4] = ((Element) doc.getElementsByTagName("LetVydani").item(0)).getTextContent();
        
        spusteniPovoleno.setTextContent(NO);
        
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(fXmlFile);
        transformer.transform(source, result);
        
        return r;
    }
    
    /*
    @DefaultParameterValue("name")
    public static String DEFAULT_NAME = "DEFAULT";
    @DefaultParameterValue("value")
    public static String DEFAULT_VALUE = "VALUE";
    @Process
    public static void process(@ParameterName("name") String name, @ParameterName("value") String value) {
    */
    /*
    @DefaultParameterValue("update")
    public static String DEFAULT_UPDATE = "updatefalse";
    @DefaultParameterValue("modifiedbefore")
    public static String DEFAULT_MODIFIEDBEFORE = "3333-03-25 23:59:02.222";
    @DefaultParameterValue("output")
    public static String DEFAULT_OUTPUT = "c:/FedoraIteratorOutput/";
    @Process    
    public static void process(@ParameterName("update")String update,
            @ParameterName("modifiedbefore")String modifiedbefore, 
            @ParameterName("output")String output) throws Exception {
    */
    /*
    @DefaultParameterValue("isUpdate")
    public static final String DEFAULT_IS_UPDATE = UPDATEFALSE;
    @DefaultParameterValue("modifiedBefore")
    public static String DEFAULT_MODIFIED_BEFORE = "3333-03-25-23:59:02.222";
    @DefaultParameterValue("outputDirPath")
    public static final String DEFAULT_OUTPUT_DIR_PATH = "c:/FedoraIteratorOutput/";
    @DefaultParameterValue("yearsAuthor")
    public static final String DEFAULT_YEARS_AUTHOR = "70";
    @DefaultParameterValue("yearsIssued")
    public static final String DEFAULT_YEARS_ISSUED = "50";
    @Process
    */
    public static void process(String dirPathParam
            /*
            @ParameterName("isUpdate")String isUpdate, 
            @ParameterName("modifiedBefore")String modifiedBefore, 
            @ParameterName("outputDirPath")String outputDirPath,
            @ParameterName("yearsAuthor")String yearsAuthor,
            @ParameterName("yearsIssued")String yearsIssued
            */
            ) throws Exception {
        String[] p = getParams(dirPathParam);
        LOGGER.info("FedoraIterator started.");
        LOGGER.info("canRun: " + p[0]);
        LOGGER.info("isUpdate: " + p[1]);
        LOGGER.info("modifiedBefore: " + p[2]);
        LOGGER.info("yearsAuthor: " + p[3]);
        LOGGER.info("yearsIssued: " + p[4]);
        ProcessStarter.updateName("Zp��stup�ov�n� d�l - b��");
        FedoraIterator inst = new FedoraIterator();
        inst.configuration = KConfiguration.getInstance();
        inst.initAndExecute(dirPathParam, p);
        ProcessStarter.updateName("Zp��stup�ov�n� d�l - dokon�eno.");
        LOGGER.info("FedoraIterator finished.");
    }
    
    /*
    <info:fedora/uuid:0f33a3e0-2edd-11e0-8e8b-001c259520c6> 
    <info:fedora/fedora-system:def/model#hasModel> <info:fedora/model:periodical> .    
    */
    private void initAndExecute(String dirPathParam, String[] params) throws Exception {
        dirPath = dirPathParam;
        if (!dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }
        dirPathOutput = dirPath + "Vystup/" + System.currentTimeMillis() + "/";
        (new File(dirPathOutput)).mkdirs();

        xpath = XPathFactory.newInstance().newXPath();
        
        if (params[0].equalsIgnoreCase(YES)) {
            canRun = true;
        } else if (params[0].equalsIgnoreCase(NO)) {
            canRun = false;
        } else {
            throw new IllegalArgumentException(
                    "Invalid or missing parameter \"" + CAN_RUN + "\" " + 
                     NO + "/" + YES);
        }
        
        if (params[1].equalsIgnoreCase(YES)) {
            isUpdate = true;
        } else if (params[1].equalsIgnoreCase(NO)) {
            isUpdate = false;
        } else {
            throw new IllegalArgumentException(
                    "Invalid or missing parameter \"" + IS_UPDATE + "\" " + 
                     NO + "/" + YES);
        }
        
        modifiedBefore = (new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS")).parse(params[2]);
        
        yearsAuthor = Integer.valueOf(params[3]).intValue();
        yearsIssued = Integer.valueOf(params[4]).intValue();
        DateEvaluator.init(dirPath, yearsAuthor, yearsIssued);
        execute();
        
        /*
        if (links) {
            persistentUrlBeginning = args[...];
        }
        */
    }

    private void execute() throws Exception {
        String s = "Begin " + getTime();
        System.out.println(s);
        log(s);
        
        if (!canRun) {
            log("--------------------------------------------");
            log("Parameter \"" + CAN_RUN + "\": " + NO);
            log("--------------------------------------------");
        } else {
            log("------------------------------------------------------------b");
            try {
                /*
                FedoraCredentials credentials = new FedoraCredentials(
                        new URL(args[4]), args[5], args[6]);
                        */
                FedoraCredentials credentials = new FedoraCredentials(
                        new URL(configuration.getFedoraHost()), 
                        configuration.getFedoraUser(), configuration.getFedoraPass());
                fedora = new FedoraClient(credentials);
                fedora.debug(false);
                FedoraRequest.setDefaultClient(fedora);  
            } catch (MalformedURLException e) {
                throwRuntimeException(e);
            }

            /*
            FindObjectsResponse response = FedoraClient.findObjects().terms("*").pid()
                    .maxResults(OBJECTS_LIMIT)
                    .execute(fedora);
            executeResponse(response);
            token = response.getToken();
            while (token != null) {
                response = FedoraClient.findObjects()
                        .sessionToken(token)
                        .execute(fedora);
                executeResponse(response);
                token = response.getToken();
            }
            */
            
            /*
            String[] models = { 
                    "repository",
                    "monograph",
                    "monographunit",
                    "periodical",
                    "periodicalvolume",
                    "periodicalitem",
                    "article",
                    "manuscript",
                    "map",
                    "internalpart",
                    "sheetmusic",
                    "supplement",
                    "soundrecording",
                    "soundunit",
                    "track",
                    "graphic"
                  };
                  */
            String[] models = configuration.getPropertyList("fedora.modelsWithYearsAuthors");
            
            for (int i = 0; i < models.length; i++) {
                model = models[i];
                RiSearchResponse response = null;
                try {
                    String q = " *  <info:fedora/fedora-system:def/model#hasModel>   <info:fedora/model:"
                            + model + ">"; 
                    LOGGER.info("query: " + q);
                    response = FedoraClient.riSearch(q).lang("spo")
                            .type("triples").flush(true).execute();
                    if (response.getStatus() != 200) {
                        writeError("response.getStatus() != 200 " + "query: " + q);
                    } else {
                        executeResponse(response);
                    }
                } catch (FedoraClientException e) {
                     writeError(e);
                }
            }
            
            /* in result were duplicates:
            <info:fedora/uuid:85560f80-355c-11e3-8d9d-005056827e51> <info:fedora/fedora-system:def/model#hasModel> <info:fedora/model:monograph> .
            <info:fedora/uuid:3e1564a0-2ce1-11e3-a5bb-005056827e52> <info:fedora/fedora-system:def/model#hasModel> <info:fedora/model:monograph> .
            <info:fedora/uuid:9c940885-d0c4-11e1-8140-005056a60003> <info:fedora/fedora-system:def/model#hasModel> <info:fedora/model:monograph> .
            <info:fedora/uuid:85560f80-355c-11e3-8d9d-005056827e51> <info:fedora/fedora-system:def/model#hasModel> <info:fedora/model:monograph> .
            <info:fedora/uuid:3e1564a0-2ce1-11e3-a5bb-005056827e52> <info:fedora/fedora-system:def/model#hasModel> <info:fedora/model:monograph> .
             */
        }
        
        /* was tested:
        for (int i = 0; i < 400000; i++) {
            log(lineNumber + " aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaas");
        }
        */
        log(getMessageCounts(true));
        System.out.println(getMessageCounts(true));
        
        closeOutputs();
    }
    
    private String getMessageCounts(boolean end) {
        String s = "NotEnd: ";
        if (end) {
            s = "End: ";
        }
        return s + getTime() + " selected objects: " + objectsCountSelected 
                + "; processed objects: " + objectsCountAll;
    }
    
    private void closeOutputs() {
        if (output != null) {
            output.close();
            outputYes.close();
            outputYesNonstandard.close();
            outputNo.close();
        }   
        if (outputLinks != null) {
            outputLinks.println("</body></html>");
            outputLinks.close();
        }
    }
    
    private PrintStream getStreamForOutput(String fileName) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(dirPathOutput + fileName));
    }
    
    private void setNewOutputs() {
        closeOutputs();
        outputNumber++;
        try {
            output = getStreamForOutput("Output" + outputNumber + ".txt");
            outputYes = getStreamForOutput("OutputYes" + outputNumber + ".txt");
            outputYesNonstandard = getStreamForOutput("OutputYesNonstandard" + outputNumber + ".txt");
            outputNo = getStreamForOutput("OutputNo" + outputNumber + ".txt");
            if (links) {
                String fileName = "OutputLinks" + outputNumber + ".html";
                outputLinks = getStreamForOutput(fileName);
                outputLinks.println(
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" + 
                		"<html>" + 
                		"<head>" + 
                		"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" + 
                		"<title>" + fileName + "</title>" + 
                		"</head>" + 
                		"<body>");
            }
        } catch (FileNotFoundException e) {
            throwRuntimeException(e);
        }
    }
    
    private void logYesNo(String s, boolean yes) {
        log(s, false, false, yes);
    }
    
    private void logPid(String s) {
        log(s, true, false, null);
    }
    
    private void log(String s) {
        log(s, false, false, null);
    }

    /*
    http://localhost:8080/search/handle/uuid:70bd68e1-4dd9-11e3-9ed9-001b63bd97ba
    http://localhost:8080/search/handle/uuid:a0a37da4-4e0c-11e3-b12f-001b63bd97ba
    */
    private void log(String s, boolean pid, boolean debugMessage, Boolean yes) {
        if (!ignore) {
            if (outputNumber < 11) {
                if (lineNumber % 100 == 0) {
                    setNewOutputs();
                }
            } else {
                if (lineNumber % 100000 == 0) {
                    setNewOutputs();
                }
            }
            lineNumber++;
            
            output.println(s);
            if (yes != null) {
                if (yes) {
                    if (areDatesAlmostStandard) {
                        outputYes.println(s);
                    } else {
                        outputYesNonstandard.println(s);
                    }
                } else {
                    outputNo.println(s);
                }
            }
            
            if (!debugMessage) {
                if (links) {
                    if (pid) {
                        outputLinks.println("<p><a href=\"" + persistentUrlBeginning + s + "\">" + objectsCountSelected + "</a></p>");
                    } else {
                        outputLinks.println("<p>" + s + "</p>");
                    }
                }
            }
        }
    }

    private String getTime() {
        return (new Timestamp(System.currentTimeMillis())).toString();
    }

    /*
    private void executeResponse(FindObjectsResponse response) {
        ignore = false;
        try {
            List<String> pids = response.getPids();
            for (String p : pids) {
                try {
                    pid = p;
                    objectsCountAll++;
                    if (objectsCountAll % 100 == 0) {
                        log(getMessageCounts(false), false, true, null);
                        log("------------------------------------------------------------c", false, true, null);
                    }
                    String ds = getDatastream(DSID);
                    if (ds != null) {
                        executeMods(ds);
                    }
                } catch (Throwable e) {
                    writeError(e);
                }
                ignore = false;
            }
        } catch (Throwable e) {
            writeError(e);
        }
        pid = null;
    }
    */
    private void executeResponse(RiSearchResponse response) {
        try {
            PidGetter pidGetter = new PidGetter(response.getEntity(String.class));
            pid = pidGetter.getNextPid();
            while (pid != null) {
                ignore = false;
                areDatesAlmostStandard = true;
                try {
                    objectsCountAll++;
                    if (objectsCountAll % 100 == 0) {
                        log(getMessageCounts(false), false, true, null);
                        log("------------------------------------------------------------c", false, true, null);
                    }
                    String ds = getDatastream(DSID);
                    if (ds != null) {
                        executeMods(ds);
                    }
                } catch (Throwable e) {
                    writeError(e);
                }
                ignore = false;
                pid = pidGetter.getNextPid();
            }
        } catch (Throwable e) {
            writeError(e);
        }
        pid = null;
    }

    
    /*
    Příklad dat - hvězdičkami (*) označeno důležité:

    ---------------------------------------
    <mods:modsCollection xmlns:mods="http://www.loc.gov/mods/v3">
        <mods:mods ID="MODS_VOLUME_0001" version="3.4"*>
            <mods:titleInfo>
                <mods:title>Věčné dobrodružství</mods:title>
                <mods:subTitle>četba pro žáky zákl. a stř. škol</mods:subTitle>
            </mods:titleInfo>
            <mods:titleInfo type="alternative">
                <mods:title>Povídky a apokryfy</mods:title>
            </mods:titleInfo>
            <mods:titleInfo type="alternative">
                <mods:title>Cestopisy</mods:title>
            </mods:titleInfo>
            <mods:titleInfo type="alternative">
                <mods:title>Eseje</mods:title>
            </mods:titleInfo>
            <mods:name type="personal" usage="primary">
                <mods:namePart>Čapek, Karel</mods:namePart>
               *<mods:namePart type="date">1890-1938</mods:namePart>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">aut</mods:roleTerm>
                </mods:role>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">aut</mods:roleTerm>
                </mods:role>
            </mods:name>
            <mods:name type="personal">
                <mods:namePart>Skřeček, Rudolf</mods:namePart>
               *<mods:namePart type="date">1908-1983</mods:namePart>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">aut</mods:roleTerm>
                </mods:role>
            </mods:name>
            <mods:name type="personal">
                <mods:namePart>Víšková, Jarmila</mods:namePart>
               *<mods:namePart type="date">1927-</mods:namePart>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">aut</mods:roleTerm>
                </mods:role>
            </mods:name>
            <mods:name type="personal">
                <mods:namePart>Čapek, Josef</mods:namePart>
               *<mods:namePart type="date">1887-1945</mods:namePart>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">ill</mods:roleTerm>
                </mods:role>
            </mods:name>
            <mods:name type="personal">
                <mods:namePart>Holý, Jiří</mods:namePart>
               *<mods:namePart type="date">1953-</mods:namePart>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">aui</mods:roleTerm>
                </mods:role>
            </mods:name>
            <mods:name type="personal">
                <mods:namePart>Nováková, Zdeňka</mods:namePart>
               *<mods:namePart type="date">1950-</mods:namePart>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">aut</mods:roleTerm>
                </mods:role>
                <mods:role>
                    <mods:roleTerm authority="marcrelator" type="code">com</mods:roleTerm>
                </mods:role>
            </mods:name>
            <mods:typeOfResource>text</mods:typeOfResource>
            <mods:genre>volume</mods:genre>
            <mods:originInfo>
                <mods:place>
                    <mods:placeTerm authority="marccountry" type="text">xr</mods:placeTerm>
                </mods:place>
                <mods:place>
                    <mods:placeTerm type="text">Praha</mods:placeTerm>
                </mods:place>
                <mods:publisher>Československý spisovatel</mods:publisher>
                <mods:edition>1. vyd. v této podobě</mods:edition>
                <mods:issuance>single unit</mods:issuance>
               *<mods:dateIssued>1984</mods:dateIssued>
            </mods:originInfo>
    ...
    ---------------------------------------
    */
    
    
    /*
    Algoritmus vyhledává a posuzuje datum vydání i podle následujících pravidel, která byla
    z wiki Krameria https://github.com/ceskaexpedice/kramerius/wiki/Prava
    zkopírována sem:
    ---------------------------------------
    Poznámky k implementaci
    
    Pravidlo zkoumá stream BIBLIO_MODS hledá datum v následujícíh elementech:
    
    Element originInfo
    
    <mods:originInfo>
       ...
       ...
       <mods:dateIssued>1862</mods:dateIssued>
    </mods:originInfo>
    
    Element originInfo s atributem publisher
    
    <mods:originInfo transliteration="publisher">
       ...
       ...
       <mods:dateIssued>1862</mods:dateIssued>
    </mods:originInfo>
    
    Element part
    
    <mods:part>
       ...
       ...
       <mods:date>1941</mods:date>
    </mods:part>
    
    Očekávaný formát datumu může dle specifikace( http://www.ndk.cz/digitalizace/nove-standardy-digitalizace-od-roku-2011) být:
    
     RRRR                  specifikuje konkretní rok
     RRRR - RRRR           specifikuje rozsah let
     MM. RRRR              specifikuje konkretní měsíc 
     MM.-MM. RRRR          specifikuje rozsah měsíců
     DD. MM. RRRR          specifikuje konkretní den, měsíc a rok
     DD. - DD. MM. RRRR    specifikuje rozsah dní 

    Pokud datum vydání není v metadatech uvedeno, rozhodne stavem NOT_APPLICABLE
    ---------------------------------------
    */
                
                
    /*
    --------------------
    - datum vydání
        - Existuje spousta výjimek a speciálních případů a může tam být 
          napsáno v podstatě cokoliv, včetně různých kombinací písmen, 
          čísel a znaků. V marcu je to volně tvořené pole.
        - je ve starších metadatech jako <mods:date>
            <mods:part>
               ...
               ...
               <mods:date>1941</mods:date>
            </mods:part>        
        - dateIssued
            - není pravda: je v modsu jen na jednom místě
            - neměl by se opakovat
            - pravidla:
              ----------------
                Komplet viz http://www.ndk.cz/digitalizace/nove-standardy-digitalizace-od-roku-2011
                
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
              ----------------
    --------------------


    - datum úmrtí
        - dle specifikace ndk: namePart type=date může být i v elementu subject. V tomhle elementu ho ale nechceme vyhodnocovat.
        - namePart type=date
            - datum narození a úmrtí autora - můžou tam být různé hodnoty včetně slovního vyjádření. Když přeskočím spoustu výjimek, 
              tak nejčastější je rozsah (1812-1893), přibližná hodnota (ca1920-ca1990), jenom datum úmrtí (zemř. 1920; zemřel 1920). 
              Opět je to volně tvořené pole, takže tam může být spousta jiných věcí.
              Je časté také pouze datum narození (1920; 1920-; nar. 1920)
    */
    
    
    private void executeMods(String dsContent) {
        try {
            boolean yes = false;
            doc = buildDocument(dsContent);

            yes = areDatesInNamePartsOk(); 
            //ignore = false; //was used for testing
            if (!ignore) {
                yes = yes
                      & //!!!execute method below, so not &&
                      isDateIssuedOk();
            }
            
            //yes = true && !ignore; //was used for testing
            if (yes) {
                yes = isPolicyPrivate();
                if (yes) {
                    if (fedora.getLastModifiedDate(pid).getTime() >= modifiedBefore.getTime()) {
                        ignore = true;
                        yes = false;
                    }
                }
            }

            if (!ignore) {
                logYesNo(pid, yes);
                logYesNo("model: " + model, yes);
                logYesNo("title: " + getTitleInfo("title"), yes);
                logYesNo("subTitle: " + getTitleInfo("subTitle"), yes);
                logDatesIssued(yes);
                logNameParts(yes);
                logPid(pid);
                logYesNo("------------------------------------------------------------r",yes);
            }
            
            //was used for testing: if (yes || pid.equals("uuid:85560f80-355c-11e3-8d9d-005056827e51") || pid.equals("uuid:3e1564a0-2ce1-11e3-a5bb-005056827e52")) {
            if (yes) {
                objectsCountSelected++;
				/* 
				aaaaaaaaaaaaaaaaaaaaaaaaaaaas
				TODO: Uncomment this after everything (programming, testing,...) will be finished.
                if (isUpdate) {
                    setPolicyPublic();
                }
				*/
            }
        } catch (Exception e) {
            writeError(e);
        }
    }
    
    private void setPolicyPublic() throws Exception {
        LOGGER.info("setPolicyPublic begin");
        PolicyServiceImpl.main(new String[]{"public", pid/*.substring(5)*/});
        LOGGER.info("setPolicyPublic end");
    }
    
    private static Document buildDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory
                .newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
    
    private String getDatastream(String dsId) {
        String r = null;
        try {
            FedoraResponse responseDs = getDatastreamDissemination(pid, dsId).execute();
            r = responseDs.getEntity(String.class);
        } catch (FedoraClientException e) {
            if (e.getStatus() != NOT_FOUND) {
                writeError(e);
            }
        }
        return r;
    }
    
    
    /*
    <policy xmlns="http://www.nsdl.org/ontologies/relationships#">policy:private</policy>
    
    xmlns:kramerius="http://www.nsdl.org/ontologies/relationships#"
    <kramerius:policy>policy:private</kramerius:policy>
    */
    private boolean isPolicyPrivate() throws Exception {
        boolean r = false;
        String ds = getDatastream("RELS-EXT");
        if (ds != null) {
            Document d = buildDocument(ds);
            try {
                String p = xpath.compile(
                        "/*[local-name() = 'RDF']/" +
                        "*[local-name() = 'Description']/" +
                        "*[local-name() = 'policy' and namespace-uri() = 'http://www.nsdl.org/ontologies/relationships#']"
                        ).evaluate(d);
                if (p == null || p.isEmpty()) {
                    writeError("policy not found");
                } else {
                    r = p.equals("policy:private");
                }
            } catch (Exception e) {
                writeError(e);
            }
        }
        return r;
    }
    
    private String getTitleInfo(String titleOrSubtitle) throws Exception {
        return xpath.compile(
                "/*[local-name() = 'modsCollection' and namespace-uri() = namespace-uri(/*)]/" +
                "*[local-name() = 'mods' and namespace-uri() = namespace-uri(/*)]/" +
                "*[local-name() = 'titleInfo' and namespace-uri() = namespace-uri(/*)]/" +
                "*[local-name() = '" + titleOrSubtitle + "' and namespace-uri() = namespace-uri(/*)]")
                .evaluate(doc);
    }
    
    private XPathExpression getExprDateIssued(String elementName, String childName) throws Exception {
        return xpath.compile(
            "/*[local-name() = 'modsCollection' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = 'mods' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = '" + elementName + "' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = '" + childName + "' and namespace-uri() = namespace-uri(/*)]");
    }
    
    private boolean isDateIssuedOk() throws Exception {
        boolean r = true;
        boolean searchDateInPart = false;
        try {
            dateIssuedNL = null;
            try {
                dateIssuedNL = (NodeList) getExprDateIssued("originInfo", "dateIssued").evaluate(
                        doc, XPathConstants.NODESET);
            } catch (Exception e) {
                writeError(e);
                searchDateInPart = true;
            }

            if (dateIssuedNL == null || dateIssuedNL.getLength() == 0) {
                searchDateInPart = true;
            }
                   
            if (searchDateInPart) {
                try {
                    dateIssuedNL = (NodeList) getExprDateIssued("part", "date").evaluate(
                            doc, XPathConstants.NODESET);
                } catch (Exception e) {
                    writeError(e);
                    r = false;
                }
            }
            
            if (dateIssuedNL != null && dateIssuedNL.getLength() > 0) {
                int i = 0;
                while ((r == true) &&  (i < dateIssuedNL.getLength())) {
                    Element element = (Element) dateIssuedNL.item(i);
                    switch (DateEvaluator.evaluate(false, element).getResult()) {
                    case DateResult.YES:
                        setAreDatesAlmostStandard(true);
                        break;
                    case DateResult.YES_NONSTANDARD:
                        setAreDatesAlmostStandard(false);
                        break;
                    case DateResult.NO:
                        r = false;
                        writeWarning("date issued: " + element.getTextContent());
                        break;
                    }
                    i++;
                }
            } else {
                r = false;
            }
            
        } catch (Exception e) {
            writeError(e);
            r = false;
        }
        return r;
    }
    
    private XPathExpression getExprNames() throws Exception {
        return xpath.compile(
            "/*[local-name() = 'modsCollection' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = 'mods' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = 'name' and namespace-uri() = namespace-uri(/*)]");
    }
    
    /*
    /*[local-name() = 'modsCollection' and namespace-uri() = namespace-uri(/*)]/*[local-name() = 'mods' and namespace-uri() = namespace-uri(/*)]/*[local-name() = 'name' and namespace-uri() = namespace-uri(/*)]/*[local-name() = 'namePart' and namespace-uri() = namespace-uri(/*) and @type='date']
    */
    
    private XPathExpression getExprDatesInNameParts() throws Exception {
        return xpath.compile(
            "*[local-name() = 'namePart' and namespace-uri() = namespace-uri(/*) and @type='date']");
    }
    
    private boolean areDatesInNamePartsOk() throws Exception {
        boolean r = true;
        ignore = true;
        try {
            NodeList nlName = (NodeList) getExprNames().evaluate(doc, XPathConstants.NODESET);
            
            int i = 0;
            while ((r == true) &&  (i < nlName.getLength())) {
                Node node = nlName.item(i);
                NodeList nlNamePartDate = (NodeList) getExprDatesInNameParts().evaluate(node, XPathConstants.NODESET);
                if (nlNamePartDate.getLength() == 0) {
                    r = false;
                }
                
                int i2 = 0;
                while ((r == true) &&  (i2 < nlNamePartDate.getLength())) {
                    ignore = false;
                    //System.out.println(nlNamePartDate.item(i2).getTextContent());
                    Element element = (Element) nlNamePartDate.item(i2);
                    switch (DateEvaluator.evaluate(true, element).getResult()) {
                    case DateResult.YES:
                        setAreDatesAlmostStandard(true);
                        break;
                    case DateResult.YES_NONSTANDARD:
                        setAreDatesAlmostStandard(false);
                        break;
                    case DateResult.NO:
                        r = false;
                        writeWarning("namePart/date: " + element.getTextContent());
                        break;
                    }
                    i2++;
                }
                
                i++;
            }
            
            if (ignore) {
                r = false;
            }
        } catch (Exception e) {
            writeError(e);
            r = false;
        }
        return r;
    }
    
    private void setAreDatesAlmostStandard(boolean isNewDateAlmostStandard) {
        if (!isNewDateAlmostStandard) {
            areDatesAlmostStandard = false;
        }
    }

    private XPathExpression getExprNameParts() throws Exception {
        return xpath.compile(
            "/*[local-name() = 'modsCollection' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = 'mods' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = 'name' and namespace-uri() = namespace-uri(/*)]/" +
            "*[local-name() = 'namePart' and namespace-uri() = namespace-uri(/*)]");
    }
    
    private void logDatesIssued(boolean yes) throws Exception {
        for (int i = 0; i < dateIssuedNL.getLength(); i++) {
            Element element = (Element) dateIssuedNL.item(i);
            String s = DateResult.getAttrQualifierApproximate(element) 
                    + "date issued" + ":-----:     " + element.getTextContent(); 
            logYesNo(s, yes);
        }
    }
    
    private void logNameParts(boolean yes) throws Exception {
        NodeList nl = (NodeList) getExprNameParts().evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String s = "namePart/" + ((Element) node).getAttribute("type") 
                    + ":-----:     " + node.getTextContent(); 
            logYesNo(s, yes);
        }
    }
    
    private void writeWarning(String s) {
        outputNo.println(pid + " -----------------------warning begin");
        outputNo.println(s);
        outputNo.println(pid + " -----------------------warning end");
    }
    private void writeError(String s) {
        outputNo.println(pid + " -----------------------error begin");
        outputNo.println(s);
        outputNo.println(pid + " -----------------------error end");
    }
    private void writeError(Throwable e) {
        outputNo.println(pid + " -----------------------error exception begin");
        e.printStackTrace(outputNo);
        e.printStackTrace();
        outputNo.println(pid + " -----------------------error exception end");
    }
    
    private static void throwRuntimeException(Exception e) {
        throw new RuntimeException(e.getMessage(), e);
    }

}




















































