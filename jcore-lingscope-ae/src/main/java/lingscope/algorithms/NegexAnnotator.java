/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lingscope.algorithms;

import lingscope.algorithms.negex.GenNegEx;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Annotates negation using Negex
 *
 * @author shashank
 */
public abstract class NegexAnnotator extends Annotator {

    protected GenNegEx negex;
    protected ArrayList<String> rules;

    public NegexAnnotator(String beginTag, String interTag, String otherTag) {
        super(beginTag, interTag, otherTag);
        negex = null;
    }

    @Override
    public void serializeAnnotator(String trainingFile, String modelFile) {
        throw new UnsupportedOperationException("NegEx's serialized version can be downloaded from the internet.");
    }

    @Override
    public void loadAnnotator(InputStream is) {
        negex = new GenNegEx();
        Scanner sc = new Scanner(is);
        rules = new ArrayList();
        while (sc.hasNextLine()) {
            rules.add(sc.nextLine());
        }
        sc.close();
    }
}
