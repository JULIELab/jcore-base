package de.julielab.jules.ae.genemapper.utils.dict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class EntrezGeneUniProtLexcionCreator {

	// IMPORTANT: only take first entrezGene ID in mapping file (the most representative)!!!
	private static final File ENTREZUNIPROT_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/up2eg.test");
	//private static final File UNIPROT_FILE = new File("/data/data_resources/biology/Julie_BT/bt_iproclass.HGNC.2_11.norm.unique");
	private static final File UNIPROT_FILE = new File("/data/data_resources/biology/Julie_BT/uniprot_subset/bt_iproclass_cleaned.reviewed.allOrgs.uniprot.2_11.human.norm.unique");
	private static final File ENTREZ_DESIGNATIONS_FILE = new File("/data/data_resources/biology/entrez/gene/gene2other_designations.human");
	
	private HashMap<String,String> uniprot2Syns;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		EntrezGeneUniProtLexcionCreator creator = new EntrezGeneUniProtLexcionCreator();
		//creator.makeUniprotSyns(UNIPROT_FILE);
		//creator.makeEntrezFile(ENTREZUNIPROT_FILE);
		creator.makeEntrezDesignations(ENTREZ_DESIGNATIONS_FILE);
		
	}
	
	
	
	private void makeEntrezDesignations(File entrezDesignationsFile) throws IOException {
		
		BufferedReader biothesaurusReader = new BufferedReader(new FileReader(entrezDesignationsFile));
		String line = "";
		
		PrintWriter pw = new PrintWriter(new FileWriter("/home/jwermter/biocreative2_data/entrezGeneDesignationsLexicon"));
		
		try {

			while ((line = biothesaurusReader.readLine()) != null) {

				String[] values = line.split("\t");
				
				String entrezID = values[0];
				String designations = values[1].trim();
				String[] syns = designations.split("\\|");
				
				for (int i=0; i < syns.length; i++) {
					pw.println(syns[i] + "\t" +  entrezID);
				}
			}
			
			biothesaurusReader.close();
			pw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	private void makeEntrezFile(File entrez2uniprot) throws IOException {
		
		BufferedReader biothesaurusReader = new BufferedReader(new FileReader(entrez2uniprot));
		String line = "";
		
		
		
		PrintWriter pw = new PrintWriter(new FileWriter("/home/jwermter/biocreative2_data/entrezGeneUniprotSynsLexicon"));
		
		
		
		try {

			while ((line = biothesaurusReader.readLine()) != null) {

				String[] values = line.split("\t");
				
				String uniprotID = values[0];
				String entrezID = values[1].trim();
			
				if(uniprot2Syns.containsKey(uniprotID)) {
					String uniprotSyns = uniprot2Syns.get(uniprotID);
					String[] syns = uniprotSyns.split("\\|");
					
					for(int i=0; i < syns.length; i++) {
						pw.println(syns[i] + "\t" +  entrezID);
						//System.out.println(syns[i] + "\t" +  entrezID);
					}
					
				}
				
			}
			biothesaurusReader.close();
			pw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
	}
	
	
	private void makeUniprotSyns(File uniprot) throws FileNotFoundException {
		
		BufferedReader biothesaurusReader = new BufferedReader(new FileReader(uniprot));
		String line = "";
		uniprot2Syns = new HashMap<String, String>();
		
		try {

			while ((line = biothesaurusReader.readLine()) != null) {

				String[] values = line.split("\t");
				
				String id = values[1].trim();
				String uniprotSyn = values[0].trim();
				
				//id = id + "_HUMAN";
				
				if(uniprot2Syns.containsKey(id)) {
					uniprotSyn += "|" + uniprot2Syns.get(id);
					uniprot2Syns.put(id, uniprotSyn);
					//System.out.println(uniprotSyn);
					
				} else {
					uniprot2Syns.put(id, uniprotSyn);
					//System.out.println(line);
				}
			
			}
			biothesaurusReader.close();

			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
