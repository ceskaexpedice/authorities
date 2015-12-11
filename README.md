#Proces pro zpřístupňování děl v systému Kramerius dle roků úmrtí autorů a roků vydání

Proces posuzuje roky úmrtí autorů a roky vydání a na základě toho rozhoduje o možnosti zpřístupnění. Do jeho konfiguračních souborů je možno zadat vzory (např. "cca CC. - RR. stol.") a způsoby vyhodnocení (např. "RR*100+150") pro všelijaké velmi nestandardní tvary zadání roků úmrtí autorů a roků vydání (např. "cca 17. - 18. stol."). Proces je možno spustit s parametrem, aby nic ve Fedoře neměnil a pouze vytvořil výstupní soubory s informacemi, jak co vyhodnotil.

Po dokončení testování pouze s výstupními soubory:
Aby proces kromě vytváření výstupních souborů i rovnou zpřístupňoval, co je možno zpřístupnit, je potřeba v souboru FedoraIterator.java odkomentovat toto:

				/* 
				TODO: Uncomment this after testing will be finished.
                if (isUpdate) {
                    setPolicyPublic();
                }
				*/

##Začlenění tohoto procesu do Krameria je možno provést např. takto:

1) Po případném zbuildování Krameria (viz níže) a jeho umístění na server "Apache Tomcat" dát ".jar" soubor tohoto procesu do adresáře Krameria "search\WEB-INF\lib".

2) Do C:\.kramerius4\lp.xml přidat:

    <process>
        <id>fedoraiterator</id>
        <description>Zpřístupňování děl</description>
        <mainClass>cz.knav.fedora.client.FedoraIterator</mainClass>
        <standardOs>lrOut</standardOs>
        <errOs>lrErr</errOs>
        <parameters>C:/Zpristupnovani/</parameters>
    </process>

3) Vytvořit adresář "C:\Zpristupnovani" a dát do něj konfigurační soubory tohoto procesu (Parametry.xml, VzoryDatumUmrti.xml a VzoryRokVydani.xml) a podle potřeby si je upravit.

4) C:\Program Files\Apache Software Foundation\Tomcat X.X\webapps\search\WEB-INF\lib\common-X.X.X-SNAPSHOT.jar\res\configuration.properties:

Např. pod toto

fedora.topLevelModels=monograph,periodical,soundrecording,manuscript,map,sheetmusic

dát toto

fedora.modelsWithYearsAuthors=repository,monograph,monographunit,periodical,periodicalvolume,periodicalitem,article,manuscript,map,internalpart,sheetmusic,supplement,soundrecording,soundunit,track,graphic

Nebo v případě buildování Krameria si to upravit v Krameriovi před buildem.

5) Nyní by již měl jít tento proces stejně jako ostatní procesy Krameria pouštět vzdáleně přes "Remote API" Krameria (viz dokumentace Krameria) - např. přes HTTP pomocí curl. Následující body se již týkají úprav zdrojových souborů Krameria, aby se v menu grafického rozhraní Krameria objevila položka pro spouštění tohoto procesu.

6) Do "kramerius\common\src\main\java\cz\incad\kramerius\security\SecuredActions.java" přidat "FEDORAITERATOR("fedoraiterator"),"

7) Do "kramerius\search\src\java\cz\incad\Kramerius\exts\menu\main\impl\adm\items" vytvořit soubor ParametrizedFedoraIterator.java s následujícím obsahem:

    package cz.incad.Kramerius.exts.menu.main.impl.adm.items;

    import java.io.IOException;

    import cz.incad.Kramerius.exts.menu.main.impl.AbstractMainMenuItem;
    import cz.incad.Kramerius.exts.menu.main.impl.adm.AdminMenuItem;

    public class ParametrizedFedoraIterator extends AbstractMainMenuItem implements AdminMenuItem {

    @Override
    public boolean isRenderable() {
        return (hasUserAllowedPlanProcess("fedoraiterator"));
    }

    @Override
    public String getRenderedItem() throws IOException {
        return renderMainMenuItem(
            "javascript:noParamsProcess('fedoraiterator'); javascript:hideAdminMenu();",
            "administrator.menu.dialogs.fedoraiterator.title", false);
    }
    }

