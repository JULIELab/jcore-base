package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.xml.XmiBuilder;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

public class XmiDBMultiplier extends DBMultiplier implements Initializable {




    private Initializer initializer;
    private CasPopulator casPopulator;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
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
