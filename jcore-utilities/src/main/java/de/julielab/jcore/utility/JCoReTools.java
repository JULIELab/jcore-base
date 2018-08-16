/**
 * JulesTools.java
 * <p>
 * Copyright (c) 2006, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * <p>
 * Author: muehlhausen
 * <p>
 * Current version: 1.3
 * Since version:   1.0
 * <p>
 * Creation date: Dec 11, 2006
 * <p>
 * Tool for creating new UIMA Objects and other UIMA related things
 **/

package de.julielab.jcore.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.DataResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.julielab.jcore.types.Header;

/**
 * <ul>
 * <li>The binarySearch methods work specifically on Annotation objects, sorted by given function.</li>
 * <li>The addToFSArray methods are useful for adding elements to FSArrays which are rather awkward to use and, especially, to extend.</li>
 * <li>The addToStringArray methods serve a similar purpose.</li>
 * <li>One of the most used methods from this list is {@link #getDocId(JCas)} which will look for an annotation of type de.julielab.jcore.types.Header and return its docId feature value.</li>
 * <li>The {@link #deserializeXmi(CAS, InputStream, int)} method is used in UIMA 2.x to fix issues with special Unicode characters. For more information, refer to the JavaDoc of the method.</li>
 * </ul>
 *
 * @author faessler
 */
public class JCoReTools {

    /**
     * Number of elements to be added if an FSArray needs to be resized, effectively creating a new, larger FSArray.
     */
    public static final int DEFAULT_ADDITION_SIZE = 10;
    /**
     * Logger for this class
     */
    private static final Logger log = LoggerFactory.getLogger(JCoReTools.class);

    /**
     * <p>
     * Returns an <code>FSArray</code> that contains all elements of the given array and <code>newElement</code>.
     * </p><p>The new element
     * is set into <code>array</code> if it has trailing <code>null</code> entries. Then, the new element is set to
     * the first position of <code>array</code> that is <code>null</code> and only followed by <code>null</code> entries until the end of the
     * array. If <code>array</code> is full, i.e. there are no trailing <code>null</code> entries, a new <code>FSArray</code>
     * of size <code>array.size()+{@link #DEFAULT_ADDITION_SIZE}</code> is created. All elements of <code>array</code> are copied into
     * the new <code>FSArray</code> and <code>newElement</code> is added after the last element of <code>array</code>.
     * Depending on <code>{@link #DEFAULT_ADDITION_SIZE}</code> there might be trailing <code>null</code> entries left in the new
     * <code>FSArray</code> they can be used to set further elements without the need to create a new <code>FSArray</code>.
     * </p><p>
     * In any case, it should be assumed that the return value is a new <code>FSArray</code>. Thus, one should not rely
     * on the possible in-place change of the passed <code>array</code> and replace the variable holding <code>array</code>
     * with the return value of this method.
     * </p>
     *
     * @param array      The array to what the feature structure should be added
     * @param newElement The feature structure that should be added to the array
     * @return An <code>FSArray</code> containing all entries from <code>array</code> plus <code>newElement</code>. The
     * returned <code>FSArray</code> will be <code>array</code> if there was enough space to add <code>newElement</code>
     * or a new <code>FSArray</code> otherwise.
     */
    public static FSArray addToFSArray(final FSArray array, final FeatureStructure newElement) {
        return addToFSArray(array, newElement, DEFAULT_ADDITION_SIZE);
    }

