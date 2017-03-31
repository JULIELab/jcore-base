package de.julielab.jcore.reader.pmc.parser;

import java.util.Optional;

import org.apache.uima.jcas.cas.FSArray;

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.pmc.parser.NxmlDocumentParser.Tagset;
import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.OtherID;

public class FrontMatterParser extends NxmlElementParser {

	public FrontMatterParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "front";
	}

	@Override
	public ElementParsingResult parse() throws ElementParsingException, DocumentParsingException {
		try {
			ElementParsingResult frontResult = createParsingResult();
			checkCursorPosition();
			vn.push();
			
			parseXPath("/article/front/article-meta/title-group/article-title").ifPresent(frontResult::addSubResult);
			parseXPath("/article/front/article-meta/abstract").ifPresent(frontResult::addSubResult);
			parseXPath("/article/front/article-meta/contrib-group").ifPresent(frontResult::addSubResult);

			Optional<String> pmid = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='pmid']");
			Optional<String> pmcid = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='pmc']");
			Optional<String> doi = getXPathValue("/article/front/article-meta/article-id[@pub-id-type='doi']");

			Optional<String> year = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/year");
			Optional<String> month = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/month");
			Optional<String> day = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/day");
			Optional<String> journalTitle = nxmlDocumentParser.getTagset() == Tagset.NLM_2_3
					? getXPathValue("/article/front/journal-meta/journal-title")
					: getXPathValue("/article/front/journal-meta/journal-title-group/journal-title");
			Optional<String> volume = getXPathValue("/article/front/article-meta/volume");
			Optional<String> issue = getXPathValue("/article/front/article-meta/issue");
			Optional<String> firstPage = getXPathValue("/article/front/article-meta/fpage");
			Optional<String> lastPage = getXPathValue("/article/front/article-meta/lpage");
			Optional<String> issn = getXPathValue("/article/front/journal-meta/issn[@pub-type='ppub']");
			
			Optional<String> copyrightStatement = getXPathValue("/article/front/article-meta/permissions/copyright-statement");

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
			
			frontResult.setAnnotation(header);
			
			vn.pop();
			vn.toElement(VTDNav.NEXT_SIBLING);
			frontResult.setLastTokenIndex(vn.getCurrentIndex());
			return frontResult;
		} catch (XPathParseException | XPathEvalException | NavException e) {
			throw new ElementParsingException(e);
		}
	}

}
