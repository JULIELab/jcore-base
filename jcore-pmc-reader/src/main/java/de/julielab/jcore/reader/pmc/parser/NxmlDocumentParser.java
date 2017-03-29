package de.julielab.jcore.reader.pmc.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.ximpleware.NavException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

public class NxmlDocumentParser extends NxmlParser {

	private static final Logger log = LoggerFactory.getLogger(NxmlDocumentParser.class);

	/**
	 * The tagset the parsed document is modeled after. The options are very
	 * similar to each other but may exhibit differences in details.
	 * 
	 * @author faessler
	 * @see https://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html
	 */
	public enum Tagset {
		/**
		 * NISO JATS Journal Publishing DTD, v. 1.0
		 * 
		 * @see https://jats.nlm.nih.gov/publishing/tag-library/1.0/index.html
		 */
		JATS_1_0,
		/**
		 * NLM Journal Publishing DTD v. 2.3
		 * 
		 * @see https://dtd.nlm.nih.gov/publishing/tag-library/2.3/index.html
		 */
		NLM_2_3,
		/**
		 * NLM Journal Publishing DTD v. 3.0
		 * 
		 * @see https://dtd.nlm.nih.gov/publishing/tag-library/3.0/index.html
		 */
		NLM_3_0
	}

	private Map<String, NxmlElementParser> parserRegistry;
	private DefaultElementParser defaultElementParser;
	private Map<String, Map<String, Object>> tagProperties;
	private Tagset tagset;
	private File nxmlFile;
	protected JCas cas;

	public void reset(File nxmlFile, JCas cas) throws DocumentParsingException {
		this.nxmlFile = nxmlFile;
		this.cas = cas;
		try {
			VTDGen vg = new VTDGen();
			if (nxmlFile.getName().endsWith(".gz"))
				vg.parseGZIPFile(nxmlFile.getAbsolutePath(), false);
			else
				vg.parseFile(nxmlFile.getAbsolutePath(), false);
			vn = vg.getNav();
			setTagset();
			setupParserRegistry();
		} catch (NavException e) {
			throw new DocumentParsingException(e);
		}
	}

	/**
	 * Reads the doctype of the XML input file and sets the appropriate tagset
	 * enum element from {@link Tagset}.
	 * 
	 * @throws NavException
	 */
	private void setTagset() throws NavException {
		for (int i = 0; i < vn.getTokenCount(); i++) {
			if (vn.getTokenType(i) == VTDNav.TOKEN_DTD_VAL) {
				String docType = vn.toString(i).trim();
				if (docType.equals(
						"article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.0 20120330//EN\" \"JATS-archivearticle1.dtd\""))
					tagset = Tagset.JATS_1_0;
				else if (docType.equals(
						"article PUBLIC \"-//NLM//DTD Journal Publishing DTD v2.3 20070202//EN\" \"journalpublishing.dtd\""))
					tagset = Tagset.NLM_2_3;
				else if (docType.equals(
						"article PUBLIC \"-//NLM//DTD Journal Publishing DTD v3.0 20080202//EN\" \"journalpublishing3.dtd\""))
					tagset = Tagset.NLM_3_0;
				else
					log.warn("Unsupported document type in file {}: {}", nxmlFile, docType);
				return;
			}
		}
		log.warn("Could not find a doctype in file {}", nxmlFile);
	}

	private void setupParserRegistry() {
		this.defaultElementParser = new DefaultElementParser(this);
		parserRegistry = new HashMap<>();
//		parserRegistry.put("p", new ParagraphParser(this));
//		parserRegistry.put("sec", new SectionParser(this));
		// TODO extend
	}

	public VTDNav getVn() {
		return vn;
	}

	public File getNxmlFile() {
		return nxmlFile;
	}

	public Tagset getTagset() {
		return tagset;
	}

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
}