    /**
     * <p>
     * Returns an <code>FSArray</code> that contains all elements of the given array and <code>newElement</code>.
     * </p><p>The new element
     * is set into <code>array</code> if it has trailing <code>null</code> entries. Then, the new element is set to
     * the first position of <code>array</code> that is <code>null</code> and only followed by <code>null</code> entries until the end of the
     * array. If <code>array</code> is full, i.e. there are no trailing <code>null</code> entries, a new <code>FSArray</code>
     * of size <code>array.size()+additionSize</code> is created. All elements of <code>array</code> are copied into
     * the new <code>FSArray</code> and <code>newElement</code> is added after the last element of <code>array</code>.
     * Depending on <code>additionSize</code> there might be trailing <code>null</code> entries left in the new
     * <code>FSArray</code> they can be used to set further elements without the need to create a new <code>FSArray</code>.
     * </p><p>
     * In any case, it should be assumed that the return value is a new <code>FSArray</code>. Thus, one should not rely
     * on the possible in-place change of the passed <code>array</code> and replace the variable holding <code>array</code>
     * with the return value of this method.
     * </p>
     *
     * @param array        The array to what the feature structure should be added
     * @param newElement   The feature structure that should be added to the array
     * @param additionSize The size the array should be expanded
     * @return An <code>FSArray</code> containing all entries from <code>array</code> plus <code>newElement</code>. The
     * returned <code>FSArray</code> will be <code>array</code> if there was enough space to add <code>newElement</code>
     * or a new <code>FSArray</code> otherwise.
     */
    public static FSArray addToFSArray(final FSArray array, final FeatureStructure newElement, final int additionSize) {

        assert (additionSize > 0);

        FSArray outputArray = array;

        if (null == outputArray) {
            try {
                outputArray = new FSArray(newElement.getCAS().getJCas(), 1);
            } catch (CASException e1) {
                throw new JCoReUtilitiesException(e1);
            }
        }

        int lastElementIndex = outputArray.size() - 1;

        if (outputArray.get(lastElementIndex) != null) {
            log.trace("Last element of passed array was not null, thus array is full and a new one is created.");
            try {
                FSArray array2 = new FSArray(outputArray.getCAS().getJCas(), outputArray.size() + additionSize);
                array2.copyFromArray(outputArray.toArray(), 0, 0, outputArray.size());
                array2.set(lastElementIndex + 1, newElement);
                outputArray = array2;
                log.trace("New array is of size {}.", array2.size());
                return array2;
            } catch (CASException e) {
                e.printStackTrace();
            }
        } else {
            log.trace(
                    "There is still room left over in the passed array, new element is appended after the last non-null element.");
        }

        while ((lastElementIndex > 0) && (outputArray.get(lastElementIndex - 1) == null)) {
            lastElementIndex--;
        }
        log.trace("Last non-null element was found on index {}, adding new element on position {}.",
                lastElementIndex - 1, lastElementIndex);
        outputArray.set(lastElementIndex, newElement);
        return outputArray;
    }

