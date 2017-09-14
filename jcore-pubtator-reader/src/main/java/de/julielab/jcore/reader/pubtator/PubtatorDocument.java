package de.julielab.jcore.reader.pubtator;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubtatorDocument {
	private static final Logger log = LoggerFactory.getLogger(PubtatorDocument.class);

	public final static PubtatorDocument EMPTY_DOCUMENT = new PubtatorDocument();

	// we only describe the beginning of a line
	// example:
	// 17317680|t|Delayed expression of apoptotic and cell-cycle control genes
	// in carcinogen-exposed bladders of mice lacking p53.S389 phosphorylation.
	private static Pattern pubtatorTextLine = Pattern.compile("[0-9]+\\|[a-z]\\|");
	// we only describe the beginning of a line
	// example:
	// 17317680 134 138 Mice Species 10090
	private static Pattern pubtatorEntityLine = Pattern.compile("[0-9]+\\t[0-9]+");

	private String documentId;
	private String title;
	private String abstractText;
	private List<PubtatorEntity> entities = Collections.emptyList();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAbstractText() {
		return abstractText;
	}

	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}

	public List<PubtatorEntity> getEntities() {
		return entities;
	}

	public void setEntities(List<PubtatorEntity> entities) {
		this.entities = entities;
	}

	public static PubtatorDocument parseNextDocument(BufferedReader reader) throws IOException {
		PubtatorDocument ret = EMPTY_DOCUMENT;
		PubtatorDocument readDocument = new PubtatorDocument();
		Matcher entityMatchter = pubtatorEntityLine.matcher("");
		Matcher textMatcher = pubtatorTextLine.matcher("");
		String line;
		// read up to the next empty line
		while ((line = reader.readLine()) != null && (!line.trim().isEmpty() || ret == EMPTY_DOCUMENT)) {
			line = line.trim();
			if (line.isEmpty() && ret == EMPTY_DOCUMENT)
				continue;
			ret = readDocument;
			try {
				entityMatchter.reset(line);
				if (entityMatchter.find()) {
					String[] split = line.split("\\t");
					if (split.length < 5)
						throw new IllegalArgumentException(
								"PubTator format error: The given line looks like an entity line but it does not have at least 5 fields: "
										+ line);
					PubtatorEntity entity = null;
					if (split.length == 5)
						entity = new PubtatorEntity(split[0], split[1], split[2], split[4]);
					if (split.length == 6)
						entity = new PubtatorEntity(split[0], split[1], split[2], split[4], split[5]);
					readDocument.addEntity(entity);
				} else {
					textMatcher.reset(line);
					if (textMatcher.find()) {
						String[] split = line.split("\\|");
						// there must be exactly 3 fields
						if (split.length != 3)
							throw new IllegalArgumentException(
									"PubTator format error: The expected text line does not have exactly three fields. The line was "
											+ line);
						readDocument.setDocumentId(split[0]);
						switch (split[1]) {
						case "t":
							readDocument.setTitle(split[2]);
							break;
						case "a":
							readDocument.setAbstractText(split[2]);
							break;
						default:
							throw new IllegalArgumentException(
									"PubTator format error: The given line looks like a text line but does not specify a known text type (|a|bstract or |t|itle): "
											+ line);
						}
					} else {
						throw new IllegalArgumentException(
								"Unknown PubTator format: The given line is not recognized as a part of a PubTator document: "
										+ line);
					}
				}
			} catch (Exception e) {
				log.error("Error occurred at line {}", line);
				throw e;
			}
		}
		return ret;
	}

	private void addEntity(PubtatorEntity entity) {
		if (entities.isEmpty())
			entities = new ArrayList<>();
		entities.add(entity);
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
}
