package de.julielab.jules.ae.genemapper.eval.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.SynHit;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;

public class InteractiveCandidateFinder {

	public static void main(String[] args) throws IOException, GeneMapperException {
		String propertiesFile = "src/main/resources/ignJnetGeneMapper.properties";
		GeneMapper mapper = new GeneMapper(new File(propertiesFile));
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
		while (true) {
			System.out.println("Enter term:");
			String input = br.readLine();
			String normalizedTerm = mapper.getMappingCore().getTermNormalizer().normalize(input);
			System.out.println("Normalized term: " + normalizedTerm);
			GeneMention geneMention = new GeneMention(input);
			geneMention.setNormalizer(mapper.getMappingCore().getTermNormalizer());
			List<SynHit> candidates = mapper.getMappingCore().getCandidateRetrieval().getCandidates(geneMention);
			for (SynHit hit : candidates) {
				System.out.println(hit.getSynonym() + "\t" + hit.getId());
			}
		}
	}

}