    /**
     * <p>
     * Returns an <code>FSArray</code> that contains all elements of the given array and <code>newElements</code>.
     * </p><p>The new elements
     * are set into <code>inputArray</code> if it has trailing <code>null</code> entries. Then, the new elements are set to
     * the first positions of <code>inputArray</code> that are <code>null</code> and only followed by <code>null</code> entries until the end of the
     * array. If <code>inputArray</code> is too small, i.e. there are not enough trailing <code>null</code> entries, a new <code>FSArray</code>
     * of size <code>inputArray.size()+newElements.size()</code> is created. All elements of <code>inputArray</code> are copied into
     * the new <code>FSArray</code> and <code>newElements</code> are added after the last element of <code>inputArray</code>.
     * <p>
     * In any case, it should be assumed that the return value is a new <code>FSArray</code>. Thus, one should not rely
     * on the possible in-place change of the passed <code>array</code> and replace the variable holding <code>array</code>
     * with the return value of this method.
     * </p>
     *
     * @param inputArray  The array to what the feature structures should be added
     * @param newElements The feature structure that should be added to the array
     * @return An <code>FSArray</code> containing all entries from <code>inputArray</code> plus <code>newElements</code>. The
     * returned <code>FSArray</code> will be <code>inputArray</code> if there was enough space to add <code>newElements</code>
     * or a new <code>FSArray</code> otherwise.
     */
    public static FSArray addToFSArray(final FSArray inputArray,
                                       final Collection<? extends FeatureStructure> newElements) {

        FSArray array = inputArray;

        if (null == newElements || newElements.size() == 0)
            return array;

        if (null == array) {
            try {
                array = new FSArray(newElements.iterator().next().getCAS().getJCas(), 1);
            } catch (CASException e1) {
                throw new JCoReUtilitiesException(e1);
            }
        }

        try {
            int lastElementIndex = array.size() - 1;

            // Search for the last non-null element. If none is found,
            // lastElementIndex will actually be -1 after the
            // loop.
            while (lastElementIndex >= 0 && array.get(lastElementIndex) == null) {
                lastElementIndex--;
            }

            FSArray ret = null;
            // Is there enough space in the existing array to put all new
            // elements in it?
            int requiredSpace = lastElementIndex + 1 + newElements.size();
            if (requiredSpace <= array.size()) {
                log.trace(
                        "Existing array has size {}. Since space for {} elements is required the passed array is kept.",
                        array.size(), requiredSpace);
                ret = array;
            } else {
                log.trace("Passed array has size {} but there are {} elements overall, thus a new FSArray is created.",
                        array.size(), requiredSpace);
                // There is not enough space for all new elements in the given
                // FSArray so create a new one
                ret = new FSArray(array.getCAS().getJCas(), requiredSpace);
                for (int i = 0; i <= lastElementIndex; i++)
                    ret.set(i, array.get(i));
            }
            // Add the new elements.
            int currentIndex = lastElementIndex + 1;
            for (Iterator<? extends FeatureStructure> it = newElements.iterator(); it.hasNext(); currentIndex++) {
                ret.set(currentIndex, it.next());
            }
            return ret;
        } catch (CASException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a new <code>FSArray</code> with the exact size and contents of <code>array</code>. This is a shallow
     * copy, the array entries are copied by reference.
     *
     * @param array The <code>FSArray</code> to copy.
     * @return A new <code>FSArray</code> with the size and contents of <code>array</code>.
     */
    public static FSArray copyFSArray(FSArray array) {
        FSArray output = null;
        try {
            output = new FSArray(array.getCAS().getJCas(), array.size());
            for (int i = 0; i < array.size(); ++i)
                output.set(i, array.get(i));
        } catch (CASException e) {
            throw new JCoReUtilitiesException(e);
        }
        return output;
    }

    /**
     * <p>
     * Creates a new string array, copies the values of <code>array</code> into it and adds <code>element</code>.
     * </p>
     * <p>
     * This method does not handle <code>null</code> values as {@link #addToFSArray(FSArray, FeatureStructure, int)} does.
     * To add multiple elements at once, avoiding excessive copying, refer to {@link #addToStringArray(StringArray, String[])}.
     * </p>
     *
     * @param array   The source array to extend.
     * @param element The element to add.
     * @return A new <code>StringArray</code> with the same contents as <code>array</code> extended by <code>element</code>.
     */
    public static StringArray addToStringArray(StringArray array, String element) {
        try {
            StringArray newArray = null;
            if (array == null) {
                newArray = new StringArray(array.getCAS().getJCas(), 1);
            } else {
                newArray = new StringArray(array.getCAS().getJCas(), array.size() + 1);
                newArray.copyFromArray(array.toArray(), 0, 0, array.size());
            }
            newArray.set(newArray.size() - 1, element);
            return newArray;
        } catch (CASException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * <p>
     * Creates a new string array, copies the values of <code>array</code> into it and adds <code>elements</code>.
     * </p>
     *
     * @param array    The array to extend.
     * @param elements The elements to add into a new array.
     * @return A new <code>StringArray</code> containing all values of <code>array</code> plus <code>elements</code>.
     */
    public static StringArray addToStringArray(StringArray array, String[] elements) {
        if (null == elements)
            return null;
        try {
            StringArray newArray = null;
            if (array == null) {
                newArray = new StringArray(array.getCAS().getJCas(), elements.length);
                newArray.copyFromArray(elements, 0, 0, elements.length);
            } else {
                newArray = new StringArray(array.getCAS().getJCas(), array.size() + elements.length);
                newArray.copyFromArray(array.toArray(), 0, 0, array.size());
                newArray.copyFromArray(elements, 0, array.size(), elements.length);
            }
            return newArray;
        } catch (CASException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Prints the content of the FSArray to System.out
     *
     * @param array The array to be printed
     */
    public static void printFSArray(FSArray array) {
        for (int i = 0; i < array.size(); i++) {
            FeatureStructure fs = array.get(i);
            System.out.println("fs[" + i + "] =  " + fs);
        }
    }

    public static void printAnnotationIndex(JCas jCas, int type) {
        for (Iterator<Annotation> it = jCas.getAnnotationIndex(type).iterator(); it.hasNext(); ) {
            Annotation a = it.next();
            System.out.println("[" + a.getBegin() + "-" + a.getEnd() + "] " + a.getCoveredText());
        }

    }

    /**
     * <p>
     * Returns the document ID of the document in the <code>JCas</code>.
     * </p>
     * <p>
     * This can only be done when an annotation of type
     * <code>de.julielab.jcore.types.Header</code> (or a subtype) is present and
     * its feature <code>docId</code> is set.
     * </p>
     *
     * @param aJCas
     * @return The value of of {@link de.julielab.jcore.types.Header#getDocId()}
     */
    public static String getDocId(JCas aJCas) {
        AnnotationIndex<Annotation> headerIndex = aJCas.getAnnotationIndex(Header.type);
        FSIterator<Annotation> it = headerIndex.iterator();
        if (!it.hasNext())
            return null;
        Header header = (Header) it.next();
        String pubmedId = header.getDocId();
        return pubmedId;
    }

    /**
     * <p>
     * Deserializes an UTF-8 encoded XMI input stream into the given CAS.
     * </p>
     * <p>
     * This method has largely been taken directly from
     * {@link XmiCasDeserializer#deserialize(InputStream, CAS)}. However, the
     * given input stream is explicitly transformed into an UTF-8 encoded
     * {@link InputSource} for the XML parsing process. This is necessary
     * because the Xerces internal UTF-8 handling is faulty with Unicode
     * characters above the BMP (see
     * https://issues.apache.org/jira/browse/XERCESJ-1257). Thus, this method
     * explicitly uses UTF-8 encoding. For other encodings, use the default UIMA
     * deserialization mechanism.
     * </p>
     * <p>
     * The {@code attributeBufferSize} parameter only has an effect if the
     * julielab Xerces version is on the classpath. Then, the XMLStringBuffer
     * initial size is set via a system property. This can be very helpful for
     * documents because UIMA stores the document text as an attribute to the
     * sofa element in the XMI format. Such long attribute values are not
     * expected by Xerces which initializes its attribute buffers with a size of
     * 32 chars. Then, reading a large sofa (= document text) string results in
     * a very long process of resizing the buffer array and copying the old
     * buffer contents into the larger array. By setting a larger size from the
     * beginning, a lot of time can be saved.
     * </p>
     *
     * @param cas                 The CAS to populate.
     * @param is                  The XMI data stream to populate the CAS with.
     * @param attributeBufferSize
     * @throws SAXException
     * @throws IOException
     * @see <a href="https://issues.apache.org/jira/browse/XERCESJ-1257" target="_top">https://issues.apache.org/jira/browse/XERCESJ-1257</a>
     */
    public static void deserializeXmi(CAS cas, InputStream is, int attributeBufferSize)
            throws SAXException, IOException {
        Reader reader = new InputStreamReader(is, "UTF-8");
        InputSource source = new InputSource(reader);
        source.setEncoding("UTF-8");

        if (attributeBufferSize > 0)
            System.setProperty("julielab.xerces.attributebuffersize", String.valueOf(attributeBufferSize));

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        XmiCasDeserializer deser = new XmiCasDeserializer(cas.getTypeSystem());
        ContentHandler handler = deser.getXmiCasHandler(cas, false, null, -1);
        xmlReader.setContentHandler(handler);
        xmlReader.parse(source);

        System.clearProperty("julielab.xerces.attributebuffersize");
    }

    public static <T extends Annotation, R extends Comparable<R>> int binarySearch(List<T> annotations,
                                                                                   Function<T, R> comparisonValueFunction, R searchValue) {
        return binarySearch(annotations, comparisonValueFunction, searchValue, 0, annotations.size() - 1);
    }

    public static <T extends Annotation, R extends Comparable<R>> int binarySearch(List<T> annotations,
                                                                                   Function<T, R> comparisonValueFunction, R searchValue, int from, int to) {
        assert from <= to : "End offset is smaller than begin offset";
        int lookupIndex = from + (to - from) / 2;
        T annotation = annotations.get(lookupIndex);
        R comparisonValue = comparisonValueFunction.apply(annotation);
        int comparison = searchValue.compareTo(comparisonValue);
        if (comparison == 0)
            return lookupIndex;
        else if (comparison < 0) {
            if (from > lookupIndex - 1)
                return -(lookupIndex) - 1;
            return binarySearch(annotations, comparisonValueFunction, searchValue, from, lookupIndex - 1);
        } else {
            if (to < lookupIndex + 1)
                return -(lookupIndex + 1) - 1;
            return binarySearch(annotations, comparisonValueFunction, searchValue, lookupIndex + 1, to);
        }
    }

    /**
     * <p>
     * Helper method to transparently handle GZIPPed external resource files.
     * </p>
     * <p>When using external resources for analysis engines in UIMA, typically a custom object implementing {@link org.apache.uima.resource.SharedResourceObject}
     * is created as the resource provider. Since the overhead in handling external resources is mostly done when the resource is rather large, file
     * resources are commonly compressed using GZIP. This method takes the input stream of the {@link DataResource} object
     * passed by UIMA to {@link org.apache.uima.resource.SharedResourceObject#load(DataResource)} and checks if its URI
     * ends with .gzip or .gz. If so, the input stream is wrapped into a {@link GZIPInputStream}. This way a gzipped or
     * plain resource file can be used without further code adaptions.</p>
     *
     * @param resource The {@link DataResource} object passed to {@link org.apache.uima.resource.SharedResourceObject#load(DataResource)}.
     * @return The original input stream, if the resource URI did not end in .gz or .gzip, a GZIP input stream otherwise.
     * @throws IOException If reading the resource file fails.
     */
    public static InputStream resolveExternalResourceGzipInputStream(DataResource resource) throws IOException {
        InputStream is = resource.getInputStream();
        String lcUriString = resource.getUri().toString().toLowerCase();
        if (lcUriString.endsWith(".gz") || lcUriString.endsWith(".gzip"))
            is = new GZIPInputStream(is);
        return is;
    }
}
