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
package de.julielab.jcore.ae.jnet.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class IOBEvaluation {

	public String getType() {
		return "IOB tags";
	}

	/**
	 * calculates precision, recall and f-measure, the first argument is takens
	 * as the goldstandard, the second one is to be evaluated. To get the IOB
	 * format out of the PipedFormat, use class Conversion (convertIOB)
	 * 
	 * @param gold_tok
	 *            ArrayList, each element is a String containing the token and
	 *            the (IOB) tag (seperated with a tab)
	 * @param eval_tok
	 *            same format as gold_tok
	 * @return P/R/F
	 */
	public static double[] evaluate(final ArrayList<?> gold_tok,
			final ArrayList<?> eval_tok) throws Exception {

		if (gold_tok.size() != eval_tok.size()) {
			System.out.println(gold_tok);
			System.out.println(eval_tok);
			throw new Exception(
					"size of gold standard different from predicted data");
		}

		final ArrayList<String> gold = new ArrayList<String>();
		final ArrayList<String> eval = new ArrayList<String>();
		for (int i = 0; i < gold_tok.size(); i++) {
			final String[] gTok = ((String) gold_tok.get(i)).split("[\\s]+");
			final String[] eTok = ((String) eval_tok.get(i)).split("\t");

			// check format
			if (gTok.length != 2) {
				System.err
						.println("ERR: format error in gold file. IOB format must be: token<tab>label");
				System.exit(-1);
			} else if (eTok.length != 2) {
				System.err
						.println("ERR: format error in eval file. IOB format must be: token<tab>label");
				System.exit(-1);
			}

			gold.add(gTok[1]);
			eval.add(eTok[1]);
		}

		return getValuesMulti(gold, eval);
	}

	private static double[] getValuesMulti(final ArrayList<String> gold,
			final ArrayList<String> eval) {

		if (gold.size() != eval.size()) {
			System.err.println("error!, gold.size!=eval.size -> I quit!");
			System.exit(0);
		}

		final HashMap<?, ?> gold_chunks = getChunksMulti(gold);
		final HashMap<?, ?> eval_chunks = getChunksMulti(eval);

		int numcorr = 0;
		final int numans = eval_chunks.size();
		final int numref = gold_chunks.size();

		// now check the blocks
		for (final Iterator<?> iter = eval_chunks.keySet().iterator(); iter
				.hasNext();) {
			final String offset = (String) iter.next();
			if (gold_chunks.containsKey(offset)) {
				final String tags_eval = (String) eval_chunks.get(offset);
				final String tags_gold = (String) gold_chunks.get(offset);
				if (tags_eval.equals(tags_gold))
					numcorr++;
				// System.out.println ("hit! " + offset + ": " + tags_eval);
			}
		}

		// System.out.println("\n");
		// System.out.println("numref (# entities in gold): " + numref);
		// System.out.println("numans (# entities in test): " + numans);
		// System.out.println("numcorr (# correct entities in test): " +
		// numcorr);

		double precision = 0;
		double recall = 0;
		double fscore = 0;

		if (numans > 0)
			precision = numcorr / (double) numans;
		if (numref > 0)
			recall = numcorr / (double) numref;
		if ((precision + recall) > 0)
			fscore = (2 * precision * recall) / (precision + recall);
		// System.out.println("recall: " + recall);
		// System.out.println("precision: " + precision);
		// System.out.println("f-measure: " + fscore);
		final double[] values = new double[] { recall, precision, fscore };
		return values;
	}

	static HashMap<String, String> getChunksMulti(final ArrayList<String> taglist) {
		int begin = -1;
		int end = -1;
		boolean inside = false;

		final HashMap<String, String> blocks = new HashMap<String, String>();

		for (int i = 0; i < taglist.size(); i++) {
			String curr_tag = (String) taglist.get(i);

			String curr_marker = "";
			if (curr_tag.length() != 0) {
				curr_marker = curr_tag.substring(0, 1);

				if (curr_marker.equals("O"))
					curr_tag = "";
				else
					curr_tag = curr_tag.substring(2, curr_tag.length());
				// TODO: error occurred in line above, index -1 -> how?
			}

			if (!inside) {
				// not inside
				if (curr_marker.equals("B")) {
					// was outside
					inside = true;
					begin = i;
					end = -1;
				}

			} else if (curr_marker.equals("B")) {
				// new chunk
				end = i - 1;
				String info = "";
				for (int j = begin; j < (end + 1); j++) {
					// System.out.println (j + ": " +
					// (String)taglist.get(j));
					if (info.length() > 0)
						info += "#";
					info += (String) taglist.get(j);
				}
				blocks.put(begin + "," + end, info);
				begin = i;
				end = -1;
			} else if (curr_marker.equals("O") || curr_marker.equals("")) {
				// is now outside
				end = i - 1;
				String info = "";
				for (int j = begin; j < (end + 1); j++) {
					// System.out.println (j + ": " +
					// (String)taglist.get(j));
					if (info.length() > 0)
						info += "#";
					info += (String) taglist.get(j);
				}
				blocks.put(begin + "," + end, info);
				begin = -1;
				end = -1;
				inside = false;
			}
		}
		return blocks;
	}
}
