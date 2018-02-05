package de.julielab.jcore.consumer.es;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.utility.JCoReTools;

public abstract class AbstractCasToJsonConsumer extends JCasAnnotator_ImplBase {
	private static final Logger log = LoggerFactory.getLogger(AbstractCasToJsonConsumer.class);

	public static final String PARAM_FILTER_BOARDS = "FilterBoards";
	public static final String PARAM_FIELD_GENERATORS = "FieldGenerators";
	public static final String PARAM_DOC_GENERATORS = "DocumentGenerators";
	public static final String PARAM_ID_FIELD = "IdField";
	public static final String PARAM_ID_PREFIX = "IdPrefix";

	@ConfigurationParameter(name = PARAM_FIELD_GENERATORS, mandatory = false, description = "An array of qualified Java class names. Each enumerated class must implement the FieldGenerator interface and is delivered by the user. These classes will be applied to the consumed CAS and populate Document instances with fields and thus determine the structure and content of the output documents. The field values are derived from CAS data. FieldGenerators always populate a single Document instance with fields. If multiple documents must be created for each CAS, refer to the DocumentGenerators parameter.")
	private List<FieldGenerator> fieldGenerators;
	@ConfigurationParameter(name = PARAM_DOC_GENERATORS, mandatory = false, description = "An array of qualified Java class names. Each enumerated class must extend the abstract DocumentGenerator class and is delivered by the user. Unlike FieldGenerator classes, DocumentGenerators put out whole Document instances instead of only populating a single Document with fields. This is required when multiple ElasticSearch documents should be created from a single CAS. When only the creation of a single document with a range of fields is required, leave this parameter empty and refer to the FieldGenerators parameter.")
	private List<DocumentGenerator> documentGenerators;
	@ConfigurationParameter(name = PARAM_FILTER_BOARDS, mandatory = false, description = "An array of qualified Java names. Each enumerated class must extend the FilterBoard class and is delivered by the user. FieldGenerators and DocumentGenerators may make use of several filters that a applied to tokens derived from UIMA annotations. Often, the same kind of filter is required across differnet fields (e.g. all full text fields will undergo a very similar text transformation process to create index tokens). To centralize the creation and management of the filters, one or multiple filter boards may be created. The filter boards are passed to each field and document generator. Also, the filter boards feature an annotation-driven access to the external resource mechanism used by UIMA for shared resources. Using shared resources helps to reduce memory consumption and the annotation-driven approach facilitates configuration.")
	private List<String> filterBoardClassNames;
	@ConfigurationParameter(name = PARAM_ID_FIELD, mandatory = false, description = "The name of the field that contains the document ID. If not set, the document ID will be read from the Header annotation of the CAS. If both methods to obtain a document ID fail, an exception will be raised.")
	private String idField;
	@ConfigurationParameter(name = PARAM_ID_PREFIX, mandatory = false, description = "A string that will be prepended to each document ID.")
	private String idPrefix;
	protected Gson gson;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		String[] filterBoardClassNames = (String[]) getContext().getConfigParameterValue(PARAM_FILTER_BOARDS);
		String[] fieldGeneratorClassNames = (String[]) getContext().getConfigParameterValue(PARAM_FIELD_GENERATORS);
		String[] documentGeneratorClassNames = (String[]) getContext().getConfigParameterValue(PARAM_DOC_GENERATORS);
		idField = (String) getContext().getConfigParameterValue(PARAM_ID_FIELD);
		idPrefix = (String) getContext().getConfigParameterValue(PARAM_ID_PREFIX);

		String template = "{}: {}";
		log.info(template, PARAM_FILTER_BOARDS, filterBoardClassNames);
		log.info(template, PARAM_FIELD_GENERATORS, fieldGeneratorClassNames);
		log.info(template, PARAM_DOC_GENERATORS, documentGeneratorClassNames);
		log.info(template, PARAM_ID_FIELD, idField);
		log.info(template, PARAM_ID_PREFIX, idPrefix);

