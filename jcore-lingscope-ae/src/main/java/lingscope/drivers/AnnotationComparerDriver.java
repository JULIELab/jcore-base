package lingscope.drivers;

import lingscope.algorithms.AnnotationComparer;

/**
 * Compares annotations between a gold and test file
 * @author shashank
 */
public class AnnotationComparerDriver {

    /**
     * 
     * @param args
     * 0 - gold file
     * 1 - test file
     */
    public static void main(String[] args) {
        AnnotationComparer comparer = new AnnotationComparer(10);
        comparer.compareAnnotationFiles(args[0], args[1]);
        comparer.printStats();
    }
}
