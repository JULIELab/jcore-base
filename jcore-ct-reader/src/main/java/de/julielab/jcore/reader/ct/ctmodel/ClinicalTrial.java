package de.julielab.jcore.reader.ct.ctmodel;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

public class ClinicalTrial {

    public static Pattern INCL_EXCL_PATTERN = Pattern.compile("[Ii]nclusion [Cc]riteria:(.+)[Ee]xclusion [Cc]riteria:(.+)");

    public String id;
    public String brief_title;
    public String official_title;
    public String summary;
    public String description;
    public String studyType;    // TODO Refactor into enum
    public String interventionModel;    // TODO Refactor into enum
    public String primaryPurpose;  // TODO Refactor into enum
    public ArrayList<String> outcomeMeasures;
    public ArrayList<String> outcomeDescriptions;
    public ArrayList<String> conditions;
    public ArrayList<String> interventionTypes; // TODO Refactor into enum
    public ArrayList<String> interventionNames; // TODO Refactor into enum
    public ArrayList<String> armGroupDescriptions;
    public Set<String> sex;
    public int minAge;
    public int maxAge;
    public String inclusion;
    public String exclusion;
    public ArrayList<String> keywords;
    public ArrayList<String> meshTags;

    public ClinicalTrial() {
        this.outcomeMeasures = new ArrayList<>();
        this.outcomeDescriptions = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.interventionTypes = new ArrayList<>();
        this.interventionNames = new ArrayList<>();
        this.armGroupDescriptions = new ArrayList<>();
        this.keywords = new ArrayList<>();
        this.meshTags = new ArrayList<>();
    }

    public static ClinicalTrial fromXml(String xmlClinicalTrial){

        ClinicalTrialXmlHandler handler = new ClinicalTrialXmlHandler();

        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(xmlClinicalTrial, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(handler.getClinicalTrial());

    }
}
