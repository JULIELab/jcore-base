/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.NavException;
import com.ximpleware.VTDException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import de.julielab.xml.JulieXMLTools;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class NxmlDocumentParser extends NxmlParser {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(NxmlDocumentParser.class);
    protected JCas cas;
    private Map<String, NxmlElementParser> parserRegistry;
    private DefaultElementParser defaultElementParser;
    private Map<String, Map<String, Object>> tagProperties;
    private Tagset tagset;

    public void reset(File nxmlFile, JCas cas) throws DocumentParsingException {
        reset(nxmlFile.toURI(), cas);
    }

    public void reset(URI uri, JCas cas) throws DocumentParsingException {
        boolean gzipped = uri.toString().endsWith(".gz") || uri.toString().endsWith(".gzip");
        try {
            log.debug("Reading from URL {}", uri.toURL());
            InputStream is = uri.toURL().openStream();
            if (gzipped)
                is = new GZIPInputStream(is);
            reset(is, cas);
        } catch (IOException e) {
            throw new DocumentParsingException(e);
        }
    }

    public void reset(InputStream is, JCas cas) throws DocumentParsingException {
        this.cas = cas;
        try {
            byte[] bytes = JulieXMLTools.readStream(is, 8192);

            VTDGen vg = new VTDGen();
            vg.setDoc(bytes);
            // If we don't set this to true, some whitespaces, for example
            // directly after closing tags, would be omitted. We don't want
            // this, the NXML format is very specific in its whitespaces.
            vg.enableIgnoredWhiteSpace(true);
            vg.parse(false);
            vn = vg.getNav();
            setTagset();
            setupParserRegistry();
        } catch (IOException | VTDException e) {
            throw new DocumentParsingException(e);
        }

    }

    /**
     * Reads the doctype of the XML input file and sets the appropriate tagset
     * enum element from {@link Tagset}.
     *
     * @throws NavException
     * @throws DocTypeNotFoundException
     */
    private void setTagset() throws NavException, DocTypeNotFoundException, DocTypeNotSupportedException {
        for (int i = 0; i < vn.getTokenCount(); i++) {
            if (vn.getTokenType(i) == VTDNav.TOKEN_DTD_VAL) {
                String docType = StringUtils.normalizeSpace(vn.toString(i)).replaceAll("'", "\"");
                if (docType.contains("JATS-archivearticle1.dtd"))
                    tagset = Tagset.JATS_1_0;
                else if (docType.contains("JATS-archivearticle1-mathml3.dtd"))
                    tagset = Tagset.JATS_1_2_MATH_ML_3;
                else if (docType.contains("journalpublishing.dtd") || docType.contains("archivearticle.dtd"))
                    tagset = Tagset.NLM_2_3;
                else if (docType.contains("journalpublishing3.dtd") || docType.contains("archivearticle3.dtd"))
                    tagset = Tagset.NLM_3_0;
                else
                    throw new DocTypeNotSupportedException("Unsupported document type: "  + docType);
                return;
            }
        }
        throw new DocTypeNotFoundException("Could not find a doctype.");
    }

    private void setupParserRegistry() {
        this.defaultElementParser = new DefaultElementParser(this);
        parserRegistry = new HashMap<>();
        parserRegistry.put("front", new FrontParser(this));
        // filters for authors currently (thus, omits editors, for example)
        parserRegistry.put("contrib-group", new ContribGroupParser(this));
        // does only create AuthorInfo annotations and expects the contrib-type
        // "author"
        parserRegistry.put("contrib", new ContribParser(this));
        parserRegistry.put("sec", new SectionParser(this));
        parserRegistry.put("table-wrap", new TableWrapParser(this));
        parserRegistry.put("fig", new FigParser(this));
        parserRegistry.put("list", new ListParser(this));
        parserRegistry.put("xref", new XRefParser(this));
    }

    public VTDNav getVn() {
        return vn;
    }

    public Tagset getTagset() {
        return tagset;
    }

    /**
     * The parser registry is a simple map that associates XML element names
     * with a parser for the respective element type. When parsing a document,
     * all elements are traversed in a depth-first fashion. For each element,
     * this registry is checked for a parser handling the current element. If
     * one is found, this parser is used for the element. Otherwise, the
     * {@link DefaultElementParser} is employed.
     *
     * @return The parser registry.
     */
    public Map<String, NxmlElementParser> getParserRegistry() {
        return parserRegistry;
    }

    public ElementParsingResult parse() throws ElementParsingException, DocumentParsingException {
        String startingElement = moveToNextStartingTag();
        assert startingElement.equals("article") : "Did not encounter an article element as first start element";
        return getParser(startingElement).parse();
    }

    public NxmlElementParser getParser(String tagName) {
        NxmlElementParser nxmlElementParser = parserRegistry.getOrDefault(tagName, defaultElementParser);
        return nxmlElementParser;
    }

    public Map<String, Object> getTagProperties(String tag) {
        if (tagProperties != null) {
            return tagProperties.getOrDefault(tag, Collections.emptyMap());
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public void loadElementPropertyFile(String file) throws IOException {
        Yaml yaml = new Yaml();
        InputStream is = getClass().getResourceAsStream(file.startsWith("/") ? file : "/" + file);
        if (is == null && new File(file).exists())
            is = new FileInputStream(file);
        if (is == null)
            throw new IOException("Resource " + file + " could neither be found as a file nor as a classpath resource");
        Iterable<Object> allProperties = yaml.loadAll(is);
        for (Object tag : allProperties) {
            tagProperties = (Map<String, Map<String, Object>>) tag;
        }

    }

    /**
     * The tagset the parsed document is modeled after. The options are very
     * similar to each other but may exhibit differences in details.
     * <p>
     * Just for an overview, in May 2019 the distribution of doctypes on the
     * PMC OA set looked like this:
     *
     * <pre>
     * 2294794 PUBLIC "-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.0 20120330//EN" "JATS-archivearticle1.dtd">
     *       1 PUBLIC "-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.0 20120330//EN" "/pmc/load/converter3/dtd/niso-Z39.96/1.0/JATS-archivearticle1.dtd">
     *     198 PUBLIC "-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD with MathML3 v1.2 20190208//EN" "JATS-archivearticle1-mathml3.dtd">
     *  128798 PUBLIC "-//NLM//DTD Journal Archiving and Interchange DTD v2.3 20070202//EN" "archivearticle.dtd">
     * </pre>
     * <p>
     * As can be seen, the tag library v3.0 isn't used at all.
     * </p>
     *
     * @author faessler
     * @see <url>https://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html</url>
     */
    public enum Tagset {
        /**
         * NISO JATS Journal Publishing DTD, v. 1.0
         *
         * @see <url>https://jats.nlm.nih.gov/publishing/tag-library/1.0/index.html</url>
         */
        JATS_1_0,
        /**
         * NISO JATS Journal Publishing DTD, v. 1.2 with MathML3.
         *
         * @see <url>https://jats.nlm.nih.gov/publishing/tag-library/1.2/index.html</url>
         */
        JATS_1_2_MATH_ML_3,
        /**
         * NLM Journal Publishing DTD v. 2.3
         *
         * @see <url>https://dtd.nlm.nih.gov/publishing/tag-library/2.3/index.html</url>
         */
        NLM_2_3,
        /**
         * NLM Journal Publishing DTD v. 3.0
         *
         * @see <url>https://dtd.nlm.nih.gov/publishing/tag-library/3.0/index.html</url>
         */
        NLM_3_0
    }
}
