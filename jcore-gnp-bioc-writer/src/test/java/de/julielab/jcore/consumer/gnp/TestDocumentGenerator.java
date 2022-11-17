package de.julielab.jcore.consumer.gnp;

import de.julielab.jcore.types.*;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

public class TestDocumentGenerator {

    public static JCas createTestJCas() throws UIMAException {
        return JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types");
    }

    public static JCas prepareCas(int docId) throws UIMAException {
        JCas jCas = createTestJCas();
        return prepareCas(jCas, docId);
    }

    public static JCas prepareCas(JCas jCas, int docId) {
        Header h = new de.julielab.jcore.types.pubmed.Header(jCas);
        h.setDocId(String.valueOf(docId));
        h.addToIndexes();

        StringBuilder sb = new StringBuilder();
        String ls = System.getProperty("line.separator");
        int currentBegin = sb.length();
        sb.append("This is the title of document ").append(docId).append(".");
        Title t = new Title(jCas, currentBegin, sb.length());
        t.setTitleType("document");
        t.addToIndexes();
        currentBegin = sb.length();
        sb.append("This abstract section belongs to document ").append(docId).append(".");
        AbstractSectionHeading ash1 = new AbstractSectionHeading(jCas);
        ash1.setLabel("BACKGROUND");
        ash1.setTitleType("abstract");
        AbstractSection as1 = new AbstractSection(jCas, currentBegin, sb.length());
        as1.setAbstractSectionHeading(ash1);
        sb.append(ls);
        currentBegin = sb.length();
        sb.append("There are certainly some results reported by document ").append(docId).append(".");
        AbstractSectionHeading ash2 = new AbstractSectionHeading(jCas);
        ash2.setLabel("RESULTS");
        ash2.setTitleType("abstract");
        AbstractSection as2 = new AbstractSection(jCas, currentBegin, sb.length());
        as2.setAbstractSectionHeading(ash2);
        AbstractText at = new AbstractText(jCas, as1.getBegin(), as2.getEnd());
        at.setStructuredAbstractParts(JCoReTools.addToFSArray(JCoReTools.addToFSArray(null, as1), as2));
        at.addToIndexes();
        sb.append(ls);
        currentBegin = sb.length();
        sb.append("INTRODUCTION This is section 1, paragraph 1 of document ").append(docId).append(".");
        SectionTitle st1 = new SectionTitle(jCas, currentBegin, currentBegin + 12);
        st1.setTitleType("section");
        Section s1 = new Section(jCas, currentBegin, sb.length());
        st1.addToIndexes();
        s1.setSectionHeading(st1);
        s1.addToIndexes();
        // paragraphs do not include the heading
        Paragraph p11 = new Paragraph(jCas, s1.getBegin() + 13, s1.getEnd());
        p11.addToIndexes();
        currentBegin = sb.length();
        sb.append("This is a second paragraph in the first section.");
        Paragraph p12 = new Paragraph(jCas, currentBegin, sb.length());
        p12.addToIndexes();
        currentBegin = sb.length();
        int objectBegin = sb.length();
        sb.append("Let this be table content.");
        currentBegin = sb.length();
        sb.append("Tab1.");
        Title tabTitle = new Title(jCas, currentBegin, sb.length());
        tabTitle.setTitleType("table");
        tabTitle.addToIndexes();
        currentBegin = sb.length();
        sb.append("This is the table1 caption.");
        Caption tCap = new Caption(jCas, currentBegin, sb.length());
        tCap.setCaptionType("table");
        tCap.addToIndexes();
        Table tab = new Table(jCas, objectBegin, sb.length());
        tab.setObjectTitle(tabTitle);
        tab.setObjectCaption(tCap);
        tab.addToIndexes();
        tab.addToIndexes();
        jCas.setDocumentText(sb.toString());
        return jCas;
    }
}
