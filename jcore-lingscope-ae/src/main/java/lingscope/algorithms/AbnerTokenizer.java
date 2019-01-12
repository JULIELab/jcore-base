/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lingscope.algorithms;

import abner.Scanner;
import java.io.StringReader;

/**
 *
 * @author shashank
 */
public class AbnerTokenizer {
    ////////////////////////////////////////////////////////////////
    /**
       <p>Take raw text apply ABNER's built-in tokenization on it.
     */
    public static String tokenize(String s) {
	StringBuffer sb = new StringBuffer();
	try {
	    Scanner scanner = new Scanner(new StringReader(s));
	    String t;
	    while ((t = scanner.nextToken()) != null) {
		sb.append(t+" ");
		if (t.toString().matches("[?!\\.]"))
		    sb.append("\n");
	    }
	    return sb.toString();
	} catch (Exception e) {
	    System.err.println(e);
	}
	return sb.toString();
    }

    /**
     * Takes an input and splits the sentence by punctuations and spaces, then
     * stitches it back together with a space and returns
     * @param input the input string to process
     * @return processed input string, where all words and punctuations are
     * seperated by space
     */
    public static String splitTermsByPunctuation(String input) {
        if (input.isEmpty()) {
            return "";
        }
        input = input.replaceAll("\\n", " ");
        String ret = tokenize(input).trim();
        if (ret.matches(".*\\w\\.$")) { // If a space is not put between the period in the end, then introduce one
            ret += " .";
        }
        if (input.endsWith(".") && (!ret.endsWith("."))) {
            ret += " .";
        }
        return ret;
    }
}
