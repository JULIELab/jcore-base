# JCoRe Utilities

This project offers a range of useful classes and static methods in the context of UIMA and JCoRe. For details please 
refer to the following class descriptions.

## JCoReAnnotationIndexMerger

This class takes multiple UIMA indices, e.g. Token, EntityMention and Part of Speech, and merges them into a single
iterator that then outputs the respective annotations as a sequence. The most useful functionality is the capability of
the annotation merger to sort the annotations efficiently on the fly by start offsets. Thus, when given a range of UIMA
annotation indices, the annotation merger is able to intertwine the annotations in correct reading order.

## JCoReAnnotationTools

This class offers the following static methods:
* `getAnnotationByClassName(JCas, String): Annotation`
* `getAnnotationAtOffset(JCas, String, int, int): Annotation`
* `getAnnotationAtMatchingOffsets(JCas, Annotation, Class): Annotation`
* `getOverlappingAnnotation(JCas, String, int, int): Annotation`
* `getPartiallyOverlappingAnnotation(JCas, Annotation, Class): Annotation`
* `getPartiallyOverlappingAnnotation(JCas, String, int, int): Annotation`
* `getIncludedAnnotations(JCas, Annotation, Class): List`
* `getIncludingAnnotation(JCas, Annotation, Class): Annotation`
* `getNearestIncludingAnnotation(JCas, Annotation, Class): Annotation`
* `getNearestOverlappingAnnotations(JCas, int, int, Class): List`
* `getNearestOverlappingAnnotations(JCas, Annotation, Class): List`
* `getLastOverlappingAnnotation(JCas, Annotation, Class): Annotation`