8) Do "kramerius\search\src\java\cz\incad\Kramerius\exts\menu\main\guice\MainMenuConfiguration.java" přidat následující řádek:

        adminItems.addBinding().to(ParametrizedFedoraIterator.class);

9) Do "kramerius\search\src\java\labels_cs.properties" a do "kramerius\search\src\java\labels.properties" přidat:

"administrator.menu.dialogs.fedoraiterator.title=Hromadné zpřístupňování děl..."

Po zbuildování Krameria a jeho umístění na server "Apache Tomcat" by se v menu grafického rozhraní Krameria měla objevit právě tato položka pro spouštění tohoto procesu.

##Konfigurační soubory procesu

Tento proces má tři konfigurační soubory - Parametry.xml, VzoryDatumUmrti.xml a VzoryRokVydani.xml. Tyto konfigurační soubory procesu se umisťují do pracovního adresáře procesu. Cestu na svůj pracovní adresář (např. "C:\Zpristupnovani") dostává proces jako parametr - viz popis výše.

Soubor Parametry.xml obsahuje parametry procesu v následujících XML elementech:

V elementu SpusteniPovoleno se uvádí hodnota "ano" nebo "ne", která určuje, zda administrátor povolil spuštění procesu. Pokud je zde hodnota "ne", tak proces po spuštění pouze zapíše do svého výstupního souboru, že je parametr SpusteniPovoleno nastaven na hodnotu "ne" a ukončí se. Pokud je hodnota "ano", tak se proces normálně rozběhne a při svém ukončení nastaví tuto hodnotu na "ne".

V elementu Zpristupnovat se uvádí hodnota "ano" nebo "ne", která určuje, zda má proces nastavovat příznaky zpřístupnění, nebo zda má proces pouze vytvářet výstupní soubory s informacemi, co by bylo možno zpřístupnit. Výstupní soubory proces vytváří vždy - když je zde hodnota "ano", tak proces navíc nastavuje příznaky zpřístupnění.

V elementu ZmenenePred se uvádí datum a čas ve formátu "RRRR-MM-DD-HH:MM:SS.mmm". Proces prochází pouze objekty, které mají datum a čas poslední modifikace starší než zde uvedený datum a čas.

V elementu LetAutori se uvádí kolik let muselo uplynout od smrti všech autorů díla, aby mohlo být dílo zpřístupněno.

V elementu LetVydani se uvádí kolik let muselo uplynout od roku vydání díla, aby mohlo být dílo zpřístupněno.

V souborech VzoryDatumUmrti.xml a VzoryRokVydani.xml je seznam elementů VzorDefinice. Každý element VzorDefinice obsahuje následující XML elementy:

V elementu IAtributApproximate se uvádí hodnota "ano" nebo "ne", zda je možno daný vzor používat i na data, která se nacházejí v XML elementu s atributem "qualifier", jehož hodnota je "approximate" - tzn., že se jedná pouze o přibližný údaj.

V elementu "Cislice" se uvádí znak (např. C), který v níže uvedeném elementu "Vzor" zastupuje libovolnou číslici.

V elementu CisliceProVypocet (např. R) se uvádí znak, který v níže uvedeném elementu "Vzor" zastupuje číslici, která se používá ve výrazu v níže uvedeném elementu PrevodNaRok pro výpočet roku, který se má posoudit. Stejný znak se používá i v uvedeném výrazu v elementu "PrevodNaRok". Např. dvojciferné číslo je tedy reprezentováno dvěma těmito znaky (tedy např. RR).

V elementu "Vzor" se uvádí vzor, v jakém tvaru mohou být data.

V elementu PrevodNaRok se uvádí výraz, jak data, která odpovídají vzoru, převádět na rok (úmrtí, nebo vydání), který se pak vyhodnotí.

