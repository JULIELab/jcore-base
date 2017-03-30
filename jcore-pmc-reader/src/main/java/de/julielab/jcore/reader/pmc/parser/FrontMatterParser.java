package de.julielab.jcore.reader.pmc.parser;

import java.util.Optional;

import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.pubmed.Header;

public class FrontMatterParser extends NxmlElementParser {

	public FrontMatterParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "front";
	}

	@Override
	public ElementParsingResult parse() throws ElementParsingException, DocumentParsingException {
		try {
			ElementParsingResult frontResult = createParsingResult();
			parseXPath("/article/front/article-meta/title-group/article-title").ifPresent(frontResult::addSubResult);
			parseXPath("/article/front/article-meta/abstract").ifPresent(frontResult::addSubResult);
			
			Optional<String> year = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/year");
			Optional<String> month = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/month");
			Optional<String> day = getXPathValue("/article/front/article-meta/pub-date[@pub-type='epub']/day");
			
			
			Header header = new Header(nxmlDocumentParser.cas);
			Journal journal = new Journal(nxmlDocumentParser.cas);
			String issue;
			String volume;
			String firstPage;
			String lastPage;
			Date pubDate = new Date(nxmlDocumentParser.cas);
			pubDate.setDay();
			pubDate.setMonth();
			pubDate.setYear();
//           xpaths.put("month", "//front/article-meta/pub-date[@pub-type='epub']/month"); 
//           xpaths.put("day",  "//front/article-meta/pub-date[@pub-type='epub']/day");
//           xpaths.put("journal", "//front/journal-meta/journal-title");
//           xpaths.put("volume",  "//front/article-meta/volume");
//           xpaths.put("issue", "//front/article-meta/issue");
//           xpaths.put("issn", "//front/journal-meta/issn[@pub-type='ppub']");
			// TODO Auto-generated method stub
			
			
//			 <permissions>
//            <copyright-statement>© The Author(s) 2010</copyright-statement>
//        </permissions>
		} catch (XPathParseException | XPathEvalException | NavException e) {
			throw new ElementParsingException(e);
		}
		return null;
	}
	

}
