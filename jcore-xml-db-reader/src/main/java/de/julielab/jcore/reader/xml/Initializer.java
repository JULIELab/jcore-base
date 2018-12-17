package de.julielab.jcore.reader.xml;

import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Initializer {
    public static final String PARAM_ROW_MAPPING = "RowMapping";
    /**
     * Configuration parameter defined in the descriptor
     */
    public static final String PARAM_MAPPING_FILE = "MappingFile";
    private final XMLMapper xmlMapper;
    private Row2CasMapper row2CasMapper;


    public Initializer(String mappingFileStr, String[] rowMappingArray, Supplier<List<Map<String, Object>>> columnsToRetrieveSupplier) throws ResourceInitializationException {

        InputStream is = null;

        File mappingFile = new File(mappingFileStr);
        if (mappingFile.exists()) {
            try {
                is = new FileInputStream(mappingFile);
            } catch (FileNotFoundException e1) {
                throw new ResourceInitializationException(e1);
            }
        } else {
            if (!mappingFileStr.startsWith("/"))
                mappingFileStr = "/" + mappingFileStr;

            is = getClass().getResourceAsStream(mappingFileStr);
            if (is == null) {
                throw new IllegalArgumentException("MappingFile " + mappingFileStr
                        + " could not be found as a file or on the classpath (note that the prefixing '/' is added automatically if not already present for classpath lookup)");
            }
        }

        try {
            xmlMapper = new XMLMapper(is);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        if (rowMappingArray != null && rowMappingArray.length > 0)
            row2CasMapper = new Row2CasMapper(rowMappingArray, columnsToRetrieveSupplier);
    }

    public XMLMapper getXmlMapper() {
        return xmlMapper;
    }

    public Row2CasMapper getRow2CasMapper() {
        return row2CasMapper;
    }
}
