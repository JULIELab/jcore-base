package de.julielab.jcore.reader.xmlmapper.typeParser;

import org.apache.uima.jcas.JCas;

import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;

public class StructuredAbstractParser implements TypeParser {

	private String documentText;
	
	@Override
	public TypeBuilder getTypeBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void parseType(ConcreteType concreteType, VTDNav vn, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception {
		// TODO Auto-generated method stub

	}

	public String getDocumentText() {
		return documentText;
	}


}
