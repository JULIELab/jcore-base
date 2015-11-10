package de.julielab.jcore.reader.xmlmapper.mapper;

import java.util.List;

import org.apache.uima.jcas.JCas;

import com.ximpleware.VTDException;
import com.ximpleware.VTDNav;

public interface DocumentTextPartParser {
	List<String> parseDocumentPart(VTDNav vn, PartOfDocument docTextPart, int offset, JCas jCas, byte[] identifier) throws VTDException;
}
