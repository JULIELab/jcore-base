package de.julielab.jcore.consumer.coreference;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.CorefExpression;
import de.julielab.jcore.types.CorefRelation;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterators;

@ResourceMetaData(name = "JCoRe Coreference Writer", description = "Writes co-reference annotation to a text file.")
public class CoreferenceWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_OUTPUTFILE = "OutputFile";

    @ConfigurationParameter(name = PARAM_OUTPUTFILE)
    private String outputFile;
    private OutputStream os;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        outputFile = (String) aContext.getConfigParameterValue(PARAM_OUTPUTFILE);
        try {
            os = FileUtilities.getOutputStreamToFile(new File(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
        try {
            String pubmedId = JCoReTools.getDocId(jcas);
            FSIterator<CorefRelation> it = jcas.<CorefRelation>getAnnotationIndex(CorefRelation.type).iterator();

            int relcount = 0;
            while (it.hasNext()) {
                CorefRelation rel = it.next();
                de.julielab.jcore.types.Annotation anaphora = rel.getAnaphora();

                String abbrId = "Ana" + relcount;

                IOUtils.write(String.join("\t", pubmedId, abbrId, String.valueOf(anaphora.getBegin()),
                        String.valueOf(anaphora.getEnd())) + "\n", os, "UTF-8");

                Iterator<FeatureStructure> antecedentsIt = rel.getAntecedents() != null ? rel.getAntecedents().iterator() : null;
                while (antecedentsIt != null && antecedentsIt.hasNext()) {
                    CorefExpression antecedent = (CorefExpression) antecedentsIt.next();
                    if (antecedent != null) {
                        String antecedentGroup = "Ant" + relcount;
                        IOUtils.write(String.join("\t", pubmedId, antecedentGroup, String.valueOf(antecedent.getBegin()),
                                String.valueOf(antecedent.getEnd())) + "\n", os, "UTF-8");
                    }
                }


                ++relcount;
            }
        } catch (CASRuntimeException | IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            os.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

}
