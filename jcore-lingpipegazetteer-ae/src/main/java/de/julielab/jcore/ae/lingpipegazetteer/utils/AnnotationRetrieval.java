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
package de.julielab.jcore.ae.lingpipegazetteer.utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

//import edu.colorado.cleartk.types.SplitAnnotation;

/**
 * Copyright 2007 Regents of the University of Colorado. All Rights Reserved. This software is provided under the terms
 * of the <a href="https://www.cusys.edu/techtransfer_edit/downloads/Bulletin-SourceCodeAgreementNonprofitResearch.pdf">
 * CU Non-Profit Research License Agreement</a>
 * <p>
 * 
 * @author Philip Ogren, Philipp Wetzler
 * 
 */
public class AnnotationRetrieval {
	/**
	 * This method exists simply as a convenience method for unit testing. It is not very efficient and should not, in
	 * general be used outside the context of unit testing.
	 */
	public static <T extends Annotation> T get(JCas jCas, Class<T> cls, int index) {
		int type;
		try {
			type = (Integer) cls.getField("type").get(null);
		} catch (IllegalAccessException e) {
			throw new RuntimeException();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException();
		}

		// TODO we should probably iterate from the end rather than
		// iterating forward from the begining.
		FSIndex<?> fsIndex = jCas.getAnnotationIndex(type);
		if (index < 0)
			index = fsIndex.size() + index;

		if (index < 0 || index >= fsIndex.size())
			return null;
		FSIterator<?> iterator = fsIndex.iterator();
		Object returnValue = iterator.next();
		for (int i = 0; i < index; i++) {
			returnValue = iterator.next();
		}
		return cls.cast(returnValue);
	}

