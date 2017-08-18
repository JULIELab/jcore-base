package de.julielab.jcore.reader.pmc.parser;

import java.util.stream.IntStream;

import org.apache.uima.jcas.cas.FSArray;

import de.julielab.jcore.types.List;
import de.julielab.jcore.types.ListItem;

public class ListParser extends DefaultElementParser {

	public ListParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		this.elementName = "list";
	}

	@Override
	protected void parseElement(ElementParsingResult result) throws ElementParsingException {
		super.parseElement(result);
		
		List list = (List) result.getAnnotation();
		java.util.List<ListItem> listItems = result.getSubResultAnnotations(ListItem.class);
		FSArray fsArray = new FSArray(nxmlDocumentParser.cas, listItems.size());
		IntStream.range(0, listItems.size()).forEach(i -> fsArray.set(i, listItems.get(i)));
		list.setItemList(fsArray);
	}

	
	
}