Příklad souboru Parametry.xml:

    <?xml version="1.0" encoding="UTF-8"?><Parametry>
    <SpusteniPovoleno>ne</SpusteniPovoleno>
    <Zpristupnovat>ne</Zpristupnovat>
    <ZmenenePred>3333-03-25-23:59:02.222</ZmenenePred>
    <LetAutori>70</LetAutori>
    <LetVydani>50</LetVydani>
    </Parametry>

Příklad souboru VzoryDatumUmrti.xml:

    <?xml version="1.0" encoding="UTF-8"?>
    <Vzory>
    <VzorDefinice>
    <IAtributApproximate>ne</IAtributApproximate>
    <Cislice>C</Cislice>
    <CisliceProVypocet>R</CisliceProVypocet>
    <Vzor>cca CC. - RR. stol.</Vzor>
    <PrevodNaRok>RR*100+150</PrevodNaRok>
    </VzorDefinice>
    <VzorDefinice>
    <IAtributApproximate>ne</IAtributApproximate>
    <Cislice>C</Cislice>
    <CisliceProVypocet>R</CisliceProVypocet>
    <Vzor>asi CC. - RR. stol.</Vzor>
    <PrevodNaRok>RR*100+150</PrevodNaRok>
    </VzorDefinice>
    </Vzory>

Příklad souboru VzoryRokVydani.xml:

    <?xml version="1.0" encoding="UTF-8"?>
    <Vzory>
    <VzorDefinice>
    <IAtributApproximate>ne</IAtributApproximate>
    <Cislice>C</Cislice>
    <CisliceProVypocet>R</CisliceProVypocet>
    <Vzor>cca CC. - RR. stol.</Vzor>
    <PrevodNaRok>RR*100+150</PrevodNaRok>
    </VzorDefinice>
    <VzorDefinice>
    <IAtributApproximate>ne</IAtributApproximate>
    <Cislice>C</Cislice>
    <CisliceProVypocet>R</CisliceProVypocet>
    <Vzor>asi CC. - RR. stol.</Vzor>
    <PrevodNaRok>RR*100+150</PrevodNaRok>
    </VzorDefinice>
    </Vzory>

##Výstupní soubory procesu

Výstupní soubory vytváří proces do adresáře "Vystup", který se nachází v pracovním adresáři procesu. Při každém spuštění proces vytvoří v adresáři "Vystup" podadresář, jehož jméno je čas v milisekundách, jak je reprezentovaný v programovacím jazyce Java.

Proces vytváří čtyři typy (očíslovaných pořadovým číslem) výstupních textových textových souborů:

Output1.txt - Vše, co prošel.

OutputNo1.txt - Co vyhodnotil jako nezpřístupňovat.

OutputYes1.txt - Co vyhodnotil jako zpřístupňovat a všechny příslušné datumy jsou v tom v "docela normálním" standardním nebo téměř standardním tvaru.

OutputYesNonstandard1.txt - Co vyhodnotil, že by se možná mohlo zpřístupnit, ale některý z datumů je zapsán nějakým divným způsobem (např. "zemř. 1850")

Výstupní soubory typu OutputYesNonstandard1.txt a případně i typu OutputNo1.txt slouží administrátorovi pro vytvoření vzorů a způsobů vyhodnocení všelijakých tvarů datumů. Např.:

V OutputYesNonstandard4.txt uvidí administrátor:

činný 15. století-16. století

Do konfiguračního souboru procesu VzoryDatumUmrti.xml (viz výše) tedy přidá toto:

    <VzorDefinice>
    <IAtributApproximate>ne</IAtributApproximate>
    <Cislice>C</Cislice>
    <CisliceProVypocet>R</CisliceProVypocet>
    <Vzor>činný CC. století-RR. století</Vzor>
    <PrevodNaRok>RR*100+150</PrevodNaRok>
    </VzorDefinice>

Proces pak bude při příštím běhu informace o dílech, která vyhověla některému ze vzorů a vyšel z toho dostatečně starý rok, dávat již do výstupních souborů typu OutputYes1.txt a pokud bude proces spuštěn s parametrem, že má rovnou zpřístupňovat vyhovující díla, tak bude takováto díla také rovnou zpřístupňovat.

