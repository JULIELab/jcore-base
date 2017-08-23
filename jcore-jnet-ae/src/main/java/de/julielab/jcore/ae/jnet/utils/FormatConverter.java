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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

public class FormatConverter {

	public static void main(final String[] args) {
		try {

			if (args.length < 3) {
				System.out
						.println("usage: java FormatConverter <iobFile> <posFile> [further meta data files] <outFile> <taglist (or 0 if not used)>");
				System.exit(0);
			}

			final File iobFile = new File(args[0]);
			final ArrayList<File> metaDataFiles = new ArrayList<File>();
			for (int i = 1; i < (args.length - 2); i++)
				metaDataFiles.add(new File(args[i]));
			final File outFile = new File(args[args.length - 2]);

			System.out.println("Reading iob and meta data files...");
			final ArrayList<String> iobData = Utils.readFile(iobFile);
			final ArrayList<ArrayList<String>> metaData = new ArrayList<ArrayList<String>>();
			for (int i = 0; i < metaDataFiles.size(); i++)
				metaData.add(Utils.readFile(metaDataFiles.get(i)));

			TreeSet<String> tagList = null;
			if (!args[args.length - 1].equals("0"))
				tagList = new TreeSet<String>(Utils.readFile(new File(
						args[args.length - 1])));

			// make piped format
			System.out.println("Making piped format...");
			final ArrayList<String> pipedData = makePipedFormat(iobData,
					metaData, tagList);
			Utils.writeFile(outFile, pipedData);

		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * converts a IOB and a POS file into one file in piped format
	 */
	public static ArrayList<String> makePipedFormat(
			final ArrayList<String> iobData,
			final ArrayList<ArrayList<String>> metaData,
			final TreeSet<String> tags) {

		boolean checkTags = true;
		if (tags == null)
			// ignore tags check
			checkTags = false;

		for (int i = 0; i < metaData.size(); i++)
			if (iobData.size() != metaData.get(i).size()) {
				System.err.println("Error: IOB file and " + (i + 1)
						+ ". meta data file have different length!");
				System.exit(-1);
			}

		final ArrayList<String> pipedData = new ArrayList<String>();
		final StringBuffer sentence = new StringBuffer();

		for (int i = 0; i < iobData.size(); i++) {
			String line_iob = iobData.get(i);
			final String[] meta_lines = new String[metaData.size()];
			for (int j = 0; j < meta_lines.length; j++)
				meta_lines[j] = metaData.get(j).get(i);

			// conversion: several white spaces to a tab
			line_iob = line_iob.replaceAll("[\\s]+", "\t");
			for (int j = 0; j < meta_lines.length; j++)
				meta_lines[j] = meta_lines[j].replaceAll("[\\s]+", "\t");

			if (line_iob.equals("-DOCSTART-\tO")) {
				// ignore this line
			} else if (line_iob.equals("") || line_iob.equals("\t")) {
				if (sentence.length() > 0) // sentence finished

					pipedData.add(sentence.toString());
				sentence.delete(0, sentence.length());
			} else {
				final String[] toks_iob = line_iob.split("[\t]");
				final String[][] toks_meta = new String[meta_lines.length][];
				for (int j = 0; j < meta_lines.length; j++) {
					toks_meta[j] = meta_lines[j].split("[\t]");

					if ((toks_iob.length != 2) || (toks_meta[j].length != 2)) {
						System.err
								.println("Error: format error. Incorrect size of line.");
						System.err.println(line_iob + " - " + toks_iob.length);
						System.err.println(meta_lines[j] + " - "
								+ toks_meta[j].length);
					}

					if (!toks_meta[j][0].equals(toks_iob[0])) {
						System.err.println("error reading, word pos!=word iob");
						System.out.println("IOB: " + toks_iob[0]);
						System.out.println("POS: " + toks_meta[j][0]);
						System.out.println(toks_meta[j][0] + " - "
								+ toks_iob[0]);
						System.out.println(line_iob + " -- " + meta_lines[j]);
						System.out.println("line number: " + i);
					}
				}

				// check tags: if tag is not contained in taglist, replace it
				// with the "O" tag
				if (checkTags)
					if (!tags.contains(toks_iob[1]))
						toks_iob[1] = "O";

				String token = toks_iob[0];
				for (int j = 0; j < meta_lines.length; j++)
					token += "|" + toks_meta[j][1];
				token += "|" + toks_iob[1] + " ";
				sentence.append(token);
			}
		}
		return pipedData;
	}

	/**
	 * split data in piped format into pool data (corpus) and gold data
	 * 
	 * @param fractionGold
	 *            the fraction of the gold input
	 * @param poolOut
	 *            output of pooldata
	 * @param goldOut
	 *            output of golddata
	 */
	public static void makeDataSplit(final double fractionGold,
			final ArrayList<String> pipedData, final ArrayList<String> poolOut,
			final ArrayList<String> goldOut) {
		final ArrayList<String> dummy = new ArrayList<String>();
		makeDataSplit(fractionGold, 0, pipedData, dummy, poolOut, goldOut);
	}

	/**
	 * split data in piped format into pool data (corpus) and gold data and a
	 * inital trainingset
	 * 
	 * @param fractionGold
	 *            the fraction of the gold
	 * @param initSize
	 *            size of initial trainingset
	 * @param initOut
	 *            output of initial trainingset
	 * @param poolOut
	 *            output of pooldata
	 * @param goldOut
	 *            output of golddata
	 */
	public static void makeDataSplit(final double fractionGold,
			final int initSize, final ArrayList<String> pipedData,
			final ArrayList<String> initOut, final ArrayList<String> poolOut,
			final ArrayList<String> goldOut) {

		// remove all elements from out-lists
		initOut.clear();
		poolOut.clear();
		goldOut.clear();

		final int goldSize = (int) ((pipedData.size() - initSize) * fractionGold);
		final int poolSize = pipedData.size() - goldSize;
		System.out.println("datasize: " + pipedData.size());
		System.out.println("initSize: " + initSize);
		System.out.println("goldSize: " + goldSize);
		System.out.println("poolSize: " + poolSize);

		if ((fractionGold < 0.01) || ((goldSize < 1) || (fractionGold > 0.98))) {
			System.err
					.println("Error: fractionGold too small/large! Must be between 0.01 and 0.98 and result in at least one sentence.");
			System.exit(-1);
		}

		Collections.shuffle(pipedData);

		for (int i = 0; i < pipedData.size(); i++)
			if (initOut.size() < initSize)
				initOut.add(pipedData.get(i));
			else if (goldOut.size() < goldSize)
				goldOut.add(pipedData.get(i));
			else
				poolOut.add(pipedData.get(i));

	}

}
