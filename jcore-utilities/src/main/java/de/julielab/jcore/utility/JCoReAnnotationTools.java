/**
 * AnnotationTools.java
 * <p>
 * Copyright (c) 2006, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * <p>
 * Author: tomanek
 * <p>
 * Current version: 1.3
 * Since version:   1.0
 * <p>
 * Creation date: Feb 19, 2006
 * <p>
 * Tool for creating new UIMA annotation Objects and other annotation related things
 * <p>
 * //TODO: we may move some functions from JulesTools here...
 **/
package de.julielab.jcore.utility;

import org.apache.commons.lang3.Range;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// import de.julielab.jcore.types.Annotation;

/**
 * Most of this functionality is found in UIMAfit nowadays and should be used from there.
 * Some very specific methods like {@link #getOverlappingAnnotation(JCas, String, int, int)}  do not have a
 * direct correspondent, however, and might still be useful.
 */
public class JCoReAnnotationTools {

    /**
     * returns an annotation object (de.julielab.jcore.types.annotation) of the
     * type specified by fullEntityClassName. This is done by means of dynamic
     * class loading and reflection.
     *
     * @param aJCas
     *            the jcas to which to link this annotation object
     * @param fullAnnotationClassName
     *            the full class name of the new annotation object
     * @return
     */
    public static Annotation getAnnotationByClassName(JCas aJCas, String fullAnnotationClassName)
            throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class[] parameterTypes = new Class[]{JCas.class};
        Class myNewClass = Class.forName(fullAnnotationClassName);
        Constructor myConstructor = myNewClass.getConstructor(parameterTypes);
        Annotation anno = (Annotation) myConstructor.newInstance(aJCas);
        return anno;
    }

    /**
     * returns an annotation of the type fullEntityClassName which has exactly
     * the specified offset
     *
     * @param aJCas
     *            the cas to search in
     * @param fullAnnotationClassName
     *            the full class name of the specific annotation type
     *
     * @param startOffset
     * @param endOffset
     * @return the first annotation object of the given type at exactly the
     *         given offset. If no annotation is found there, NULL is returned
     */
    public static Annotation getAnnotationAtOffset(JCas aJCas, String fullAnnotationClassName, int startOffset,
                                                   int endOffset) throws SecurityException, IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        Annotation anno = getAnnotationByClassName(aJCas, fullAnnotationClassName);
        JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
        Iterator annoIter = indexes.getAnnotationIndex(anno.getTypeIndexID()).iterator();
        while (annoIter.hasNext()) {
            Annotation currAnno = (Annotation) annoIter.next();
            if ((currAnno.getBegin() == startOffset) && (currAnno.getEnd() == endOffset)) {
                return currAnno;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getAnnotationAtMatchingOffsets(JCas aJCas, Annotation focusAnnotation,
                                                                          Class<T> cls) {
        FSIterator<Annotation> cursor = aJCas.getAnnotationIndex().iterator();

        cursor.moveTo(focusAnnotation);

        if (!cursor.isValid())
            throw new IllegalArgumentException(
                    "Given FocusAnnotation was not found in the JCas' annotation index: " + focusAnnotation);

        while (cursor.isValid() && cursor.get().getBegin() >= focusAnnotation.getBegin()) {
            cursor.moveToPrevious();
        }
        if (!cursor.isValid())
            cursor.moveToFirst();
        else
            cursor.moveToNext();

        // Now that we have our starting point, we go to the right until we find
        // an annotation of the correct type and
        // the same offsets as focusAnnotation
        Annotation currentAnnotation = null;
        while (cursor.isValid() && (currentAnnotation = cursor.get()).getBegin() <= focusAnnotation.getEnd()) {
            if (!cls.isInstance(currentAnnotation)) {
                cursor.moveToNext();
                continue;
            }
            Range<Integer> currentRange = Range.between(currentAnnotation.getBegin(), currentAnnotation.getEnd());
            Range<Integer> focusRange = Range.between(focusAnnotation.getBegin(), focusAnnotation.getEnd());
            if (cursor.isValid() && cls.isInstance(currentAnnotation) && focusRange.equals(currentRange))
                return (T) currentAnnotation;
            cursor.moveToNext();
        }
        return null;
    }

    /**
     * returns an annotation of the type fullEntityClassName which overlaps an
     * or is overlapped by an annotation of the same type at the given offset
     *
     * @param aJCas
     *            The cas to search in
     * @param ullAnnotationClassName
     *            The full class name of the specific annotation type
     *
     * @param startOffset
     * @param endOffset
     * @return The first annotation object of the given type at exactly the
     *         given offset. If no annotation is found there, NULL is returned
     */
    public static Annotation getOverlappingAnnotation(JCas aJCas, String fullAnnotationClassName, int startOffset,
                                                      int endOffset) throws SecurityException, IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        Annotation anno = getAnnotationByClassName(aJCas, fullAnnotationClassName);
        JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
        Iterator annoIter = indexes.getAnnotationIndex(anno.getTypeIndexID()).iterator();
        while (annoIter.hasNext()) {
            Annotation currAnno = (Annotation) annoIter.next();

            if ((currAnno.getBegin() <= startOffset) && (currAnno.getEnd() >= endOffset)) {
                return currAnno;
            } else if ((currAnno.getBegin() >= startOffset) && (currAnno.getEnd() <= endOffset)) {
                return currAnno;
            }
            //
            else if ((currAnno.getBegin() < endOffset) && (currAnno.getEnd() > endOffset)) {
                return currAnno;
            } else if ((currAnno.getBegin() < startOffset) && (currAnno.getEnd() > startOffset)) {
                return currAnno;
            }
        }
        return null;
    }

    /**
     * returns an annotation of the type fullEntityClassName which partially
     * overlaps an or is overlapped by an annotation of the same type at the
     * given offset
     *
     * @param aJCas
     *            The cas to search in
     * @param fullAnnotationClassName
     *            The full class name of the specific annotation type
     *
     * @param startOffset
     * @param endOffset
     * @return The first annotation object of the given type at exactly the
     *         given offset. If no annotation is found there, NULL is returned
     */
    public static Annotation getPartiallyOverlappingAnnotation(JCas aJCas, String fullAnnotationClassName,
                                                               int startOffset, int endOffset) throws SecurityException, IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        Annotation anno = getAnnotationByClassName(aJCas, fullAnnotationClassName);
        JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
        Iterator annoIter = indexes.getAnnotationIndex(anno.getTypeIndexID()).iterator();
        while (annoIter.hasNext()) {
            Annotation currAnno = (Annotation) annoIter.next();

            if ((currAnno.getBegin() < endOffset) && (currAnno.getEnd() > endOffset)) {
                return currAnno;
            } else if ((currAnno.getBegin() < startOffset) && (currAnno.getEnd() > startOffset)) {
                return currAnno;
            }
        }
        return null;
    }

    /**
     * Returns the leftmost annotation of type <tt>cls</tt> that overlaps
     * <tt>focusAnnotation</tt>. That is, if multiple annotations of type
     * <tt>cls</tt> overlap with <tt>focusAnnotation</tt>, the one with the
     * lowest begin offset will be chosen.
     * <p>
     * The two annotations may overlap in any way (partial, nested, inclusion,
     * exact match). This algorithm has <tt>O(n)</tt> runtime with <tt>n</tt>
     * being the number of annotations in the annotation index.
     * </p>
     * *
     * <p>
     * TODO: A start offset parameter could be introduced from where to start
     * looking. This way, when iterating over a number of different
     * focusAnnotations in ascending order, one would have only to check from
     * focusAnnotation to focusAnnotation and not always from the very beginning
     * of the annotation index. Same thing for getIncludingAnnotation().
     * </p>
     *
     * @param aJCas
     * @param focusAnnotation
     * @param cls
     * @return the leftmost annotation of type <tt>cls</tt> that overlaps
     *         <tt>focusAnnotation</tt>.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getPartiallyOverlappingAnnotation(JCas aJCas, Annotation focusAnnotation,
                                                                             Class<T> cls) {
        FSIterator<Annotation> cursor = aJCas.getAnnotationIndex().iterator();

        // Annotations are sorted by begin offset and may be arbitrarily long.
        // Thus we just have to start from the
        // beginning.
        cursor.moveToFirst();

        // Now go to the right as long as we don't yet overlap with the focus
        // annotation, then stop.
        Annotation currentAnnotation = null;
        while (cursor.isValid() && ((currentAnnotation = cursor.get()).getEnd() <= focusAnnotation.getBegin()
                || !cls.isInstance(currentAnnotation))) {
            cursor.moveToNext();
        }

        // Check whether we have found an overlapping annotation.
        Range<Integer> currentRange = Range.between(currentAnnotation.getBegin(), currentAnnotation.getEnd());
        Range<Integer> focusRange = Range.between(focusAnnotation.getBegin(), focusAnnotation.getEnd());
        if (cursor.isValid() && cls.isInstance(currentAnnotation) && currentRange.isOverlappedBy(focusRange))
            return (T) cursor.get();

        return null;
    }

    /**
     * Returns, in ascending order, all annotations of type <tt>cls</tt> that
     * are completely included - perhaps with having the same begin and/or end
     * as the <tt>focusAnnotation</tt> - in <tt>focusAnnotation</tt>.
     *
     * @param aJCas
     * @param focusAnnotation
     * @param cls
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> getIncludedAnnotations(JCas aJCas, Annotation focusAnnotation,
                                                                        Class<T> cls) {
        FSIterator<Annotation> cursor = aJCas.getAnnotationIndex().iterator();

        // for debugging: print out absolutely all annotations
        // cursor.moveToFirst();
        // while (cursor.isValid()) {
        // System.out.println(cursor.get());
        // cursor.moveToNext();
        // }

        cursor.moveTo(focusAnnotation);
        if (!cursor.isValid())
            throw new IllegalArgumentException(
                    "Given FocusAnnotation was not found in the JCas' annotation index: " + focusAnnotation);

        // The annotations are sorted by begin offset. So go to the first
        // annotation with a lower begin offset compared
        // to the focusAnnotation.
        while (cursor.isValid() && cursor.get().getBegin() >= focusAnnotation.getBegin()) {
            cursor.moveToPrevious();
        }
        if (!cursor.isValid())
            cursor.moveToFirst();
        else
            cursor.moveToNext();

        // Now that we have our starting point, we go to the right as long as
        // there is a possibility to still find
        // annotations included in the focusAnnotation, i.e. as long the current
        // begin offset is still lower (or equal
        // for the weird case of zero-length-annotations) than the
        // end offset of the focusAnnotation
        Annotation currentAnnotation = null;
        List<T> includedAnnotations = new ArrayList<>();
        while (cursor.isValid() && (currentAnnotation = cursor.get()).getBegin() <= focusAnnotation.getEnd()) {
            if (!cls.isInstance(currentAnnotation)) {
                cursor.moveToNext();
                continue;
            }
            Range<Integer> currentRange = Range.between(currentAnnotation.getBegin(), currentAnnotation.getEnd());
            Range<Integer> focusRange = Range.between(focusAnnotation.getBegin(), focusAnnotation.getEnd());
            if (cursor.isValid() && cls.isInstance(currentAnnotation) && focusRange.containsRange(currentRange))
                includedAnnotations.add((T) currentAnnotation);
            cursor.moveToNext();
        }
        return includedAnnotations;
    }

    /**
     * Returns the leftmost annotation of type <tt>cls</tt> that completely
     * includes <tt>focusAnnotation</tt>. That is, if multiple annotations of
     * type <tt>cls</tt> include <tt>focusAnnotation</tt>, the one with the
     * lowest begin offset will be chosen.
     * <p>
     * This algorithm has <tt>O(n)</tt> runtime with <tt>n</tt> being the number
     * of annotations in the annotation index.
     * </p>
     * <p>
     * TODO: A start offset parameter could be introduced from where to start
     * looking. This way, when iterating over a number of different
     * focusAnnotations in ascending order, one would have only to check from
     * focusAnnotation to focusAnnotation and not always from the very beginning
     * of the annotation index. Same thing for
     * getPartiallyOverlappingAnnotation().
     * </p>
     *
     * @param aJCas
     * @param focusAnnotation
     * @param cls
     * @return the leftmost annotation of type <tt>cls</tt> that completely
     *         includes <tt>focusAnnotation</tt>.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getIncludingAnnotation(JCas aJCas, Annotation focusAnnotation,
                                                                  Class<T> cls) {
        FSIterator<Annotation> cursor = aJCas.getAnnotationIndex().iterator();

        // Annotations are sorted by begin offset and may be arbitrarily long.
        // Thus we just have to start from the
        // beginning.
        cursor.moveToFirst();

        // Now go to the right as long as we don't yet overlap with the focus
        // annotation, then stop.
        Annotation currentAnnotation = null;
        while (cursor.isValid() && ((currentAnnotation = cursor.get()).getEnd() < focusAnnotation.getEnd()
                || !cls.isInstance(currentAnnotation))) {
            cursor.moveToNext();
        }

        // Check whether we have found an overlapping annotation.
        Range<Integer> currentRange = Range.between(currentAnnotation.getBegin(), currentAnnotation.getEnd());
        Range<Integer> focusRange = Range.between(focusAnnotation.getBegin(), focusAnnotation.getEnd());
        if (cursor.isValid() && cls.isInstance(currentAnnotation) && currentRange.containsRange(focusRange))
            return (T) cursor.get();

        return null;
    }

    /**
     * Returns the nearest annotation of class <tt>cls</tt> to
     * <tt>focusAnnotation</tt>, i.e. the one (or just one, if multiple exist)
     * with the highest start-offset that completely overlaps
     * <tt>focusAnnotation</tt>.
     * <p>
     * This method has nice performance properties when it is known that the
     * annotation looked for is near, e.g. finding the nearest token or
     * sentence.
     * </p>
     *
     * @param aJCas
     * @param focusAnnotation
     * @param cls
     * @return the leftmost annotation of type <tt>cls</tt> that completely
     *         includes <tt>focusAnnotation</tt>.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getNearestIncludingAnnotation(JCas aJCas, Annotation focusAnnotation,
                                                                         Class<T> cls) {
        FSIterator<Annotation> cursor = aJCas.getAnnotationIndex().iterator();

        if (!cursor.isValid())
            throw new IllegalArgumentException(
                    "Given FocusAnnotation was not found in the JCas' annotation index: " + focusAnnotation);

        // The annotations are sorted by begin offset. So go to the first
        // annotation with a larger begin offset compared
        // to the focusAnnotation. Afterwards we we search for an including
        // annotation to the left.
        cursor.moveTo(focusAnnotation);
        while (cursor.isValid() && cursor.get().getBegin() <= focusAnnotation.getBegin()) {
            cursor.moveToNext();
        }
        if (!cursor.isValid())
            cursor.moveTo(focusAnnotation);
        else
            cursor.moveToPrevious();

        // Now that we have our starting point, we go to the left until we find
        // the first annotation of correct type
        // completely overlapping the focus annotation.
        while (cursor.isValid()) {
            Annotation currentAnnotation = cursor.get();
            if (!cls.isInstance(currentAnnotation)) {
                cursor.moveToPrevious();
                continue;
            }
            Range<Integer> currentRange = Range.between(currentAnnotation.getBegin(), currentAnnotation.getEnd());
            Range<Integer> focusRange = Range.between(focusAnnotation.getBegin(), focusAnnotation.getEnd());
            if (cursor.isValid() && cls.isInstance(currentAnnotation) && currentRange.containsRange(focusRange))
                return (T) currentAnnotation;
            cursor.moveToPrevious();
        }
        return null;
    }

    /**
     * Convenience method that internally calls
     * {@link #getNearestOverlappingAnnotations(JCas, Annotation, Class)}. This
     * method creates a dummy focus annotation with the given offsets, adds it
     * to CAS indexes, receives the overlapping annotations and removes the
     * focus annotation from indexes again.
     * <p>
     * This method has nice performance properties when it is known that the
     * annotation looked for is near, e.g. finding overlapping tokens.
     * </p>
     *
     * @param aJCas
     *            CAS with the annotations.
     * @param begin
     *            Focus begin.
     * @param end
     *            Focus end.
     * @param cls
     *            The class of the sought annotations.
     * @return the leftmost annotation of type <tt>cls</tt> that completely
     *         includes <tt>focusAnnotation</tt>.
     */
    public static <T extends Annotation> List<T> getNearestOverlappingAnnotations(JCas aJCas, int begin, int end,
                                                                                  Class<T> cls) {
        Annotation focusAnnotation = new Annotation(aJCas, begin, end);
        focusAnnotation.addToIndexes();
        try {
            return getNearestOverlappingAnnotations(aJCas, focusAnnotation, cls);
        } finally {
            focusAnnotation.removeFromIndexes();
        }
    }

    /**
     * Returns the nearest annotations of class <tt>cls</tt> to
     * <tt>focusAnnotation</tt>. Those are all annotations <tt>a</tt> with
     * <code>a.begin > focus.begin</code> but still overlapping <tt>focus</tt>
     * plus the annotation with <code>a.begin <= focus.begin</code>.
     * <p>
     * This method has nice performance properties when it is known that the
     * annotation looked for is near, e.g. finding overlapping tokens.
     * </p>
     *
     * @param aJCas
     *            The CAS with annotations.
     * @param focus
     *            The focus annotation to get overlapping annotations for.
     * @param cls
     * @return the leftmost annotation of type <tt>cls</tt> that completely
     *         includes <tt>focusAnnotation</tt>.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> getNearestOverlappingAnnotations(JCas aJCas, Annotation focus,
                                                                                  Class<T> cls) {
        FSIterator<Annotation> cursor = aJCas.getAnnotationIndex().iterator();

//		 for debugging: print out absolutely all annotations
//		 cursor.moveToFirst();
//		 while (cursor.isValid()) {
//		 System.out.println(cursor.get());
//		 cursor.moveToNext();
//		 }

        cursor.moveTo(focus);
        if (!cursor.isValid())
            throw new IllegalArgumentException(
                    "Given FocusAnnotation was not found in the JCas' annotation index: " + focus);

        // The annotations are sorted by begin offset. So go to the first
        // annotation with a larger begin offset compared
        // to the focusAnnotation's end offset since then there won't be any
        // more overlapping annotations to the right.
        while (cursor.isValid() && cursor.get().getBegin() <= focus.getEnd()) {
            cursor.moveToNext();
        }
        if (!cursor.isValid())
            cursor.moveToLast();
        else
            cursor.moveToPrevious();

        List<T> overlappingAnnotations = new ArrayList<>();
        while (cursor.isValid()) {
            Annotation currentAnnotation = cursor.get();
            if (!cls.isInstance(currentAnnotation)) {
                cursor.moveToPrevious();
                continue;
            }
            if (cursor.isValid() && currentAnnotation.getBegin() < focus.getEnd()
                    && currentAnnotation.getEnd() > focus.getBegin()) {
                overlappingAnnotations.add((T) currentAnnotation);
                // As soon as we have an overlapping annotation of the correct
                // type that begins at or before the begin
                // offset of the
                // focusAnnotation, we are finished.
                if (currentAnnotation.getBegin() <= focus.getBegin()) {
                    Collections.reverse(overlappingAnnotations);
                    return overlappingAnnotations;
                }
            }
            cursor.moveToPrevious();
        }
        // Order by ascending begin offsets.
        Collections.reverse(overlappingAnnotations);
        return overlappingAnnotations;
    }

    /**
     * Returns the annotation with the highest end offset of type <tt>cls</tt>
     * overlapping <tt>focusAnnotation</tt>.
     * <p>
     * This method is very similar to
     * {@link #getNearestOverlappingAnnotations(JCas, Annotation, Class)}.
     * Actually, the last result element of
     * {@link #getNearestOverlappingAnnotations(JCas, Annotation, Class)} equals
     * the returned annotation from this method.
     * </p>
     *
     * @param aJCas
     * @param focusAnnotation
     * @param cls
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getLastOverlappingAnnotation(JCas aJCas, Annotation focusAnnotation,
                                                                        Class<T> cls) {
        FSIterator<Annotation> cursor = aJCas.getAnnotationIndex().iterator();

        // for debugging: print out absolutely all annotations
        // cursor.moveToFirst();
        // while (cursor.isValid()) {
        // System.out.println(cursor.get());
        // cursor.moveToNext();
        // }

        cursor.moveTo(focusAnnotation);
        if (!cursor.isValid())
            throw new IllegalArgumentException(
                    "Given FocusAnnotation was not found in the JCas' annotation index: " + focusAnnotation);

        // The annotations are sorted by begin offset. So go to the first
        // annotation with a larger begin offset compared
        // to the focusAnnotation's end offset since then there won't be any
        // more overlapping annotations to the right.
        while (cursor.isValid() && cursor.get().getBegin() <= focusAnnotation.getEnd()) {
            cursor.moveToNext();
        }
        if (!cursor.isValid())
            cursor.moveToLast();
        else
            cursor.moveToPrevious();

        while (cursor.isValid()) {
            Annotation currentAnnotation = cursor.get();
            if (!cls.isInstance(currentAnnotation)) {
                cursor.moveToPrevious();
                continue;
            }
            if (cursor.isValid() && currentAnnotation.getBegin() < focusAnnotation.getEnd()
                    && currentAnnotation.getEnd() > focusAnnotation.getBegin()) {
                return (T) currentAnnotation;
            }
            cursor.moveToPrevious();
        }
        return null;
    }

}
