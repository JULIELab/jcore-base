package de.julielab.jcore.reader.pmc.parser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.uima.jcas.cas.FSArray;

import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.pmc.parser.NxmlDocumentParser.Tagset;
import de.julielab.jcore.types.AuthorInfo;
import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.Keyword;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.ManualDescriptor;
import de.julielab.jcore.types.pubmed.OtherID;

public class FrontParser extends NxmlElementParser {

	public FrontParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "front";
	}

	@Override
	public void parseElement(ElementParsingResult frontResult) throws ElementParsingException {
		try {

			// title and abstract
			parseXPath("/article/front/article-meta/title-group/article-title").ifPresent(frontResult::addSubResult);
			parseXPath("/article/front/article-meta/abstract").ifPresent(frontResult::addSubResult);

			// article IDs
			Optional<String> pmid = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='pmid']");
			Optional<String> pmcid = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='pmc']");
			Optional<String> doi = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='doi']");

			// publication details
			Optional<String> year = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/year");
			Optional<String> month = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/month");
			Optional<String> day = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/day");
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
			Optional<String> issn = getXPathValue("/article/front/journal-meta/issn[@pub-type='ppub']");

			// copyright statement
			Optional<String> copyrightStatement = getXPathValue(
					"/article/front/article-meta/permissions/copyright-statement");

			// keywords
			Optional<List<String>> keywords = getXPathValues("/article/front/article-meta/kwd-group/kwd");

			Header header = new Header(nxmlDocumentParser.cas);

			pmcid.ifPresent(header::setDocId);
			pmid.ifPresent(p -> {
				OtherID otherID = new OtherID(nxmlDocumentParser.cas);
				otherID.setId(p);
				otherID.setSource("PubMed");
				FSArray otherIDs = new FSArray(nxmlDocumentParser.cas, 1);
				header.setOtherIDs(otherIDs);
			});
			doi.ifPresent(header::setDoi);

			copyrightStatement.ifPresent(header::setCopyright);

			Journal journal = new Journal(nxmlDocumentParser.cas);
			journalTitle.ifPresent(journal::setTitle);
			abbrevJournalTitle.ifPresent(journal::setShortTitle);
			volume.ifPresent(journal::setVolume);
			issue.ifPresent(journal::setIssue);
			issn.ifPresent(journal::setISSN);
			journal.setPages(firstPage.get() + "--" + lastPage.get());
			FSArray pubTypes = new FSArray(nxmlDocumentParser.cas, 1);
			pubTypes.set(0, journal);
			Date pubDate = new Date(nxmlDocumentParser.cas);
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
					keyword.setName(keywordList.get(i));
					fsArray.set(i, keyword);
				});
				ManualDescriptor manualDescriptor = new ManualDescriptor(nxmlDocumentParser.cas);
				manualDescriptor.setKeywordList(fsArray);
				manualDescriptor.addToIndexes();
			}

		} catch (XPathParseException | XPathEvalException | NavException e) {
			throw new ElementParsingException(e);
		}
	}

}