	public static <T extends Annotation> T get(JCas jCas, T annotation, int relativePosition) {
		return get(jCas, annotation, relativePosition, null);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T get(JCas jCas, T annotation, int relativePosition,
			Annotation windowAnnotation) {
		FSIterator<?> cursor = jCas.getAnnotationIndex(annotation.getType()).iterator();
		cursor.moveTo(annotation);

		if (relativePosition > 0) {
			for (int i = 0; i < relativePosition && cursor.isValid(); i++)
				cursor.moveToNext();
		} else {
			for (int i = 0; i < -relativePosition && cursor.isValid(); i++)
				cursor.moveToPrevious();
		}
		if (cursor.isValid()) {
			Annotation relativeAnnotation = (Annotation) cursor.get();

			T returnValue = ((Class<T>) annotation.getClass()).cast(relativeAnnotation);

			if (windowAnnotation != null) {
				if (AnnotationUtil.contains(windowAnnotation, relativeAnnotation))
					return returnValue;
				else
					return null;
			} else
				return returnValue;
		} else
			return null;
	}

	/**
	 * We initially used the AnnotationIndex.subiterator() but ran into issues that we wanted to work around concerning
	 * the use of type priorities when the window and windowed annotations have the same begin and end offsets. By using
	 * our own iterator we can back up to the first annotation that has the same begining as the window annotation and
	 * avoid having to set type priorities.
	 * 
	 * @param jCas
	 * @param windowAnnotation
	 * @return an FSIterator that is at the correct position
	 */
	private static FSIterator<?> initializeWindowCursor(JCas jCas, Annotation windowAnnotation) {

		FSIterator<?> cursor = jCas.getAnnotationIndex().iterator();

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
	 * @param <T>
	 *            determines the return type of the method
	 * @param jCas
	 *            the current jCas or view
	 * @param windowAnnotation
	 *            an annotation that defines a window
	 * @param cls
	 *            determines the return type of the method
	 * @return the last annotation of type cls that is "inside" the window
	 * @see #getAnnotations(JCas, Annotation, Class)
	 */
	public static <T extends Annotation> T getLastAnnotation(JCas jCas, Annotation windowAnnotation, Class<T> cls) {
		FSIterator<?> cursor = initializeWindowCursor(jCas, windowAnnotation);

		T currentBestGuess = null;
		while (cursor.isValid() && ((Annotation) cursor.get()).getBegin() <= windowAnnotation.getEnd()) {
			Annotation annotation = (Annotation) cursor.get();

			if (cls.isInstance(annotation) && annotation.getEnd() <= windowAnnotation.getEnd())
				currentBestGuess = cls.cast(annotation);
			cursor.moveToNext();
		}
		return currentBestGuess;
	}

	/**
	 * @param <T>
	 *            determines the return type of the method
	 * @param jCas
	 *            the current jCas or view
	 * @param windowAnnotation
	 *            an annotation that defines a window
	 * @param cls
	 *            determines the return type of the method
	 * @return the first annotation of type cls that is "inside" the window
	 * @see #getAnnotations(JCas, Annotation, Class)
	 */
	public static <T extends Annotation> T getFirstAnnotation(JCas jCas, Annotation windowAnnotation, Class<T> cls) {
		FSIterator<?> cursor = initializeWindowCursor(jCas, windowAnnotation);

		// I left in the while loop because the first annotation we see might not be the right class
		while (cursor.isValid() && ((Annotation) cursor.get()).getBegin() <= windowAnnotation.getEnd()) {
			Annotation annotation = (Annotation) cursor.get();
			if (cls.isInstance(annotation) && annotation.getEnd() <= windowAnnotation.getEnd())
				return cls.cast(annotation);
			cursor.moveToNext();
		}
		return null;
	}

	/*
	 * Find an annotation of a different type that covers the same span.
	 * 
	 * @param <T> is the type of annotation we're looking for
	 * 
	 * @param jCas is the current CAS view
	 * 
	 * @param windowAnnotation determines the span of the annotation
	 * 
	 * @return the first annotation in the index that has the same span as windowAnnotation
	 */
	@Deprecated
	public static <T extends Annotation> T getMatchingAnnotation(JCas jCas, Annotation windowAnnotation, Class<T> cls) {
		if (cls.isInstance(windowAnnotation))
			return cls.cast(windowAnnotation);
		// System.out.println("WINDOW ANNOTATION: " + windowAnnotation);
		FSIterator<?> cursor = jCas.getAnnotationIndex().iterator();
		/*
		 * while(cursor.hasNext()) { FeatureStructure fs = (FeatureStructure)cursor.next();
		 * System.err.println("fs in retrieval: " + fs); }
		 */
		cursor.moveTo(windowAnnotation);
		if (!cursor.isValid()) {
			// System.out.println("CURSOR NOT VALID!");
			throw new IllegalStateException(
					"An annotation of type "
							+ cls.getCanonicalName()
							+ " at offsets "
							+ windowAnnotation.getBegin()
							+ "-"
							+ windowAnnotation.getEnd()
							+ " es searched by giving a window annotation with these offsets. However, not even the window annotation itself was found (most certainly because it wasn't added to the CAS indexes). As a consequence, the desired annotation cannot be found even if it's there.");
//			return null;
		}
		Annotation cursorAnnotation = (Annotation) cursor.get();
		if (cursorAnnotation.getBegin() != windowAnnotation.getBegin()
				|| cursorAnnotation.getEnd() != windowAnnotation.getEnd()) {
			throw new IllegalStateException(
					"An annotation of type "
							+ cls.getCanonicalName()
							+ " at offsets "
							+ windowAnnotation.getBegin()
							+ "-"
							+ windowAnnotation.getEnd()
							+ " es searched by giving a window annotation with these offsets. However, not even the window annotation itself was found (most certainly because it wasn't added to the CAS indexes). As a consequence, the desired annotation cannot be found even if it's there.");
			// return null;
		}

		while (cursor.isValid() && cursorAnnotation.getBegin() == windowAnnotation.getBegin()
				&& cursorAnnotation.getEnd() == windowAnnotation.getEnd()) {
			cursor.moveToPrevious();
			if (cursor.isValid())
				cursorAnnotation = (Annotation) cursor.get();
			else
				cursorAnnotation = null;
		}

		if (cursor.isValid()) {
			cursor.moveToNext();
			cursorAnnotation = (Annotation) cursor.get();
		} else {
			cursor.moveToFirst();
			cursorAnnotation = (Annotation) cursor.get();
		}

		while (cursor.isValid() && cursorAnnotation.getBegin() == windowAnnotation.getBegin()
				&& cursorAnnotation.getEnd() == windowAnnotation.getEnd()) {
			if (cls.isInstance(cursorAnnotation))
				return cls.cast(cursorAnnotation);
			cursor.moveToNext();
			if (cursor.isValid())
				cursorAnnotation = (Annotation) cursor.get();
			else
				cursorAnnotation = null;
		}

		return null;
	}

	/**
	 * This method returns all annotations (in order) that are inside a "window" annotation of a particular kind. The
	 * functionality provided is similar to using AnnotationIndex.subiterator(), however we found the use of type
	 * priorities which is documented in detail in their javadocs to be distasteful. This method does not require that
	 * type priorities be set in order to work as expected for the condition where the window annotation and the
	 * "windowed" annotation have the same size and location.
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
		if (windowAnnotation instanceof SplitAnnotation)
			return getAnnotations(jCas, (SplitAnnotation) windowAnnotation, cls);

		FSIterator<?> cursor = initializeWindowCursor(jCas, windowAnnotation);

		List<T> annotations = new ArrayList<T>();
		while (cursor.isValid() && ((Annotation) cursor.get()).getBegin() <= windowAnnotation.getEnd()) {
			Annotation annotation = (Annotation) cursor.get();

			if (cls.isInstance(annotation) && annotation.getEnd() <= windowAnnotation.getEnd())
				annotations.add(cls.cast(annotation));

			cursor.moveToNext();
		}
		return annotations;
	}

	public static <T extends Annotation> List<T> getAnnotations(JCas jCas, SplitAnnotation windowAnnotation,
			Class<T> cls) {
		List<T> returnValues = new ArrayList<T>();

		for (FeatureStructure subAnnotation : windowAnnotation.getAnnotations().toArray()) {
			returnValues.addAll(getAnnotations(jCas, (Annotation) subAnnotation, cls));
		}

		return returnValues;
	}

	public static <T extends Annotation> List<T> getAnnotations(JCas jCas, int begin, int end, Class<T> cls) {
		Annotation annotation = new Annotation(jCas, begin, end);
		return getAnnotations(jCas, annotation, cls);
	}

	/**
	 * This method provides a way to have multiple annotations define the window that is used to constrain the
	 * annotations in the returned list. The intersection of the windows is determined and used. This method will treat
	 * any split annotations in the list as a contiguous annotation.
	 * 
	 * @see #getAnnotations(JCas, Annotation, Class)
	 * @see #getAnnotations(JCas, SplitAnnotation, Class)
	 */
	public static <T extends Annotation> List<T> getAnnotations(JCas jCas, List<Annotation> windowAnnotations,
			Class<T> cls) {
		if (windowAnnotations == null || windowAnnotations.size() == 0)
			return null;

		int windowBegin = Integer.MIN_VALUE;
		int windowEnd = Integer.MAX_VALUE;
		for (Annotation windowAnnotation : windowAnnotations) {
			if (windowAnnotation.getBegin() > windowBegin)
				windowBegin = windowAnnotation.getBegin();
			if (windowAnnotation.getEnd() < windowEnd)
				windowEnd = windowAnnotation.getEnd();
		}
		return getAnnotations(jCas, windowBegin, windowEnd, cls);
	}

	/**
	 * This method finds the smallest annotation of containingType that begins before or at the same character as the
	 * focusAnnotation and ends after or at the same character as the focusAnnotation.
	 * 
	 * @param jCas
	 * @param focusAnnotation
	 * @param containingType
	 *            the type of annotation you want returned
	 * @return an annotation of type containingType that contains the focus annotation or null it one does not exist.
	 */
	/*
	 * //TODO think about how to make this method faster by avoiding using a constrained iterator. See TODO note for
	 * //get adjacent annotation. public static Annotation getContainingAnnotation(JCas jCas, Annotation
	 * focusAnnotation, Class<? extends Annotation> cls) { Type containingType = UIMAUtil.getCasType(jCas, cls);
	 * ConstraintFactory constraintFactory = jCas.getConstraintFactory();
	 * 
	 * FeaturePath beginFeaturePath = jCas.createFeaturePath();
	 * beginFeaturePath.addFeature(containingType.getFeatureByBaseName("begin")); FSIntConstraint intConstraint =
	 * constraintFactory.createIntConstraint(); intConstraint.leq(focusAnnotation.getBegin()); FSMatchConstraint
	 * beginConstraint = constraintFactory.embedConstraint(beginFeaturePath, intConstraint);
	 * 
	 * FeaturePath endFeaturePath = jCas.createFeaturePath();
	 * endFeaturePath.addFeature(containingType.getFeatureByBaseName("end")); intConstraint =
	 * constraintFactory.createIntConstraint(); intConstraint.geq(focusAnnotation.getEnd()); FSMatchConstraint
	 * endConstraint = constraintFactory.embedConstraint(endFeaturePath, intConstraint);
	 * 
	 * FSMatchConstraint windowConstraint = constraintFactory.and(beginConstraint,endConstraint); FSIndex windowIndex =
	 * jCas.getAnnotationIndex(containingType); FSIterator windowIterator =
	 * jCas.createFilteredIterator(windowIndex.iterator(), windowConstraint);
	 * 
	 * Annotation shortestWindow = null; int shortestWindowSize = Integer.MAX_VALUE; while(windowIterator.hasNext()) {
	 * Annotation window = (Annotation) windowIterator.next(); int windowSize = AnnotationUtil.size(window);
	 * if(shortestWindow == null || windowSize < shortestWindowSize) { shortestWindow = window; shortestWindowSize =
	 * windowSize; } } return shortestWindow; }
	 */
	/**
	 * Finds and returns the annotation of the provided type that is adjacent to the focus annotation in either to the
	 * left or right. Adjacent simply means the last annotation of the passed in type that ends before the start of the
	 * focus annotation (for the left side) or the first annotation that starts after the end of the focus annotation
	 * (for the right side). Thus, adacent refers to order of annotations at the annotation level (e.g. give me the
	 * first annotation of type x to the left of the focus annotation) and does not mean that the annotations are
	 * adjacenct at the character offset level.
	 * 
	 * <br>
	 * <b>note:</b> This method runs <b>much</b> faster if the type of the passed in annotation and the passed in type
	 * are the same.
	 * 
	 * @param jCas
	 * @param focusAnnotation
	 *            an annotation that you want to find an annotation adjacent to this.
	 * @param adjacentType
	 *            the type of annotation that you want to find
	 * @param adjacentBefore
	 *            if true then returns an annotation to the left of the passed in annotation, otherwise an annotation to
	 *            the right will be returned.
	 * @return an annotation of type adjacentType or null
	 */
	/*
	 * public static Annotation getAdjacentAnnotation(JCas jCas, Annotation focusAnnotation, Class<? extends Annotation>
	 * adjacentClass, boolean adjacentBefore) { try { Type adjacentType = UIMAUtil.getCasType(jCas, adjacentClass);
	 * if(focusAnnotation.getType().equals(adjacentType)) { FSIterator iterator =
	 * jCas.getAnnotationIndex(adjacentType).iterator(); iterator.moveTo(focusAnnotation); if(adjacentBefore)
	 * iterator.moveToPrevious(); else iterator.moveToNext(); return (Annotation) iterator.get(); } else { FSIterator
	 * cursor = jCas.getAnnotationIndex().iterator(); cursor.moveTo(focusAnnotation); if(adjacentBefore) {
	 * while(cursor.isValid()) { cursor.moveToPrevious(); Annotation annotation = (Annotation) cursor.get();
	 * if(adjacentClass.isInstance(annotation) && annotation.getEnd() <= focusAnnotation.getBegin()) return annotation;
	 * } } else { while(cursor.isValid()) { cursor.moveToNext(); Annotation annotation = (Annotation) cursor.get();
	 * if(adjacentClass.isInstance(annotation) && annotation.getBegin() >= focusAnnotation.getEnd()) return annotation;
	 * } } } }catch(NoSuchElementException nsee) { return null; } return null; }
	 */
}

// public static FSIterator getWindowIterator(JCas jCas, Annotation windowAnnotation, Class<? extends Annotation>
// featureClass)
// {
// // Type type = UIMAUtil.getCasType(jCas, featureClass);
// // return jCas.getAnnotationIndex(type).subiterator(windowAnnotation);
// return getWindowIterator(jCas, windowAnnotation.getBegin(), windowAnnotation.getEnd(), featureClass);
// }
//
// public static FSIterator getWindowIterator(JCas jCas, List<Annotation> windowAnnotations, Class<? extends Annotation>
// featureClass) {
//
// int windowBegin = Integer.MIN_VALUE;
// int windowEnd = Integer.MAX_VALUE;
// for(Annotation windowAnnotation : windowAnnotations)
// {
// if(windowAnnotation.getBegin() > windowBegin)
// windowBegin = windowAnnotation.getBegin();
// if(windowAnnotation.getEnd() < windowEnd)
// windowEnd = windowAnnotation.getEnd();
// }
// System.out.println("windowBegin = "+windowBegin);
// return getWindowIterator(jCas, windowBegin, windowEnd, featureClass);
// }
//
// public static FSIterator getWindowIterator(JCas jCas, int windowBegin, int windowEnd, Class<? extends Annotation>
// featureClass) {
// Type type = UIMAUtil.getCasType(jCas, featureClass);
// ConstraintFactory constraintFactory = jCas.getConstraintFactory();
//
// FeaturePath beginFeaturePath = jCas.createFeaturePath();
// beginFeaturePath.addFeature(type.getFeatureByBaseName("begin"));
// FSIntConstraint intConstraint = constraintFactory.createIntConstraint();
// intConstraint.geq(windowBegin);
// FSMatchConstraint beginConstraint = constraintFactory.embedConstraint(beginFeaturePath, intConstraint);
//
// FeaturePath endFeaturePath = jCas.createFeaturePath();
// endFeaturePath.addFeature(type.getFeatureByBaseName("end"));
// intConstraint = constraintFactory.createIntConstraint();
// intConstraint.leq(windowEnd);
// FSMatchConstraint endConstraint = constraintFactory.embedConstraint(endFeaturePath, intConstraint);
//
// FSMatchConstraint windowConstraint = constraintFactory.and(beginConstraint,endConstraint);
// FSIndex windowIndex = jCas.getAnnotationIndex(type);
// FSIterator windowIterator = jCas.createFilteredIterator(windowIndex.iterator(), windowConstraint);
//
// return windowIterator;
//
// }

