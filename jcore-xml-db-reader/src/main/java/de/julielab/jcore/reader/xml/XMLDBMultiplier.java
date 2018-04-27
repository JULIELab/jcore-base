package de.julielab.jcore.reader.xml;

import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ResourceMetaData(name = "JCoRe XML Database Multiplier", description = "This CAS multiplier receives information about " +
        "documents to be read from an instance of the XML Database Multiplier reader from the jcore-db-reader project. " +
        "The multiplier employs the jcore-xml-mapper to map the document XML structure into CAS instances. It also " +
        "supports additional tables sent by the DB Multiplier Reader that are then joined to the main table. This " +
        "mechanism is used to load separate data from additional database tables and populate the " +
        "CAS with them via the 'RowMapping' parameter. This component is part of the Jena Document Information System, " +
        "JeDIS."
        , vendor = "JULIE Lab Jena, Germany", copyright = "JULIE Lab Jena, Germany")
public class XMLDBMultiplier extends DBMultiplier {
private final static Logger log = LoggerFactory.getLogger(XMLDBMultiplier.class);
    public static final String PARAM_ROW_MAPPING = Initializer.PARAM_ROW_MAPPING;
    public static final String PARAM_MAPPING_FILE = Initializer.PARAM_MAPPING_FILE;
    /**
     * Mapper which maps medline XML to a CAS with the specified UIMA type system
     * via an XML configuration file.
     */
    protected XMLMapper xmlMapper;
    @ConfigurationParameter(name = PARAM_ROW_MAPPING, mandatory = false, description = XMLDBReader.DESC_ROW_MAPPING)
    protected String[] rowMappingArray;
    @ConfigurationParameter(name = PARAM_MAPPING_FILE, description = XMLDBReader.DESC_MAPPING_FILE)
    protected String mappingFileStr;
    private Row2CasMapper row2CasMapper;
    private CasPopulator casPopulator;
    private boolean initialized;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        mappingFileStr = (String) aContext.getConfigParameterValue(PARAM_MAPPING_FILE);
        rowMappingArray = (String[]) aContext.getConfigParameterValue(PARAM_ROW_MAPPING);

        // We don't know yet which tables to read. Thus, we leave the row mapping out.
        // We will now once the DBMultiplier#process(JCas) will have been run.
        Initializer initializer = new Initializer(mappingFileStr, null, null);
        xmlMapper = initializer.getXmlMapper();
        initialized = false;
    }


    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        JCas jCas = getEmptyJCas();
        try {
            if (documentDataIterator.hasNext()) {
                if (!initialized) {
                    try {
                        row2CasMapper = new Row2CasMapper(rowMappingArray, () -> getAllRetrievedColumns());
                    } catch (ResourceInitializationException e) {
                        throw new AnalysisEngineProcessException(e);
                    }
                    // The DBC is initialized in the super class in the process() method. Thus, at this point
                    // the DBC should be set.
                    casPopulator = new CasPopulator(dbc, xmlMapper, row2CasMapper, rowMappingArray);
                    initialized = true;
                }
                byte[][] documentData = documentDataIterator.next();
                populateCas(jCas, documentData);
            }
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
        return jCas;
    }

    private void populateCas(JCas jCas, byte[][] documentData) throws AnalysisEngineProcessException {
        try {
            casPopulator.populateCas(jCas, documentData,
                    (docData, jcas) -> DBReader.setDBProcessingMetaData(dbc, readDataTable, tableName, docData, jcas));
        } catch (CasPopulationException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    protected List<Map<String, Object>> getAllRetrievedColumns() {
        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        Pair<Integer, List<Map<String, String>>> numColumnsAndFields = dbc.getNumColumnsAndFields(tables.length > 1, schemaNames);
        return numColumnsAndFields.getRight().stream().map(HashMap<String, Object>::new).collect(Collectors.toList());
    }
}
