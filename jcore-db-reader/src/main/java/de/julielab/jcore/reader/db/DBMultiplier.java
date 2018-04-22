package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.xmlData.dataBase.DBCIterator;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.julielab.jcore.reader.db.DBSubsetReader.DESC_ADDITIONAL_TABLES;
import static de.julielab.jcore.reader.db.DBSubsetReader.DESC_ADDITIONAL_TABLE_SCHEMAS;
import static de.julielab.jcore.reader.db.SubsetReaderConstants.PARAM_ADDITIONAL_TABLES;
import static de.julielab.jcore.reader.db.SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS;
import static de.julielab.jcore.reader.db.TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME;
import static de.julielab.jcore.reader.db.TableReaderConstants.PARAM_DB_DRIVER;

/**
 * A multiplier retrieving feature structures of type of {@link RowBatch} in its {@link #process(JCas)} method.
 * Each <code>RowBatch</code> lists IDs of documents to read and the table to read them from.
 * The part of actual reading the documents into CAS instances is subject to implementation for extending classes.
 * For this purpose, the iterator holding the actual document data, {@link #documentDataIterator}, must be used
 * to retrieve document data and use it to populate CAS instances in the {@link JCasMultiplier_ImplBase#next()}
 * method.
 */
public abstract class DBMultiplier extends JCasMultiplier_ImplBase {

    private final static Logger log = LoggerFactory.getLogger(DBMultiplier.class);

    @ConfigurationParameter(name = PARAM_DB_DRIVER, mandatory = false, description = "Currently unused because the " +
            "Hikari JDBC library should recognize the correct driver. However, there seem to be cases where this " +
            "doesn't work (HSQLDB). So we keep the parameter for later. When this issue comes up, the driver would " +
            "have to be set manually. This isn't done right now.")
    private String driver;
    @ConfigurationParameter(name = PARAM_COSTOSYS_CONFIG_NAME, mandatory = true, description = "File path or classpath" +
            " resource location to the CoStoSys XML configuration. This configuration must specify the table schema " +
            "of the table referred to by the 'Table' parameter as active table schema. The active table schema " +
            "is always the schema of the data table that is either queried directly for documents or, if 'Table' " +
            "points to a subset table, indirectly through the subset table. Make also sure that the active " +
            "database connection in the configuration points to the correct database.")
    private String costosysConfig;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLES, mandatory = false, description = DESC_ADDITIONAL_TABLES)
    protected String[] additionalTableNames;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLE_SCHEMAS, mandatory = false, description = DESC_ADDITIONAL_TABLE_SCHEMAS)
    protected String[] additionalTableSchemas;


    protected DataBaseConnector dbc;
    protected DBCIterator<byte[][]> documentDataIterator;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        driver = (String) aContext.getConfigParameterValue(PARAM_DB_DRIVER);
        costosysConfig = (String) aContext.getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);

        try {
            dbc = new DataBaseConnector(costosysConfig);
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        log.info("{}:{}", PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
    }

    @Override
    public void process(JCas aJCas) {
        // TODO table joining
        RowBatch rowbatch = JCasUtil.selectSingle(aJCas, RowBatch.class);
        List<Object[]> documentIdsForQuery = new ArrayList<>();
        FSArray identifiers = rowbatch.getIdentifiers();
        for (int i = 0; i < identifiers.size(); i++) {
            StringArray primaryKey = (StringArray) identifiers.get(i);
            documentIdsForQuery.add(primaryKey.toArray());
        }
        documentDataIterator = dbc.retrieveColumnsByTableSchema(documentIdsForQuery, rowbatch.getTable());
    }

    @Override
    public boolean hasNext() {
        return documentDataIterator != null && documentDataIterator.hasNext();
    }
}
