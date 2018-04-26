package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.reader.db.SubsetReaderConstants;
import de.julielab.jcore.reader.db.TableReaderConstants;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.xml.XmiBuilder;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class XmiDBMultiplier extends DBMultiplier implements Initializable {

    private Initializer initializer;
    private CasPopulator casPopulator;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        RowBatch rowBatch = JCasUtil.selectSingle(aJCas, RowBatch.class);

        // TODO finish this
//        List<Map<String, String>> activeTableSchemaPrimaryKey;
//        if (rowBatch.getTables().size() > 1) {
//            // As a convenience and to avoid mistakes, we want to automatically fill the annotation table schemas into the
//            // additionalTableSchemas array. For this, we need to get a local DBC instance to get the correct field definition.
//            try {
//                activeTableSchemaPrimaryKey = dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList());
//                String xmiAnnotationSchemaName = dbc.addXmiAnnotationFieldConfiguration(activeTableSchemaPrimaryKey, rowBatch.getGzipped()).getName();
//                String[] additionalTableSchemas = new String[additionalTables.length];
//                Arrays.fill(additionalTableSchemas, xmiAnnotationSchemaName);
//                // We need to set the value to the meta data of the component because at the end of the method, we call
//                // super.initialize() which will then read the table schemas as parameters from the meta data
//                setConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS, additionalTableSchemas);
//            } catch (FileNotFoundException e) {
//                throw new ResourceInitializationException(e);
//            }
//        }
//        super.process(aJCas);
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        JCas jCas = getEmptyJCas();
        if (documentDataIterator.hasNext()) {
            if (initializer == null) {
                initializer = new Initializer(this, dbc, getAdditionalTableNames(), getAdditionalTableNames().length > 0);
                casPopulator = new CasPopulator(dataTable, initializer, readDataTable, tableName);
            }
            populateCas(jCas);
        }
        return jCas;
    }

    private void populateCas(JCas jCas) throws AnalysisEngineProcessException {
        try {
            casPopulator.populateCas(documentDataIterator.next(), jCas);
        } catch (CasPopulationException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }


    @Override
    public String[] getTables() {
        return tables;
    }
}
