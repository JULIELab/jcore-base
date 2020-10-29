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
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;
import de.julielab.jcore.reader.pmc.PMCReader;
import de.julielab.jcore.reader.pmc.parser.NxmlDocumentParser.Tagset;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.ManualDescriptor;
import de.julielab.jcore.types.pubmed.OtherID;
import org.apache.uima.jcas.cas.FSArray;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FrontParser extends NxmlElementParser {

	public FrontParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "front";
	}

	@Override
	protected void parseElement(ElementParsingResult frontResult) throws ElementParsingException {
		try {
			// Only handle the front matter of the actual article, not sub-articles
			final String elementPath = getElementPath();
			if (!elementPath.endsWith("/article/front")) {
				int firstIndexAfterElement = skipElement();
				frontResult.setLastTokenIndex(firstIndexAfterElement);
				frontResult.setResultType(ParsingResult.ResultType.NONE);
				return;
			}

			// title and abstract
			parseXPath("/article/front/article-meta/title-group/article-title").ifPresent(r -> {
				ElementParsingResult er = (ElementParsingResult) r;
				Title articleTitle = (Title) er.getAnnotation();
				articleTitle.setTitleType("document");
				frontResult.addSubResult(r);
			});
			parseXPath("/article/front/article-meta/abstract").ifPresent(r -> {
				ElementParsingResult er = (ElementParsingResult) r;
				AbstractText abstractText = (AbstractText) er.getAnnotation();
				List<AbstractSection> abstractSections = er.getSubResultAnnotations(AbstractSection.class);
				FSArray fsArray = new FSArray(nxmlDocumentParser.cas, abstractSections.size());
				IntStream.range(0, abstractSections.size()).forEach(i -> fsArray.set(i, abstractSections.get(i)));
				abstractText.setStructuredAbstractParts(fsArray);
				frontResult.addSubResult(r);
			});

			// article IDs
			Optional<String> pmid = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='pmid']");
			Optional<String> pmcid = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='pmc']");
			Optional<String> doi = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='doi']");

			// publication details
			String pubType = "";
			String pubDateFmt = "/article/front/article-meta/pub-date[@pub-type='%s']";
			if (xPathExists(String.format(pubDateFmt, "epub")))
				pubType = "epub";
			else if (xPathExists(String.format(pubDateFmt, "ppub")))
				pubType = "ppub";
			else if (xPathExists(String.format(pubDateFmt, "pmc-release")))
				pubType = "pmc-release";
			Optional<String> year = getXPathValue(String.format("/article/front/article-meta/pub-date[@pub-type='%s']/year", pubType));
			Optional<String> month = getXPathValue(String.format("/article/front/article-meta/pub-date[@pub-type='%s']/month", pubType));
			Optional<String> day = getXPathValue(String.format("/article/front/article-meta/pub-date[@pub-type='%s']/day", pubType));
			Optional<String> journalTitle = nxmlDocumentParser.getTagset() == Tagset.NLM_2_3
					? getXPathValue("/article/front/journal-meta/journal-title")
					: getXPathValue("/article/front/journal-meta/journal-title-group/journal-title");
			// there actually might be several abbreviated titles but here, we
			// only use the first; our type system currently cannot represent
			// more anyway. One could try decide for an preferred one since the
			// abbrev-type attribute disposes the source of the abbreviated
			// title (e.g. publisher or nlm-ta).
			Optional<String> abbrevJournalTitle = nxmlDocumentParser.getTagset() == Tagset.NLM_2_3
					? getXPathValue("/article/front/journal-meta/abbrev-journal-title")
					: getXPathValue("/article/front/journal-meta/journal-title-group/abbrev-journal-title");
			Optional<String> volume = getXPathValue("/article/front/article-meta/volume");
			Optional<String> issue = getXPathValue("/article/front/article-meta/issue");
			Optional<String> firstPage = getXPathValue("/article/front/article-meta/fpage");
			Optional<String> lastPage = getXPathValue("/article/front/article-meta/lpage");
			Optional<String> elocation = getXPathValue("/article/front/article-meta/elocation-id");
			Optional<String> issn = getXPathValue("/article/front/journal-meta/issn[@pub-type='ppub']");

			// copyright statement
			Optional<String> copyrightStatement = getXPathValue(
					"/article/front/article-meta/permissions/copyright-statement");

			// keywords
			Optional<List<String>> keywords = getXPathValues("/article/front/article-meta/kwd-group/kwd");

			assert volume.isPresent();

			Header header = new Header(nxmlDocumentParser.cas);
			header.setComponentId(PMCReader.class.getName());

			pmcid.ifPresent(id -> header.setDocId("PMC" + id));
			pmid.ifPresent(p -> {
				OtherID otherID = new OtherID(nxmlDocumentParser.cas);
				otherID.setComponentId(PMCReader.class.getName());
				otherID.setId(p);
				otherID.setSource("PubMed");
				FSArray otherIDs = new FSArray(nxmlDocumentParser.cas, 1);
				otherIDs.set(0, otherID);
				header.setOtherIDs(otherIDs);
			});
			doi.ifPresent(header::setDoi);

			copyrightStatement.ifPresent(header::setCopyright);

			Journal journal = new Journal(nxmlDocumentParser.cas);
			journal.setComponentId(PMCReader.class.getName());
			journalTitle.ifPresent(journal::setTitle);
			abbrevJournalTitle.ifPresent(journal::setShortTitle);
			volume.ifPresent(journal::setVolume);
			issue.ifPresent(journal::setIssue);
			issn.ifPresent(journal::setISSN);
			String pages = null;
			if (firstPage.isPresent() && lastPage.isPresent())
				pages = firstPage.get() + "--" + lastPage.get();
			else if (firstPage.isPresent())
				pages = firstPage.get();
			else if (elocation.isPresent())
				pages = elocation.get();
			journal.setPages(pages);
			FSArray pubTypes = new FSArray(nxmlDocumentParser.cas, 1);
			pubTypes.set(0, journal);
			Date pubDate = new Date(nxmlDocumentParser.cas);
			pubDate.setComponentId(PMCReader.class.getName());
			day.map(Integer::parseInt).ifPresent(pubDate::setDay);
			month.map(Integer::parseInt).ifPresent(pubDate::setMonth);
			year.map(Integer::parseInt).ifPresent(pubDate::setYear);
			journal.setPubDate(pubDate);
			header.setPubTypeList(pubTypes);

			// authors (more general: contributors; but for the moment we
			// restrict ourselves to authors)
			parseXPath("/article/front/article-meta/contrib-group").map(ElementParsingResult.class::cast)
					.ifPresent(r -> {
						// currently only authors
						List<AuthorInfo> authors = r.getSubResults().stream().map(ElementParsingResult.class::cast)
								.map(e -> e.getAnnotation()).filter(AuthorInfo.class::isInstance)
								.map(AuthorInfo.class::cast).collect(Collectors.toList());
						FSArray aiArray = new FSArray(nxmlDocumentParser.cas, authors.size());
						IntStream.range(0, authors.size()).forEach(i -> {
							aiArray.set(i, authors.get(i));
						});
						if (aiArray.size() > 0)
							header.setAuthors(aiArray);
					});

			frontResult.setAnnotation(header);

			if (keywords.isPresent()) {
				List<String> keywordList = keywords.get();
				FSArray fsArray = new FSArray(nxmlDocumentParser.cas, keywordList.size());
				IntStream.range(0, keywordList.size()).forEach(i -> {
					Keyword keyword = new Keyword(nxmlDocumentParser.cas);
					keyword.setComponentId(PMCReader.class.getName());
					keyword.setName(keywordList.get(i));
					fsArray.set(i, keyword);
				});
				ManualDescriptor manualDescriptor = new ManualDescriptor(nxmlDocumentParser.cas);
				manualDescriptor.setComponentId(PMCReader.class.getName());
				manualDescriptor.setKeywordList(fsArray);
				manualDescriptor.addToIndexes();
			}

		} catch (XPathParseException | XPathEvalException | NavException e) {
			throw new ElementParsingException(e);
		}
	}

}
