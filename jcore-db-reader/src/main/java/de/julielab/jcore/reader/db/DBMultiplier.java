package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.casmultiplier.DocumentIds;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
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
import java.util.List;
import java.util.Optional;

import static de.julielab.jcore.reader.db.TableReaderConstants.*;

public class DBMultiplier extends JCasMultiplier_ImplBase {

    private final static Logger log = LoggerFactory.getLogger(DBMultiplier.class);

    @ConfigurationParameter(name = PARAM_DB_DRIVER, mandatory = false, description = "Currently unused because the " +
            "Hikari JDBC library should recognize the correct driver. However, there seem to be cases where this " +
            "doesn't work (HSQLDB). So we keep the parameter for later. When this issue comes up, the driver would " +
            "have to be set manually. This isn't done right now.")
    private String driver;
    @ConfigurationParameter(name = PARAM_TABLE, description = "The data table to read the document batches from. " +
            "The list of document IDs to read must be provided by the DBMultiplierReader.")
    private String tableName;
    @ConfigurationParameter(name = PARAM_COSTOSYS_CONFIG_NAME, mandatory = true, description = "File path or classpath" +
            " resource location to the CoStoSys XML configuration. This configuration must specify the table schema " +
            "of the table referred to by the 'tableName' parameter as active table schema. The active table schema " +
            "is always the schema of the data table that is either queried directly for documents or, if 'tableName' " +
            "points to a subset table, indirectly through the subset table. Make also sure that the active " +
            "database connection in the configuration points to the correct database.")
    private String costosysConfig;

    private DataBaseConnector dbc;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        driver = (String) aContext.getConfigParameterValue(PARAM_DB_DRIVER);
        tableName = (String) aContext.getConfigParameterValue(PARAM_TABLE);
        costosysConfig = (String) aContext.getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);

        try {
            dbc = new DataBaseConnector(costosysConfig);
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        log.info("{}:{}", PARAM_TABLE, tableName);
        log.info("{}:{}", PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        DocumentIds documentIds = JCasUtil.selectSingle(aJCas, DocumentIds.class);
        List<Object[]> documentIdsForQuery = new ArrayList<>();
        FSArray identifiers = documentIds.getIdentifiers();
        for (int i = 0; i < identifiers.size(); i++) {
            StringArray primaryKey = (StringArray) identifiers.get(i);
            documentIdsForQuery.add(primaryKey.toArray());
        }
    }

    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
        return false;
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        return null;
    }
}
