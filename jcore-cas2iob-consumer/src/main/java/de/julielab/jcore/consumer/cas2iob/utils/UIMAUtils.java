/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.consumer.cas2iob.utils;

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
	
	/**
	 * same as getAnnotations however boundaries for windowAnnotation and cls must exactly match!
	 * @param <T>
	 * @param jCas
	 * @param windowAnnotation
	 * @param cls
	 * @return list of annotations of type cls
	 */
	public static <T extends Annotation> List<T> getExactAnnotations(JCas jCas, Annotation windowAnnotation, Class<T> cls) {
		List<T> tmp = getAnnotations(jCas, windowAnnotation, cls);
		List<T> finalList = new ArrayList<T>();
		for(T anno:tmp) {
			//System.out.println("offset: " + anno.getBegin() + " - " + anno.getEnd() + " for  " + windowAnnotation.getBegin() + " - " + windowAnnotation.getEnd());
			if (hasSameOffset(windowAnnotation,anno)) {
				finalList.add(anno);
				//System.out.println("adding: ''" + anno.getCoveredText() + "'' for ''" + windowAnnotation.getCoveredText() + "''");
			} else {
				//System.out.println("not adding: " + anno.getCoveredText() + " for " + windowAnnotation.getCoveredText());
			}
		}
		return finalList;
	}
	
	/**
	 * check whether two annotation have exactly the same offset
	 * @param a annotation 1
	 * @param b annotation 2
	 * @return true or false
	 */
	public static boolean hasSameOffset(Annotation a, Annotation b) {
		if (a.getBegin()==b.getBegin() && a.getEnd() == b.getEnd()) {
			return true;
		} else {
			return false;
		}
	}

}
