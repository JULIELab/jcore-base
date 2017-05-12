
package de.julielab.jcore.ae.jnet.uima;

import org.apache.uima.jcas.tcas.Annotation;

public class UIMAUtils {


	/**
	 * check whether two annotation have exactly the same offset
	 * 
	 * @param a
	 *            annotation 1
	 * @param b
	 *            annotation 2
	 * @return true or false
	 */
	public static boolean hasSameOffset(final Annotation a, final Annotation b) {
		if ((a.getBegin() == b.getBegin()) && (a.getEnd() == b.getEnd()))
			return true;
		else
			return false;
	}

}
