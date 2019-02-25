package de.julielab.jcore.reader.ct.ctmodel;


import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    USEFUL links:
        https://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
        http://www.journaldev.com/1198/java-sax-parser-example
        http://stackoverflow.com/questions/7209946/using-sax-parser-how-do-you-parse-an-xml-file-which-has-same-name-tags-but-in-d
 */
public class ClinicalTrialXmlHandler extends DefaultHandler {

    public static Pattern INCL_EXCL_PATTERN = Pattern.compile("(?:[Ii]nclusion [Cc]riteria:?)?(.+?)(?:[Ee]xclusion [Cc]riteria:?(.+))?$");

    private static final String TAG_START = "clinical_study";
    private static final String TAG_ID = "nct_id";

    private final Stack<String> tagsStack = new Stack<String>();
    private final StringBuilder tempVal = new StringBuilder();

    private ClinicalTrial clinicalTrial;

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) {
        pushTag(qName);
        tempVal.setLength(0);
        if (TAG_START.equalsIgnoreCase(qName)) {
            clinicalTrial = new ClinicalTrial();
        }
    }

    @Override
    public void endElement(String uri, String localName,
                           String qName) {

        String tag = peekTag();
        if (!qName.equals(tag)) {
            throw new InternalError();
        }

        popTag();
        String parentTag = peekTag();

        if (tag.equalsIgnoreCase(TAG_ID)) {
            clinicalTrial.id = tempVal.toString().trim();
        }

        if (tag.equalsIgnoreCase("brief_title")) {
            clinicalTrial.brief_title = tempVal.toString().trim();
        }

        if (tag.equalsIgnoreCase("official_title")) {
            clinicalTrial.official_title = tempVal.toString().trim();
        }

        if (tag.equalsIgnoreCase("textblock") &&
                parentTag.equalsIgnoreCase("brief_summary")) {
            clinicalTrial.summary = cleanup(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("textblock") &&
                parentTag.equalsIgnoreCase("detailed_description")) {
            clinicalTrial.description = cleanup(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("study_type")) {
            clinicalTrial.studyType = tempVal.toString().trim();
        }

        if (tag.equalsIgnoreCase("intervention_model") &&
                parentTag.equalsIgnoreCase("study_design_info")) {
            clinicalTrial.interventionModel = tempVal.toString().trim();
        }

        if (tag.equalsIgnoreCase("primary_purpose") &&
                parentTag.equalsIgnoreCase("study_design_info")) {
            clinicalTrial.primaryPurpose = tempVal.toString().trim();
        }

        if (tag.equalsIgnoreCase("measure") &&
                isOutcome(parentTag)) {
            clinicalTrial.outcomeMeasures.add(cleanup(tempVal.toString().trim()));
        }

        if (tag.equalsIgnoreCase("description") &&
                isOutcome(parentTag)) {
            clinicalTrial.outcomeDescriptions.add(cleanup(tempVal.toString().trim()));
        }

        if (tag.equalsIgnoreCase("condition")) {
            clinicalTrial.conditions.add(cleanup(tempVal.toString().trim()));
        }

        if (tag.equalsIgnoreCase("intervention_type") &&
                parentTag.equalsIgnoreCase("intervention")) {
            clinicalTrial.interventionTypes.add(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("intervention_name") &&
                parentTag.equalsIgnoreCase("intervention")) {
            clinicalTrial.interventionNames.add(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("description") &&
                parentTag.equalsIgnoreCase("arm_group")) {
            clinicalTrial.armGroupDescriptions.add(cleanup(tempVal.toString().trim()));
        }

        if (tag.equalsIgnoreCase("gender")) {
            clinicalTrial.sex = parseSex(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("minimum_age")) {
            clinicalTrial.minAge = parseMinimumAge(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("maximum_age")) {
            clinicalTrial.maxAge = parseMaximumAge(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("criteria")) {
            clinicalTrial.inclusion = parseInclusion(tempVal.toString().trim());
            clinicalTrial.exclusion = parseExclusion(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("keyword")) {
            clinicalTrial.keywords.add(tempVal.toString().trim());
        }

        if (tag.equalsIgnoreCase("mesh_term")) {
            clinicalTrial.meshTags.add(tempVal.toString().trim());
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        tempVal.append(ch, start, length);
    }

    public void startDocument() {
        pushTag("");
    }

    public ClinicalTrial getClinicalTrial() {
        return clinicalTrial;
    }

    private void pushTag(String tag) {
        tagsStack.push(tag);
    }

    private String popTag() {
        return tagsStack.pop();
    }

    private String peekTag() {
        return tagsStack.peek();
    }

    private static Set<String> parseSex(String sex) {

        Set<String> sexSet = new HashSet<>();

        try {
            switch (sex.toLowerCase()) {
                case "male":
                    sexSet.add("male");
                    break;
                case "female":
                    sexSet.add("female");
                    break;
                case "all":
                    sexSet.add("male");
                    sexSet.add("female");
                    break;
                default:
                    throw new IllegalArgumentException("Invalid sex: " + sex);
            }

            return sexSet;
        }
        catch (Exception e) {
            sexSet.add("male");
            sexSet.add("female");
            return sexSet;
        }
    }

    private static int parseMinimumAge(String age) {
        try {
            return Integer.parseInt(age.replaceAll("[^0-9]+", ""));
        }
        catch (Exception e) {
            return 0;
        }
    }

    private static int parseMaximumAge(String age) {
        try {
            return Integer.parseInt(age.replaceAll("[^0-9]+", ""));
        }
        catch (Exception e) {
            return 100;
        }
    }

    private static String parseInclusion(String inclusionExclusion) {
        try {
            Matcher m = INCL_EXCL_PATTERN.matcher(cleanup(inclusionExclusion));
            m.find();
            return (m.group(1));
        }
        catch(Exception e) {
            return "";
        }
    }

    private static String parseExclusion(String inclusionExclusion) {
        try {
            Matcher m = INCL_EXCL_PATTERN.matcher(cleanup(inclusionExclusion));
            m.find();
            return (m.group(2));
        }
        catch(Exception e) {
            return "";
        }
    }

    private static boolean isOutcome(String tag) {
        return tag.equalsIgnoreCase("primary_outcome") ||
                tag.equalsIgnoreCase("secondary_outcome") ||
                tag.equalsIgnoreCase("other_outcome");
    }

    private static String cleanup(String text) {
        text = text.replace("\n", "").replace("\r", "");
        text = text.replace("\t", "");
        //text = text.replace("-", "");
        text = text.trim().replaceAll(" +", " ");
        return text;
    }
}
