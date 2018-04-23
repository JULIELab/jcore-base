package de.julielab.jcore.reader.xml;

import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ResourceMetaData(name="XML Database Multiplier", description = "This CAS multiplier receives information about " +
        "documents to be read from an instance of the XML Database Multiplier reader from the jcore-db-reader project. " +
        "The multiplier employs the jcore-xml-mapper to map the document XML structure into CAS instances. It also " +
        "supports additional tables sent be the DB Multiplier Reader that are then joined to the main table. This " +
        "mechanism is used to load separatly data from additional database tables and populate the " +
        "CAS with them via the 'RowMapping' parameter. This component is part of the Jena Document Information System, " +
        "JeDIS."
        , vendor = "JULIE Lab Jena, Germany", copyright = "JULIE Lab Jena, Germany")
public class XMLDBMultiplier extends DBMultiplier {

    public static final String PARAM_ROW_MAPPING = Initializer.PARAM_ROW_MAPPING;
    public static final String PARAM_MAPPING_FILE = Initializer.PARAM_MAPPING_FILE;
    /**
     * Mapper which maps medline XML to a CAS with the specified UIMA type system
     * via an XML configuration file.
     */
    protected XMLMapper xmlMapper;
    @ConfigurationParameter(name = PARAM_ROW_MAPPING)
    protected String[] rowMappingArray;
    @ConfigurationParameter(name = PARAM_MAPPING_FILE, mandatory = true)
    protected String mappingFileStr;
    private Row2CasMapper row2CasMapper;
    private CasPopulator casPopulator;
    private boolean initialized;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        mappingFileStr = (String)aContext. getConfigParameterValue(PARAM_MAPPING_FILE);
        rowMappingArray = (String[])aContext. getConfigParameterValue(PARAM_ROW_MAPPING);

        // We don't know yet which tables to read. Thus, we leave the row mapping out.
        // We will now once the DBMultiplier#process(JCas) will have been run.
        Initializer initializer = new Initializer(mappingFileStr, null, null);
        xmlMapper = initializer.getXmlMapper();
        casPopulator = new CasPopulator(dbc, xmlMapper, row2CasMapper, rowMappingArray);
        initialized = false;
    }


    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        JCas jCas = getEmptyJCas();
        if (documentDataIterator.hasNext()) {
            if (!initialized) {
                try {
                    row2CasMapper = new Row2CasMapper(rowMappingArray, () -> getAllRetrievedColumns());
                } catch (ResourceInitializationException e) {
                    throw new AnalysisEngineProcessException(e);
                }
                initialized = true;
            }
            byte[][] documentData = documentDataIterator.next();
            populateCas(jCas, documentData);
        }
        return jCas;
    }

    private void populateCas(JCas jCas, byte[][] documentData) throws AnalysisEngineProcessException {
        try {
            casPopulator.populateCas(jCas, documentData,
                    (docData, jcas) -> DBReader.setDBProcessingMetaData(dbc, readDataTable, tables[0], docData, jcas));
        } catch (CasPopulationException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    protected List<Map<String, Object>> getAllRetrievedColumns() {
        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        List<Object> numColumnsAndFields = dbc.getNumColumnsAndFields(tables.length > 1, tables, schemaNames);
        for (int i = 1; i < numColumnsAndFields.size(); i++) {
            List<Map<String, Object>> retrievedSchemaFields = (List<Map<String, Object>>) numColumnsAndFields.get(i);
            for (Map<String, Object> field : retrievedSchemaFields)
                fields.add(field);
        }
        return fields;

    }
}
