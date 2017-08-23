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

public class IOEvaluation {

	public String getType() {
		return "IO tags";
	}

	public static double[] evaluate(final ArrayList<?> gold_tok,
			final ArrayList<?> eval_tok) {
		// get taglist for goldstandard and model
		if (gold_tok.size() != eval_tok.size())
			System.err.println("gold size different from predicted");

		final ArrayList<String> gold = new ArrayList<String>();
		final ArrayList<String> eval = new ArrayList<String>();
		for (int i = 0; i < gold_tok.size(); i++) {
			final String[] gTok = ((String) gold_tok.get(i)).replaceAll(
					"[\\s]+", "\t").split("\t");
			final String[] eTok = ((String) eval_tok.get(i)).replaceAll(
					"[\\s]+", "\t").split("\t");

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
		return getValuesIO(gold, eval);
	}

	private static double[] getValuesIO(final ArrayList<String> gold,
			final ArrayList<String> eval) {

		if (gold.size() != eval.size()) {
			System.err.println("error!, gold.size!=eval.size -> I quit!");
			System.exit(0);
		}

		final HashMap<String, String> gold_chunks = getChunksIO(gold);

		final HashMap<String, String> eval_chunks = getChunksIO(eval);

		int numcorr = 0;
		final int numans = eval_chunks.size();
		final int numref = gold_chunks.size();

		// now check the blocks
		for (final Iterator<String> iter = eval_chunks.keySet().iterator(); iter
				.hasNext();) {
			final String offset = (String) iter.next();
			if (gold_chunks.containsKey(offset)) {
				final String tags_eval = (String) eval_chunks.get(offset);
				final String tags_gold = (String) gold_chunks.get(offset);
				if (tags_eval.equals(tags_gold))
					numcorr++;
			}
		}

		double precision = 0;
		double recall = 0;
		double fscore = 0;

		if (numans > 0)
			precision = numcorr / (double) numans;
		if (numref > 0)
			recall = numcorr / (double) numref;
		if ((precision + recall) > 0)
			fscore = (2 * precision * recall) / (precision + recall);

		final double[] values = new double[] { recall, precision, fscore };
		return values;
	}

	public static HashMap<String, String> getChunksIO(final ArrayList<String> taglist) {
		int begin = -1;
		int end = -1;

		final HashMap<String, String> blocks = new HashMap<String, String>();
		String old_tag = "O";
		String curr_tag = "";
		for (int i = 0; i < taglist.size(); i++) {
			curr_tag = (String) taglist.get(i);

			if (curr_tag.equals(old_tag)) {
				// we are inside the same entity ... do nothing
			} else {
				// tags change

				// if we came from an entity: save old one
				if (begin > -1) {
					end = i - 1;
					String info = "";
					for (int j = begin; j < (end + 1); j++) {

						if (info.length() > 0)
							info += "#";
						info += (String) taglist.get(j);

					}

					blocks.put(begin + "," + end, info);
				}

				// reset offsets
				if (!curr_tag.equals("O"))
					begin = i; // if a new entity starts
				else
					begin = -1; // if we are outside

				end = -1;
			}
			old_tag = curr_tag;
		}

		return blocks;
	}

}
