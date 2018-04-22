package de.julielab.jcore.reader.xml;

import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XMLDBMultiplier extends DBMultiplier {

    public static final String PARAM_ROW_MAPPING = Initializer.PARAM_ROW_MAPPING;
    public static final String PARAM_MAPPING_FILE = Initializer.PARAM_MAPPING_FILE;
    /**
     * Mapper which maps medline XML to a CAS with the specified UIMA type system
     * via an XML configuration file.
     */
    protected XMLMapper xmlMapper;
    protected int totalDocumentCount;
    protected int processedDocuments;
    @ConfigurationParameter(name = PARAM_ROW_MAPPING)
    protected String[] rowMappingArray;
    @ConfigurationParameter(name = PARAM_MAPPING_FILE, mandatory = true)
    protected String mappingFileStr;
    private Row2CasMapper row2CasMapper;
    private CasPopulator casPopulator;
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        mappingFileStr = (String)aContext. getConfigParameterValue(PARAM_MAPPING_FILE);
        rowMappingArray = (String[])aContext. getConfigParameterValue(PARAM_ROW_MAPPING);
        Initializer initializer = new Initializer(mappingFileStr, rowMappingArray, () -> getAllRetrievedColumns());
        row2CasMapper = initializer.getRow2CasMapper();
        xmlMapper = initializer.getXmlMapper();
        casPopulator = new CasPopulator(dbc, xmlMapper, row2CasMapper, rowMappingArray);
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        return null;
    }

    protected List<Map<String, Object>> getAllRetrievedColumns() {
        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        List<Object> numColumnsAndFields = dbc.getNumColumnsAndFields(joinTables, tables, schemas);
        for (int i = 1; i < numColumnsAndFields.size(); i++) {
            List<Map<String, Object>> retrievedSchemaFields = (List<Map<String, Object>>) numColumnsAndFields.get(i);
            for (Map<String, Object> field : retrievedSchemaFields)
                fields.add(field);
        }
        return fields;

    }
}