		FilterRegistry filterRegistry = null;
		if (null != filterBoardClassNames) {
			filterRegistry = new FilterRegistry(aContext);
			filterRegistry.addFilterBoards(filterBoardClassNames);
		}
		fieldGenerators = new ArrayList<>();
		if (fieldGeneratorClassNames != null) {
			for (int i = 0; i < fieldGeneratorClassNames.length; i++) {
				String className = fieldGeneratorClassNames[i];
				try {
					Class<?> fieldsGeneratorClass = Class.forName(className);
					Constructor<?> constructor = fieldsGeneratorClass.getConstructor(FilterRegistry.class);
					FieldGenerator fieldsGenerator = (FieldGenerator) constructor.newInstance(filterRegistry);
					fieldGenerators.add(fieldsGenerator);
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
						| IllegalAccessException | InvocationTargetException e) {
					throw new ResourceInitializationException(e);
				}
			}
		}
		documentGenerators = new ArrayList<>();
		if (documentGeneratorClassNames != null) {
			for (int i = 0; i < documentGeneratorClassNames.length; i++) {
				String className = documentGeneratorClassNames[i];
				try {
					Class<?> documentsGeneratorClass = Class.forName(className);
					Constructor<?> constructor = documentsGeneratorClass.getConstructor(FilterRegistry.class);
					DocumentGenerator documentGenerator = (DocumentGenerator) constructor.newInstance(filterRegistry);
					documentGenerators.add(documentGenerator);
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
						| IllegalAccessException | InvocationTargetException e) {
					throw new ResourceInitializationException(e);
				}
			}
		}
		if (fieldGenerators.isEmpty() && documentGenerators.isEmpty())
			throw new ResourceInitializationException(new IllegalArgumentException(
					"Both FieldGenerators and DocumentGenerators are empty. At least one must be non-empty to create any documents."));

		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(RawToken.class, new RawToken.RawTokenGsonAdapter());
		builder.registerTypeAdapter(PreanalyzedFieldValue.class,
				new PreanalyzedFieldValue.PreanalyzedFieldValueGsonAdapter());
		gson = builder.create();
	}

	/**
	 * This is the default case: For each CAS, create one document. This document is
	 * populated with fields by field generators. The field generator classes are
	 * delivered by the user.
	 * 
	 * @param aJCas
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	protected Document convertCasToDocument(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			Document doc = new Document();
			for (int i = 0; i < fieldGenerators.size(); ++i) {
				fieldGenerators.get(i).addFields(aJCas, doc);
			}
			if (doc.isEmpty())
				log.debug("Document for document with ID {} does not contain any non-empty fields.",
						JCoReTools.getDocId(aJCas));
			String docId = JCoReTools.getDocId(aJCas);
			if (null != idField) {
				IFieldValue idFieldValue = doc.get(idField);
				if (idFieldValue instanceof RawToken) {
					docId = String.valueOf(((RawToken) idFieldValue).token);
				} else if (idFieldValue instanceof PreanalyzedFieldValue) {
					PreanalyzedFieldValue preAnalyzedIdValue = (PreanalyzedFieldValue) idFieldValue;
					docId = preAnalyzedIdValue.fieldString;
				} else
					throw new IllegalArgumentException("Class " + idFieldValue.getClass() + " for value of field "
							+ idField + " is not supported as ID field value");
			}
			if (null != idPrefix)
				docId = idPrefix + docId;
			if (docId == null)
				throw new AnalysisEngineProcessException(new IllegalStateException(
						"Could neither get a document ID from the generated document nor from the CAS directly. The generated document is: "
								+ gson.toJson(doc)));
			doc.setId(docId);
			return doc;
		} catch (Exception e) {
			log.error("Error with document ID {}.", JCoReTools.getDocId(aJCas));
			throw new AnalysisEngineProcessException(e);
		}
	}

	/**
	 * Advanced mode: It is also possible to create more than one document per CAS.
	 * By delivering DocumentGenerators, an arbitrary number of documents may be
	 * created for a CAS. Examples include one document for each sentence or one
	 * document for each detected gene etc. Internally, the DocumentGenerators
	 * employ FieldGenerators just as above.
	 * 
	 * @param aJCas
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	protected List<Document> convertCasToDocuments(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			List<Document> docs = new ArrayList<>();
			for (int i = 0; i < documentGenerators.size(); i++) {
				DocumentGenerator docGenerator = documentGenerators.get(i);
				docs.addAll(docGenerator.createDocuments(aJCas));
			}
			return docs;
		} catch (Exception e) {
			log.error("Error with document ID {}.", JCoReTools.getDocId(aJCas));
			throw new AnalysisEngineProcessException(e);
		}
	}

}