Most of this functionality is found in [UIMAfit](https://uima.apache.org/uimafit.html) nowadays and should be used from
there. Some very specific methods like `getLastOverlappingAnnotation` do not have a direct correspondent, however, and
might still be useful.

## JCoReCondensedDocumentText

This class is helpful when some parts of the CAS document text should be cut
out according to a set of specific annotations. The class then represents the
document text that results from cutting out said text passages. It offers a
method to return the actual text string and a method to map the character
offsets of the compacted string to the original CAS document text.

## JCoReFeaturePath

This class is an implementation of the UIMA [FeaturePath](https://uima.apache.org/d/uimaj-2.9.0/apidocs/org/apache/uima/cas/FeaturePath.html)
interface and adds some capabilities to it. Note that it also is currently missing some capabilities, most importantly it only
does not implement a range of interface methods. However, it is able to query arbitrary feature values through its
`getValue(FeatureStructure, int)` method, even if the methods to retrieve specific types of feature values (integers,
floats, bytes etc) are currently not implemented.
The `JCoReFeaturePath` uses the the same syntax as traditional UIMA feature paths but is also able to address specific
indices in multi-valued features. Suppose you have a `Gene` annotation and want to create feature
paths to access various information of the annotation. Then you could specify the following feature
paths in the constructor of a new `JCoReFeaturePath`: 

* `/specificType`: Returns the string value of the `specificType` feature
* `/resourceEntryList`: Returns the `FSArray` holding the `ResourceEntry` instances of the `gene` (a resource entry is a pointer into a database which is the 'resource')
* `/resourceEntryList/entryId`: Returns the `entryId` feature values of all `ResourceEntry` instances as an array
* `/resourceEntryList[0]/entryId`: Returns the `entryId` feature value of the first `ResourceEntry` instance
* `/resourceEntryList[-1]/entryId`: Returns the `entryId` feature value of the last `ResourceEntry` instance

Additionally, `JCoReFeaturePath` supports the following built-in functions:

* `coveredText()`: Calls `Annotation.getCoveredText` on the annotation pointed to with the feature path
* `typeName()`: Calls `Object.getClass().getName()` on the annotation pointed to by the feature path.

Built-in functions are used by specifying a feature path like illustrated above, appending a colon (`:`) and then one of
the built-in functions of the above. Suppose you have an `EventMention` with two `Argument` annotatations and want to get the covered
text of the first argument. Then create a `JCoReFeaturePath` with the following feature path and execute it on the `EventMention`:

*  `/arguments[0]/:coveredText()`: Returns the document text covered by the first `Argument` of the `EventMention`.

It might also be possible that your `EventMention` instances may have arguments of different types, e.g. `Gene` and
`miRNA`. To get the name of the actual argument type, use:

* `/arguments[0]/:typeName()`: Returns the name of type - or class - of the first argument

Finally, it is also possible to execute built-in functions directly on the annotation the feature path is executed upon:

* `/:coveredText()`: Returns the covered text of the input annotation and thus is equivalent to `annotation.getCoveredText()`

### Feature Value Replacement

An entire new capability of `JCoReFeaturePath` in comparison to the default UIMA feature paths is its capability to
replace existing feature values with a new value. For this purpose, the `JCoReFeaturePath` is given a map of replacements.
This map can be read from a two-column file with the `=` character as the separator. Lines beginning with `#` will be
ignored. Alternatively, the replacement map can be set directly.
When replacing values, `JCoReFeaturePath` will navigate to the feature pointed to by the given feature path and look up
the found feature value in the replacement map. If found, the mapped value from the map is placed to the feature instead
of the original value. If the feature value is not found in the map, a preconfigured default value can be used or the
feature value is left untouched.

## JCoReFSListIterator

An iterator implementing [FSIterator](http://uima.apache.org/d/uimaj-2.9.0/apidocs/org/apache/uima/cas/FSIterator.html) 
that takes a list of [FeatureStructures](http://uima.apache.org/d/uimaj-2.9.0/apidocs/org/apache/uima/cas/FeatureStructure.html) (i.e. annotations and their supertypes) and returns or navigates
its elements exactly in the order the list defines. Actually, the iterator
operates on the exact list, so changes are write-through. External changes to
the list while iterating may lead to undefined behavior of the iterator.<br>
Since no natural order of the list elements is assumed, the
`#moveTo(FeatureStructure)` method is currently not implemented since
it couldn't behave as defined in the contract of `FSIterator`. The purpose of this class
is to quickly obtain an `FSIterator` covering a list of given annotations for some APIs explicitly
requiring an `FSIterator`.

## JCoReTools

This class offers the following methods:

* `binarySearch(List, Function, Comparable): int`
* `binarySearch(List, Function, Comparable, int, int): int`
* `addToFSArray(FSArray, FeatureStructure, int): FSArray`
* `addToFSArray(FSArray, FeatureStructure): FSArray`
* `addToFSArray(FSArray, Collection): FSArray`
* `copyFSArray(FSArray): FSArray`
* `addToStringArray(StringArray, String[]): StringArray`
* `addToStringArray(StringArray, String): StringArray`
* `printFSArray(FSArray): void`
* `printAnnotationIndex(JCas, int): void`
* `getDocId(JCas): String`
* `deserializeXmi(CAS, InputStream, int): void`

The `binarySearch` methods work specifically on `Annotation` objects, sorted by given function.
The `addToFSArray` methods are useful for adding elements to `FSArrays` which are rather awkward
to use and, especially, to extend. The `addToStringArray` methods serve a similar purpose.
One of the most used methods from this list is `getDocId` which will look for an annotation of
type `de.julielab.jcore.types.Header` and return its `docId` feature value.
The `deserializeXmi` method is used in UIMA 2.x to fix issues with special Unicode characters. For
more information, refer to the JavaDoc of the method.

## Indexers

In the `index` subpackage, a range of `JCoReAnnotationIndex` implementations can be found.
These classes leverage the [Map](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html) and
[TreeSet](https://docs.oracle.com/javase/8/docs/api/java/util/TreeMap.html) classes to structure a given
set of annotations by some comparator, e.g. start offset comparator. After a first run where a concrete
index implementation is filled with the annotations from a CAS, the CAS annotation indices can be left
alone and the index can be consulted efficiently for specific annotations. This is used, for example,
using the overlap comparator defined in `de.julielab.jcore.utility.index.Comparators`. With this comparator,
one can find annotations overlapping a given annotation.

When using a map annotation index, the map keys are so-called `IndexTerms`. Consult the
`de.julielab.jcore.utility.index.TermGenerators` class for predefined index term generators which are
used for indexing and for searching. Index terms can be `long` numbers encoding start and end offsets of
annotations, for example, allowing for a very efficient retrieval of overlapping annotations.