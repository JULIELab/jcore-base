/** 
 * 
 * These are utils by Philipp Ogren.
 * They were taken from:
 * 
 * http://cslr.colorado.edu/ClearTK/index.cgi/chrome/site/api/src-html/edu/colorado/cleartk/util/AnnotationRetrieval.html#line.237
 **/

package de.julielab.jules.coordinationtagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * TODO this tools should be added to JULES Utils
 */
public class UIMAUtils {

	/**
	 * We initially used the AnnotationIndex.subiterator() but ran into issues that we wanted to
	 * work around concerning the use of type priorities when the window and windowed annotations
	 * have the same begin and end offsets. By using our own iterator we can back up to the first
	 * annotation that has the same begining as the window annotation and avoid having to set type
	 * priorities.
	 * 
	 * @param jCas
	 * @param windowAnnotation
	 * @return an FSIterator that is at the correct position
	 */
	private static FSIterator initializeWindowCursor(JCas jCas, Annotation windowAnnotation) {

		FSIterator cursor = jCas.getAnnotationIndex().iterator();

		cursor.moveTo(windowAnnotation);
		while (cursor.isValid() && ((Annotation) cursor.get()).getBegin() >= windowAnnotation.getBegin()) {
			cursor.moveToPrevious();
		}

		if (cursor.isValid()) {
			cursor.moveToNext();
		} else {
			cursor.moveToFirst();
		}
		return cursor;
	}

	/**
	 * This method returns all annotations (in order) that are inside a "window" annotation of a
	 * particular kind. The functionality provided is similar to using
	 * AnnotationIndex.subiterator(), however we found the use of type priorities which is
	 * documented in detail in their javadocs to be distasteful. This method does not require that
	 * type priorities be set in order to work as expected for the condition where the window
	 * annotation and the "windowed" annotation have the same size and location.
	 * 
	 * @param <T>
	 *            determines the return type of the method
	 * @param jCas
	 *            the current jCas or view
	 * @param windowAnnotation
	 *            an annotation that defines a window
	 * @param cls
	 *            determines the return type of the method
	 * @return a list of annotations of type cls that are "inside" the window
	 * @see AnnotationIndex#subiterator(org.apache.uima.cas.text.AnnotationFS)
	 */
	public static <T extends Annotation> List<T> getAnnotations(JCas jCas, Annotation windowAnnotation, Class<T> cls) {

		FSIterator cursor = initializeWindowCursor(jCas, windowAnnotation);

		List<T> annotations = new ArrayList<T>();
		while (cursor.isValid() && ((Annotation) cursor.get()).getBegin() <= windowAnnotation.getEnd()) {
			Annotation annotation = (Annotation) cursor.get();

			if (cls.isInstance(annotation) && annotation.getEnd() <= windowAnnotation.getEnd())
				annotations.add(cls.cast(annotation));

			cursor.moveToNext();
		}
		return annotations;
	}

}
