package de.julielab.jules.ae.genemapper.eval.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.Range;

import com.google.common.collect.Multimap;

import de.julielab.jules.ae.genemapper.genemodel.Acronym;

public class AcronymTextCoverWriter {

	public static void main(String[] args) {
		String dataType = "train";

		try {
			appendTextRepresentation("data/eval_data/ign_data/ign-" + dataType + "-acronyms.ann",
					"data/eval_data/ign_data/txt-train",
					dataType + "-ign-acronymcover.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void appendTextRepresentation(String acronymsPath, String documentsPath, String output) throws IOException {
		File acronymCover = new File(output);
		if (acronymCover.exists()) {
			if (!acronymCover.delete()) {
				System.err.println("Output file " + output + "could not be cleared prior to writing.");
			}
		}
		
		Multimap<String, Acronym> acronyms = EvalToolUtilities.readAcronymAnnotations(acronymsPath);
		Map<String, String> bc2TrainContexts = EvalToolUtilities.readGeneContexts(documentsPath);
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(acronymCover));
			
			for (String docId : bc2TrainContexts.keySet()) {
				Collection<Acronym> acronymsInDoc = acronyms.get(docId);
				String documentContext = bc2TrainContexts.get(docId);
				
				for (Acronym acronym : acronymsInDoc) {
					Range<Integer> offsets = acronym.getOffsets();
					int start = offsets.getMinimum();
					int end = offsets.getMaximum();
					if (start > documentContext.length() || end > documentContext.length()) {
						bw.write(docId + "\t" + start + "\t" + end + "\t\"%OUTOFRANGE%\"\n");
					} else {
						String coveredText = documentContext.substring(start, end);
						bw.write(docId + "\t" + start + "\t" + end + "\t" + coveredText + "\n");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != bw) {
				try{bw.close();} catch (IOException e){}
			}
		}
	}

}
