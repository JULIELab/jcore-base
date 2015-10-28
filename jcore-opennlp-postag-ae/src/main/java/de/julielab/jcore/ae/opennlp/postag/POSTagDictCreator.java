package de.julielab.jcore.ae.opennlp.postag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSSample;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class POSTagDictCreator {

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err
					.println("Usage: " + POSTagDictCreator.class.getSimpleName()
							+ " <POS sample file in OpenNLP format to derive dictionary from> <dictionary output file> <case sensitive true/false>");
			System.exit(1);
		}
		File sampleFile = new File(args[0]);
		File dictOutputFile = new File(args[1]);
		boolean caseSensitive = Boolean.parseBoolean(args[2]);

		
		POSDictionary posDictionary = createPOSTagDict(sampleFile, caseSensitive);
		try (FileOutputStream os = FileUtils.openOutputStream(dictOutputFile)) {
			posDictionary.serialize(os);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("POS dictionary written to " + dictOutputFile + ".");
	}

	public static POSDictionary createPOSTagDict(File sampleFile, boolean caseSensitive) {
		Multimap<String, String> tokenTagMap = HashMultimap.create();
		List<POSSample> posSamples = readPOSSamples(sampleFile);
		POSDictionary posDictionary = new POSDictionary(caseSensitive);
		for (POSSample sample : posSamples) {
			for (int i = 0; i < sample.getSentence().length; ++i) {
				String token = sample.getSentence()[i];
				if (!caseSensitive)
					token = token.toLowerCase();
				String posTag = sample.getTags()[i];
				tokenTagMap.put(token, posTag);
			}
		}

		for (String token : tokenTagMap.keySet()) {
			Collection<String> tagsForToken = tokenTagMap.get(token);
			posDictionary.put(token, tagsForToken.toArray(new String[tagsForToken.size()]));
		}
		
		return posDictionary;
	}

	public static List<POSSample> readPOSSamples(File sampleFile) {
		List<POSSample> posSamples = new ArrayList<>();
		try (FileInputStream is = FileUtils.openInputStream(sampleFile)) {
			List<String> lines = IOUtils.readLines(is);
			for (String sentenceString : lines) {
				POSSample posSample = POSSample.parse(sentenceString);
				posSamples.add(posSample);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return posSamples;
	}
}
