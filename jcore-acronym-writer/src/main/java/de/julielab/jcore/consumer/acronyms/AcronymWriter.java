
package de.julielab.jcore.consumer.acronyms;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@ResourceMetaData(name = "JCoRe Acronym Writer", description = "Writes acronym annotation to a text file.")
public class AcronymWriter extends JCasAnnotator_ImplBase {

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
			FSIterator<Annotation> it = jcas.getAnnotationIndex(Abbreviation.type).iterator();

			Map<de.julielab.jcore.types.Annotation, String> fullForms = new HashMap<>();
			int abbrCount = 0;
			while (it.hasNext()) {
				Abbreviation abbr = (Abbreviation) it.next();
				de.julielab.jcore.types.Annotation textReference = abbr.getTextReference();

				String abbrId = "A" + abbrCount;

				String fullformId = fullForms.get(textReference);
				if (fullformId == null) {
					fullformId = "F" + abbrCount;
					fullForms.put(textReference, fullformId);
					IOUtils.write(String.join("\t", pubmedId, fullformId, String.valueOf(textReference.getBegin()),
							String.valueOf(textReference.getEnd())) + "\n", os, "UTF-8");
				}

				IOUtils.write(String.join("\t", pubmedId, abbrId, String.valueOf(abbr.getBegin()),
						String.valueOf(abbr.getEnd()), fullformId) + "\n", os, "UTF-8");

				++abbrCount;
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
