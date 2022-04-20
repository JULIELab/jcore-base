package de.julielab.jcore.reader.xmlmapper.mapper;

import com.ximpleware.VTDNav;
import de.julielab.jcore.types.AbstractSection;
import de.julielab.jcore.types.AbstractSectionHeading;
import de.julielab.jcore.types.pubmed.AbstractText;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;

/**
 * This class expects a MEDLINE structured abstract pointed to by the
 * {@link PartOfDocument} given to the
 * {@link #parseDocumentPart(VTDNav, PartOfDocument, int, JCas, byte[])} method or just plain
 * text at the XPath location. It parses the abstract sections and creates the
 * text as well as abstract section annotations and the AbstractText annotation
 * itself, containing all its sections. The section titles are also created as
 * annotations but not added to the text. This should be done by a consuming
 * component, if required. <br>
 * <em>NOTE</em>: Using this parser, the AbstractText annotation is already
 * created and should not be set in the mapping file.
 *
 * @author faessler
 */
public class StructuredAbstractParser implements DocumentTextPartParser {

    private static final boolean newlineBetweenSections = true;

    public List<String> parseDocumentPart(VTDNav vn, PartOfDocument docTextPart, int offset, JCas jCas,
                                          byte[] identifier) {
        String baseXPath = docTextPart.getXPath();

        List<Map<String, String>> fields = new ArrayList<>();
        Map<String, String> field = new HashMap<>();
        field.put(JulieXMLConstants.NAME, "Label");
        field.put(JulieXMLConstants.XPATH, "@Label");
        fields.add(field);

        field = new HashMap<>();
        field.put(JulieXMLConstants.NAME, "NlmCategory");
        field.put(JulieXMLConstants.XPATH, "@NlmCategory");
        fields.add(field);

        field = new HashMap<>();
        field.put(JulieXMLConstants.NAME, "AbstractText");
        field.put(JulieXMLConstants.XPATH, ".");
        fields.add(field);
        Iterator<Map<String, Object>> rowIterator = JulieXMLTools.constructRowIterator(vn, baseXPath + "/AbstractText",
                fields, new String(identifier));
        List<AbstractSection> abstractParts = new ArrayList<>();
        // for the text contents
        StringBuilder sb = new StringBuilder();

        int sectionOffset = offset;
        while (rowIterator.hasNext()) {
            Map<String, Object> abstractSectionData = rowIterator.next();
            String label = (String) abstractSectionData.get("Label");
            String nlmCategory = (String) abstractSectionData.get("NlmCategory");
            String abstractSectionText = (String) abstractSectionData.get("AbstractText");
            if (newlineBetweenSections) {
                // in case the last section was empty, we delete the trailing
                // newline
                if (sb.length() > 0 && StringUtils.isBlank(abstractSectionText)) {
                    sb.deleteCharAt(sb.length() - 1);
                    --sectionOffset;
                }
            }
            // comment in to add the structured abstract section labels to the text, e.g. "AIMS: ...", "BACKGROUND: ..."
//            if (null != label && !"unlabelled".equalsIgnoreCase(label))
//                sb.append(label).append(": ");
            sb.append(abstractSectionText);

            // if label and nlmCategory are null, there is no section heading;
            // most probably this just isn't a structured abstract
            if (null != label || null != nlmCategory) {
                AbstractSectionHeading abstractPartHeading = new AbstractSectionHeading(jCas);
                abstractPartHeading.setLabel(label);
                abstractPartHeading.setNlmCategory(nlmCategory);
                abstractPartHeading.setTitleType("abstractSection");
                abstractPartHeading.addToIndexes();

                AbstractSection abstractPart = new AbstractSection(jCas);
                abstractPart.setBegin(sectionOffset);
                sectionOffset += abstractSectionText.length();
                abstractPart.setEnd(sectionOffset);
                abstractPart.setAbstractSectionHeading(abstractPartHeading);
                abstractPart.addToIndexes();

                abstractParts.add(abstractPart);
            } else {
                sectionOffset += abstractSectionText.length();
            }

            // let's insert a line break after each section text
            if (newlineBetweenSections && sb.length() > 0 && rowIterator.hasNext()) {
                sb.append("\n");
                ++sectionOffset;
            }
        }

        // only create an abstract annotation if there actually is an abstract
        if (!abstractParts.isEmpty() || sectionOffset > offset) {
            if (sectionOffset == offset) {
                // there was no abstract but just empty abstract sections; decrement the offsets so we stay with existing document text
                --offset;
                --sectionOffset;
                for (AbstractSection section : abstractParts) {
                    section.setBegin(offset);
                    section.setEnd(offset);
                }
            }
            AbstractText abstractText = new AbstractText(jCas, offset, sectionOffset);
            abstractText.setAbstractType("main");
            if (abstractParts.size() > 0) {
                FSArray sectionsArray = new FSArray(jCas, abstractParts.size());
                for (int i = 0; i < abstractParts.size(); ++i)
                    sectionsArray.set(i, abstractParts.get(i));
                abstractText.setStructuredAbstractParts(sectionsArray);
            }
            abstractText.addToIndexes();
            return Collections.singletonList(sb.toString());
        }
        return Collections.emptyList();
    }

}
