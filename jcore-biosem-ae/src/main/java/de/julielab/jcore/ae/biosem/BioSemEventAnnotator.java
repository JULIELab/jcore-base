/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.biosem;

import corpora.DataLoader;
import de.julielab.jcore.types.*;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import relations.EventExtraction;
import relations.PData;
import relations.Word;
import utils.BioSemException;
import utils.DBUtils;

import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class BioSemEventAnnotator extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(BioSemEventAnnotator.class);

	public final static String RESOURCE_TRAINED_DB = "TrainedDB";

	private DataLoader loader;

	private DBUtils trainedDb;

	@ExternalResource(key = RESOURCE_TRAINED_DB, mandatory = true)
	private DBUtilsProvider dbUtilsProvider;

	private EventExtraction xtr;

	/**
	 * We use this static object to synchronize open and close calls to the
	 * document database between multiple threads. This stuff should be thread
	 * safe but pipelines get stuck on a regular basis on database opening which
	 * in turn ends up in HSQLDB where in a static HashMap all databases are
	 * stored. The access is synchronized and everything should be alright, but
	 * obviously, it isn't.
	 */
	private static Object lock = new Object();

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			dbUtilsProvider = (DBUtilsProvider) aContext.getResourceObject(RESOURCE_TRAINED_DB);
			trainedDb = dbUtilsProvider.getTrainedDatabase();
		} catch (ResourceAccessException e) {
			throw new ResourceInitializationException(e);
		}
		loader = new DataLoader();
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String text = aJCas.getDocumentText();
		FSIterator<Annotation> headerIt = aJCas.getAnnotationIndex(Header.type).iterator();
		String docId;
		if (headerIt.hasNext()) {
			Header header = (Header) headerIt.next();
			docId = header.getDocId();
		} else {
			docId = "<unknown ID>";
		}
		DBUtils docDb = null;
		try {
			log.debug("Processing document {}", docId);
			Map<String, Gene> proteins = enumerateProteins(aJCas);
			if (proteins.isEmpty()) {
				log.debug(
						"Skipping event extraction for this document because no proteins have been found that could be involved in an event.");
				return;
			}
            System.out.println(aJCas.getDocumentText());
			List<String> proteinLines = getProteinLines(proteins, docId);
			// Sometimes we have problems creating the text database.
			// Unfortunately, I'm not sure why this is. However, we'd rather
			// want to skip those cases instead of letting the pipeline fail as
			// a whole.
			synchronized (lock) {
				try {
					docDb = loader.Txt2Db(docId, text, proteinLines);
				} catch (NullPointerException e) {
					log.debug(
							"Could not create text database for document {} due to NullPointerException during creation. Trying to close the DB and open it again after a short delay",
							docId);
					Thread.sleep(10000);
					try {
						docDb = loader.Txt2Db(docId + "-secondtry", text, proteinLines);
					} catch (Exception e2) {
						log.error("Repeatedly failed to create text database for document " + docId
								+ ". This document will be skipped. Exception was: ", e2);
						throw e2;
					}
				}
			}
			if (null == xtr) {
				xtr = new EventExtraction(trainedDb, docDb);
			} else {
				xtr.setDb(docDb);
			}
			xtr.Test();
			Set<Word> triggers = xtr.getExtractedTriggers();
			Set<PData> events = xtr.getExtractedEvents();
			log.debug("Got {} triggers from BioSem.", triggers.size());
			log.debug("Got {} events from BioSem.", events.size());
			addEventsToIndexes(events, proteins, triggers, aJCas);
		} catch (BioSemException e) {
			log.debug("BioSemException occurred: ", e);
		} catch (Exception e) {
			log.error("Error occurred in document " + docId + ":", e);
			throw new AnalysisEngineProcessException(e);
		} finally {
			try {
				if (docDb != null) {
					synchronized (lock) {
						docDb.closeDB();
					}
				}
			} catch (Exception e) {
				log.warn(
						"Exception while shutting down document database for document {}. Since events have already been extracted, this is a minor error taken for itself. However it could lead to subsequent errors in the HSQL database system which could be critical.",
						docId);
			}

		}
	}

	private void addEventsToIndexes(Set<PData> events, Map<String, Gene> proteinMap, Set<Word> triggers, JCas aJCas) {
		Map<String, Word> triggerMap = new HashMap<>();
		Map<String, PData> eventMap = new HashMap<>();
		HashMap<String, EventMention> eventAnnotations = new HashMap<String, EventMention>();
		HashMap<String, EventTrigger> triggerAnnotations = new HashMap<String, EventTrigger>();

		for (Word trg : triggers) {
			triggerMap.put(trg.TID, trg);
		}
		for (PData event : events) {
			eventMap.put(event.PID, event);
		}

		for (PData event : events) {
			try {
				addEventToIndexes(event, proteinMap, triggerMap, eventAnnotations, triggerAnnotations, aJCas);
			} catch (UnknownProteinIdException e) {
				String docId = "<unknown>";
				FSIterator<Annotation> it = aJCas.getAnnotationIndex(Header.type).iterator();
				if (it.hasNext()) {
					Header h = (Header) it.next();
					docId = h.getDocId();
				}
				log.debug("Exception occurred in document " + docId
						+ ". The respective event will be skipped. Error was: ", e);
			}
		}

	}

	private EventMention addEventToIndexes(PData event, Map<String, Gene> proteinMap, Map<String, Word> triggerMap,
			HashMap<String, EventMention> eventAnnotations, HashMap<String, EventTrigger> triggerAnnotations,
			JCas aJCas) throws UnknownProteinIdException {
		EventTrigger uimaTrigger = triggerAnnotations.get(event.trig_ID);
		if (null == uimaTrigger) {
			Word trg = triggerMap.get(event.getTrigger().TID);
			if (null == trg) {
				throw new IllegalStateException("BioSem event \"" + event + "\" refers to trigger with ID "
						+ event.trig_ID + " which could not be found.");
			}
			uimaTrigger = addTriggerToIndexes(trg, aJCas);
		}
		EventMention uimaEvent;
		uimaEvent = eventAnnotations.get(event.PID);
		if (null == uimaEvent) {
			int begin = uimaTrigger.getBegin();
			int end = uimaTrigger.getEnd();
			Word protArg1 = event.getPro1();
			Word protArg2 = event.getPro2();
			PData eventArg1 = event.getPdata1();
			PData eventArg2 = event.getPdata2();
			uimaEvent = new EventMention(aJCas, begin, end);
			uimaEvent.setId(event.PID);
			uimaEvent.setSpecificType(uimaTrigger.getSpecificType());
			uimaEvent.setTrigger(uimaTrigger);
			// Begin at the LAST argument! This way we don't have to bother so
			// much with the FSArray holding the arguments.
			if (null != protArg2) {
				addUimaEventArgument(uimaEvent, protArg2, 2, proteinMap, triggerMap, eventAnnotations,
						triggerAnnotations, aJCas);
			}
			if (null != eventArg2) {
				addUimaEventArgument(uimaEvent, eventArg2, 2, proteinMap, triggerMap, eventAnnotations,
						triggerAnnotations, aJCas);
			}
			if (null != protArg1) {
				addUimaEventArgument(uimaEvent, protArg1, 1, proteinMap, triggerMap, eventAnnotations,
						triggerAnnotations, aJCas);
			}
			if (null != eventArg1) {
				addUimaEventArgument(uimaEvent, eventArg1, 1, proteinMap, triggerMap, eventAnnotations,
						triggerAnnotations, aJCas);
			}
			uimaEvent.addToIndexes();
			eventAnnotations.put(event.PID, uimaEvent);
		}

		return uimaEvent;
	}

	/**
	 * 
	 * @param uimaEvent
	 *            The UIMA event annotation to add a new argument to
	 * @param bioSemArg
	 *            The BioSem-object of the argument, i.e. a <tt>Word</tt> when
	 *            the argument is a protein or a <tt>PData</tt> if the argument
	 *            is another event
	 * @param argPos
	 *            The position of the argument, 1 (Theme) or 2 (Theme2 when the
	 *            argument is a protein and it's a Binding event, Cause
	 *            otherwise)
	 * @param proteinMap
	 * @param triggerAnnotations
	 * @param triggerMap
	 * @param aJCas
	 *            The CAS to connect the new <tt>ArgumentMention</tt> annotation
	 *            with
	 * @throws UnknownProteinIdException
	 */
	private void addUimaEventArgument(EventMention uimaEvent, Object bioSemArg, int argPos,
			Map<String, Gene> proteinMap, Map<String, Word> triggerMap, HashMap<String, EventMention> eventAnnotations,
			HashMap<String, EventTrigger> triggerAnnotations, JCas aJCas) throws UnknownProteinIdException {
		if (null == bioSemArg) {
			throw new IllegalArgumentException("An argument that should be added to the event " + uimaEvent
					+ " at position " + argPos + " was null.");
		}
		// Create the UIMA ArgumentMention
		ArgumentMention uimaArg = null;
		if (bioSemArg instanceof Word) {
			Word wordArg = (Word) bioSemArg;
			Gene protein = proteinMap.get(wordArg.TID);
			if (null == protein) {
				// This error could be checked some day, BioSem seems to extend
				// protein annotations based on string matching or something
				// which
				// could cause this error. There is also sometimes an error
				// about 'missing' proteins which seems odd because we give all
				// protein annotations to BioSem as an input. What exactly is
				// meant by 'missing'?. Currently the reason for the error
				// is not really known but seems rare enough to ignore for
				// the moment.
				throw new UnknownProteinIdException("BioSem returned a protein event argument with ID " + wordArg.TID
						+ " which is no valid gene/protein annotation ID in this CAS. Protein keys: "
						+ proteinMap.keySet() + ". Word: " + wordArg.word
						+ ". This event is skipped. This error should be examined. It hasn't yet due to time reasons...");

			}
			// we need to set the specific type to protein because otherwise the
			// BioNLP writer won't write the .a1 file (i.e. this is irrelevant
			// if we don't want to use the writer).
			protein.setSpecificType("protein");
			uimaArg = new ArgumentMention(aJCas, protein.getBegin(), protein.getEnd());
			uimaArg.setRef(protein);
			uimaArg.setRole(determineArgumentRole(uimaEvent, uimaArg, argPos));
		} else if (bioSemArg instanceof PData) {
			PData eventArg = (PData) bioSemArg;
			EventMention uimaEventArg = eventAnnotations.get(eventArg.PID);
			// The argument is an event itself. So we have to create the
			// EventMention first. This is a recursive call to the method that
			// called this method in the first place.
			if (null == uimaEventArg) {
				uimaEventArg = addEventToIndexes(eventArg, proteinMap, triggerMap, eventAnnotations, triggerAnnotations,
						aJCas);
			}
			if (null == uimaEventArg) {
				throw new IllegalStateException("Creating UIMA EventMention annotation for BioSem event \""
						+ eventArg.toString() + "\" failed, the UIMA EventMention is null.");
			}
			uimaArg = new ArgumentMention(aJCas, uimaEventArg.getBegin(), uimaEventArg.getEnd());
			uimaArg.setRef(uimaEventArg);
			uimaArg.setRole(determineArgumentRole(uimaEvent, uimaArg, argPos));
		} else {
			throw new IllegalArgumentException(
					"Unsupported event argument was passed for the creation of a UIMA ArgumentMention: " + bioSemArg);
		}
		// End of UIMA ArgumentMention creation

		// Now add the newly created ArgumentMention to the EventMention.
		// In the calling code, we always begin with the last argument. So the
		// argument array has only created once with the correct size to hold
		// all arguments. Thus, we either have an existing array with enough
		// space or we create a new one. Everything else is an error.
		FSArray uimaArgs = uimaEvent.getArguments();
		if (uimaArgs != null && argPos <= uimaArgs.size()) {
			if (uimaArgs.get(argPos - 1) != null) {
				throw new IllegalStateException("The UIMA ArgumentMention " + uimaArg + " should be put on position "
						+ (argPos - 1) + " of UIMA event " + uimaEvent + ". But there is already an argument there: "
						+ uimaArgs.get(argPos - 1));
			}
			uimaArgs.set(argPos - 1, uimaArg);
		} else if (null != uimaArgs) {
			throw new IllegalStateException("The UIMA ArgumentMention " + uimaArg
					+ " should be put on position position " + (argPos - 1) + " of UIMA event " + uimaEvent
					+ ". However, there already exists an argument FSArray but it is too small (size: "
					+ uimaArgs.size() + " to take the new argument. This shouldn't happen by design of the code.");
		} else {
			uimaArgs = new FSArray(aJCas, argPos);
			uimaArgs.set(argPos - 1, uimaArg);
		}
		uimaEvent.setArguments(uimaArgs);
	}

	/**
	 * 
	 * @param uimaEvent
	 * @param uimaArg
	 * @param argPos
	 * @return
	 * 		<ul>
	 *         <li><em>Theme</em> if <tt>argPos</tt> is 1</li>
	 *         <li><em>Theme2</em> when it's the second argument and it's a
	 *         protein (actually: when the argument is not another
	 *         <tt>EventMention</tt>) and the event type is "Binding"</li>
	 *         <li><em>Cause</em> otherwise</li>
	 *         </ul>
	 */
	private String determineArgumentRole(EventMention uimaEvent, ArgumentMention uimaArg, int argPos) {
		if (argPos == 1) {
			return "Theme";
		}
		if (uimaEvent.getSpecificType().equals("Binding") && !(uimaArg.getRef() instanceof EventMention)) {
			return "Theme2";
		}
		return "Cause";
	}

	private EventTrigger addTriggerToIndexes(Word trg, JCas aJCas) {
		String id = trg.TID;
		int begin = trg.locs[0];
		int end = trg.locs[1];
		String type = trg.type;
		EventTrigger uimaTrigger = new EventTrigger(aJCas, begin, end);
		uimaTrigger.setId(id);
		uimaTrigger.setSpecificType(type);
		return uimaTrigger;
	}

	/**
	 * The protein lines have to match the Shared Task 2011 format:<br/>
	 * <code>
	 * ID&lt;tab&gt;Entity-Type[Protein]&lt;tab&gt;start&lt;tab&gt;end&lt;tab&gt;Mention name
	 * </code> <br/>
	 * Example: <samp> T3 Protein 166 174 TGF-beta </samp>
	 * 
	 * @return
	 */
	private List<String> getProteinLines(Map<String, Gene> proteins, String docId) throws AnnotatorProcessException {
		List<String> proteinLines = new ArrayList<>();
		for (Entry<String, Gene> proteinEntry : proteins.entrySet()) {
			String id = proteinEntry.getKey();
			Gene gene = proteinEntry.getValue();
			try {
				proteinLines.add(
						id + "\tProtein\t" + gene.getBegin() + "\t" + gene.getEnd() + "\t" + gene.getCoveredText());
			} catch (Exception e) {
				log.error("Failed to collect protein information for relation extraction for document {}", docId, e);
				throw new AnnotatorProcessException(e);
			}
		}
		return proteinLines;
	}

	/**
	 * Assigns an ID of the form <tt>Ti</tt> to each gene in the CAS, <tt>i</tt>
	 * being an enumeration number beginning at 0.
	 * 
	 * @param aJCas
	 * @return
	 */
	private Map<String, Gene> enumerateProteins(JCas aJCas) {
		int i = 0; // just enumerate all genes
		Map<String, Gene> proteins = new HashMap<>();
		FSIterator<Annotation> geneIt = aJCas.getAnnotationIndex(Gene.type).iterator();
		// lastEnd holds the end offset of the gene in the prior iteration; we
		// use it to avoid overlapping genes. Those would most likely be an
		// error and cause errors in BioSem
		int lastEnd = 0;
		while (geneIt.hasNext()) {
			Gene gene = (Gene) geneIt.next();
			if (gene.getBegin() < lastEnd)
				continue;
			String id = gene.getId();
			// if (StringUtils.isBlank(id))
			id = "T" + i++;
			gene.setId(id);
			proteins.put(id, gene);
			lastEnd = gene.getEnd();
		}
		log.debug("Got {} non-overlapping genes/proteins in the document.", proteins.size());
		return proteins;
	}

	@Override
	public void destroy() {
		super.destroy();
		dbUtilsProvider.closeDatabase();
	}

}
