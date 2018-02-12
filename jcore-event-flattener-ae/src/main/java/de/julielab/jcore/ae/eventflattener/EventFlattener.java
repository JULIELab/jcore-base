package de.julielab.jcore.ae.eventflattener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.ext.FlattenedRelation;

/**
 * Creates {@link FlattenedRelation} instances according to {@link EventMention}
 * annotations in the CAS. This means that multiple <tt>EventMention</tt>
 * annotations that are connected with each other (in the way that one event is
 * the argument of another) are all flattened together in a single annotation.
 * The position of the annotation will be the position of the root event type,
 * i.e. the event that is no argument to another event.
 * <p>
 * Each <tt>EventMention</tt> that is not the argument of another
 * <tt>EventMention</tt> is taken as a kind of 'event structure root'. For these
 * root events, the root itself and all its direct and indirect arguments are
 * assembled to form an instance of <tt>FlattenedRelation</tt>.
 * </p>
 * <p>
 * The features <tt>agents</tt> and <tt>patients</tt> are only filled for
 * regulations and their subtypes (negative/positive regulation). This is
 * because according to the event definition of BioNLP Shared Task - in which
 * format the <tt>EventMention</tt> annotations are expected only define causal
 * roles for arguments of those event types. For more information, please refer
 * to http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/detail.shtml#event.
 * 
 * @see http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/detail.shtml#event
 *      </p>
 * 
 * @author faessler
 * 
 */
@TypeCapability(inputs= {"de.julielab.jcore.types.EventMention"}, outputs= {"de.julielab.jcore.types.ext.FlattenedRelation"})
public class EventFlattener extends JCasAnnotator_ImplBase {

	private static final Logger log = LoggerFactory
			.getLogger(EventFlattener.class);

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			int flatEventCounter = 0;
			List<EventMention> topEvents = determineTopEvents(aJCas);
			for (EventMention topEvent : topEvents) {
				List<EventMention> events = collectEventsInTree(topEvent,
						new ArrayList<EventMention>());
				List<ArgumentMention> arguments = collectPrimitiveArguments(
						topEvent, new ArrayList<ArgumentMention>());
				List<ArgumentMention> agentArguments = null;
				List<ArgumentMention> patientArguments = null;
				switch (topEvent.getSpecificType()) {
				case "Regulation":
				case "Positive_regulation":
				case "Negative_regulation":
					agentArguments = collectAgentArguments(topEvent,
							new ArrayList<ArgumentMention>());
					patientArguments = collectPatientArguments(topEvent,
							new ArrayList<ArgumentMention>());
					break;
				default:
					break;
				}

				FSArray flatArgs = createFSArrayForList(aJCas, arguments);
				FSArray agentArgs = null != agentArguments ? createFSArrayForList(
						aJCas, agentArguments) : null;
				FSArray patientArgs = null != patientArguments ? createFSArrayForList(
						aJCas, patientArguments) : null;
				FSArray flatEvents = createFSArrayForList(aJCas, events);

				FlattenedRelation flattenedRelation = new FlattenedRelation(
						aJCas, topEvent.getBegin(), topEvent.getEnd());
				flattenedRelation.setRootRelation(topEvent);
				flattenedRelation.setArguments(flatArgs);
				flattenedRelation.setAgents(agentArgs);
				flattenedRelation.setPatients(patientArgs);
				flattenedRelation.setRelations(flatEvents);
				flattenedRelation.setId("FE" + flatEventCounter++);
				flattenedRelation.addToIndexes();
			}

		} catch (Exception e) {
			Header header = (Header) aJCas.getAnnotationIndex(Header.type)
					.iterator().next();
			log.error("Exception occurred in document {}: {}",
					header.getDocId(), e);
			throw new AnalysisEngineProcessException(e);
		}
	}

	private FSArray createFSArrayForList(JCas aJCas,
			List<? extends Annotation> arguments) {
		FSArray flatArgs = new FSArray(aJCas, arguments.size());
		for (int i = 0; i < arguments.size(); ++i)
			flatArgs.set(i, arguments.get(i));
		return flatArgs;
	}

	/**
	 * Collects all events that are descendants of <tt>event</tt> via argument
	 * connections in <tt>collector</tt> and returns it.
	 * 
	 * @param event
	 * @param collector
	 * @return
	 */
	private List<EventMention> collectEventsInTree(EventMention event,
			ArrayList<EventMention> collector) {
		collector.add(event);
		for (int i = 0; i < event.getArguments().size(); ++i) {
			ArgumentMention arg = event.getArguments(i);
			if (arg.getRef() instanceof EventMention)
				collectEventsInTree((EventMention) arg.getRef(), collector);
		}
		return collector;
	}

	private List<ArgumentMention> collectPatientArguments(
			EventMention topEvent, ArrayList<ArgumentMention> collector) {
		for (int i = 0; i < topEvent.getArguments().size(); ++i) {
			ArgumentMention arg = topEvent.getArguments(i);
			if (arg.getRole().equals("Theme")) {
				if (arg.getRef() instanceof EventMention)
					collectPrimitiveArguments((EventMention) arg.getRef(),
							collector);
				else
					collector.add(arg);
			}
		}
		return collector;
	}

	private List<ArgumentMention> collectAgentArguments(EventMention topEvent,
			ArrayList<ArgumentMention> collector) {
		for (int i = 0; i < topEvent.getArguments().size(); ++i) {
			ArgumentMention arg = topEvent.getArguments(i);
			if (arg.getRole().equals("Cause")) {
				if (arg.getRef() instanceof EventMention)
					collectPrimitiveArguments((EventMention) arg.getRef(),
							collector);
				else
					collector.add(arg);
			}
		}
		return collector;
	}

	private List<ArgumentMention> collectPrimitiveArguments(
			EventMention topEvent, List<ArgumentMention> collector) {
		for (int i = 0; i < topEvent.getArguments().size(); ++i) {
			ArgumentMention arg = topEvent.getArguments(i);
			if (arg.getRef() instanceof EventMention)
				collectPrimitiveArguments((EventMention) arg.getRef(),
						collector);
			else
				collector.add(arg);
		}
		return collector;
	}

	/**
	 * Returns the <tt>EventMention</tt>s in the CAS that are not the argument
	 * of another event.
	 * 
	 * @param events
	 * @return
	 */
	private List<EventMention> determineTopEvents(JCas aJCas) {
		List<EventMention> topEvents = new ArrayList<>();
		Set<EventMention> nonTopEvents = new HashSet<>();

		FSIterator<Annotation> eventit = aJCas.getAnnotationIndex(
				EventMention.type).iterator();
		while (eventit.hasNext()) {
			EventMention em = (EventMention) eventit.next();
			for (int i = 0; i < em.getArguments().size(); ++i) {
				ArgumentMention arg = em.getArguments(i);
				if (arg.getRef() instanceof EventMention) {
					nonTopEvents.add((EventMention) arg.getRef());
				}
			}

		}

		eventit = aJCas.getAnnotationIndex(EventMention.type).iterator();
		while (eventit.hasNext()) {
			EventMention em = (EventMention) eventit.next();
			if (!nonTopEvents.contains(em))
				topEvents.add(em);
		}
		return topEvents;
	}
}
